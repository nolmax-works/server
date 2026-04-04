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
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.kqueue.KQueue;
import io.netty.channel.kqueue.KQueueEventLoopGroup;
import io.netty.channel.kqueue.KQueueServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.uring.IoUring;
import io.netty.channel.uring.IoUringIoHandler;
import io.netty.channel.uring.IoUringServerSocketChannel;
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

    @SuppressWarnings("deprecation") // YES i know what i am doing
    public void start() {
        EventLoopGroup bossGroup;
        EventLoopGroup workerGroup;
        Class<? extends ServerSocketChannel> channelClass;

        if (IoUring.isAvailable()) {
            log.info("Detected that io_uring syscall interface is available. You should be on Linux.");
            bossGroup = new MultiThreadIoEventLoopGroup(1, IoUringIoHandler.newFactory());
            workerGroup = new MultiThreadIoEventLoopGroup(IoUringIoHandler.newFactory());
            channelClass = IoUringServerSocketChannel.class;
        } else if (Epoll.isAvailable()) {
            log.info("Detected that epoll syscall interface is available. You should be on Linux.");
            bossGroup = new EpollEventLoopGroup(1);
            workerGroup = new EpollEventLoopGroup();
            channelClass = EpollServerSocketChannel.class;
        } else if (KQueue.isAvailable()) {
            log.info("Detected that kqueue syscall interface is available. You should be on *BSD or Darwin (Apple *OS).");
            bossGroup = new KQueueEventLoopGroup(1);
            workerGroup = new KQueueEventLoopGroup();
            channelClass = KQueueServerSocketChannel.class;
        } else {
            log.info("Performant OS-specific I/O interface is not detected. Using Java's Non-blocking I/O interface...");
            bossGroup = new NioEventLoopGroup(1);
            workerGroup = new NioEventLoopGroup();
            channelClass = NioServerSocketChannel.class;
        }

        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(channelClass)
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