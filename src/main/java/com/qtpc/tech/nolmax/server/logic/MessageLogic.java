package com.qtpc.tech.nolmax.server.logic;

import com.nolmax.database.database.MessageDAO;
import com.nolmax.database.model.Message;
import com.nolmax.database.util.IdGenerator;
import com.qtpc.tech.nolmax.proto.CreateMessageRequest;
import com.qtpc.tech.nolmax.proto.CreateMessageResponse;
import com.qtpc.tech.nolmax.proto.PullMessagesRequest;
import com.qtpc.tech.nolmax.proto.PullMessagesResponse;
import com.qtpc.tech.nolmax.server.utils.HandlerUtils;
import com.qtpc.tech.nolmax.server.utils.ProtoMapper;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MessageLogic {
    private static final Logger log = LoggerFactory.getLogger(MessageLogic.class);
    private final MessageDAO messageDAO = new MessageDAO();

    public void handleCreateMessageRequest(ChannelHandlerContext ctx, CreateMessageRequest request) {
        HandlerUtils.logDebug(log, "Handling CreateMessageRequest from {}", ctx.channel().remoteAddress());

        long conversation_id = request.getConversationId();
        long sender_id = request.getSenderId();
        String content = request.getContent();

        Message message = new Message();
        message.setId(IdGenerator.getInstance().nextId());
        message.setConversationId(conversation_id);
        message.setSenderId(sender_id);
        message.setContent(content);

        boolean success = messageDAO.createMessage(message);
        HandlerUtils.sendResponse(ctx, CreateMessageResponse.newBuilder()
                .setErrorCode(success ? 0 : 1)
                .setMessage(ProtoMapper.toProtoMessage(message))
                .build());
    }

    public void handlePullMessageRequest(ChannelHandlerContext ctx, PullMessagesRequest request) {
        HandlerUtils.logDebug(log, "Handling PullMessagesRequest from {}", ctx.channel().remoteAddress());

        long conversationId = request.getConversationId();
        long lastUpdateId = request.getLastUpdateId();

        List<com.qtpc.tech.nolmax.proto.Message> protoMessages = messageDAO.pull(conversationId, lastUpdateId)
                .stream()
                .map(ProtoMapper::toProtoMessage)
                .toList();

        log.info("PullMessageRequest processed for conversationId={}, lastUpdateId={}, returnedMessages={}", conversationId, lastUpdateId, protoMessages.size());

        HandlerUtils.sendResponse(ctx, PullMessagesResponse.newBuilder().setErrorCode(0).addAllMessages(protoMessages).build());
    }
}