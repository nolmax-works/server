package com.qtpc.tech.nolmax.server.logic;

import com.nolmax.database.database.UserDAO;
import com.qtpc.tech.nolmax.proto.ChatPacket;
import com.qtpc.tech.nolmax.proto.PullUsersRequest;
import com.qtpc.tech.nolmax.proto.PullUsersResponse;
import com.qtpc.tech.nolmax.proto.UpdateUserAvatarRequest;
import com.qtpc.tech.nolmax.proto.UpdateUserAvatarResponse;
import com.qtpc.tech.nolmax.proto.User;
import com.qtpc.tech.nolmax.proto.LogoutResponse;
import com.qtpc.tech.nolmax.server.utils.ConnectionManager;
import com.qtpc.tech.nolmax.server.utils.HandlerUtils;
import com.qtpc.tech.nolmax.server.utils.ProtoMapper;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserLogic {
    public static final Logger log = LoggerFactory.getLogger(UserLogic.class);
    public static final UserDAO userDAO = new UserDAO();

    public void handleUpdateUserAvatarRequest(ChannelHandlerContext ctx, UpdateUserAvatarRequest request) {
        HandlerUtils.logDebug(log, "Handling UpdateUserAvatarRequest from {}", ctx.channel().remoteAddress());

        long userId = HandlerUtils.getUserId(ctx);
        boolean success = userDAO.updateAvatar(userId, request.getAvatarUrl());
        log.info("UpdateUserAvatarRequest processed for userId={}: success={}", userId, success);

        UpdateUserAvatarResponse response = UpdateUserAvatarResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setUpdateUserAvatarResponse(response).build());

        if (success) {
            com.qtpc.tech.nolmax.proto.UpdateBroadcastUser broadcastObj = com.qtpc.tech.nolmax.proto.UpdateBroadcastUser.newBuilder().setUser(User.newBuilder().setAvatarUrl(request.getAvatarUrl()).setId(userId)).build();
            ChatPacket broadcastPacket = ChatPacket.newBuilder().setUpdateBroadcastUser(broadcastObj).build();

            List<Long> conversationList = ConversationLogic.conversationDAO.takeUserConversations(userId);
            Set<Long> uniqueUserIds = new HashSet<>();

            for (int i = 0; i < conversationList.size(); i++) {
                List<com.nolmax.database.model.Participant> participants = ParticipantLogic.participantDAO.getParticipantsByConversation(conversationList.get(i));
                for (int j = 0; j < participants.size(); j++) {
                    uniqueUserIds.add(participants.get(j).getUserId());
                }
            }

            for (Long targetUserId : uniqueUserIds) {
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

    public void handlePullUsersRequest(ChannelHandlerContext ctx, PullUsersRequest request) {
        HandlerUtils.logDebug(log, "Handling PullUsersRequest from {}", ctx.channel().remoteAddress());

        long conversation_id = request.getConversationId();
        long last_update_id = request.getLastUpdateId();

        List<User> protoUsers = userDAO.pull(conversation_id, last_update_id)
                .stream()
                .map(ProtoMapper::toProtoUser)
                .toList();

        log.info("PullUsersRequest processed for conversationId={}, lastUpdateId={}, returnedUsers={}", conversation_id, last_update_id, protoUsers.size());

        PullUsersResponse response = PullUsersResponse.newBuilder().setErrorCode(0).addAllUsers(protoUsers).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setPullUsersResponse(response).build());
    }

    public void handleLogoutRequest(ChannelHandlerContext ctx) {
        HandlerUtils.logDebug(log, "Handling LogoutRequest from {}", ctx.channel().remoteAddress());

        long userId = HandlerUtils.getUserId(ctx);
        String token = HandlerUtils.getToken(ctx);

        boolean success = userDAO.logout(userId, token);
        log.info("LogoutRequest processed for userId={}: success={}", userId, success);

        LogoutResponse response = LogoutResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setLogoutResponse(response).build()).addListener(ChannelFutureListener.CLOSE);

        ConnectionManager.getChannels(userId).remove(ctx.channel());
    }
}