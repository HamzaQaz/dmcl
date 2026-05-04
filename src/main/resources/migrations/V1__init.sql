CREATE TABLE IF NOT EXISTS linked_account (
  mc_uuid       TEXT PRIMARY KEY,
  discord_id    INTEGER NOT NULL UNIQUE,
  linked_at     INTEGER NOT NULL
);

CREATE TABLE IF NOT EXISTS pending_link (
  code          TEXT PRIMARY KEY,
  mc_uuid       TEXT NOT NULL UNIQUE,
  expires_at    INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_pending_expires ON pending_link(expires_at);
