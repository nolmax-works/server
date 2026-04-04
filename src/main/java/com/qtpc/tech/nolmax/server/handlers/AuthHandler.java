package com.qtpc.tech.nolmax.server.handlers;

import com.nolmax.database.database.UserDAO;
import com.nolmax.database.model.User;
import com.qtpc.tech.nolmax.proto.AuthRequest;
import com.qtpc.tech.nolmax.proto.AuthResponse;
import com.qtpc.tech.nolmax.proto.ChatPacket;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthHandler extends SimpleChannelInboundHandler<ChatPacket> {
    private static final Logger log = LoggerFactory.getLogger(AuthHandler.class);
    public static final AttributeKey<Long> USER_ID = AttributeKey.newInstance("userId");
    public static final AttributeKey<String> USER_TOKEN = AttributeKey.newInstance("userToken");
    private boolean authenticated;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatPacket packet) {
        if (authenticated) {
            ctx.close();
            return;
        }

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
            authenticated = true;
            ctx.channel().attr(USER_ID).set(user.getId()); // store the user ID
            ctx.channel().attr(USER_TOKEN).set(user.getToken()); // store the user token for future logouts
            ctx.writeAndFlush(responsePacket);
            ctx.pipeline().remove(this); // remove pipeline so we may not need to reauth again once we're in reducing overhead
            log.info("Authentication success for user: " + user.getId());
        } else {
            sendAuthFailureAndClose(ctx);
        }
    }

    private void sendAuthFailureAndClose(ChannelHandlerContext ctx) {
        AuthResponse failedResponse = AuthResponse.newBuilder().setErrorCode(1).build();
        ChatPacket responsePacket = ChatPacket.newBuilder().setAuthResponse(failedResponse).build();
        log.info("Failed authentication!");
        ctx.writeAndFlush(responsePacket).addListener(ChannelFutureListener.CLOSE);
    }
}