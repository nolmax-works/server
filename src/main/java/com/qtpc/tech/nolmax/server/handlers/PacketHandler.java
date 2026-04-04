package com.qtpc.tech.nolmax.server.handlers;

import com.qtpc.tech.nolmax.proto.ChatPacket;
import com.qtpc.tech.nolmax.server.logic.ConversationLogic;
import com.qtpc.tech.nolmax.server.logic.MessageLogic;
import com.qtpc.tech.nolmax.server.logic.ParticipantLogic;
import com.qtpc.tech.nolmax.server.logic.UserLogic;
import com.qtpc.tech.nolmax.server.utils.HandlerUtils;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PacketHandler extends SimpleChannelInboundHandler<ChatPacket> {
    private static final Logger log = LoggerFactory.getLogger(PacketHandler.class);

    private final ConversationLogic conversationLogic = new ConversationLogic();
    private final MessageLogic messageLogic = new MessageLogic();
    private final ParticipantLogic participantLogic = new ParticipantLogic();
    private final UserLogic userLogic = new UserLogic();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatPacket packet) {
        HandlerUtils.logDebug(log, "Received packet from {}: {}", ctx.channel().remoteAddress(), packet.getDescriptorForType().getName());

        // conversation-focused
        if (packet.hasCreateConversationRequest()) {
            conversationLogic.handleCreateConversation(ctx, packet.getCreateConversationRequest());
        } else if (packet.hasUpdateConversationAvatarRequest()) {
            conversationLogic.handleUpdateConversationAvatar(ctx, packet.getUpdateConversationAvatarRequest());
        } else if (packet.hasUpdateConversationNameRequest()) {
            conversationLogic.handleUpdateConversationName(ctx, packet.getUpdateConversationNameRequest());
        } else if (packet.hasUpdateConversationLastMessageRequest()) {
            conversationLogic.handleUpdateConversationLastMessage(ctx, packet.getUpdateConversationLastMessageRequest());
        } else if (packet.hasDeleteConversationRequest()) {
            conversationLogic.handleDeleteConversation(ctx, packet.getDeleteConversationRequest());
        } else if (packet.hasPullConversationsRequest()) {
            conversationLogic.handlePullConversations(ctx, packet.getPullConversationsRequest());
        }

        // message-focused
        else if (packet.hasCreateMessageRequest()) {
            messageLogic.handleCreateMessageRequest(ctx, packet.getCreateMessageRequest());
        } else if (packet.hasPullMessagesRequest()) {
            messageLogic.handlePullMessageRequest(ctx, packet.getPullMessagesRequest());
        }

        // participant-focused
        else if (packet.hasJoinConversationRequest()) {
            participantLogic.handleJoinConversationRequest(ctx, packet.getJoinConversationRequest());
        } else if (packet.hasLeaveConversationRequest()) {
            participantLogic.handleLeaveConversationRequest(ctx, packet.getLeaveConversationRequest());
        } else if (packet.hasUpdateParticipantRoleRequest()) {
            participantLogic.handleUpdateParticipantRoleRequest(ctx, packet.getUpdateParticipantRoleRequest());
        } else if (packet.hasUpdateLastReadMessageRequest()) {
            participantLogic.handleUpdateLastReadMessageRequest(ctx, packet.getUpdateLastReadMessageRequest());
        } else if (packet.hasPullParticipantsRequest()) {
            participantLogic.handlePullParticipantsRequest(ctx, packet.getPullParticipantsRequest());
        }

        // user-focused
        else if (packet.hasUpdateUserAvatarRequest()) {
            userLogic.handleUpdateUserAvatarRequest(ctx, packet.getUpdateUserAvatarRequest());
        } else if (packet.hasPullUsersRequest()) {
            userLogic.handlePullUsersRequest(ctx, packet.getPullUsersRequest());
        } else if (packet.hasLogoutRequest()) {
            userLogic.handleLogoutRequest(ctx); // packet has blank body
        }

        // warn if unsupported packet type
        else {
            log.warn("Received unsupported packet type from {}: {}", ctx.channel().remoteAddress(), packet.getDescriptorForType().getName());
        }
    }
}