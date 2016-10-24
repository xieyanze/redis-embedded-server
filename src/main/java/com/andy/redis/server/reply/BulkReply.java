package com.andy.redis.server.reply;

import com.google.common.base.Charsets;

import java.io.IOException;
import java.nio.charset.Charset;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import static com.andy.redis.server.util.Encoding.numToBytes;


public class BulkReply implements Reply<ByteBuf> {
  public static final BulkReply NIL_REPLY = new BulkReply();

  static final char MARKER = '$';
  private final ByteBuf bytes;
  private final int capacity;

  private BulkReply() {
    bytes = null;
    capacity = -1;
  }

  public BulkReply(byte[] bytes) {
    this.bytes = Unpooled.wrappedBuffer(bytes);
    capacity = bytes.length;
  }

  BulkReply(ByteBuf bytes) {
    this.bytes = bytes;
    capacity = bytes.capacity();
  }

  @Override
  public ByteBuf data() {
    return bytes;
  }

  @SuppressWarnings("unused")
  public String asAsciiString() {
    if (bytes == null) return null;
    return bytes.toString(Charsets.US_ASCII);
  }

  private String asUTF8String() {
    if (bytes == null) return null;
    return bytes.toString(Charsets.UTF_8);
  }

  String asString(Charset charset) {
    if (bytes == null) return null;
    return bytes.toString(charset);
  }

  @Override
  public void write(ByteBuf os) throws IOException {
    os.writeByte(MARKER);
    os.writeBytes(numToBytes(capacity, true));
    if (capacity > 0) {
      os.writeBytes(bytes);
      os.writeBytes(CRLF);
    }
  }

  public String toString() {
    return asUTF8String();
  }
}
