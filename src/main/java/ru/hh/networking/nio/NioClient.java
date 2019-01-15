package ru.hh.networking.nio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

public class NioClient {
  private static final ByteBuffer buffer = ByteBuffer.allocate(1024);

  public static void main(String[] args) {
    InetSocketAddress address = new InetSocketAddress("localhost", 5656);
    try (SocketChannel client = SocketChannel.open(address);
         BufferedReader br = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8))
    ) {
      System.out.println("NIO client connected to " + address + ", blocking: "+ client.isBlocking());
      while (client.isOpen()) {
        if (br.ready()) {
          String clientCommand = br.readLine();
          buffer.clear();
          buffer.put(clientCommand.getBytes(StandardCharsets.UTF_8));
          buffer.flip();
          client.write(buffer);

          buffer.clear();
          int bytesRead = client.read(buffer);
          buffer.flip();
          String response = StandardCharsets.UTF_8.decode(buffer).toString();
          System.out.println("bytes read: " + bytesRead + ", response: " + response);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
