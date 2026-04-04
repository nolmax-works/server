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
                .setId(c.getId())
                .setType(ConversationType.forNumber(c.getType()))
                .setName(c.getName())
                .setAvatarUrl(c.getAvatarUrl())
                .setCreatedBy(c.getCreatedBy())
                .setUpdateId(c.getUpdateId())
                .setLastMessageId(c.getLastMessageId())
                .build();
    }

    public static com.qtpc.tech.nolmax.proto.Message toProtoMessage(Message c) {
        return com.qtpc.tech.nolmax.proto.Message.newBuilder()
                .setId(c.getId())
                .setConversationId(c.getConversationId())
                .setSenderId(c.getSenderId())
                .setContent(c.getContent())
                .setSentAt(toProtoTimestamp(c.getSentAt()))
                .build();
    }

    public static com.qtpc.tech.nolmax.proto.Participant toProtoParticipant(Participant c) {
        return com.qtpc.tech.nolmax.proto.Participant.newBuilder()
                .setConversationId(c.getConversationId())
                .setUserId(c.getUserId())
                .setRole(ParticipantRole.forNumber(c.getRole()))
                .setJoinedAt(toProtoTimestamp(c.getJoinedAt()))
                .setLeftAt(toProtoTimestamp(c.getLeftAt()))
                .setLastReadMessageId(c.getLastReadMessageId())
                .setUpdateId(c.getUpdateId())
                .build();
    }

    public static com.qtpc.tech.nolmax.proto.User toProtoUser(User c) {
        return com.qtpc.tech.nolmax.proto.User.newBuilder()
                .setId(c.getId())
                .setUsername(c.getUsername())
                .setAvatarUrl(c.getAvatarUrl())
                .setUpdateId(c.getUpdateId())
                .build();
    }

    public static Timestamp toProtoTimestamp(LocalDateTime ldt) {
        if (ldt == null) return Timestamp.getDefaultInstance();
        Instant instant = ldt.toInstant(ZoneOffset.UTC);
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    public static LocalDateTime fromProtoTimestamp(Timestamp ts) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(ts.getSeconds(), ts.getNanos()), ZoneOffset.UTC);
    }
}