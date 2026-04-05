package com.qtpc.tech.nolmax.server.handlers;

import com.nolmax.database.database.UserDAO;
import com.nolmax.database.model.User;
import com.qtpc.tech.nolmax.proto.AuthRequest;
import com.qtpc.tech.nolmax.proto.AuthResponse;
import com.qtpc.tech.nolmax.proto.ChatPacket;
import com.qtpc.tech.nolmax.server.Main;
import com.qtpc.tech.nolmax.server.utils.ConnectionManager;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AuthHandler extends SimpleChannelInboundHandler<ChatPacket> {
    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    public static final AttributeKey<Long> USER_ID = AttributeKey.newInstance("userId");
    public static final AttributeKey<String> USER_TOKEN = AttributeKey.newInstance("userToken");

    private final AtomicBoolean authenticating = new AtomicBoolean(false);
    private final AtomicBoolean authenticated = new AtomicBoolean(false);
    private final ConcurrentLinkedQueue<ChatPacket> packetQueue = new ConcurrentLinkedQueue<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatPacket packet) {
        if (authenticated.get()) {
            ctx.fireChannelRead(packet);
            return;
        }

        if (!authenticating.compareAndSet(false, true)) {
            packetQueue.offer(packet);
            return;
        }

        Main.blockingExecutor.submit(() -> {
            try {
                if (!packet.hasAuthRequest()) {
                    log.error("Rejected connection: first packet was not an auth packet.");
                    ctx.close();
                    return;
                }

                AuthRequest request = packet.getAuthRequest();
                String token = request.getToken();

                if (token == null || token.isBlank()) {
                    sendAuthFailureAndClose(ctx);
                    return;
                }

                UserDAO dao = new UserDAO();
                User user = new User();
                user.setToken(token);

                if (dao.loginWithToken(user)) {
                    AuthResponse successResponse = AuthResponse.newBuilder().setErrorCode(0).build();
                    ChatPacket responsePacket = ChatPacket.newBuilder().setAuthResponse(successResponse).build();

                    ctx.channel().attr(USER_ID).set(user.getId()); // store token in attribute
                    ctx.channel().attr(USER_TOKEN).set(user.getToken()); // store token in attribute
                    ConnectionManager.addChannel(user.getId(), ctx.channel()); // add channel for online state tracking

                    ctx.writeAndFlush(responsePacket);

                    authenticated.set(true);

                    ctx.executor().execute(() -> {
                        ctx.pipeline().remove(this);
                        ChatPacket queuedPacket;
                        while ((queuedPacket = packetQueue.poll()) != null) {
                            ctx.fireChannelRead(queuedPacket);
                        }
                    });

                    log.info("Authentication success for user: {}", user.getId());
                } else {
                    sendAuthFailureAndClose(ctx);
                }
            } catch (Exception e) {
                log.error("Catastrophic error during authentication", e);
                ctx.close();
            }
        });
    }

    private void sendAuthFailureAndClose(ChannelHandlerContext ctx) {
        AuthResponse failedResponse = AuthResponse.newBuilder().setErrorCode(1).build();
        ChatPacket responsePacket = ChatPacket.newBuilder().setAuthResponse(failedResponse).build();
        log.info("Failed authentication!");
        ctx.writeAndFlush(responsePacket).addListener(ChannelFutureListener.CLOSE);
    }
}