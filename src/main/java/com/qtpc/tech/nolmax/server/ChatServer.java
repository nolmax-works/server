package com.qtpc.tech.nolmax.server;

import com.qtpc.tech.nolmax.proto.ChatPacket;
import com.qtpc.tech.nolmax.server.configuration.AppConfig;
import com.qtpc.tech.nolmax.server.handlers.AuthHandler;
import com.qtpc.tech.nolmax.server.handlers.PacketHandler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChatServer {
    private static final Logger log = LoggerFactory.getLogger(ChatServer.class);
    private final AppConfig config;

    public ChatServer(AppConfig config) {
        this.config = config;
    }

    public void start() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 protected void initChannel(SocketChannel ch) {
                     ChannelPipeline pipeline = ch.pipeline();
                     pipeline.addLast(new ProtobufVarint32FrameDecoder());
                     pipeline.addLast(new ProtobufDecoder(ChatPacket.getDefaultInstance()));
                     pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                     pipeline.addLast(new ProtobufEncoder());
                     pipeline.addLast(new AuthHandler());
                     pipeline.addLast(new PacketHandler());
                 }
             });

            log.info("Server is about to run at {}:{}", config.server.listen_address, config.server.port);

            ChannelFuture f = b.bind(config.server.listen_address, config.server.port).sync();
            log.info("Server started successfully!");

            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Server interrupted", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("Something catastrophically happened! :(");
            System.err.println(e.getMessage());
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            log.info("Server shut down gracefully.");
        }
    }
}