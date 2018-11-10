package ru.kspt.highload.service;

import lombok.extern.slf4j.Slf4j;
import ru.kspt.highload.rest.KeyValueStorageController;
import ru.mail.polis.KVDao;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.util.NoSuchElementException;

@Slf4j
public class KeyValueStorageService implements KVService {
    private final KVDao storage;

    private final KeyValueStorageController controller;

    public KeyValueStorageService(final int port, final KVDao storage)
    throws IOException {
        this.storage = storage;
        this.controller = new KeyValueStorageController(this, port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        controller.startHttpServer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop() {
        controller.stopHttpServer();
    }

    public byte[] getEntity(final byte[] keyBytes) throws NoSuchElementException, IOException {
        return storage.get(keyBytes);
    }

    public void putEntity(final byte[] keyBytes, final byte[] entity) throws Exception {
        storage.upsert(keyBytes, entity);
    }

    public void deleteEntity(final byte[] keyBytes) throws Exception {
        storage.remove(keyBytes);
    }
}
