package ru.hh.networking;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;

public class EchoHandler extends AbstractHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(EchoHandler.class);

  @Override
  public void handle(String pathInContext, Request request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws IOException, ServletException {

    LOGGER.debug("Echo received request {} on path {}", request, pathInContext);

    if (httpRequest.getHeader("X-HEAD") != null) {
      httpResponse.setContentLength(1);
    }

    if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
      httpResponse.addHeader("Allow", "GET,HEAD,POST,OPTIONS,TRACE");
    }

    Enumeration<String> e = httpRequest.getHeaderNames();
    String headerName;
    while (e.hasMoreElements()) {
      headerName = e.nextElement();
      if (headerName.startsWith("LockThread")) {
        final int sleepTime = httpRequest.getIntHeader(headerName);
        try {
          Thread.sleep(sleepTime == -1 ? 40 : sleepTime * 1000);
        } catch (InterruptedException ex) {
          //
        }
      }

      if (headerName.startsWith("X-redirect")) {
        httpResponse.sendRedirect(httpRequest.getHeader("X-redirect"));
        return;
      }
      httpResponse.addHeader("X-" + headerName, httpRequest.getHeader(headerName));
    }

    String pathInfo = httpRequest.getPathInfo();
    if (pathInfo != null)
      httpResponse.addHeader("X-pathInfo", pathInfo);

    String queryString = httpRequest.getQueryString();
    if (queryString != null)
      httpResponse.addHeader("X-queryString", queryString);

    httpResponse.addHeader("X-KEEP-ALIVE", httpRequest.getRemoteAddr() + ":" + httpRequest.getRemotePort());

    Cookie[] cs = httpRequest.getCookies();
    if (cs != null) {
      for (Cookie c : cs) {
        httpResponse.addCookie(c);
      }
    }

    Enumeration<String> i = httpRequest.getParameterNames();
    if (i.hasMoreElements()) {
      StringBuilder requestBody = new StringBuilder();
      while (i.hasMoreElements()) {
        headerName = i.nextElement();
        httpResponse.addHeader("X-" + headerName, httpRequest.getParameter(headerName));
        requestBody.append(headerName);
        requestBody.append("_");
      }

      if (requestBody.length() > 0) {
        String body = requestBody.toString();
        httpResponse.getOutputStream().write(body.getBytes());
      }
    }

    String requestBodyLength = httpRequest.getHeader("X-" + CONTENT_LENGTH);

    if (requestBodyLength != null) {
      byte[] requestBodyBytes = IOUtils.toByteArray(httpRequest.getInputStream());
      int total = requestBodyBytes.length;

      httpResponse.addIntHeader("X-" + CONTENT_LENGTH, total);
//      String md5 = TestUtils.md5(requestBodyBytes, 0, total);
//      httpResponse.addHeader(CONTENT_MD5.toString(), md5);

      httpResponse.getOutputStream().write(requestBodyBytes, 0, total);
    } else {
      int size = 16384;
      if (httpRequest.getContentLength() > 0) {
        size = httpRequest.getContentLength();
      }
      if (size > 0) {
        int read = 0;
        while (read > -1) {
          byte[] bytes = new byte[size];
          read = httpRequest.getInputStream().read(bytes);
          if (read > 0) {
            httpResponse.getOutputStream().write(bytes, 0, read);
          }
        }
      }
    }

    request.setHandled(true);
    httpResponse.getOutputStream().flush();
    httpResponse.getOutputStream().close();
  }
}
