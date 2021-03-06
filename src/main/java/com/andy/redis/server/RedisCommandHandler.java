package com.andy.redis.server;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;

import com.andy.redis.server.reply.InlineReply;
import com.andy.redis.server.reply.StatusReply;
import com.andy.redis.server.reply.Command;
import com.andy.redis.server.reply.ErrorReply;
import com.andy.redis.server.reply.Reply;
import com.andy.redis.server.util.BytesKey;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;

import static com.andy.redis.server.reply.ErrorReply.NYI_REPLY;


/**
 * Handle decoded commands
 */
@ChannelHandler.Sharable
class RedisCommandHandler extends SimpleChannelInboundHandler<Command> {

  private Map<BytesKey, Wrapper> methods = Maps.newHashMap();

  interface Wrapper {
    Reply execute(Command command) throws RedisException;
  }

  RedisCommandHandler(final RedisServerHandle rs) {
    Class<? extends RedisServerHandle> aClass = rs.getClass();
    for (final Method method : aClass.getMethods()) {
      final Class<?>[] types = method.getParameterTypes();
      methods.put(new BytesKey(method.getName().getBytes()), command -> {
        Object[] objects = new Object[types.length];
        try {
          command.toArguments(objects, types);
          return (Reply) method.invoke(rs, objects);
        } catch (IllegalAccessException e) {
          throw new RedisException("Invalid server implementation");
        } catch (InvocationTargetException e) {
          Throwable te = e.getTargetException();
          if (!(te instanceof RedisException)) {
            te.printStackTrace();
          }
          return new ErrorReply("ERR " + te.getMessage());
        } catch (Exception e) {
          return new ErrorReply("ERR " + e.getMessage());
        }
      });
    }
  }

  private static final byte LOWER_DIFF = 'a' - 'A';

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
    ctx.flush();
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, Command msg) throws Exception {
    byte[] name = msg.getName();
    for (int i = 0; i < name.length; i++) {
      byte b = name[i];
      if (b >= 'A' && b <= 'Z') {
        name[i] = (byte) (b + LOWER_DIFF);
      }
    }
    Wrapper wrapper = methods.get(new BytesKey(name));
    Reply reply;
    if (wrapper == null) {
      reply = new ErrorReply("unknown command '" + new String(name, Charsets.US_ASCII) + "'");
    } else {
      reply = wrapper.execute(msg);
    }
    if (reply == StatusReply.QUIT) {
      ctx.close();
    } else {
      if (msg.isInline()) {
        if (reply == null) {
          reply = new InlineReply(null);
        } else {
          reply = new InlineReply(reply.data());
        }
      }
      if (reply == null) {
        reply = NYI_REPLY;
      }
      ctx.write(reply);
    }
  }
}
