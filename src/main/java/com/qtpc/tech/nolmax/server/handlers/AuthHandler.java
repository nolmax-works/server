package com.qtpc.tech.nolmax.server.handlers;

import com.nolmax.database.database.UserDAO;
import com.nolmax.database.model.User;
import com.qtpc.tech.nolmax.proto.AuthRequest;
import com.qtpc.tech.nolmax.proto.AuthResponse;
import com.qtpc.tech.nolmax.proto.ChatPacket;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class AuthHandler extends SimpleChannelInboundHandler<ChatPacket> {

    private boolean authenticated;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatPacket packet) throws Exception {
        if (authenticated) {
            ctx.close();
            return;
        }

        if (!packet.hasAuthRequest()) {
            System.err.println("Rejected connection: first packet was not an auth packet.");
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
            ctx.writeAndFlush(responsePacket);
            ctx.pipeline().remove(this);
            System.out.println("Authentication success!");
        } else {
            sendAuthFailureAndClose(ctx);
        }
    }

    private void sendAuthFailureAndClose(ChannelHandlerContext ctx) {
        AuthResponse failedResponse = AuthResponse.newBuilder().setErrorCode(1).build();
        ChatPacket responsePacket = ChatPacket.newBuilder().setAuthResponse(failedResponse).build();
        System.out.println("Failed authentication!");
        ctx.writeAndFlush(responsePacket).addListener(ChannelFutureListener.CLOSE);
    }
}