package com.github.bitfexl;

import org.rocksdb.RocksDBException;

import java.io.IOException;

public class Main {
    final static KeyGenerator keyGenerator = new KeyGenerator(7);

    final static Db db = new Db("db", "db-log");

    final static String URL_BASE = "http://localhost/";

    public static void main(String[] args) throws IOException {
        final AbstractShortUrlServer server = new AbstractShortUrlServer(80, URL_BASE) {

            @Override
            protected String storeUrl(String url, boolean caseInsensitive) {
                try {
                    return writeValue(url, caseInsensitive);
                } catch (RocksDBException ex) {
                    throw new RuntimeException(ex);
                }
            }

            @Override
            protected String retrieveUrl(String key) {
                if (key.length() != keyGenerator.length) {
                    return null;
                }

                key = keyGenerator.normalizeKey(key);

                try {
                    return db.read(key);
                } catch (RocksDBException ex) {
                    ex.printStackTrace(System.err);
                    return null;
                }
            }

            @Override
            protected boolean checkUrl(String url) {
                return true;
            }
        };

        try (server; db) {
            System.out.println("Server started.\nPres ENTER key to stop...");
            System.in.read();
        }
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