package com.qtpc.tech.nolmax.server.utils;

import com.google.protobuf.Timestamp;
import com.nolmax.database.model.Conversation;
import com.nolmax.database.model.Message;
import com.nolmax.database.model.Participant;
import com.nolmax.database.model.User;
import com.qtpc.tech.nolmax.proto.ConversationType;
import com.qtpc.tech.nolmax.proto.ParticipantRole;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class ProtoMapper {

    public static com.qtpc.tech.nolmax.proto.Conversation toProtoConversation(Conversation c) {
        return com.qtpc.tech.nolmax.proto.Conversation.newBuilder()
                .setId(c.getId() != null ? c.getId() : 0L)
                .setType(ConversationType.forNumber(c.getType() != null ? c.getType() : 0))
                .setName(c.getName() != null ? c.getName() : "")
                .setAvatarUrl(c.getAvatarUrl() != null ? c.getAvatarUrl() : "")
                .setCreatedBy(c.getCreatedBy() != null ? c.getCreatedBy() : 0L)
                .setUpdateId(c.getUpdateId() != null ? c.getUpdateId() : 0L)
                .setLastMessageId(c.getLastMessageId() != null ? c.getLastMessageId() : 0L)
                .build();
    }

    public static com.qtpc.tech.nolmax.proto.Message toProtoMessage(Message c) {
        return com.qtpc.tech.nolmax.proto.Message.newBuilder()
                .setId(c.getId() != null ? c.getId() : 0L)
                .setConversationId(c.getConversationId() != null ? c.getConversationId() : 0L)
                .setSenderId(c.getSenderId() != null ? c.getSenderId() : 0L)
                .setContent(c.getContent() != null ? c.getContent() : "")
                .setSentAt(toProtoTimestamp(c.getSentAt()))
                .build();
    }

    public static com.qtpc.tech.nolmax.proto.Participant toProtoParticipant(Participant c) {
        return com.qtpc.tech.nolmax.proto.Participant.newBuilder()
                .setConversationId(c.getConversationId() != null ? c.getConversationId() : 0L)
                .setUserId(c.getUserId() != null ? c.getUserId() : 0L)
                .setRole(ParticipantRole.forNumber(c.getRole() != null ? c.getRole() : 0))
                .setJoinedAt(toProtoTimestamp(c.getJoinedAt()))
                .setLeftAt(toProtoTimestamp(c.getLeftAt()))
                .setLastReadMessageId(c.getLastReadMessageId() != null ? c.getLastReadMessageId() : 0L)
                .setUpdateId(c.getUpdateId() != null ? c.getUpdateId() : 0L)
                .build();
    }

    public static com.qtpc.tech.nolmax.proto.User toProtoUser(User c) {
        return com.qtpc.tech.nolmax.proto.User.newBuilder()
                .setId(c.getId() != null ? c.getId() : 0L)
                .setUsername(c.getUsername() != null ? c.getUsername() : "")
                .setAvatarUrl(c.getAvatarUrl() != null ? c.getAvatarUrl() : "")
                .setUpdateId(c.getUpdateId() != null ? c.getUpdateId() : 0L)
                .build();
    }

    public static Timestamp toProtoTimestamp(LocalDateTime ldt) {
        if (ldt == null) return Timestamp.getDefaultInstance();
        Instant instant = ldt.toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    public static LocalDateTime fromProtoTimestamp(Timestamp ts) {
        if (ts == null || (ts.getSeconds() == 0 && ts.getNanos() == 0)) return null;
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()), ZoneOffset.UTC);
    }
}