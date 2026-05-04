package com.westwardmc.dmcl.adapter.sqlite;

import com.westwardmc.dmcl.core.domain.LinkedAccount;
import com.westwardmc.dmcl.core.domain.PendingLink;
import com.westwardmc.dmcl.core.port.LinkRepo;

import java.sql.*;
import java.time.Instant;
import java.util.*;

public final class SqliteLinkRepo implements LinkRepo, AutoCloseable {
    private final String url;
    private Connection conn;

    public SqliteLinkRepo(String jdbcUrl) throws SQLException {
        this.url = jdbcUrl;
        this.conn = DriverManager.getConnection(url);
        try (var st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA busy_timeout=5000");
        }
    }

    public void migrate() throws Exception {
        try (var in = SqliteLinkRepo.class.getResourceAsStream("/migrations/V1__init.sql")) {
            if (in == null) throw new IllegalStateException("V1__init.sql missing");
            var sql = new String(in.readAllBytes());
            try (var st = conn.createStatement()) {
                for (String stmt : sql.split(";")) {
                    if (!stmt.isBlank()) st.execute(stmt);
                }
            }
        }
    }

    @Override public void savePending(PendingLink p) {
        try (var ps = conn.prepareStatement(
            "INSERT OR REPLACE INTO pending_link(code, mc_uuid, expires_at) VALUES(?,?,?)")) {
            ps.setString(1, p.code());
            ps.setString(2, p.mcUuid().toString());
            ps.setLong(3, p.expiresAt().toEpochMilli());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public synchronized Optional<UUID> consumePending(String code) {
        try (var st = conn.createStatement()) { st.execute("BEGIN IMMEDIATE TRANSACTION"); }
        catch (SQLException e) { throw new RuntimeException(e); }
        try {
            UUID found = null;
            try (var ps = conn.prepareStatement("SELECT mc_uuid FROM pending_link WHERE code=?")) {
                ps.setString(1, code);
                try (var rs = ps.executeQuery()) {
                    if (rs.next()) found = UUID.fromString(rs.getString(1));
                }
            }
            if (found != null) {
                try (var del = conn.prepareStatement("DELETE FROM pending_link WHERE code=?")) {
                    del.setString(1, code);
                    del.executeUpdate();
                }
            }
            try (var st = conn.createStatement()) { st.execute("COMMIT"); }
            return Optional.ofNullable(found);
        } catch (SQLException e) {
            try (var st = conn.createStatement()) { st.execute("ROLLBACK"); } catch (SQLException ignore) {}
            throw new RuntimeException(e);
        }
    }

    @Override public void link(UUID mcUuid, long discordId) {
        try (var ps = conn.prepareStatement(
            "INSERT OR REPLACE INTO linked_account(mc_uuid, discord_id, linked_at) VALUES(?,?,?)")) {
            ps.setString(1, mcUuid.toString());
            ps.setLong(2, discordId);
            ps.setLong(3, System.currentTimeMillis());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override public void unlinkByMc(UUID mcUuid) { exec("DELETE FROM linked_account WHERE mc_uuid=?", mcUuid.toString()); }
    @Override public void unlinkByDiscord(long discordId) { exec("DELETE FROM linked_account WHERE discord_id=?", discordId); }

    @Override public Optional<LinkedAccount> byMcUuid(UUID mcUuid) {
        return queryOne("SELECT mc_uuid, discord_id, linked_at FROM linked_account WHERE mc_uuid=?", mcUuid.toString());
    }
    @Override public Optional<LinkedAccount> byDiscordId(long discordId) {
        return queryOne("SELECT mc_uuid, discord_id, linked_at FROM linked_account WHERE discord_id=?", discordId);
    }

    @Override public List<LinkedAccount> all() {
        var out = new ArrayList<LinkedAccount>();
        try (var st = conn.createStatement();
             var rs = st.executeQuery("SELECT mc_uuid, discord_id, linked_at FROM linked_account")) {
            while (rs.next()) out.add(new LinkedAccount(
                UUID.fromString(rs.getString(1)), rs.getLong(2), Instant.ofEpochMilli(rs.getLong(3))));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return out;
    }

    @Override public int deleteExpiredPending(Instant now) {
        try (var ps = conn.prepareStatement("DELETE FROM pending_link WHERE expires_at <= ?")) {
            ps.setLong(1, now.toEpochMilli());
            return ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void exec(String sql, Object param) {
        try (var ps = conn.prepareStatement(sql)) {
            if (param instanceof String s) ps.setString(1, s);
            else if (param instanceof Long l) ps.setLong(1, l);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Optional<LinkedAccount> queryOne(String sql, Object param) {
        try (var ps = conn.prepareStatement(sql)) {
            if (param instanceof String s) ps.setString(1, s);
            else if (param instanceof Long l) ps.setLong(1, l);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(new LinkedAccount(
                    UUID.fromString(rs.getString(1)), rs.getLong(2), Instant.ofEpochMilli(rs.getLong(3))));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    @Override public void close() throws SQLException { if (conn != null) conn.close(); }
}
