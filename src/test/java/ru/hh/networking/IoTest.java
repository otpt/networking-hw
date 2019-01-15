package ru.hh.networking;

import org.testng.annotations.Test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

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
              // TODO: 1 line removed
              outputStream.writeUTF(data + data);
              outputStream.flush();
            }
          } catch (IOException e) {
            System.out.println("exception:" + e.getMessage());
          }
        }
      } catch (IOException e) { // TODO: line can be modified
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

      // TODO: implement successfull client request

      assertEquals(answer, "testtest");
      socket.close();

      // client 2 timeout exception
      socket = new Socket("localhost", port);
      // TODO: 1 line removed
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
