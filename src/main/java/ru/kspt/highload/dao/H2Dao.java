package ru.kspt.highload.dao;

import org.jetbrains.annotations.NotNull;
import ru.mail.polis.KVDao;

import java.io.File;
import java.util.NoSuchElementException;

public class H2Dao implements KVDao {
    private final H2Bridge h2Bridge;

    public H2Dao(final File dataDirectory) {
        final String path = dataDirectory.getAbsolutePath();
        h2Bridge = new H2Bridge(path);
    }

    @NotNull
    @Override
    public byte[] get(@NotNull byte[] keyBytes) throws NoSuchElementException {
        final Value value = getValue(keyBytes);
        if (value.isDeleted) {
            throw new NoSuchElementException();
        } else {
            return value.bytes;
        }
    }

    @NotNull
    public Value getValue(@NotNull byte[] keyBytes) throws NoSuchElementException {
        return h2Bridge.get(new Key(keyBytes));
    }

    @Override
    public void upsert(@NotNull byte[] keyBytes, @NotNull byte[] value) {
        final Key key = new Key(keyBytes);
        if (h2Bridge.contains(key)) {
            h2Bridge.update(key, value);
        } else {
            h2Bridge.insert(key, value);
        }
    }

    @Override
    public void remove(@NotNull byte[] keyBytes) {
        final Key key = new Key(keyBytes);
        h2Bridge.remove(key);
    }

    @Override
    public void close() {
        h2Bridge.closeConnection();
    }
}
