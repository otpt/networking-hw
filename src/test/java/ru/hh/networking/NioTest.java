package ru.hh.networking;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.testng.Assert.assertEquals;

public class NioTest {
  @Test
  public void byteBufferTest() {
    ByteBuffer buffer = ByteBuffer.allocate(10);
    IntStream.range(0, 5).forEach(i -> buffer.put((byte) i));
    // TODO: 1 line removed
    assertEquals(buffer.get(), 0);
    assertEquals(buffer.get(), 1);
    assertEquals(buffer.get(), 2);
    // TODO: 1 line removed
    assertEquals(buffer.get(), 3);
    assertEquals(buffer.get(), 4);
    // TODO: 1 line removed
    assertEquals(buffer.get(), 3);
  }

  @Test
  public void donationServer() throws IOException, InterruptedException {
    int port = 5656;
    AtomicInteger acceptedRequests = new AtomicInteger();
    AtomicInteger totalAmount = new AtomicInteger();
    Selector selector = Selector.open();
    ServerSocketChannel ssc = ServerSocketChannel.open();
    Thread donationServerThread = new Thread(() -> {
      try {
        ByteBuffer echoBuffer = ByteBuffer.allocate(1024);
        ssc.configureBlocking(false);
        ServerSocket serverSocket = ssc.socket();
        InetSocketAddress address = new InetSocketAddress(port);
        serverSocket.bind(address);
        ssc.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
          int num = selector.select(5000L);
          if (num <= 0) {
            System.out.println("closing selector");
            selector.close();
            serverSocket.close();
            break;
          }

          Set<SelectionKey> selectedKeys = selector.selectedKeys();
          Iterator<SelectionKey> it = selectedKeys.iterator();

          while (it.hasNext()) {
            SelectionKey key = it.next();
            it.remove();

            if (key.isAcceptable()) {
              // TODO: 1 line removed
              ServerSocketChannel sscAccept = (ServerSocketChannel) key.channel();
              SocketChannel scAccept = sscAccept.accept();
              scAccept.configureBlocking(false);
              scAccept.register(selector, SelectionKey.OP_READ);
              System.out.println("Got connection from " + scAccept);
            } else if (key.isReadable()) {
              SocketChannel sc = (SocketChannel) key.channel();
              sc.configureBlocking(false);

              // TODO: implement reading donate value, adding it the totalAmount
              // and writing "ok" response to the client
            }
          }
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    });

    try {
      donationServerThread.start();
      Thread.sleep(2000L);

      Random random = new Random(12345);
      int expectedSum = 0;
      int expectedRequestsCount = 20 + random.nextInt(20);
      ArrayList<Thread> clientThreads = new ArrayList<>();
      for (int i = 0; i < expectedRequestsCount; i++) {
        int donation = random.nextInt(1_000_000);
        Thread clientThread = new Thread(() -> donate(donation));
        clientThread.start();
        expectedSum += donation;
        clientThreads.add(clientThread);
      }
      for (Thread clientThread : clientThreads) {
        clientThread.join();
      }
      assertEquals(totalAmount.get(), expectedSum);
      assertEquals(acceptedRequests.get(), expectedRequestsCount);
    } finally {
      donationServerThread.join();
    }
  }

  private static void donate(int sum) {
    ByteBuffer clientBuffer = ByteBuffer.allocate(1024);
    InetSocketAddress address = new InetSocketAddress("localhost", 5656);
    try {
      SocketChannel client = SocketChannel.open(address);

      System.out.println("NIO client connected to " + address);
      ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
      requestBuffer.asIntBuffer().put(sum);
      client.write(requestBuffer);

      clientBuffer.clear();
      int bytesRead = client.read(clientBuffer);
      clientBuffer.flip();
      String response = StandardCharsets.UTF_8.decode(clientBuffer).toString();
      assertEquals(response, "ok");
      client.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
