package com.github.bitfexl.urlshortener;

import org.rocksdb.CompressionOptions;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Db implements Closeable {
    // https://github.com/facebook/rocksdb/wiki/RocksJava-Basics

    private static final Charset charset = StandardCharsets.UTF_8;

    private final RocksDB db;

    static {
        RocksDB.loadLibrary();
    }

    public Db(String dbDir, String logDir) {
        try (final CompressionOptions compressionOptions = new CompressionOptions().setEnabled(false)) {
            try (final Options options = new Options()
                    .setCompressionOptions(compressionOptions)
                    .optimizeForSmallDb()
                    .setCreateIfMissing(true)
                    .setDbLogDir(logDir)
            ) {
                db = RocksDB.open(options, dbDir);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public int getKeyCount() {
        // todo: implement
        return -1;
    }

    /**
     * Write a key and value pair to the db, but only if the key not already exists.
     * @param key The key to add to the database.
     * @param value The associated value.
     * @return true: written successfully, false: key already exists;
     */
    public boolean writeIfNotPresent(String key, String value) throws RocksDBException {
        final byte[] keyBytes = key.getBytes(charset);
        if (db.keyExists(keyBytes)) {
            return false;
        }
        db.put(keyBytes, value.getBytes(charset));
        return true;
    }

    /**
     * Read the value to a key from the database.
     * @param key The key to read.
     * @return The value or null if it does not exist.
     */
    public String read(String key) throws RocksDBException {
        final byte[] value = db.get(key.getBytes(charset));
        if (value == null) {
            return null;
        }
        return new String(value, charset);
    }

    @Override
    public void close() throws IOException {
        db.close();
    }
}
