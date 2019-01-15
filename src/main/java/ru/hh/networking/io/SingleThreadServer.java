package ru.hh.networking.io;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class SingleThreadServer {
  public static void main(String[] args) throws IOException {
    int port = 5656;
    ServerSocket serverSocket = new ServerSocket(port);
    System.out.println("Blocking IO 1-threaded server started on port " + serverSocket.getLocalPort());
    while (true) {
      try (Socket socket = serverSocket.accept()) {
        System.out.println("Accept connection");
        DataInputStream inputStream = new DataInputStream(socket.getInputStream());
        DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
        while (!socket.isInputShutdown()) {
          String data = inputStream.readUTF();
          System.out.println(data);
          outputStream.writeUTF("echo: " + data);
          outputStream.flush();
        }
      } catch (IOException e) {
        System.out.println("exception:" + e.getMessage());
      }
    }
  }
}
