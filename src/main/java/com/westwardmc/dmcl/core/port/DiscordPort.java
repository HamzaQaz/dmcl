package com.westwardmc.dmcl.core.port;

import com.westwardmc.dmcl.core.domain.Attachment;
import com.westwardmc.dmcl.core.domain.Author;
import com.westwardmc.dmcl.core.domain.BridgeError;
import com.westwardmc.dmcl.core.domain.BridgeMessage;
import com.westwardmc.dmcl.core.domain.ReplyContext;
import com.westwardmc.dmcl.core.domain.Result;
import com.westwardmc.dmcl.core.domain.Scope;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface DiscordPort {
    Result<PostedRef, BridgeError> sendWebhook(
        Scope scope, Author asAuthor, String body,
        List<Attachment> attachments, Optional<ReplyContext> replyTo);

    Result<Void, BridgeError> editWebhook(PostedRef ref, String newBody);
    Result<Void, BridgeError> deleteWebhook(PostedRef ref);
    Result<Void, BridgeError> postSystem(Scope scope, SystemEvent ev);

    void onInbound(Consumer<BridgeMessage> handler);
    void onReaction(Consumer<ReactionEvent> handler);

    void start();
    void shutdown();
}
