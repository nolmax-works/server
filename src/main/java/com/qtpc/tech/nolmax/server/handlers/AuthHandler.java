package com.qtpc.tech.nolmax.server.handlers;

import com.qtpc.tech.nolmax.proto.AuthRequest;
import com.qtpc.tech.nolmax.proto.AuthResponse;
import com.qtpc.tech.nolmax.proto.ChatPacket;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

public class AuthHandler extends SimpleChannelInboundHandler<ChatPacket> {

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ChatPacket packet) throws Exception {
        if (packet.hasAuthRequest()) {
            AuthRequest request = packet.getAuthRequest();
            String token = request.getToken();
            System.out.println("Got token: " + token);

            ChatPacket responsePacket;

            if (token.equals("1234")) {
                // use builder to build packet
                AuthResponse successResponse = AuthResponse.newBuilder()
                        .setErrorCode(0) // 0 là thành công
                        .build();

                responsePacket = ChatPacket.newBuilder()
                        .setAuthResponse(successResponse)
                        .build();

                System.out.println("Authentication success!");
            } else {
                AuthResponse failedResponse = AuthResponse.newBuilder()
                        .setErrorCode(1)
                        .build();

                responsePacket = ChatPacket.newBuilder()
                        .setAuthResponse(failedResponse)
                        .build();
                System.out.println("Failed authentication!");
            }

            ctx.writeAndFlush(responsePacket);
        }
    }
}