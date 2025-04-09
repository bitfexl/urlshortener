package com.github.bitfexl.urlshortener;

/**
 * Generate a random key (letters only).
 * If the first letter is a 'A' or 'a' then the key is case-sensitive
 * otherwise it is case-insensitive.
 */
public class KeyGenerator {
    private static final char[] chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public final int length;

    public KeyGenerator(int length) {
        this.length = length;
    }

    public String generateKey() {
        final StringBuilder sb = new StringBuilder(length);

        sb.append(Math.random() >= 0.5 ? 'a': 'A');

        for (int i = 1; i < length; i++) {
            sb.append(chars[(int)(Math.random() * 52)]);
        }

        return sb.toString();
    }

    public String generateInsensitiveKey() {
        final StringBuilder sb = new StringBuilder(length);

        sb.append(chars[1 + (int)(Math.random() * 25)]);

        for (int i = 1; i < length; i++) {
            sb.append(chars[(int)(Math.random() * 26)]);
        }

        return sb.toString();
    }

    public String normalizeKey(String key) {
        final char first = key.charAt(0);
        if (first == 'a' || first == 'A') {
            return key;
        }
        return key.toLowerCase();
    }
}
