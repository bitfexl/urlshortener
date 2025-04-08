package com.github.bitfexl;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.util.Deque;

public class Main {
    final static KeyGenerator keyGenerator = new KeyGenerator(7);

    final static Db db = new Db("db", "db-log");

    final static String URL_BASE = "http://localhost/";

    public static void main(String[] args) throws IOException {
//        try (db) {
//        }

        final Undertow httpServer = Undertow.builder()
                .addHttpListener(80, "localhost")
                .setHandler(new HttpHandler() {
                    private static final HttpString LOCATION_HEADER = HttpString.tryFromString("Location");

                    @Override
                    public void handleRequest(HttpServerExchange exchange) throws Exception {
                        final String path = exchange.getRequestPath();

                        if (path.equalsIgnoreCase("/ping")) {
                            exchange.getResponseSender().send("pong");
                            return;
                        }

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

                            final String newKey = writeValue(url, caseInsensitive != null);

                            exchange.getResponseSender().send(URL_BASE + newKey);
                            return;
                        }

                        String key = exchange.getRequestPath().substring(1);

                        if (key.length() != keyGenerator.length) {
                            exchange.setStatusCode(404);
                            exchange.getResponseSender().close();
                            return;
                        }

                        key = keyGenerator.normalizeKey(key);

                        final String value = db.read(key);

                        if (value == null) {
                            exchange.setStatusCode(404);
                            exchange.getResponseSender().close();
                            return;
                        }

                        exchange.getResponseHeaders().add(LOCATION_HEADER, value);
                        exchange.setStatusCode(303);
                        exchange.getResponseSender().close();
                    }
                }).build();
        httpServer.start();
    }

    public static String writeValue(String value, boolean caseInsensitive) throws RocksDBException {
        String key;
        boolean success;

        do {
            key = caseInsensitive ? keyGenerator.generateInsensitiveKey() : keyGenerator.generateKey();
            success = db.writeIfNotPresent(key, value);
        } while (!success);

        return key;
    }
}