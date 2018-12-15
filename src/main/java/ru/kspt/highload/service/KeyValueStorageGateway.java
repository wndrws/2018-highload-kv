package ru.kspt.highload.service;

import java.util.List;
import java.util.NoSuchElementException;

public class KeyValueStorageGateway {
    private final List<Replica> replicas;

    private final ReplicaResolver resolver;

    public KeyValueStorageGateway(List<Replica> replicas) {
        this.replicas = replicas;
        this.resolver = new ReplicaResolver(replicas);
    }

    public byte[] getEntity(final byte[] keyBytes, final Replica replica)
    throws NoSuchElementException {
        // TODO
        throw new RuntimeException("NOT IMPLEMENTED");
    }

    public void putEntity(final byte[] keyBytes, final byte[] entity, final Replica replica) {
        // TODO
        throw new RuntimeException("NOT IMPLEMENTED");
    }

    public void deleteEntity(final byte[] keyBytes, final Replica replica) {
        // TODO
        throw new RuntimeException("NOT IMPLEMENTED");
    }
}
