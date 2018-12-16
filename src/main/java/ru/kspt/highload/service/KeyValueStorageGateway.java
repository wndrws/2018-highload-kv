package ru.kspt.highload.service;

import org.jetbrains.annotations.Nullable;
import ru.kspt.highload.DeletedEntityException;
import ru.kspt.highload.NotEnoughReplicasException;
import ru.kspt.highload.dto.PayloadStatus;
import ru.kspt.highload.dto.ReplicaResponse;
import ru.kspt.highload.dto.ResponseStatus;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static ru.kspt.highload.dto.ReplicaResponse.entityFound;
import static ru.kspt.highload.dto.ReplicaResponse.entityNotFound;

public class KeyValueStorageGateway {
    private final KeyValueStorageService localService;

    private final List<Replica> replicas;

    private final ReplicaResolver resolver;

    public KeyValueStorageGateway(final KeyValueStorageService localService,
            final List<Replica> replicas) {
        this.localService = localService;
        this.replicas = replicas;
        this.resolver = new ReplicaResolver(replicas);
    }

    public byte[] getEntity(final String key, final ReplicationFactor rf) throws IOException,
            NoSuchElementException, DeletedEntityException, NotEnoughReplicasException {
        final byte[] localEntity = localService.getEntity(key.getBytes());
        if (rf.from == 1 && localEntity != null) {
            return localEntity;
        } else {
            return getEntityRemote(key, rf, localEntity);
        }
    }

    private byte[] getEntityRemote(final String key, final ReplicationFactor rf,
            @Nullable final byte[] localEntity) {
        final List<ReplicaResponse> replicaResponses =
                Arrays.stream(resolver.chooseReplicasForKey(key.getBytes(), rf.from))
                        .filter(localService::isNotSelfReplica)
                        .map(replica -> replica.requestGetEntity(key))
                        .filter(it -> it.responseStatus == ResponseStatus.ACK)
                        .collect(Collectors.toList());
        replicaResponses.add(localEntity == null ? entityNotFound() : entityFound(localEntity));
        return decideOnGetEntityResponses(rf.ack, replicaResponses);
    }

    private byte[] decideOnGetEntityResponses(final int requestedAcksCount,
            final List<ReplicaResponse> replicaResponses) {
        if (replicaResponses.size() >= requestedAcksCount) {
            for (ReplicaResponse response : replicaResponses) {
                // WARNING: No conflict resolution implemented!
                if (response.payloadStatus == PayloadStatus.FOUND) return response.payload;
            }
            throw new NoSuchElementException();
        } else {
            throw new NotEnoughReplicasException();
        }
    }

    public void putEntity(final String key, final byte[] entity, final ReplicationFactor rf)
    throws IOException {
        if (rf.from == 1) {
            localService.putEntity(key.getBytes(), entity);
        } else throw new RuntimeException("NOT IMPLEMENTED");
    }

    public void deleteEntity(final String key, final ReplicationFactor rf) throws IOException {
        if (rf.from == 1) {
            localService.deleteEntity(key.getBytes());
        } else throw new RuntimeException("NOT IMPLEMENTED");
    }
}
