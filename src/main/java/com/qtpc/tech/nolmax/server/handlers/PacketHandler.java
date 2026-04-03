package com.qtpc.tech.nolmax.server.handlers;

import com.nolmax.database.database.ConversationDAO;
import com.nolmax.database.database.MessageDAO;
import com.nolmax.database.database.ParticipantDAO;
import com.nolmax.database.model.Conversation;
import com.qtpc.tech.nolmax.proto.ChatPacket;
import com.qtpc.tech.nolmax.proto.ConversationType;
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
import com.qtpc.tech.nolmax.server.Main;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PacketHandler extends SimpleChannelInboundHandler<ChatPacket> {
    private static final Logger log = LoggerFactory.getLogger(PacketHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatPacket packet) {
        if (Main.config != null && Main.config.server != null && Main.config.server.debug) {
            log.debug("Received packet from {}: {}", ctx.channel().remoteAddress(), packet.getDescriptorForType().getName());
        }

        // DAO objects
        ConversationDAO conversationDAO = new ConversationDAO();
        MessageDAO messageDAO = new MessageDAO();
        ParticipantDAO participantDAO = new ParticipantDAO();

        // conversation-focused
        if (packet.hasCreateConversationRequest()) {
            if (Main.config != null && Main.config.server != null && Main.config.server.debug) {
                log.debug("Handling CreateConversationRequest from {}", ctx.channel().remoteAddress());
            }

            // initialize objects
            CreateConversationRequest createConversationRequest = packet.getCreateConversationRequest();
            com.qtpc.tech.nolmax.proto.Conversation packetConversation = createConversationRequest.getConversation();

            // initialize a new conversation obj
            Conversation conversation = new Conversation();
            conversation.setAvatarUrl(packetConversation.getAvatarUrl());
            conversation.setName(packetConversation.getName());
            conversation.setType(packetConversation.getTypeValue()); // 0 for private, 1 for group
            conversation.setCreatedBy(ctx.channel().attr(com.qtpc.tech.nolmax.server.handlers.AuthHandler.USER_ID).get()); // get channel owner

            // create the conversation in the db
            boolean result = conversationDAO.createConversation(conversation);

            log.info("CreateConversationRequest processed: success={}", result);

            // send back response
            CreateConversationResponse response = CreateConversationResponse.newBuilder().setErrorCode(result ? 0 : 1).build();
            ctx.writeAndFlush(response);
        } else if (packet.hasUpdateConversationAvatarRequest()) {
            if (Main.config != null && Main.config.server != null && Main.config.server.debug) {
                log.debug("Handling UpdateConversationAvatarRequest from {}", ctx.channel().remoteAddress());
            }

            // initialize packet object
            UpdateConversationAvatarRequest updateConversationAvatarRequest = packet.getUpdateConversationAvatarRequest();

            // grab the properties
            long conversationId = updateConversationAvatarRequest.getId();
            String avatarUrl = updateConversationAvatarRequest.getAvatarUrl();

            // backend update
            boolean result = conversationDAO.updateAvatar(conversationId, avatarUrl);

            log.info("UpdateConversationAvatarRequest processed for conversationId={}: success={}", conversationId, result);

            // send back response
            UpdateConversationAvatarResponse updateConversationAvatarResponse = UpdateConversationAvatarResponse.newBuilder().setErrorCode(result ? 0 : 1).build();
            ctx.writeAndFlush(updateConversationAvatarResponse);
        } else if (packet.hasUpdateConversationNameRequest()) {
            if (Main.config != null && Main.config.server != null && Main.config.server.debug) {
                log.debug("Handling UpdateConversationNameRequest from {}", ctx.channel().remoteAddress());
            }

            // initialize packet object
            UpdateConversationNameRequest updateConversationNameRequest = packet.getUpdateConversationNameRequest();

            // grab the properties
            long conversationId = updateConversationNameRequest.getId();
            String name = updateConversationNameRequest.getName();

            // backend update
            boolean result = conversationDAO.updateName(conversationId, name);

            log.info("UpdateConversationNameRequest processed for conversationId={}: success={}", conversationId, result);

            // send back response
            UpdateConversationNameResponse updateConversationNameResponse = UpdateConversationNameResponse.newBuilder().setErrorCode(result ? 0 : 1).build();
            ctx.writeAndFlush(updateConversationNameResponse);
        } else if (packet.hasUpdateConversationLastMessageRequest()) {
            if (Main.config != null && Main.config.server != null && Main.config.server.debug) {
                log.debug("Handling UpdateConversationLastMessageRequest from {}", ctx.channel().remoteAddress());
            }

            // initialize packet object
            UpdateConversationLastMessageRequest updateConversationLastMessageRequest = packet.getUpdateConversationLastMessageRequest();
            // grab the properties
            long conversationId = updateConversationLastMessageRequest.getId();
            long lastMessageId = updateConversationLastMessageRequest.getMessageId();

            // backend update
            boolean result = conversationDAO.updateLastMessageId(conversationId, lastMessageId);

            log.info("UpdateConversationLastMessageRequest processed for conversationId={}, lastMessageId={}: success={}",
                    conversationId, lastMessageId, result);

            // send back response
            UpdateConversationLastMessageResponse updateConversationLastMessageResponse = UpdateConversationLastMessageResponse.newBuilder().setErrorCode(result ? 0 : 1).build();
            ctx.writeAndFlush(updateConversationLastMessageResponse);
        } else if (packet.hasDeleteConversationRequest()) {
            if (Main.config != null && Main.config.server != null && Main.config.server.debug) {
                log.debug("Handling DeleteConversationRequest from {}", ctx.channel().remoteAddress());
            }

            // initialize packet object
            DeleteConversationRequest deleteConversationRequest = packet.getDeleteConversationRequest();

            // grab the properties
            long conversationId = deleteConversationRequest.getId();

            // initialize response object first
            DeleteConversationResponse deleteConversationResponse;

            // checks if the user making this action is the owner
            if (!conversationDAO.isCreator(conversationId, ctx.channel().attr(com.qtpc.tech.nolmax.server.handlers.AuthHandler.USER_ID).get())) {
                log.info("DeleteConversationRequest denied for conversationId={}: not creator", conversationId);
                deleteConversationResponse = DeleteConversationResponse.newBuilder().setErrorCode(1).build();
                ctx.writeAndFlush(deleteConversationResponse);
            } else {
                // backend delete
                boolean result = conversationDAO.deleteConversation(conversationId);

                log.info("DeleteConversationRequest processed for conversationId={}: success={}", conversationId, result);

                deleteConversationResponse = DeleteConversationResponse.newBuilder().setErrorCode(result ? 0 : 1).build();
                ctx.writeAndFlush(deleteConversationResponse);
            }
        } else if (packet.hasPullConversationsRequest()) {
            if (Main.config != null && Main.config.server != null && Main.config.server.debug) {
                log.debug("Handling PullConversationsRequest from {}", ctx.channel().remoteAddress());
            }

            // initialize packet object
            PullConversationsRequest pullConversationsRequest = packet.getPullConversationsRequest();

            // grab the properties
            long user_id = pullConversationsRequest.getUserId();
            long update_id = pullConversationsRequest.getLastUpdateId();

            // backend pull (returns a list of Conversations) and stream them to proto-type Conversations
            List<com.qtpc.tech.nolmax.proto.Conversation> protoConversations = conversationDAO.pull(update_id, user_id).stream()
                    .map(c -> com.qtpc.tech.nolmax.proto.Conversation.newBuilder()
                            .setId(c.getId())
                            .setType(ConversationType.forNumber(c.getType()))
                            .setName(c.getName())
                            .setAvatarUrl(c.getAvatarUrl())
                            .setCreatedBy(c.getCreatedBy())
                            .setUpdateId(c.getUpdateId())
                            .setLastMessageId(c.getLastMessageId())
                            .build())
                    .toList();

            log.info("PullConversationsRequest processed for userId={}, lastUpdateId={}, returnedConversations={}",
                    user_id, update_id, protoConversations.size());

            // send back response
            PullConversationsResponse pullConversationsResponse = PullConversationsResponse.newBuilder().addAllConversations(protoConversations).build();
            ctx.writeAndFlush(pullConversationsResponse);
        } else {
            log.info("Received unsupported packet type from {}: {}", ctx.channel().remoteAddress(), packet.getDescriptorForType().getName());
        }
    }
}