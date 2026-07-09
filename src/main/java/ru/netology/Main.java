package ru.netology;

import java.time.LocalDateTime;

public class Main {
  public static void main(String[] args) {
    Server server = new Server(9999);

    server.addHandler("GET", "/classic.html", (request, out) -> {
      String time = LocalDateTime.now().toString();
      String param = request.getQueryParam("msg");
      if (param != null) {
        time = param + " at " + time;
      }
      String content = "<html><body><h1>Classic Demo</h1><p>Current time: " + time + "</p></body></html>";
      byte[] bytes = content.getBytes();
      out.write("HTTP/1.1 200 OK\r\n".getBytes());
      out.write("Content-Type: text/html\r\n".getBytes());
      out.write(("Content-Length: " + bytes.length + "\r\n").getBytes());
      out.write("Connection: close\r\n".getBytes());
      out.write("\r\n".getBytes());
      out.write(bytes);
      out.flush();
    });

    server.addHandler("GET", "/messages", (request, out) -> {
      String lastParam = request.getQueryParam("last");
      int last = 10;
      if (lastParam != null) {
        try {
          last = Integer.parseInt(lastParam);
        } catch (NumberFormatException ignored) {}
      }
      String response = "Showing last " + last + " messages";
      byte[] bytes = response.getBytes();
      out.write("HTTP/1.1 200 OK\r\n".getBytes());
      out.write("Content-Type: text/plain\r\n".getBytes());
      out.write(("Content-Length: " + bytes.length + "\r\n").getBytes());
      out.write("Connection: close\r\n".getBytes());
      out.write("\r\n".getBytes());
      out.write(bytes);
      out.flush();
    });

    server.addHandler("POST", "/messages", (request, out) -> {
      String response = "Received POST to /messages with body: " + request.getBody();
      byte[] bytes = response.getBytes();
      out.write("HTTP/1.1 200 OK\r\n".getBytes());
      out.write("Content-Type: text/plain\r\n".getBytes());
      out.write(("Content-Length: " + bytes.length + "\r\n").getBytes());
      out.write("Connection: close\r\n".getBytes());
      out.write("\r\n".getBytes());
      out.write(bytes);
      out.flush();
    });

    server.listen();
  }
}