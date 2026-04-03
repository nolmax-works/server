package com.qtpc.tech.nolmax.server.logic;

import com.nolmax.database.database.ConversationDAO;
import com.nolmax.database.model.Conversation;
import com.qtpc.tech.nolmax.proto.CreateConversationRequest;
import com.qtpc.tech.nolmax.proto.CreateConversationResponse;
import com.qtpc.tech.nolmax.proto.DeleteConversationRequest;
import com.qtpc.tech.nolmax.proto.DeleteConversationResponse;
import com.qtpc.tech.nolmax.proto.PullConversationsRequest;
import com.qtpc.tech.nolmax.proto.PullConversationsResponse;
import com.qtpc.tech.nolmax.proto.UpdateConversationAvatarRequest;
import com.qtpc.tech.nolmax.proto.UpdateConversationAvatarResponse;
import com.qtpc.tech.nolmax.proto.UpdateConversationLastMessageRequest;
import com.qtpc.tech.nolmax.proto.UpdateConversationLastMessageResponse;
import com.qtpc.tech.nolmax.proto.UpdateConversationNameRequest;
import com.qtpc.tech.nolmax.proto.UpdateConversationNameResponse;
import com.qtpc.tech.nolmax.server.utils.HandlerUtils;
import com.qtpc.tech.nolmax.server.utils.ProtoMapper;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConversationLogic {
    private static final Logger log = LoggerFactory.getLogger(ConversationLogic.class);
    private final ConversationDAO conversationDAO = new ConversationDAO();

    public void handleCreateConversation(ChannelHandlerContext ctx, CreateConversationRequest request) {
        HandlerUtils.logDebug(log, "Handling CreateConversationRequest from {}", ctx.channel().remoteAddress());

        com.qtpc.tech.nolmax.proto.Conversation proto = request.getConversation();
        Conversation conversation = new Conversation();
        conversation.setAvatarUrl(proto.getAvatarUrl());
        conversation.setName(proto.getName());
        conversation.setType(proto.getTypeValue());
        conversation.setCreatedBy(HandlerUtils.getUserId(ctx));

        boolean success = conversationDAO.createConversation(conversation);
        log.info("CreateConversationRequest processed: success={}", success);

        HandlerUtils.sendResponse(ctx, CreateConversationResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build());
    }

    public void handleUpdateConversationAvatar(ChannelHandlerContext ctx, UpdateConversationAvatarRequest request) {
        HandlerUtils.logDebug(log, "Handling UpdateConversationAvatarRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        boolean success = conversationDAO.updateAvatar(conversationId, request.getAvatarUrl());
        log.info("UpdateConversationAvatarRequest processed for conversationId={}: success={}", conversationId, success);

        HandlerUtils.sendResponse(ctx, UpdateConversationAvatarResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build());
    }

    public void handleUpdateConversationName(ChannelHandlerContext ctx, UpdateConversationNameRequest request) {
        HandlerUtils.logDebug(log, "Handling UpdateConversationNameRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        boolean success = conversationDAO.updateName(conversationId, request.getName());
        log.info("UpdateConversationNameRequest processed for conversationId={}: success={}", conversationId, success);

        HandlerUtils.sendResponse(ctx, UpdateConversationNameResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build());
    }

    public void handleUpdateConversationLastMessage(ChannelHandlerContext ctx, UpdateConversationLastMessageRequest request) {
        HandlerUtils.logDebug(log, "Handling UpdateConversationLastMessageRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        long lastMessageId = request.getMessageId();
        boolean success = conversationDAO.updateLastMessageId(conversationId, lastMessageId);
        log.info("UpdateConversationLastMessageRequest processed for conversationId={}, lastMessageId={}: success={}", conversationId, lastMessageId, success);

        HandlerUtils.sendResponse(ctx, UpdateConversationLastMessageResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build());
    }

    public void handleDeleteConversation(ChannelHandlerContext ctx, DeleteConversationRequest request) {
        HandlerUtils.logDebug(log, "Handling DeleteConversationRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();

        if (!conversationDAO.isCreator(conversationId, HandlerUtils.getUserId(ctx))) {
            log.info("DeleteConversationRequest denied for conversationId={}: not creator", conversationId);
            HandlerUtils.sendResponse(ctx, DeleteConversationResponse.newBuilder().setErrorCode(1).build());
            return;
        }

        boolean success = conversationDAO.deleteConversation(conversationId);
        log.info("DeleteConversationRequest processed for conversationId={}: success={}", conversationId, success);

        HandlerUtils.sendResponse(ctx, DeleteConversationResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build());
    }

    public void handlePullConversations(ChannelHandlerContext ctx, PullConversationsRequest request) {
        HandlerUtils.logDebug(log, "Handling PullConversationsRequest from {}", ctx.channel().remoteAddress());

        long userId = request.getUserId();
        long lastUpdateId = request.getLastUpdateId();

        List<com.qtpc.tech.nolmax.proto.Conversation> protoConversations = conversationDAO.pull(lastUpdateId, userId)
                .stream()
                .map(ProtoMapper::toProtoConversation)
                .toList();

        log.info("PullConversationsRequest processed for userId={}, lastUpdateId={}, returnedConversations={}", userId, lastUpdateId, protoConversations.size());

        HandlerUtils.sendResponse(ctx, PullConversationsResponse.newBuilder().setErrorCode(0).addAllConversations(protoConversations).build());
    }
}