package ru.hh.networking;

import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.ClientStats;
import org.asynchttpclient.ListenableFuture;
import org.asynchttpclient.Response;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.asynchttpclient.Dsl.asyncHttpClient;
import static org.asynchttpclient.Dsl.config;
import static org.testng.Assert.assertEquals;

public class AsyncHttpClientAndJettyTest {
  @Test(timeOut = 20000)
  public void keepAliveTest() throws Exception {
    Server server = new Server();
    ServerConnector connector = new ServerConnector(server);
    server.addConnector(connector);
    server.setHandler(new EchoHandler());
    server.start();
    int port = connector.getLocalPort();

    try (final AsyncHttpClient client = asyncHttpClient(
        config()
            // TODO: 2 lines
            //     removed
    )) {
      ClientStats stats = client.getClientStats();
      assertEquals(stats.getTotalActiveConnectionCount(), 0);
      assertEquals(stats.getTotalIdleConnectionCount(), 0);
      assertEquals(stats.getTotalConnectionCount(), 0);

      String url = "http://localhost:" + port + "/foo/test";
      List<ListenableFuture<Response>> futures =
          Stream.generate(() -> client.prepareGet(url).setHeader("LockThread", "3").execute())
              .limit(3)
              .collect(Collectors.toList());

      Thread.sleep(1000);

      stats = client.getClientStats();
      assertEquals(stats.getTotalActiveConnectionCount(), 3);
      assertEquals(stats.getTotalIdleConnectionCount(), 0);
      assertEquals(stats.getTotalConnectionCount(), 3);

      futures.forEach(f -> f.toCompletableFuture().join());

      ListenableFuture<Response> future = client.prepareGet(url).setHeader("LockThread", "3").execute();

      Thread.sleep(1000);

      stats = client.getClientStats();
      assertEquals(stats.getTotalActiveConnectionCount(), 1);
      assertEquals(stats.getTotalIdleConnectionCount(), 2);
      assertEquals(stats.getTotalConnectionCount(), 3);

      future.toCompletableFuture().join();

      stats = client.getClientStats();
      assertEquals(stats.getTotalActiveConnectionCount(), 0);
      assertEquals(stats.getTotalIdleConnectionCount(), 3);
      assertEquals(stats.getTotalConnectionCount(), 3);

      Thread.sleep(5000);

      stats = client.getClientStats();
      assertEquals(stats.getTotalActiveConnectionCount(), 0);
      assertEquals(stats.getTotalIdleConnectionCount(), 0);
      assertEquals(stats.getTotalConnectionCount(), 0);
    } finally {
      server.stop();
      server.join();
    }
  }
}
