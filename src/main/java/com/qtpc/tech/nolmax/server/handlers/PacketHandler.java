package com.qtpc.tech.nolmax.server.handlers;

import com.nolmax.database.database.ConversationDAO;
import com.nolmax.database.database.MessageDAO;
import com.nolmax.database.database.ParticipantDAO;
import com.nolmax.database.model.Conversation;
import com.qtpc.tech.nolmax.proto.*;
import com.qtpc.tech.nolmax.server.Main;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PacketHandler extends SimpleChannelInboundHandler<ChatPacket> {
    private static final Logger log = LoggerFactory.getLogger(PacketHandler.class);

    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private final ParticipantDAO participantDAO = new ParticipantDAO();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatPacket packet) {
        logDebug("Received packet from {}: {}", ctx.channel().remoteAddress(), packet.getDescriptorForType().getName());

        if (packet.hasCreateConversationRequest()) {
            handleCreateConversation(ctx, packet.getCreateConversationRequest());
        } else if (packet.hasUpdateConversationAvatarRequest()) {
            handleUpdateConversationAvatar(ctx, packet.getUpdateConversationAvatarRequest());
        } else if (packet.hasUpdateConversationNameRequest()) {
            handleUpdateConversationName(ctx, packet.getUpdateConversationNameRequest());
        } else if (packet.hasUpdateConversationLastMessageRequest()) {
            handleUpdateConversationLastMessage(ctx, packet.getUpdateConversationLastMessageRequest());
        } else if (packet.hasDeleteConversationRequest()) {
            handleDeleteConversation(ctx, packet.getDeleteConversationRequest());
        } else if (packet.hasPullConversationsRequest()) {
            handlePullConversations(ctx, packet.getPullConversationsRequest());
        } else {
            log.warn("Received unsupported packet type from {}: {}", ctx.channel().remoteAddress(), packet.getDescriptorForType().getName());
        }
    }

    private void handleCreateConversation(ChannelHandlerContext ctx, CreateConversationRequest request) {
        logDebug("Handling CreateConversationRequest from {}", ctx.channel().remoteAddress());

        com.qtpc.tech.nolmax.proto.Conversation proto = request.getConversation();

        Conversation conversation = new Conversation();
        conversation.setAvatarUrl(proto.getAvatarUrl());
        conversation.setName(proto.getName());
        conversation.setType(proto.getTypeValue());
        conversation.setCreatedBy(getUserId(ctx));

        boolean success = conversationDAO.createConversation(conversation);
        log.info("CreateConversationRequest processed: success={}", success);

        sendResponse(ctx, CreateConversationResponse.newBuilder()
                .setErrorCode(toErrorCode(success))
                .build());
    }

    private void handleUpdateConversationAvatar(ChannelHandlerContext ctx, UpdateConversationAvatarRequest request) {
        logDebug("Handling UpdateConversationAvatarRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        boolean success = conversationDAO.updateAvatar(conversationId, request.getAvatarUrl());
        log.info("UpdateConversationAvatarRequest processed for conversationId={}: success={}", conversationId, success);

        sendResponse(ctx, UpdateConversationAvatarResponse.newBuilder()
                .setErrorCode(toErrorCode(success))
                .build());
    }

    private void handleUpdateConversationName(ChannelHandlerContext ctx, UpdateConversationNameRequest request) {
        logDebug("Handling UpdateConversationNameRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        boolean success = conversationDAO.updateName(conversationId, request.getName());
        log.info("UpdateConversationNameRequest processed for conversationId={}: success={}", conversationId, success);

        sendResponse(ctx, UpdateConversationNameResponse.newBuilder()
                .setErrorCode(toErrorCode(success))
                .build());
    }

    private void handleUpdateConversationLastMessage(ChannelHandlerContext ctx, UpdateConversationLastMessageRequest request) {
        logDebug("Handling UpdateConversationLastMessageRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        long lastMessageId = request.getMessageId();
        boolean success = conversationDAO.updateLastMessageId(conversationId, lastMessageId);
        log.info("UpdateConversationLastMessageRequest processed for conversationId={}, lastMessageId={}: success={}",
                conversationId, lastMessageId, success);

        sendResponse(ctx, UpdateConversationLastMessageResponse.newBuilder()
                .setErrorCode(toErrorCode(success))
                .build());
    }

    private void handleDeleteConversation(ChannelHandlerContext ctx, DeleteConversationRequest request) {
        logDebug("Handling DeleteConversationRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();

        if (!conversationDAO.isCreator(conversationId, getUserId(ctx))) {
            log.info("DeleteConversationRequest denied for conversationId={}: not creator", conversationId);
            sendResponse(ctx, DeleteConversationResponse.newBuilder().setErrorCode(1).build());
            return;
        }

        boolean success = conversationDAO.deleteConversation(conversationId);
        log.info("DeleteConversationRequest processed for conversationId={}: success={}", conversationId, success);

        sendResponse(ctx, DeleteConversationResponse.newBuilder()
                .setErrorCode(toErrorCode(success))
                .build());
    }

    private void handlePullConversations(ChannelHandlerContext ctx, PullConversationsRequest request) {
        logDebug("Handling PullConversationsRequest from {}", ctx.channel().remoteAddress());

        long userId = request.getUserId();
        long lastUpdateId = request.getLastUpdateId();

        List<com.qtpc.tech.nolmax.proto.Conversation> protoConversations = conversationDAO.pull(lastUpdateId, userId).stream()
                .map(this::toProtoConversation)
                .toList();

        log.info("PullConversationsRequest processed for userId={}, lastUpdateId={}, returnedConversations={}",
                userId, lastUpdateId, protoConversations.size());

        sendResponse(ctx, PullConversationsResponse.newBuilder()
                .addAllConversations(protoConversations)
                .build());
    }

    // helper methods

    private com.qtpc.tech.nolmax.proto.Conversation toProtoConversation(Conversation c) {
        return com.qtpc.tech.nolmax.proto.Conversation.newBuilder()
                .setId(c.getId())
                .setType(ConversationType.forNumber(c.getType()))
                .setName(c.getName())
                .setAvatarUrl(c.getAvatarUrl())
                .setCreatedBy(c.getCreatedBy())
                .setUpdateId(c.getUpdateId())
                .setLastMessageId(c.getLastMessageId())
                .build();
    }

    private static long getUserId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(AuthHandler.USER_ID).get();
    }

    private static int toErrorCode(boolean success) {
        return success ? 0 : 1;
    }

    private static void sendResponse(ChannelHandlerContext ctx, Object response) {
        ctx.writeAndFlush(response);
    }

    private static boolean isDebugEnabled() {
        return Main.config != null && Main.config.server != null && Main.config.server.debug;
    }

    private static void logDebug(String message, Object... args) {
        if (isDebugEnabled()) {
            log.debug(message, args);
        }
    }
}