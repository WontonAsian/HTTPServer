import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Map;

public class MyHttpServer {

    private static final String BASE_PATH = "/Users/hoangnguyen/Desktop/INFO 314/HTTPServer/HttpServer";

    private static final Map<String, String> MIME_TYPES = new HashMap<>() {{
        put("txt", "text/plain");
        put("html", "text/html");
        put("json", "text/json");
    }};

    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(8000);

        while (true) {
            Socket socket = serverSocket.accept();
            HttpRequestHandler handler = new HttpRequestHandler(socket);
            handler.start();
        }
    }

  static class HttpRequestHandler extends Thread {
    private final Socket socket;

    public HttpRequestHandler(Socket socket) {
      this.socket = socket;
    }

    public void run() {
      try {
          BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
          DataOutputStream out = new DataOutputStream(socket.getOutputStream());
  
          String requestLine = in.readLine();
          String[] requestLineParts = requestLine.split(" ");
          String requestMethod = requestLineParts[0];
          String requestPath = requestLineParts[1];
          String httpVersion = requestLineParts[2];
  
          if (!httpVersion.equals("HTTP/1.1")) {
              sendErrorResponse(out, 505);
              return;
          }
  
          if (requestMethod.equals("GET")) {
              handleGet(out, requestPath);
          } else if (requestMethod.equals("POST")) {
              handlePost(in, out, requestPath);
          } else if (requestMethod.equals("PUT")) {
              handlePut(in, out, requestPath);
          } else if (requestMethod.equals("DELETE")) {
              handleDelete(out, requestPath);
          } else if (requestMethod.equals("OPTIONS")) {
              handleOptions(out, requestPath);
          } else if (requestMethod.equals("HEAD")) {
              handleHead(out, requestPath);
          } else {
              sendErrorResponse(out, 405);
              return;
          }
  
          in.close();
          out.close();
          socket.close();
      } catch (IOException e) {
          e.printStackTrace();
      }
  }

    private void handlePost(BufferedReader in, DataOutputStream out, String requestPath) throws IOException {
      if (requestPath.endsWith(".txt")) {
          StringBuilder requestBody = new StringBuilder();
  
          // Read the request body after processing request headers
          String line;
          while ((line = in.readLine()) != null && !line.isEmpty()) {
              requestBody.append(line).append("\n");
          }
    
      // Write the request body to the file
      File file = new File(BASE_PATH + requestPath);
      if (!file.exists()) {
        file.createNewFile();
      }
      if (file.exists() && !file.isDirectory()) {
        Files.write(file.toPath(), requestBody.toString().getBytes(), StandardOpenOption.APPEND);
        out.writeBytes("HTTP/1.1 200 OK\r\n");
        out.writeBytes("\r\n");
      } else {
        sendErrorResponse(out, 404);
      }
     }
    }    

    private void handlePut(BufferedReader in, DataOutputStream out, String requestPath) throws IOException {
      if (requestPath.endsWith(".txt")) {
        StringBuilder requestBody = new StringBuilder();
    
        // Read the request body after processing request headers
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
          requestBody.append(line).append("\n");
        }
    
        // Write the request body to the file, creating a new file if it doesn't exist
        File file = new File(BASE_PATH + requestPath);
        if (!file.exists()) {
          file.createNewFile();
        }
    
        if (file.exists() && !file.isDirectory()) {
          Files.write(file.toPath(), requestBody.toString().getBytes());
          out.writeBytes("HTTP/1.1 200 OK\r\n");
          out.writeBytes("\r\n");
        } else {
          sendErrorResponse(out, 404);
        }
      } else {
        sendErrorResponse(out, 415);
      }
    }

    private void handleGet(DataOutputStream out, String requestPath) throws IOException {
      File file = new File(BASE_PATH + requestPath);

      if (file.exists() && !file.isDirectory()) {
        // get the MIME type based on the file extension
        String[] parts = file.getName().split("\\.");
        String extension = parts[parts.length - 1];
        String mimeType = MIME_TYPES.getOrDefault(extension, "application/octet-stream");

        // set the response headers
        out.writeBytes("HTTP/1.1 200 OK\r\n");
        out.writeBytes("Content-Type: " + mimeType + "\r\n");
        out.writeBytes("Content-Length: " + file.length() + "\r\n");
        out.writeBytes("\r\n");

        // write the file contents to the response body
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = fis.read(buffer)) != -1) {
          out.write(buffer, 0, bytesRead);
        }
        fis.close();
      } else {
        sendErrorResponse(out, 404);
      }
    }

    private void handleDelete(DataOutputStream out, String requestPath) throws IOException {
      File file = new File(BASE_PATH + requestPath);

      if (file.exists() && !file.isDirectory()) {
        boolean deleted = file.delete();
        if (deleted) {
          out.writeBytes("HTTP/1.1 200 OK\r\n");
          out.writeBytes("\r\n");
        } else {
          sendErrorResponse(out, 500);
        }
      } else {
        sendErrorResponse(out, 404);
      }
    }

    private void handleOptions(DataOutputStream out, String requestPath) throws IOException {
      File file = new File(BASE_PATH + requestPath);
  
      String allowedMethods = "OPTIONS, GET, HEAD";
      if (file.exists() && !file.isDirectory() && requestPath.endsWith(".txt")) {
          allowedMethods += ", POST, PUT, DELETE";
      }
  
      out.writeBytes("HTTP/1.1 200 OK\r\n");
      out.writeBytes("Allow: " + allowedMethods + "\r\n");
      out.writeBytes("\r\n");
    }
  
    private void handleHead(DataOutputStream out, String requestPath) throws IOException {
      File file = new File(BASE_PATH + requestPath);
    
      if (file.exists() && !file.isDirectory()) {
        String[] parts = file.getName().split("\\.");
        String extension = parts[parts.length - 1];
        String mimeType = MIME_TYPES.getOrDefault(extension, "application/octet-stream");
    
        out.writeBytes("HTTP/1.1 200 OK\r\n");
        out.writeBytes("Content-Type: " + mimeType + "\r\n");
        out.writeBytes("\r\n");
      } else {
        sendErrorResponse(out, 404);
      }
    }
    
    private void sendErrorResponse(DataOutputStream out, int errorCode) throws IOException {
      out.writeBytes("HTTP/1.1 " + errorCode + " " + getErrorMessage(errorCode) + "\r\n");
      out.writeBytes("Content-Type: text/plain\r\n");
      out.writeBytes("\r\n");
      out.writeBytes(getErrorMessage(errorCode));
    }

    private String getErrorMessage(int errorCode) {
      switch (errorCode) {
        case 400:
          return "Bad Request";
        case 404:
          return "Not Found";
        case 405:
          return "Method Not Allowed";
        case 415:
          return "Unsupported Media Type";
        case 500:
          return "Internal Server Error";
        case 505:
          return "HTTP Version Not Supported";
        default:
          return "Internal Server Error";
      }
    }
  }
}

