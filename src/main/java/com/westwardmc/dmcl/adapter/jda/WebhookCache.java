package com.westwardmc.dmcl.adapter.jda;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class WebhookCache {
    public record Ref(long id, String token, long channelId) {}

    private final JDA jda;
    private final Map<Long, Ref> byChannel = new ConcurrentHashMap<>();
    private final Set<Long> knownIds = ConcurrentHashMap.newKeySet();

    public WebhookCache(JDA jda) { this.jda = jda; }

    public Ref getOrCreate(long channelId) {
        var cached = byChannel.get(channelId);
        if (cached != null) return cached;
        var ch = jda.getTextChannelById(channelId);
        if (ch == null) throw new IllegalStateException("Channel not found: " + channelId);
        return getOrCreateBlocking(ch);
    }

    private Ref getOrCreateBlocking(TextChannel ch) {
        var fut = new java.util.concurrent.CompletableFuture<Ref>();
        ch.retrieveWebhooks().queue(list -> {
            for (var w : list) {
                if ("DMCL Bridge".equals(w.getName())) {
                    var ref = new Ref(w.getIdLong(), w.getToken(), ch.getIdLong());
                    byChannel.put(ch.getIdLong(), ref);
                    knownIds.add(ref.id());
                    fut.complete(ref);
                    return;
                }
            }
            ch.createWebhook("DMCL Bridge").queue(w -> {
                var ref = new Ref(w.getIdLong(), w.getToken(), ch.getIdLong());
                byChannel.put(ch.getIdLong(), ref);
                knownIds.add(ref.id());
                fut.complete(ref);
            });
        });
        try { return fut.get(10, java.util.concurrent.TimeUnit.SECONDS); }
        catch (Exception e) { throw new RuntimeException(e); }
    }

    public Set<Long> knownWebhookIds() { return knownIds; }

    public void evict(long channelId) {
        var r = byChannel.remove(channelId);
        if (r != null) knownIds.remove(r.id());
    }
}
