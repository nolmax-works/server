package com.qtpc.tech.nolmax.server.logic;

import com.nolmax.database.database.MessageDAO;
import com.nolmax.database.model.Message;
import com.qtpc.tech.nolmax.proto.ChatPacket;
import com.qtpc.tech.nolmax.proto.CreateMessageRequest;
import com.qtpc.tech.nolmax.proto.CreateMessageResponse;
import com.qtpc.tech.nolmax.proto.PullMessagesRequest;
import com.qtpc.tech.nolmax.proto.PullMessagesResponse;
import com.qtpc.tech.nolmax.server.utils.ConnectionManager;
import com.qtpc.tech.nolmax.server.utils.HandlerUtils;
import com.qtpc.tech.nolmax.server.utils.ProtoMapper;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageLogic {
    private static final Logger log = LoggerFactory.getLogger(MessageLogic.class);
    public static final MessageDAO messageDAO = new MessageDAO();

    public void handleCreateMessageRequest(ChannelHandlerContext ctx, CreateMessageRequest request) {
        HandlerUtils.logDebug(log, "Handling CreateMessageRequest from {}", ctx.channel().remoteAddress());

        long conversation_id = request.getConversationId();
        long sender_id = HandlerUtils.getUserId(ctx);

        if (!ParticipantLogic.participantDAO.isUserInConversation(conversation_id, sender_id)) {
            log.warn("User {} tried to send a message to conversation {} without being a participant", sender_id, conversation_id);
            CreateMessageResponse response = CreateMessageResponse.newBuilder().setErrorCode(1).build();
            HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setCreateMessageResponse(response).build());
            return;
        }

        String content = request.getContent();

        Message message = new Message();
        message.setConversationId(conversation_id);
        message.setSenderId(sender_id);
        message.setContent(content);

        boolean success = messageDAO.createMessage(message);

        log.info("CreateMessageRequest processed for conversationId={}, senderId={}, content={}: success={}", conversation_id, sender_id, content, success);
        CreateMessageResponse response = CreateMessageResponse.newBuilder().setErrorCode(HandlerUtils.toErrorCode(success)).setMessage(ProtoMapper.toProtoMessage(message)).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setCreateMessageResponse(response).build());

        if (success) {
            com.qtpc.tech.nolmax.proto.BroadcastMessageOnline broadcastObj = com.qtpc.tech.nolmax.proto.BroadcastMessageOnline.newBuilder().setMessage(ProtoMapper.toProtoMessage(message)).build();

            ChatPacket broadcastPacket = ChatPacket.newBuilder().setBroadcastMessageOnline(broadcastObj).build();

            List<com.nolmax.database.model.Participant> participants = ParticipantLogic.participantDAO.getParticipantsByConversation(conversation_id);

            for (com.nolmax.database.model.Participant participant : participants) {
                long targetUserId = participant.getUserId();
                io.netty.channel.group.ChannelGroup targetChannels = ConnectionManager.getChannels(targetUserId);

                if (targetChannels != null && !targetChannels.isEmpty()) {
                    if (targetUserId == sender_id) {
                        targetChannels.writeAndFlush(broadcastPacket, io.netty.channel.group.ChannelMatchers.isNot(ctx.channel()));
                    } else {
                        targetChannels.writeAndFlush(broadcastPacket);
                    }
                }
            }
        }
    }

    public void handlePullMessageRequest(ChannelHandlerContext ctx, PullMessagesRequest request) {
        HandlerUtils.logDebug(log, "Handling PullMessagesRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long lastUpdateId = request.getLastUpdateId();

        List<com.qtpc.tech.nolmax.proto.Message> protoMessages = messageDAO.pull(conversationId, lastUpdateId).stream().map(ProtoMapper::toProtoMessage).toList();

        log.info("PullMessageRequest processed for conversationId={}, lastUpdateId={}, returnedMessages={}", conversationId, lastUpdateId, protoMessages.size());

        PullMessagesResponse response = PullMessagesResponse.newBuilder().setErrorCode(0).addAllMessages(protoMessages).build();
        HandlerUtils.sendResponse(ctx, ChatPacket.newBuilder().setPullMessagesResponse(response).build());
    }
}