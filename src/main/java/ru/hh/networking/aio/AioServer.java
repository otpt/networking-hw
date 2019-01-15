package ru.hh.networking.aio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class AioServer {
  public static void main(String[] args) throws IOException, InterruptedException {
    int port = 5656;

    AsynchronousServerSocketChannel assc = AsynchronousServerSocketChannel.open();
    InetSocketAddress address = new InetSocketAddress(port);
    assc.bind(address);
    System.out.println("Async IO server started on port " + address.getPort());

    assc.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
      @Override
      public void completed(AsynchronousSocketChannel socketChannel, Object attachment) {
        System.out.println("Got connection from " + socketChannel);
        assc.accept(null, this);
        ByteBuffer buffer = ByteBuffer.allocate(32);
        socketChannel.read(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
          @Override
          public void completed(Integer bytes, ByteBuffer attachment) {
            System.out.println(bytes + " bytes read from " + socketChannel);
            attachment.flip();
            CompletionHandler readCompletionHandler = this;
            socketChannel.write(attachment, attachment, new CompletionHandler<Integer, ByteBuffer>() {
              @Override
              public void completed(Integer result, ByteBuffer attachment) {
                if (attachment.hasRemaining()) {
                  socketChannel.write(attachment, attachment, this);
                } else {
                  attachment.clear();
                  socketChannel.read(attachment, attachment, readCompletionHandler);
                }
              }

              @Override
              public void failed(Throwable exc, ByteBuffer attachment) {
              }
            });
          }

          @Override
          public void failed(Throwable exc, ByteBuffer attachment) {
          }
        });
      }

      @Override
      public void failed(Throwable exc, Object attachment) {
      }
    });
    Thread.sleep(Long.MAX_VALUE);
  }
}
