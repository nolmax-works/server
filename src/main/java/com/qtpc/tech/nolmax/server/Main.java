package com.qtpc.tech.nolmax.server;

import com.qtpc.tech.nolmax.proto.ChatPacket;
import com.qtpc.tech.nolmax.server.handlers.AuthHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;

public class Main {
    public static void main(String[] args) {
        int port = 8080;

        // netty initialization magic
        EventLoopGroup bossGroup = new NioEventLoopGroup(1); // only one group needed to serve port
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // multiple workers

        try {
            ServerBootstrap b = new ServerBootstrap(); // bootstrapping server
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // non-blocking I/O initialization
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline(); // first create a decoding pipeline

                            pipeline.addLast(new ProtobufVarint32FrameDecoder());
                            pipeline.addLast(new ProtobufDecoder(ChatPacket.getDefaultInstance()));
                            pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                            pipeline.addLast(new ProtobufEncoder());
                            pipeline.addLast(new AuthHandler());
                        }
                    });
            System.out.println("Server is about to run at port " + port);

            // bind port and start receiving connections
            ChannelFuture f = b.bind(port).sync();
            System.out.println("Server started successfully!");

            // keep server running
            f.channel().closeFuture().sync();
        } catch (Exception e) {
            System.err.println("Something catastrophically happened! :(");
            System.err.println(e.getMessage());
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            System.out.println("Server shut down gracefully.");
        }
    }
}