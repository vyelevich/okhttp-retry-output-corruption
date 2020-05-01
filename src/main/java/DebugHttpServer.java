import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class DebugHttpServer {

    private static final Logger logger = LoggerFactory.getLogger(DebugHttpServer.class);

    AtomicReference<String> lastRequestBodyHolder = new AtomicReference<>();

    private final HttpServer server;

    public DebugHttpServer(String host, int port, String path) {
        try {
            this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
            this.server.createContext(path, new DebugHttpHandler(lastRequestBodyHolder::set));
            this.server.setExecutor(Executors.newFixedThreadPool(10));
        } catch (IOException e) {
            throw new RuntimeException("Boom!", e);
        }
    }

    public DebugHttpServer run() {
        server.start();
        return this;
    }

    public String getLastRequestBody() {
        return lastRequestBodyHolder.get();
    }

    private static class DebugHttpHandler implements HttpHandler {

        private final Consumer<String> lastBodyUpdater;

        public DebugHttpHandler(Consumer<String> lastBodyUpdater) {
            this.lastBodyUpdater = lastBodyUpdater;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            logger.info("Method: {}", exchange.getRequestMethod());
            logger.info("Headers: {}", exchange.getRequestHeaders());

            byte[] readBuffer = new byte[4 * 1024];

            InputStream input = exchange.getRequestBody();

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            long count = 0;
            int n;
            while (-1 != (n = input.read(readBuffer))) {
                out.write(readBuffer, 0, n);
                count += n;
            }

            String requestBody = out.toString();
            logger.info("Body size: {}, body: {}", count, requestBody);
            lastBodyUpdater.accept(requestBody);

            String response = "OK!!!";
            OutputStream outputStream = exchange.getResponseBody();
            exchange.sendResponseHeaders(200, response.length());
            outputStream.write(response.getBytes());
            outputStream.flush();
            outputStream.close();
        }
    }
}
