package com.qtpc.tech.nolmax.server.utils;

import com.qtpc.tech.nolmax.server.Main;
import com.qtpc.tech.nolmax.server.handlers.AuthHandler;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HandlerUtils {
    private static final Logger log = LoggerFactory.getLogger(HandlerUtils.class);

    public static long getUserId(ChannelHandlerContext ctx) {
        return ctx.channel().attr(AuthHandler.USER_ID).get();
    }

    public static int toErrorCode(boolean success) {
        return success ? 0 : 1;
    }

    public static void sendResponse(ChannelHandlerContext ctx, Object response) {
        ctx.writeAndFlush(response);
    }

    public static boolean isDebugEnabled() {
        return Main.config != null && Main.config.server != null && Main.config.server.debug;
    }

    public static void logDebug(Logger logger, String message, Object... args) {
        if (isDebugEnabled()) {
            logger.debug(message, args);
        }
    }
}