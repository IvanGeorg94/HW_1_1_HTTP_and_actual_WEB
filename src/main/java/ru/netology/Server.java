package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    private final ExecutorService threadPool;
    private final Map<String, Handler> handlers;
    private final List<String> validPaths = List.of(
            "/index.html", "/spring.svg", "/spring.png",
            "/resources.html", "/styles.css", "/app.js",
            "/links.html", "/forms.html", "/classic.html",
            "/events.html", "/events.js"
    );

    public Server(int port) {
        this.port = port;
        this.threadPool = Executors.newFixedThreadPool(64);
        this.handlers = new ConcurrentHashMap<>();
    }

    public void addHandler(String method, String path, Handler handler) {
        handlers.put(method + " " + path, handler);
    }

    public void listen() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                threadPool.submit(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            threadPool.shutdown();
        }
    }

    private void handleConnection(Socket socket) {
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())) {

            String requestLine = in.readLine();
            if (requestLine == null || requestLine.isEmpty()) return;

            String[] parts = requestLine.split(" ");
            if (parts.length != 3) {
                sendError(out, 400, "Bad Request");
                return;
            }

            String method = parts[0];
            String fullPath = parts[1];

            // Отделяем путь от query-строки
            String pathWithoutQuery;
            String queryString = null;
            int qIdx = fullPath.indexOf('?');
            if (qIdx != -1) {
                pathWithoutQuery = fullPath.substring(0, qIdx);
                queryString = fullPath.substring(qIdx + 1);
            } else {
                pathWithoutQuery = fullPath;
            }

            // Читаем заголовки
            Map<String, String> headers = new HashMap<>();
            String headerLine;
            while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                int colonIdx = headerLine.indexOf(':');
                if (colonIdx > 0) {
                    String key = headerLine.substring(0, colonIdx).trim();
                    String value = headerLine.substring(colonIdx + 1).trim();
                    headers.put(key, value);
                }
            }

            // Парсим параметры через URLEncodedUtils
            Map<String, List<String>> queryParams = new HashMap<>();
            if (queryString != null && !queryString.isEmpty()) {
                List<org.apache.http.NameValuePair> pairs =
                        org.apache.http.client.utils.URLEncodedUtils.parse(queryString, java.nio.charset.StandardCharsets.UTF_8);
                for (org.apache.http.NameValuePair pair : pairs) {
                    queryParams.computeIfAbsent(pair.getName(), k -> new ArrayList<>()).add(pair.getValue());
                }
            }

            Request request = new Request(method, pathWithoutQuery, headers, "", queryParams);

            // Ищем хендлер по методу и пути без параметров
            Handler handler = handlers.get(method + " " + pathWithoutQuery);
            if (handler != null) {
                handler.handle(request, out);
                return;
            }

            // Статика
            serveStatic(pathWithoutQuery, out);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void serveStatic(String path, BufferedOutputStream out) throws IOException {
        if (!validPaths.contains(path)) {
            sendError(out, 404, "Not Found");
            return;
        }

        Path filePath = Path.of(".", "public", path);
        String mimeType = Files.probeContentType(filePath);

        if (path.equals("/classic.html")) {
            String template = Files.readString(filePath);
            byte[] content = template.replace("{time}", LocalDateTime.now().toString()).getBytes();
            sendResponse(out, 200, mimeType, content);
            return;
        }

        byte[] content = Files.readAllBytes(filePath);
        sendResponse(out, 200, mimeType, content);
    }

    private void sendResponse(BufferedOutputStream out, int statusCode, String mimeType, byte[] content) throws IOException {
        String statusLine;
        switch (statusCode) {
            case 200: statusLine = "HTTP/1.1 200 OK"; break;
            case 404: statusLine = "HTTP/1.1 404 Not Found"; break;
            default: statusLine = "HTTP/1.1 " + statusCode + " Unknown"; break;
        }
        out.write((statusLine + "\r\n").getBytes());
        out.write(("Content-Type: " + mimeType + "\r\n").getBytes());
        out.write(("Content-Length: " + content.length + "\r\n").getBytes());
        out.write("Connection: close\r\n".getBytes());
        out.write("\r\n".getBytes());
        out.write(content);
        out.flush();
    }

    private void sendError(BufferedOutputStream out, int code, String message) throws IOException {
        String body = "<h1>" + code + " " + message + "</h1>";
        byte[] content = body.getBytes();
        out.write(("HTTP/1.1 " + code + " " + message + "\r\n").getBytes());
        out.write("Content-Type: text/html\r\n".getBytes());
        out.write(("Content-Length: " + content.length + "\r\n").getBytes());
        out.write("Connection: close\r\n".getBytes());
        out.write("\r\n".getBytes());
        out.write(content);
        out.flush();
    }
}