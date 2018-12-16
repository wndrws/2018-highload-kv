package ru.kspt.highload.service;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import ru.kspt.highload.DeletedEntityException;
import ru.kspt.highload.dao.H2Dao;
import ru.kspt.highload.dao.Value;
import ru.kspt.highload.rest.KeyValueStorageController;
import ru.mail.polis.KVDao;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class KeyValueStorageService implements KVService {
    private final KVDao storage;

    private final KeyValueStorageController controller;

    private final int localPort;

    public KeyValueStorageService(final int port, final KVDao storage, final Set<String> topology)
    throws IOException {
        this.localPort = port;
        this.storage = storage;
        this.controller = new KeyValueStorageController(this, port, parseTopology(topology));
    }

    private List<Replica> parseTopology(final Set<String> topology) {
        return topology.stream()
                .map(KeyValueStorageService::urlWithoutSchema)
                .map(Replica::create)
                .collect(Collectors.toList());
    }

    private static String urlWithoutSchema(final String url) {
        final String[] parts = url.split("://");
        return parts.length == 1 ? parts[0] : parts[1];
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

    @Nullable
    byte[] getEntity(final byte[] keyBytes) throws IOException, NoSuchElementException,
            DeletedEntityException {
        if (storage instanceof H2Dao) {
            final Value value = ((H2Dao) storage).getValue(keyBytes);
            if (value.isDeleted) throw new DeletedEntityException();
            return value.bytes;
        } else {
            log.warn("Deletion detection is not available since not H2Dao is used");
            return storage.get(keyBytes);
        }
    }

    void putEntity(final byte[] keyBytes, final byte[] entity) throws IOException {
        storage.upsert(keyBytes, entity);
    }

    void deleteEntity(final byte[] keyBytes) throws IOException {
        storage.remove(keyBytes);
    }

    boolean isNotSelfReplica(final Replica replica) {
       return replica.port != localPort;
    }
}
