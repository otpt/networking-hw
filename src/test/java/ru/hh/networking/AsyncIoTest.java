package ru.hh.networking;

import org.testng.annotations.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.InterruptedByTimeoutException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@SuppressWarnings({"Duplicates", "AnonymousInnerClassMayBeStatic", "AnonymousInnerClassWithTooManyMethods"})
public class AsyncIoTest {
  private static final int SINGLE_TASK_DURATION = 2000;

  private final AtomicReference<Throwable> throwableHolder = new AtomicReference<>();

  @Test
  public void ddosAsyncServer() throws IOException, InterruptedException {
    int port = 5656;
    AtomicInteger idGenerator = new AtomicInteger();
    AtomicInteger successRequests = new AtomicInteger();

    int nThreads = 10;
    AsynchronousChannelGroup group = AsynchronousChannelGroup.withFixedThreadPool(nThreads, new ThreadFactory() {
      @Override
      public Thread newThread(Runnable r) {
        return new Thread(Thread.currentThread().getThreadGroup(), r, "async-worker-" + idGenerator.incrementAndGet());
      }
    });
    AsynchronousServerSocketChannel assc = AsynchronousServerSocketChannel.open(group);
    InetSocketAddress address = new InetSocketAddress(port);
    assc.bind(address);
    System.out.println("Async IO server started on port " + address.getPort());

    assc.accept(null, new CompletionHandler<>() {
      @Override
      public void completed(AsynchronousSocketChannel socketChannel, Object attachment) {
        System.out.println("Thread " + Thread.currentThread().getName() + " got connection from " + socketChannel);
        assc.accept(null, this);
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        socketChannel.read(buffer, buffer, new CompletionHandler<>() {
          @Override
          public void completed(Integer bytes, ByteBuffer attachment) {
            try {
              if (bytes < 0) {
                socketChannel.close();
                return;
              }
              System.out.println("Thread " + Thread.currentThread().getName() + " read " + bytes + " from " + socketChannel);

              // TODO: get task id from incoming data and set it to the following variable
              int taskId = -12345; // TODO: should be fixed

              String prefix = "Thread " + Thread.currentThread().getName();
              System.out.println(prefix + " is processing task " + taskId + " for " + SINGLE_TASK_DURATION + " millis");

              // TODO: mimic some business
              // and increment success requests counter

              System.out.println(prefix + " finished task " + taskId);
            } catch (IOException e) { // TODO: line can be modified
              e.printStackTrace();
            }
            CompletionHandler<Integer, ByteBuffer> readCompletionHandler = this;
            // TODO: implement writing some data to client,
            //  since it's waiting for it at line 122 in order to count down the latch
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


    CountDownLatch successLatch = new CountDownLatch(nThreads);
    CountDownLatch failedLatch = new CountDownLatch(1);
    for (int i = 0; i < nThreads + 1; i++) {
      asyncRequest(i, successLatch, failedLatch);
    }
    assertTrue(successLatch.await(15, TimeUnit.SECONDS));
    assertTrue(failedLatch.await(15, TimeUnit.SECONDS));
    assertTrue(throwableHolder.get() instanceof InterruptedByTimeoutException);
    assertEquals(successRequests.get(), nThreads);
    assc.close();
    group.shutdown();
  }

  private void asyncRequest(int taskId, CountDownLatch successLatch, CountDownLatch failedLatch) {
    ByteBuffer clientBuffer = ByteBuffer.allocate(1024);
    InetSocketAddress address = new InetSocketAddress("localhost", 5656);
    try {
      AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
      client.connect(address, null, new CompletionHandler<Void, Object>() {
        @Override
        public void completed(Void result, Object attachment) {
          System.out.println("AIO client connected to " + address);
          ByteBuffer buffer = ByteBuffer.allocate(1024);
          buffer.asIntBuffer().put(taskId);
          client.write(buffer, buffer, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer result, ByteBuffer attachment) {
              if (attachment.hasRemaining()) {
                client.write(attachment, attachment, this);
              } else {
                attachment.clear();
                // TODO: modify following line
                client.read(attachment, attachment, new CompletionHandler<Integer, ByteBuffer>() {
                  @Override
                  public void completed(Integer result, ByteBuffer attachment) {
                    successLatch.countDown();
                    try {
                      client.close();
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                  }

                  @Override
                  public void failed(Throwable exc, ByteBuffer attachment) {
                    System.out.println("Client thread " + Thread.currentThread().getName() + " failed to request server: " + exc);
                    try {
                      client.close();
                    } catch (IOException e) {
                      e.printStackTrace();
                    }
                    throwableHolder.set(exc);
                    failedLatch.countDown();
                  }
                });
              }
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
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
