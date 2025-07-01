package org.example.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.function.Consumer;

public class OAuthRedirectServer {
    public static void run(String state, Consumer<String> callback) throws IOException {

        int port = 1702;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/callback", new OAuthCallbackHandler(server, state, callback));
        server.setExecutor(null);
        server.start();

        System.out.println("OAuth callback server started on http://127.0.0.1:" + port + "/callback");

    }

    private record OAuthCallbackHandler(HttpServer server, String state, Consumer<String> callback) implements HttpHandler {

        @Override
            public void handle(HttpExchange exchange) throws IOException {
                URI requestURI = exchange.getRequestURI();
                String query = requestURI.getQuery(); // Extracts query parameters

                String response;
                String code = null;
                if (query != null && query.contains("code=") && query.contains("state=" + state)) {
                    code = query.split("code=")[1].split("&")[0]; // Extract code parameter
                    response = "<!DOCTYPE html>\n" +
                            "<html>\n" +
                            "<body>\n" +
                            "  <div id=\"message\">Authorization successful! You can now close the browser window.</div>\n" +
                            "  <script>\n" +
                            "    const msg = document.getElementById(\"message\");\n" +
                            "    let count = 5;\n" +
                            "    msg.textContent = `Authorization successful! Closing in ${count}...`;" +
                            "    const interval = setInterval(() => {\n" +
                            "      if (count > 0) {\n" +
                            "        count--;\n" +
                            "        msg.textContent = `Authorization successful! Closing in ${count}...`;\n" +
                            "      } else {\n" +
                            "        clearInterval(interval);\n" +
                            "        window.close();\n" +
                            "      }\n" +
                            "    }, 1000);\n" +
                            "  </script>\n" +
                            "</body>\n" +
                            "</html>\n";
                } else {
                    response = "Authorization failed or canceled.";
                }

                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();

                if (code != null) {
                    callback.accept(code);
                }

                server.stop(0);

            }
        }
}
