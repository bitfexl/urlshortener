package com.github.bitfexl;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.io.Closeable;
import java.io.IOException;
import java.util.Deque;

public abstract class AbstractShortUrlServer implements Closeable {
    private final String baseUrl;

    private final Undertow httpServer;

    public AbstractShortUrlServer(int port, String baseUrl) {
        this.baseUrl = baseUrl;

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

        // --- debug ping/server health check ---

        if (path.equalsIgnoreCase("/ping")) {
            exchange.getResponseSender().send("pong");
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

            // todo: check domain

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
}
