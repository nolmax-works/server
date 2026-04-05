package com.qtpc.tech.nolmax.server.utils;

import io.netty.channel.Channel;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.ConcurrentHashMap;

public class ConnectionManager {
    private static final ConcurrentHashMap<Long, ChannelGroup> userChannels = new ConcurrentHashMap<>();

    public static void addChannel(long userId, Channel channel) {
        ChannelGroup group = userChannels.computeIfAbsent(userId,
                id -> new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));

        group.add(channel);

        channel.closeFuture().addListener(future -> {
            group.remove(channel);
            userChannels.computeIfPresent(userId, (id, g) -> g.isEmpty() ? null : g);
        });
    }

    public static ChannelGroup getChannels(long userId) {
        return userChannels.get(userId);
    }
}