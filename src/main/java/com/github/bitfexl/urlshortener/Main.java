package com.github.bitfexl.urlshortener;

import org.rocksdb.RocksDBException;

import java.io.IOException;
import java.util.*;

public class Main {
    private static final KeyGenerator keyGenerator = new KeyGenerator(7);

    private static final Db db = new Db("db", "db-log");

    private static final String BASE_URL = System.getenv("BASEURL");

    private static final String DOMAINS = System.getenv("DOMAINS");

    private static final boolean allDomains;

    private static final Set<String> domains;

    private static final List<String> parentDomains;

    static {
        if (DOMAINS == null) {
            allDomains = true;
            domains = null;
            parentDomains = null;
        } else {
            allDomains = false;
            domains = new HashSet<>();
            parentDomains = new ArrayList<>();

            for (String domain : DOMAINS.split(",")) {
                domain = domain.trim().toLowerCase();
                if (domain.isEmpty()) {
                    continue;
                }

                if (domain.charAt(0) == '.') {
                    parentDomains.add(domain);
                    domains.add(domain.substring(1));
                } else {
                    domains.add(domain);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        final AbstractShortUrlServer server = new AbstractShortUrlServer(80, BASE_URL != null ? BASE_URL : "") {

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
                url = url.toLowerCase();

                if (url.startsWith("https://")) {
                    url = url.substring(8);
                } else if (url.startsWith("http://")) {
                    url = url.substring(7);
                } else {
                    return false;
                }

                if (allDomains) {
                    return true;
                }

                final String domain = url.split("/", 2)[0];

                if (domains.contains(domain)) {
                    return true;
                }

                for (String parentDomain : parentDomains) {
                    if (domain.endsWith(parentDomain)) {
                        return true;
                    }
                }

                return false;
            }
        };

        try (server; db) {
            System.out.println("Server started.\nPres ENTER key to stop...");
            if (System.in.read() == -1) {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException ignored) { }
            }
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