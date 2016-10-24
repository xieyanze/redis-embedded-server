package com.andy.redis.server;

import com.andy.redis.server.reply.Reply;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * Write a reply.
 */
class RedisReplyEncoder extends MessageToByteEncoder<Reply> {
  @Override
  public void encode(ChannelHandlerContext ctx, Reply msg, ByteBuf out) throws Exception {
    msg.write(out);
  }
}
