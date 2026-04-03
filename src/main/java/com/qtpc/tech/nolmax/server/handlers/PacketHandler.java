package com.qtpc.tech.nolmax.server.handlers;

import com.google.protobuf.Timestamp;
import com.nolmax.database.database.ConversationDAO;
import com.nolmax.database.database.MessageDAO;
import com.nolmax.database.database.ParticipantDAO;
import com.nolmax.database.model.Conversation;
import com.nolmax.database.model.Message;
import com.nolmax.database.model.Participant;
import com.nolmax.database.util.IdGenerator;
import com.qtpc.tech.nolmax.proto.ChatPacket;
import com.qtpc.tech.nolmax.proto.CreateConversationRequest;
import com.qtpc.tech.nolmax.proto.CreateConversationResponse;
import com.qtpc.tech.nolmax.proto.CreateMessageRequest;
import com.qtpc.tech.nolmax.proto.CreateMessageResponse;
import com.qtpc.tech.nolmax.proto.DeleteConversationRequest;
import com.qtpc.tech.nolmax.proto.DeleteConversationResponse;
import com.qtpc.tech.nolmax.proto.JoinConversationRequest;
import com.qtpc.tech.nolmax.proto.JoinConversationResponse;
import com.qtpc.tech.nolmax.proto.LeaveConversationRequest;
import com.qtpc.tech.nolmax.proto.LeaveConversationResponse;
import com.qtpc.tech.nolmax.proto.ParticipantRole;
import com.qtpc.tech.nolmax.proto.PullConversationsRequest;
import com.qtpc.tech.nolmax.proto.PullConversationsResponse;
import com.qtpc.tech.nolmax.proto.PullMessagesRequest;
import com.qtpc.tech.nolmax.proto.PullMessagesResponse;
import com.qtpc.tech.nolmax.proto.PullParticipantsRequest;
import com.qtpc.tech.nolmax.proto.PullParticipantsResponse;
import com.qtpc.tech.nolmax.proto.UpdateConversationAvatarRequest;
import com.qtpc.tech.nolmax.proto.UpdateConversationAvatarResponse;
import com.qtpc.tech.nolmax.proto.UpdateConversationLastMessageRequest;
import com.qtpc.tech.nolmax.proto.UpdateConversationLastMessageResponse;
import com.qtpc.tech.nolmax.proto.UpdateConversationNameRequest;
import com.qtpc.tech.nolmax.proto.UpdateConversationNameResponse;
import com.qtpc.tech.nolmax.proto.UpdateLastReadMessageRequest;
import com.qtpc.tech.nolmax.proto.UpdateLastReadMessageResponse;
import com.qtpc.tech.nolmax.proto.UpdateParticipantRoleRequest;
import com.qtpc.tech.nolmax.proto.UpdateParticipantRoleResponse;
import com.qtpc.tech.nolmax.proto.ConversationType;
import com.qtpc.tech.nolmax.server.Main;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class PacketHandler extends SimpleChannelInboundHandler<ChatPacket> {
    private static final Logger log = LoggerFactory.getLogger(PacketHandler.class);

    private final ConversationDAO conversationDAO = new ConversationDAO();
    private final MessageDAO messageDAO = new MessageDAO();
    private final ParticipantDAO participantDAO = new ParticipantDAO();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatPacket packet) {
        logDebug("Received packet from {}: {}", ctx.channel().remoteAddress(), packet.getDescriptorForType().getName());

        // conversation-focused
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
        }

        // message-focused
        else if (packet.hasCreateMessageRequest()) {
            handleCreateMessageRequest(ctx, packet.getCreateMessageRequest());
        } else if (packet.hasPullMessagesRequest()) {
            handlePullMessageRequest(ctx, packet.getPullMessagesRequest());
        }

        // participant-focused
        else if (packet.hasJoinConversationRequest()) {
            handleJoinConversationRequest(ctx, packet.getJoinConversationRequest());
        } else if (packet.hasLeaveConversationRequest()) {
            handleLeaveConversationRequest(ctx, packet.getLeaveConversationRequest());
        } else if (packet.hasUpdateParticipantRoleRequest()) {
            handleUpdateParticipantRoleRequest(ctx, packet.getUpdateParticipantRoleRequest());
        } else if (packet.hasUpdateLastReadMessageRequest()) {
            handleUpdateLastReadMessageRequest(ctx, packet.getUpdateLastReadMessageRequest());
        } else if (packet.hasPullParticipantsRequest()) {
            handlePullParticipantsRequest(ctx, packet.getPullParticipantsRequest());
        }

        // warn if unsupported packet type
        else {
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

        sendResponse(ctx, CreateConversationResponse.newBuilder().setErrorCode(toErrorCode(success)).build());
    }

    private void handleUpdateConversationAvatar(ChannelHandlerContext ctx, UpdateConversationAvatarRequest request) {
        logDebug("Handling UpdateConversationAvatarRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        boolean success = conversationDAO.updateAvatar(conversationId, request.getAvatarUrl());
        log.info("UpdateConversationAvatarRequest processed for conversationId={}: success={}", conversationId, success);

        sendResponse(ctx, UpdateConversationAvatarResponse.newBuilder().setErrorCode(toErrorCode(success)).build());
    }

    private void handleUpdateConversationName(ChannelHandlerContext ctx, UpdateConversationNameRequest request) {
        logDebug("Handling UpdateConversationNameRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        boolean success = conversationDAO.updateName(conversationId, request.getName());
        log.info("UpdateConversationNameRequest processed for conversationId={}: success={}", conversationId, success);

        sendResponse(ctx, UpdateConversationNameResponse.newBuilder().setErrorCode(toErrorCode(success)).build());
    }

    private void handleUpdateConversationLastMessage(ChannelHandlerContext ctx, UpdateConversationLastMessageRequest request) {
        logDebug("Handling UpdateConversationLastMessageRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getId();
        long lastMessageId = request.getMessageId();
        boolean success = conversationDAO.updateLastMessageId(conversationId, lastMessageId);
        log.info("UpdateConversationLastMessageRequest processed for conversationId={}, lastMessageId={}: success={}", conversationId, lastMessageId, success);

        sendResponse(ctx, UpdateConversationLastMessageResponse.newBuilder().setErrorCode(toErrorCode(success)).build());
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

        sendResponse(ctx, DeleteConversationResponse.newBuilder().setErrorCode(toErrorCode(success)).build());
    }

    private void handlePullConversations(ChannelHandlerContext ctx, PullConversationsRequest request) {
        logDebug("Handling PullConversationsRequest from {}", ctx.channel().remoteAddress());

        long userId = request.getUserId();
        long lastUpdateId = request.getLastUpdateId();

        List<com.qtpc.tech.nolmax.proto.Conversation> protoConversations = conversationDAO.pull(lastUpdateId, userId).stream().map(this::toProtoConversation).toList();

        log.info("PullConversationsRequest processed for userId={}, lastUpdateId={}, returnedConversations={}", userId, lastUpdateId, protoConversations.size());

        sendResponse(ctx, PullConversationsResponse.newBuilder().setErrorCode(0).addAllConversations(protoConversations).build());
    }

    private void handleCreateMessageRequest(ChannelHandlerContext ctx, CreateMessageRequest request) {
        logDebug("Handling CreateMessageRequest from {}", ctx.channel().remoteAddress());

        long conversation_id = request.getConversationId();
        long sender_id = request.getSenderId();
        String content = request.getContent();

        Message message = new Message();
        message.setId(IdGenerator.getInstance().nextId()); // use method same as the update_id one in the database backend i assume
        message.setConversationId(conversation_id);
        message.setSenderId(sender_id);
        message.setContent(content);

        boolean success = messageDAO.createMessage(message);
        sendResponse(ctx, CreateMessageResponse.newBuilder().setErrorCode(success ? 0 : 1).setMessage(toProtoMessage(message)).build());
    }

    private void handlePullMessageRequest(ChannelHandlerContext ctx, PullMessagesRequest request) {
        logDebug("Handling PullMessagesRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long lastUpdateId = request.getLastUpdateId();

        List<com.qtpc.tech.nolmax.proto.Message> protoMessages = messageDAO.pull(conversationId, lastUpdateId).stream().map(this::toProtoMessage).toList();
        log.info("PullMessageRequest processed for conversationId={}, lastUpdateId={}, returnedMessages={}", conversationId, lastUpdateId, protoMessages.size());

        sendResponse(ctx, PullMessagesResponse.newBuilder().setErrorCode(0).addAllMessages(protoMessages).build());
    }

    private void handleJoinConversationRequest(ChannelHandlerContext ctx, JoinConversationRequest request) {
        logDebug("Handling JoinConversationRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long userId = request.getUserId();

        Participant participant = new Participant();
        participant.setConversationId(conversationId);
        participant.setUserId(userId);

        boolean success = participantDAO.join(participant);
        log.info("JoinConversationRequest processed for conversationId={}, userId={}: success={}", conversationId, userId, success);

        sendResponse(ctx, JoinConversationResponse.newBuilder().setErrorCode(toErrorCode(success)).build());
    }

    private void handleLeaveConversationRequest(ChannelHandlerContext ctx, LeaveConversationRequest request) {
        logDebug("Handling LeaveConversationRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long userId = request.getUserId();

        boolean success = participantDAO.left(conversationId, userId);
        log.info("LeaveConversationRequest processed for conversationId={}, userId={}: success={}", conversationId, userId, success);

        sendResponse(ctx, LeaveConversationResponse.newBuilder().setErrorCode(toErrorCode(success)).build());
    }

    private void handleUpdateParticipantRoleRequest(ChannelHandlerContext ctx, UpdateParticipantRoleRequest request) {
        logDebug("Handling UpdateParticipantRoleRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long userId = request.getUserId();
        int role = request.getRole().getNumber();

        boolean success = participantDAO.updateRole(conversationId, userId, role);
        log.info("UpdateParticipantRoleRequest processed for conversationId={}, userId={}, role={}: success={}", conversationId, userId, role, success);

        sendResponse(ctx, UpdateParticipantRoleResponse.newBuilder().setErrorCode(toErrorCode(success)).build());
    }

    private void handleUpdateLastReadMessageRequest(ChannelHandlerContext ctx, UpdateLastReadMessageRequest request) {
        logDebug("Handling UpdateLastReadMessageRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long userId = request.getUserId();
        long messageId = request.getMessageId();

        boolean success = participantDAO.updateLastReadMessageId(conversationId, userId, messageId);
        log.info("UpdateLastReadMessageRequest processed for conversationId={}, userId={}, messageId={}: success={}", conversationId, userId, messageId, success);

        sendResponse(ctx, UpdateLastReadMessageResponse.newBuilder().setErrorCode(toErrorCode(success)).build());
    }

    private void handlePullParticipantsRequest(ChannelHandlerContext ctx, PullParticipantsRequest request) {
        logDebug("Handling PullParticipantsRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long lastUpdateId = request.getLastUpdateId();

        List<com.qtpc.tech.nolmax.proto.Participant> protoParticipants = participantDAO.pull(conversationId, lastUpdateId).stream().map(this::toProtoParticipant).toList();
        log.info("PullParticipantsRequest processed for conversationId={}, lastUpdateId={}, returnedParticipants={}", conversationId, lastUpdateId, protoParticipants);

        sendResponse(ctx, PullParticipantsResponse.newBuilder().setErrorCode(0).addAllParticipants(protoParticipants).build());
    }

    /* helper methods */
    private com.qtpc.tech.nolmax.proto.Conversation toProtoConversation(Conversation c) {
        return com.qtpc.tech.nolmax.proto.Conversation.newBuilder().setId(c.getId()).setType(ConversationType.forNumber(c.getType())).setName(c.getName()).setAvatarUrl(c.getAvatarUrl()).setCreatedBy(c.getCreatedBy()).setUpdateId(c.getUpdateId()).setLastMessageId(c.getLastMessageId()).build();
    }

    private com.qtpc.tech.nolmax.proto.Message toProtoMessage(Message c) {
        return com.qtpc.tech.nolmax.proto.Message.newBuilder().setId(c.getId()).setConversationId(c.getConversationId()).setSenderId(c.getSenderId()).setContent(c.getContent()).setSentAt(toProtoTimestamp(c.getSentAt())).build();
    }

    private com.qtpc.tech.nolmax.proto.Participant toProtoParticipant(Participant c) {
        return com.qtpc.tech.nolmax.proto.Participant.newBuilder().setConversationId(c.getConversationId()).setUserId(c.getUserId()).setRole(ParticipantRole.forNumber(c.getRole())).setJoinedAt(toProtoTimestamp(c.getJoinedAt())).setLeftAt(toProtoTimestamp(c.getLeftAt())).setLastReadMessageId(c.getLastReadMessageId()).setUpdateId(c.getUpdateId()).build();
    }

    private Timestamp toProtoTimestamp(LocalDateTime ldt) {
        Instant instant = ldt.toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    private LocalDateTime fromProtoTimestamp(Timestamp ts) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()), ZoneOffset.UTC);
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