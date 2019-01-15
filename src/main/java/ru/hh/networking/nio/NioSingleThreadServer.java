package ru.hh.networking.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public class NioSingleThreadServer {
  private static final ByteBuffer echoBuffer = ByteBuffer.allocate(1024);

  public static void main(String[] args) throws IOException {
    int port = 5656;

    Selector selector = Selector.open();

    ServerSocketChannel ssc = ServerSocketChannel.open();
    ssc.configureBlocking(false);

    ServerSocket serverSocket = ssc.socket();
    InetSocketAddress address = new InetSocketAddress(port);
    serverSocket.bind(address);

    ssc.register(selector, SelectionKey.OP_ACCEPT);

    System.out.println("Non-blocking IO server started on port " + serverSocket.getLocalPort());

    while (true) {
      int num = selector.select();

      Set<SelectionKey> selectedKeys = selector.selectedKeys();
      Iterator<SelectionKey> it = selectedKeys.iterator();

      while (it.hasNext()) {
        SelectionKey key = it.next();
        it.remove();
        if (key.isAcceptable()) {
          ServerSocketChannel sscAccept = (ServerSocketChannel) key.channel();
          SocketChannel scAccept = sscAccept.accept();
          scAccept.configureBlocking(false);
          scAccept.register(selector, SelectionKey.OP_READ);
          System.out.println("Got connection from " + scAccept);
        } else if (key.isReadable()) {
          SocketChannel sc = (SocketChannel) key.channel();

          int bytesEchoed = 0;
          while (true) {
            try {
              echoBuffer.clear();
              int r = sc.read(echoBuffer);
              if (r <= 0) {
                if (r < 0) {
                  sc.close();
                }
                break;
              }

              echoBuffer.flip();

              sc.write(echoBuffer);
              bytesEchoed += r;
            } catch (IOException e) {
              System.out.println("closing broken channel: " + sc + ", error: " + e.getMessage());
              sc.close();
              break;
            }
          }

          if (bytesEchoed > 0) {
            System.out.println("Echoed " + bytesEchoed + " from " + sc);
          }
        }
      }
    }
  }
}
