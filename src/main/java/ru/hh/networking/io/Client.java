package ru.hh.networking.io;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class Client {
  public static void main(String[] args) {
    String host = "localhost";
    int port = 5656;
    try (Socket socket = new Socket(host, port);
         BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
    ) {
      System.out.println("BIO client connected to " + host + ':' + port);
      DataOutputStream outputStream = new DataOutputStream(socket.getOutputStream());
      DataInputStream inputStream = new DataInputStream(socket.getInputStream());
      while (!socket.isOutputShutdown()) {
        if (br.ready()) {

          String clientCommand = br.readLine();

          outputStream.writeUTF(clientCommand);
          outputStream.flush();

          String serverAnswer = inputStream.readUTF();
          System.out.println(serverAnswer);
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
