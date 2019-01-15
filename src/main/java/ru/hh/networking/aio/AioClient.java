package ru.hh.networking.aio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;

public class AioClient {
  public static void main(String[] args) throws IOException, InterruptedException {
    AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
    BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    InetSocketAddress address = new InetSocketAddress("localhost", 5656);
    client.connect(address, null, new CompletionHandler<Void, Object>() {
      @Override
      public void completed(Void result, Object attachment) {
        System.out.println("AIO client connected to " + address);
        while (client.isOpen()) {
          try {
            if (br.ready()) {
              String clientCommand = br.readLine();
              ByteBuffer messageBuffer = ByteBuffer.wrap(clientCommand.getBytes(StandardCharsets.UTF_8));
              client.write(messageBuffer, messageBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer result, ByteBuffer attachment) {
                  if (attachment.hasRemaining()) {
                    client.write(messageBuffer, messageBuffer, this);
                  } else {
                    messageBuffer.clear();
                    client.read(messageBuffer, messageBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                      @Override
                      public void completed(Integer bytes, ByteBuffer attachment) {
                        attachment.flip();
                        String response = StandardCharsets.UTF_8.decode(attachment).toString();
                        System.out.println("bytes read: " + bytes + ", response: " + response);
                      }

                      @Override
                      public void failed(Throwable exc, ByteBuffer attachment) {
                      }
                    });
                  }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer attachment) {
                }
              });
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

      @Override
      public void failed(Throwable exc, Object attachment) {
      }
    });
    Thread.sleep(Long.MAX_VALUE);
  }
}
