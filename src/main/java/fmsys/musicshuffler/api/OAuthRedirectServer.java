package fmsys.musicshuffler.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class OAuthRedirectServer {
    private static final String CALLBACK_TEMPLATE = "/oauth-callback.html";

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
            String query = requestURI.getQuery();

            String code = getQueryParameter(query, "code");
            String response;
            if (code != null && state.equals(getQueryParameter(query, "state"))) {
                response = renderCallbackPage(
                        "Authorization successful",
                        "Authorization successful",
                        true
                );
            } else {
                response = renderCallbackPage(
                        "Authorization failed",
                        "Authorization failed or canceled",
                        false
                );
                code = null;
            }

            byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }

            if (code != null) {
                callback.accept(code);
            }

            server.stop(0);

        }

        private static String renderCallbackPage(String title, String message, boolean autoClose) throws IOException {
            String template = readResource();
            String script = autoClose ? """
                    <script>
                      const message = document.getElementById("message");
                      let count = 10;

                      const interval = setInterval(() => {
                        count -= 1;
                        if (count > 0) {
                          message.textContent = String(count) + " seconds until this window closes automatically...";
                        } else {
                          message.textContent = "";
                          clearInterval(interval);
                          window.close();
                        }
                      }, 1000);
                    </script>
                    """ : "";

            return template
                    .replace("{{TITLE}}", title)
                    .replace("{{MESSAGE}}", message)
                    .replace("{{AUTO_CLOSE_SCRIPT}}", script);
        }

        private static String readResource() throws IOException {
            try (InputStream inputStream = OAuthRedirectServer.class.getResourceAsStream(CALLBACK_TEMPLATE)) {
                if (inputStream == null) {
                    throw new IOException("Resource not found: " + CALLBACK_TEMPLATE);
                }
                return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
        }

        private static String getQueryParameter(String query, String name) {
            if (query == null || query.isBlank()) {
                return null;
            }

            for (String pair : query.split("&")) {
                int equalsIndex = pair.indexOf('=');
                String key = equalsIndex >= 0 ? pair.substring(0, equalsIndex) : pair;
                if (name.equals(URLDecoder.decode(key, StandardCharsets.UTF_8))) {
                    String value = equalsIndex >= 0 ? pair.substring(equalsIndex + 1) : "";
                    return URLDecoder.decode(value, StandardCharsets.UTF_8);
                }
            }

            return null;
        }
        }
}
