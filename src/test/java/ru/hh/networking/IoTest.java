package ru.hh.networking;

import org.testng.annotations.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertEquals;

public class IoTest {
  @Test(expectedExceptions = SocketTimeoutException.class)
  public void soTimeoutTest() throws IOException, InterruptedException {
    int port = 5656;
    ServerSocket serverSocket = new ServerSocket();
    Thread serverThread = new Thread(() -> {
      try {
        serverSocket.bind(new InetSocketAddress(port));
        while (!serverSocket.isClosed()) {
          try (Socket socket = serverSocket.accept()) {
            DataInputStream inputStream = new DataInputStream(socket.getInputStream());
            DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
            while (!socket.isInputShutdown()) {
              String data = inputStream.readUTF();
              TimeUnit.SECONDS.sleep(2);
              outputStream.writeUTF(data + data);
              outputStream.flush();
            }
          } catch (IOException e) {
            System.out.println("exception:" + e.getMessage());
          }
        }
      } catch (IOException | InterruptedException e) {
        e.printStackTrace();
      }
    });

    Socket socket = null;
    try {
      serverThread.start();
      DataOutputStream out;
      DataInputStream in;
      String answer = "";

      // client 1 success request

      socket = new Socket("localhost", port);
      out = new DataOutputStream(socket.getOutputStream());
      in = new DataInputStream(socket.getInputStream());
      out.writeUTF("test");
      answer = in.readUTF();

      assertEquals(answer, "testtest");
      socket.close();

      // client 2 timeout exception
      socket = new Socket("localhost", port);
      socket.setSoTimeout(1000);
      out = new DataOutputStream(socket.getOutputStream());
      in = new DataInputStream(socket.getInputStream());
      out.writeUTF("test");
      in.readUTF();
    } finally {
      socket.close();
      serverSocket.close();
      serverThread.join();
    }
  }
}
