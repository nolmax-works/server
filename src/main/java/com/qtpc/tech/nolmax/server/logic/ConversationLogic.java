package com.qtpc.tech.nolmax.server.logic;

import com.nolmax.database.database.ConversationDAO;
import com.nolmax.database.model.Conversation;
import com.qtpc.tech.nolmax.proto.*;
import com.qtpc.tech.nolmax.server.utils.ConnectionManager;
import com.qtpc.tech.nolmax.server.utils.HandlerUtils;
import com.qtpc.tech.nolmax.server.utils.ProtoMapper;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConversationLogic {
    private static final Logger log = LoggerFactory.getLogger(ConversationLogic.class);
    public static final ConversationDAO conversationDAO = new ConversationDAO();

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

        CreateConversationResponse response = CreateConversationResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setCreateConversationResponse(response).build());
    }

    public void handleUpdateConversationAvatar(ChannelHandlerContext ctx, UpdateConversationAvatarRequest request) {
        HandlerUtils.logDebug(log, "Handling UpdateConversationAvatarRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        long userId = HandlerUtils.getUserId(ctx);
        if (!conversationDAO.isCreator(conversationId, userId)) {
            log.warn("User {} tried to update avatar for conversation {}", userId, conversationId);
            UpdateConversationAvatarResponse response = UpdateConversationAvatarResponse.newBuilder().setErrorCode(1).build();
            HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setUpdateConversationAvatarResponse(response).build());
            return;
        }

        boolean success = conversationDAO.updateAvatar(conversationId, request.getAvatarUrl());
        log.info("UpdateConversationAvatarRequest processed for conversationId={}: success={}", conversationId, success);

        UpdateConversationAvatarResponse response = UpdateConversationAvatarResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setUpdateConversationAvatarResponse(response).build());

        if (success) {
            com.qtpc.tech.nolmax.proto.UpdateBroadcastConversation broadcastObj = UpdateBroadcastConversation.newBuilder().setAction(3).setConversation(com.qtpc.tech.nolmax.proto.Conversation.newBuilder().setId(conversationId).setAvatarUrl(request.getAvatarUrl()).build()).build();
            ChatPacket broadcastPacket = ChatPacket.newBuilder().setUpdateBroadcastConversation(broadcastObj).build();

            List<com.nolmax.database.model.Participant> participants = ParticipantLogic.participantDAO.getParticipantsByConversation(conversationId);
            for (com.nolmax.database.model.Participant participant : participants) {
                long targetUserId = participant.getUserId();
                io.netty.channel.group.ChannelGroup targetChannels = ConnectionManager.getChannels(targetUserId);
                if (targetChannels != null && !targetChannels.isEmpty()) {
                    if (targetUserId == userId) {
                        targetChannels.writeAndFlush(broadcastPacket, io.netty.channel.group.ChannelMatchers.isNot(ctx.channel()));
                    } else {
                        targetChannels.writeAndFlush(broadcastPacket);
                    }
                }
            }
        }
    }

    public void handleUpdateConversationName(ChannelHandlerContext ctx, UpdateConversationNameRequest request) {
        HandlerUtils.logDebug(log, "Handling UpdateConversationNameRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        long userId = HandlerUtils.getUserId(ctx);
        if (!conversationDAO.isCreator(conversationId, userId)) {
            log.warn("User {} tried to update name for conversation {}", userId, conversationId);
            UpdateConversationNameResponse response = UpdateConversationNameResponse.newBuilder().setErrorCode(1).build();
            HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setUpdateConversationNameResponse(response).build());
            return;
        }

        boolean success = conversationDAO.updateName(conversationId, request.getName());
        log.info("UpdateConversationNameRequest processed for conversationId={}: success={}", conversationId, success);

        UpdateConversationNameResponse response = UpdateConversationNameResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setUpdateConversationNameResponse(response).build());

        if (success) {
            com.qtpc.tech.nolmax.proto.UpdateBroadcastConversation broadcastObj = UpdateBroadcastConversation.newBuilder().setAction(2).setConversation(com.qtpc.tech.nolmax.proto.Conversation.newBuilder().setId(conversationId).setName(request.getName()).build()).build();
            ChatPacket broadcastPacket = ChatPacket.newBuilder().setUpdateBroadcastConversation(broadcastObj).build();

            List<com.nolmax.database.model.Participant> participants = ParticipantLogic.participantDAO.getParticipantsByConversation(conversationId);
            for (com.nolmax.database.model.Participant participant : participants) {
                long targetUserId = participant.getUserId();
                io.netty.channel.group.ChannelGroup targetChannels = ConnectionManager.getChannels(targetUserId);
                if (targetChannels != null && !targetChannels.isEmpty()) {
                    if (targetUserId == userId) {
                        targetChannels.writeAndFlush(broadcastPacket, io.netty.channel.group.ChannelMatchers.isNot(ctx.channel()));
                    } else {
                        targetChannels.writeAndFlush(broadcastPacket);
                    }
                }
            }
        }
    }

    public void handleDeleteConversation(ChannelHandlerContext ctx, DeleteConversationRequest request) {
        HandlerUtils.logDebug(log, "Handling DeleteConversationRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();

        if (!conversationDAO.isCreator(conversationId, HandlerUtils.getUserId(ctx))) {
            log.info("DeleteConversationRequest denied for conversationId={}: not creator", conversationId);
            DeleteConversationResponse failedResponse = DeleteConversationResponse.newBuilder().setErrorCode(1).build();
            HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setDeleteConversationResponse(failedResponse).build());
            return;
        }

        boolean success = conversationDAO.deleteConversation(conversationId);
        log.info("DeleteConversationRequest processed for conversationId={}: success={}", conversationId, success);

        DeleteConversationResponse response = DeleteConversationResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setDeleteConversationResponse(response).build());
    }

    public void handlePullConversations(ChannelHandlerContext ctx, PullConversationsRequest request) {
        HandlerUtils.logDebug(log, "Handling PullConversationsRequest from {}", ctx.channel().remoteAddress());

        long userId = request.getUserId();
        long lastUpdateId = request.getLastUpdateId();

        List<com.qtpc.tech.nolmax.proto.Conversation> protoConversations = conversationDAO.pull(lastUpdateId, userId).stream().map(ProtoMapper::toProtoConversation).toList();

        log.info("PullConversationsRequest processed for userId={}, lastUpdateId={}, returnedConversations={}", userId, lastUpdateId, protoConversations.size());

        PullConversationsResponse response = PullConversationsResponse.newBuilder().setErrorCode(0).addAllConversations(protoConversations).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setPullConversationsResponse(response).build());
    }

    public void handlePullUpdateCombo(ChannelHandlerContext ctx, PullUpdateComboRequest request) {
        HandlerUtils.logDebug(log, "Handling PullUpdateComboRequest from {}", ctx.channel().remoteAddress());

        long lastUpdateId = request.getLastUpdateId();
        long userId = HandlerUtils.getUserId(ctx);

        List<Long> conversationIds = conversationDAO.takeUserConversations(userId);

        if (conversationIds.isEmpty()) {
            PullUpdateComboResponse response = PullUpdateComboResponse.newBuilder().build();
            HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setPullUpdateComboResponse(response).build());
            return;
        }

        List<User> users = UserLogic.userDAO.pullBatch(conversationIds, lastUpdateId).stream()
                .map(ProtoMapper::toProtoUser).distinct().toList();

        List<Participant> participants = ParticipantLogic.participantDAO.pullBatch(conversationIds, lastUpdateId).stream()
                .map(ProtoMapper::toProtoParticipant).toList();

        List<Message> messages = MessageLogic.messageDAO.pullBatch(conversationIds, lastUpdateId).stream()
                .map(ProtoMapper::toProtoMessage).toList();

        List<com.qtpc.tech.nolmax.proto.Conversation> conversations = conversationDAO.pullBatch(conversationIds, lastUpdateId).stream()
                .map(ProtoMapper::toProtoConversation).toList();

        log.info("PullUpdateComboRequest processed for userId={}, lastUpdateId={}, returnedUsers={}, returnedParticipants={}, returnedMessages={}, returnedConversations={}",
                userId, lastUpdateId, users.size(), participants.size(), messages.size(), conversations.size());

        PullUpdateComboResponse response = PullUpdateComboResponse.newBuilder()
                .addAllUsers(users)
                .addAllConversations(conversations)
                .addAllParticipants(participants)
                .addAllMessages(messages)
                .build();

        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setPullUpdateComboResponse(response).build());
    }
}