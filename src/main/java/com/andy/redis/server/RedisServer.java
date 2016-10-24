package com.andy.redis.server;

import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

/**
 * redis server
 */
@SuppressWarnings({"unchecked", "WeakerAccess"})
public class RedisServer {

    private static Logger logger = LoggerFactory.getLogger(RedisServer.class);

    private static final String REDIS_GROUP_CONTEXT_PROPERTY_NAME = RedisServer.class.getName() + ":redisGroup";
    private static final String REDIS_CHANNEL_CONTEXT_PROPERTY_NAME = RedisServer.class.getName() + ":redisChannel";
    private static Map map = Maps.newHashMap();

    private final int port;

    private RedisServer(int port) {
        this.port = port;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public void start() {
        final RedisCommandHandler commandHandler = new RedisCommandHandler(new SimpleRedisServerHandle());

        final DefaultEventExecutorGroup redisGroup = new DefaultEventExecutorGroup(1);

        map.put(REDIS_GROUP_CONTEXT_PROPERTY_NAME, redisGroup);

        ServerBootstrap redisServerBootstrap = new ServerBootstrap();

        try {
            redisServerBootstrap.group(new NioEventLoopGroup(), new NioEventLoopGroup())
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .localAddress(port)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new RedisCommandDecoder());
                            p.addLast(new RedisReplyEncoder());
                            p.addLast(redisGroup, commandHandler);
                        }
                    });

            logger.info("Starting Redis(port=" + port + ") server...");
            ChannelFuture future = redisServerBootstrap.bind();
            ChannelFuture syncFuture = future.sync();
            map.put(REDIS_CHANNEL_CONTEXT_PROPERTY_NAME, syncFuture.channel());

        } catch (InterruptedException e) {
            logger.error("fail to start redis server", e);
            redisGroup.shutdownGracefully();
        } finally {
            if (!redisGroup.isShutdown()) {
                redisGroup.shutdownGracefully();
            }
        }
    }

    public void stop() {
        DefaultEventExecutorGroup redisGroup = (DefaultEventExecutorGroup) map.get(REDIS_GROUP_CONTEXT_PROPERTY_NAME);
        Channel redisChannel = (Channel) map.get(REDIS_CHANNEL_CONTEXT_PROPERTY_NAME);
        logger.info("Shutting down Redis server...");
        ChannelFuture closeFuture = redisChannel.close();
        try {
            closeFuture.sync();
            redisGroup.shutdownGracefully();
            while (!redisGroup.isTerminated()) {
                Thread.sleep(50);
            }
            logger.info("Redis server shutdown completed");
        } catch (InterruptedException e) {
            logger.info("fail to stop redis server", e);
        }
    }

    public static class Builder {

        private int port;

        public Builder port(int port) {
            this.port = port;
            return this;
        }

        public RedisServer build() {
            return new RedisServer(port);
        }
    }
}
