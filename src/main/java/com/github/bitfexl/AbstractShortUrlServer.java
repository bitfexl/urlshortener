package com.github.bitfexl;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.Deque;

public abstract class AbstractShortUrlServer implements Closeable {
    private final String baseUrl;

    private final Undertow httpServer;

    private byte[] indexHTML;

    public AbstractShortUrlServer(int port, String baseUrl) {
        this.baseUrl = baseUrl;

        try (final InputStream in = getClass().getResourceAsStream("/index.html")) {
            indexHTML = in.readAllBytes();
        } catch (Exception ex) {
            indexHTML = null;
        }

        httpServer = Undertow.builder()
                .addHttpListener(port, "localhost")
                .setHandler(this::handleRequest)
                .build();

        httpServer.start();
    }

    @Override
    public void close() throws IOException {
        httpServer.stop();
    }

    private void handleRequest(HttpServerExchange exchange) {
        final String path = exchange.getRequestPath();

        // --- index html file ---
        if (indexHTML != null && path.equalsIgnoreCase("/")) {
            exchange.getResponseSender().send(ByteBuffer.wrap(indexHTML));
            return;
        }

        // --- debug ping/server health check ---

        if (path.equalsIgnoreCase("/ping")) {
            exchange.getResponseSender().send("pong " + Instant.now());
            return;
        }

        // --- store/shorten long url ---

        if (path.equalsIgnoreCase("/short")) {
            final Deque<String> urlParams = exchange.getQueryParameters().get("url");

            if (urlParams == null) {
                exchange.setStatusCode(400);
                exchange.getResponseSender().send("Mandatory 'url' parameter is missing.");
                return;
            }

            final String url = urlParams.getFirst();

            final Deque<String> caseInsensitive = exchange.getQueryParameters().get("caseInsensitive");

            // check if url is accepted
            if (!checkUrl(url)) {
                exchange.setStatusCode(400);
                exchange.getResponseSender().close();
                return;
            }

            final String newKey = storeUrl(url, caseInsensitive != null);

            exchange.getResponseSender().send(baseUrl + newKey);
            return;
        }

        // --- retrieve url and redirect if possible or respond with not found ---

        final String key = exchange.getRequestPath().substring(1);

        final String value = retrieveUrl(key);

        if (value == null) {
            exchange.setStatusCode(404);
            exchange.getResponseSender().close();
            return;
        }

        exchange.getResponseHeaders().add(Headers.LOCATION, value);
        exchange.setStatusCode(303);
        exchange.getResponseSender().close();
    }

    protected abstract String storeUrl(String url, boolean caseInsensitive);

    protected abstract String retrieveUrl(String key);

    protected abstract boolean checkUrl(String url);
}
