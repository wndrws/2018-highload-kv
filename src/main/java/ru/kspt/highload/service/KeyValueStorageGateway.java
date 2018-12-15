package ru.kspt.highload.service;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

public class KeyValueStorageGateway {
    private final KeyValueStorageService localService;

    private final List<Replica> replicas;

    private final ReplicaResolver resolver;

    public KeyValueStorageGateway(KeyValueStorageService service, final List<Replica> replicas) {
        this.localService = service;
        this.replicas = replicas;
        this.resolver = new ReplicaResolver(replicas);
    }

    public byte[] getEntity(final byte[] keyBytes, final ReplicationFactor rf)
    throws NoSuchElementException, IOException {
        if (rf.from == 1) {
            return localService.getEntity(keyBytes);
        } else throw new RuntimeException("NOT IMPLEMENTED");
    }

    public void putEntity(final byte[] keyBytes, final byte[] entity, final ReplicationFactor rf)
    throws Exception {
        if (rf.from == 1) {
            localService.putEntity(keyBytes, entity);
        } else throw new RuntimeException("NOT IMPLEMENTED");
    }

    public void deleteEntity(final byte[] keyBytes, final ReplicationFactor rf) throws Exception {
        if (rf.from == 1) {
            localService.deleteEntity(keyBytes);
        } else throw new RuntimeException("NOT IMPLEMENTED");
    }
}
