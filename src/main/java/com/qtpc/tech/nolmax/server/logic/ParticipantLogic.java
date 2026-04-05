package com.qtpc.tech.nolmax.server.logic;

import com.nolmax.database.database.ParticipantDAO;
import com.nolmax.database.model.Participant;
import com.qtpc.tech.nolmax.proto.*;
import com.qtpc.tech.nolmax.server.utils.HandlerUtils;
import com.qtpc.tech.nolmax.server.utils.ProtoMapper;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ParticipantLogic {
    private static final Logger log = LoggerFactory.getLogger(ParticipantLogic.class);
    public static final ParticipantDAO participantDAO = new ParticipantDAO();

    public void handleJoinConversationRequest(ChannelHandlerContext ctx, JoinConversationRequest request) {
        HandlerUtils.logDebug(log, "Handling JoinConversationRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long userId = request.getUserId();

        Participant participant = new Participant();
        participant.setConversationId(conversationId);
        participant.setUserId(userId);

        boolean success = participantDAO.join(participant);
        log.info("JoinConversationRequest processed for conversationId={}, userId={}: success={}", conversationId, userId, success);

        JoinConversationResponse response = JoinConversationResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setJoinConversationResponse(response).build());

        if (success) {
            com.qtpc.tech.nolmax.proto.Participant protoParticipant = com.qtpc.tech.nolmax.proto.Participant.newBuilder()
                    .setConversationId(conversationId)
                    .setUserId(userId)
                    .setRole(request.getRole())
                    .build();

            UpdateBroadcastParticipant broadcastParticipantObj = UpdateBroadcastParticipant.newBuilder()
                    .setAction(1)
                    .setParticipant(protoParticipant)
                    .build();
            ChatPacket participantBroadcastPacket = ChatPacket.newBuilder().setUpdateBroadcastParticipant(broadcastParticipantObj).build();

            com.qtpc.tech.nolmax.proto.Conversation joinedConversation = com.qtpc.tech.nolmax.proto.Conversation.newBuilder()
                    .setId(conversationId)
                    .build();

            UpdateBroadcastConversation broadcastConversationObj = UpdateBroadcastConversation.newBuilder()
                    .setAction(1)
                    .setConversation(joinedConversation)
                    .build();
            ChatPacket conversationBroadcastPacket = ChatPacket.newBuilder().setUpdateBroadcastConversation(broadcastConversationObj).build();

            List<com.nolmax.database.model.Participant> allParticipants = participantDAO.getParticipantsByConversation(conversationId);
            List<com.qtpc.tech.nolmax.proto.Participant> protoAllParticipants = allParticipants.stream()
                    .map(ProtoMapper::toProtoParticipant)
                    .toList();

            PullParticipantsResponse pullResponse = PullParticipantsResponse.newBuilder()
                    .setErrorCode(0)
                    .addAllParticipants(protoAllParticipants)
                    .build();
            ChatPacket fullListPacket = ChatPacket.newBuilder().setPullParticipantsResponse(pullResponse).build();

            for (com.nolmax.database.model.Participant p : allParticipants) {
                long targetUserId = p.getUserId();
                io.netty.channel.group.ChannelGroup targetChannels = com.qtpc.tech.nolmax.server.utils.ConnectionManager.getChannels(targetUserId);

                if (targetChannels != null && !targetChannels.isEmpty()) {
                    if (targetUserId == userId) {
                        targetChannels.writeAndFlush(conversationBroadcastPacket);
                        targetChannels.writeAndFlush(fullListPacket);
                    } else {
                        targetChannels.writeAndFlush(participantBroadcastPacket);
                    }
                }
            }
        }
    }

    public void handleLeaveConversationRequest(ChannelHandlerContext ctx, LeaveConversationRequest request) {
        HandlerUtils.logDebug(log, "Handling LeaveConversationRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long userId = request.getUserId();

        boolean success = participantDAO.left(conversationId, userId);
        log.info("LeaveConversationRequest processed for conversationId={}, userId={}: success={}", conversationId, userId, success);

        LeaveConversationResponse response = LeaveConversationResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setLeaveConversationResponse(response).build());

        if (success) {
            com.qtpc.tech.nolmax.proto.Participant protoParticipant = com.qtpc.tech.nolmax.proto.Participant.newBuilder()
                    .setConversationId(conversationId)
                    .setUserId(userId)
                    .build();

            UpdateBroadcastParticipant broadcastParticipantObj = UpdateBroadcastParticipant.newBuilder()
                    .setAction(0)
                    .setParticipant(protoParticipant)
                    .build();
            ChatPacket participantBroadcastPacket = ChatPacket.newBuilder().setUpdateBroadcastParticipant(broadcastParticipantObj).build();

            com.qtpc.tech.nolmax.proto.Conversation leftConversation = com.qtpc.tech.nolmax.proto.Conversation.newBuilder()
                    .setId(conversationId)
                    .build();

            UpdateBroadcastConversation broadcastConversationObj = UpdateBroadcastConversation.newBuilder()
                    .setAction(0)
                    .setConversation(leftConversation)
                    .build();
            ChatPacket conversationBroadcastPacket = ChatPacket.newBuilder().setUpdateBroadcastConversation(broadcastConversationObj).build();

            List<com.nolmax.database.model.Participant> remainingParticipants = participantDAO.getParticipantsByConversation(conversationId);
            for (com.nolmax.database.model.Participant p : remainingParticipants) {
                long targetUserId = p.getUserId();
                io.netty.channel.group.ChannelGroup targetChannels = com.qtpc.tech.nolmax.server.utils.ConnectionManager.getChannels(targetUserId);

                if (targetChannels != null && !targetChannels.isEmpty()) {
                    targetChannels.writeAndFlush(participantBroadcastPacket);
                }
            }

            io.netty.channel.group.ChannelGroup leavingUserChannels = com.qtpc.tech.nolmax.server.utils.ConnectionManager.getChannels(userId);
            if (leavingUserChannels != null && !leavingUserChannels.isEmpty()) {
                leavingUserChannels.writeAndFlush(conversationBroadcastPacket);
            }
        }
    }

    public void handleUpdateParticipantRoleRequest(ChannelHandlerContext ctx, UpdateParticipantRoleRequest request) {
        HandlerUtils.logDebug(log, "Handling UpdateParticipantRoleRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long userId = request.getUserId();
        long requestingUserId = HandlerUtils.getUserId(ctx);
        int role = request.getRole().getNumber();
        if (!participantDAO.isAdmin(conversationId, requestingUserId)) {
            log.warn("User {} tried to change role of user {} in conversation {}", requestingUserId, userId, conversationId);
            UpdateParticipantRoleResponse response = UpdateParticipantRoleResponse.newBuilder().setErrorCode(1).build();
            HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setUpdateParticipantRoleResponse(response).build());
            return;
        }

        boolean success = participantDAO.updateRole(conversationId, userId, role);
        log.info("UpdateParticipantRoleRequest processed for conversationId={}, userId={}, role={}: success={}", conversationId, userId, role, success);

        UpdateParticipantRoleResponse response = UpdateParticipantRoleResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setUpdateParticipantRoleResponse(response).build());

        if (success) {
            com.qtpc.tech.nolmax.proto.Participant protoParticipant = com.qtpc.tech.nolmax.proto.Participant.newBuilder()
                    .setConversationId(conversationId)
                    .setUserId(userId)
                    .setRole(request.getRole())
                    .build();

            UpdateBroadcastParticipant broadcastParticipantObj = UpdateBroadcastParticipant.newBuilder()
                    .setAction(2)
                    .setParticipant(protoParticipant)
                    .build();

            ChatPacket broadcastPacket = ChatPacket.newBuilder().setUpdateBroadcastParticipant(broadcastParticipantObj).build();

            List<com.nolmax.database.model.Participant> participants = participantDAO.getParticipantsByConversation(conversationId);

            for (com.nolmax.database.model.Participant p : participants) {
                long memberId = p.getUserId();
                io.netty.channel.group.ChannelGroup targetChannels = com.qtpc.tech.nolmax.server.utils.ConnectionManager.getChannels(memberId);

                if (targetChannels != null && !targetChannels.isEmpty()) {
                    if (memberId == requestingUserId) {
                        targetChannels.writeAndFlush(broadcastPacket, io.netty.channel.group.ChannelMatchers.isNot(ctx.channel()));
                    } else {
                        targetChannels.writeAndFlush(broadcastPacket);
                    }
                }
            }
        }
    }

    public void handlePullParticipantsRequest(ChannelHandlerContext ctx, PullParticipantsRequest request) {
        HandlerUtils.logDebug(log, "Handling PullParticipantsRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long lastUpdateId = request.getLastUpdateId();

        List<com.qtpc.tech.nolmax.proto.Participant> protoParticipants = participantDAO.pull(conversationId, lastUpdateId)
                .stream()
                .map(ProtoMapper::toProtoParticipant)
                .toList();

        log.info("PullParticipantsRequest processed for conversationId={}, lastUpdateId={}, returnedParticipants={}", conversationId, lastUpdateId, protoParticipants);

        PullParticipantsResponse response = PullParticipantsResponse.newBuilder().setErrorCode(0).addAllParticipants(protoParticipants).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setPullParticipantsResponse(response).build());
    }
}