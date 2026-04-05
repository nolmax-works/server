package com.qtpc.tech.nolmax.server.logic;

import com.nolmax.database.database.ParticipantDAO;
import com.nolmax.database.model.Participant;
import com.qtpc.tech.nolmax.proto.ChatPacket;
import com.qtpc.tech.nolmax.proto.JoinConversationRequest;
import com.qtpc.tech.nolmax.proto.JoinConversationResponse;
import com.qtpc.tech.nolmax.proto.LeaveConversationRequest;
import com.qtpc.tech.nolmax.proto.LeaveConversationResponse;
import com.qtpc.tech.nolmax.proto.PullParticipantsRequest;
import com.qtpc.tech.nolmax.proto.PullParticipantsResponse;
import com.qtpc.tech.nolmax.proto.UpdateParticipantRoleRequest;
import com.qtpc.tech.nolmax.proto.UpdateParticipantRoleResponse;
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
    }

    public void handleLeaveConversationRequest(ChannelHandlerContext ctx, LeaveConversationRequest request) {
        HandlerUtils.logDebug(log, "Handling LeaveConversationRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long userId = request.getUserId();

        boolean success = participantDAO.left(conversationId, userId);
        log.info("LeaveConversationRequest processed for conversationId={}, userId={}: success={}", conversationId, userId, success);

        LeaveConversationResponse response = LeaveConversationResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setLeaveConversationResponse(response).build());
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