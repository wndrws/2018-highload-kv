package ru.kspt.highload.service;

import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import ru.kspt.highload.DeletedEntityException;
import ru.kspt.highload.NotEnoughReplicasException;
import ru.kspt.highload.dto.PayloadStatus;
import ru.kspt.highload.dto.ReplicaResponse;
import ru.kspt.highload.dto.ResponseStatus;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class KeyValueStorageGateway {
    private final KeyValueStorageService localService;

    private final List<Replica> replicas;

    private final ReplicaResolver resolver;

    private final TaskScheduler taskScheduler;

    public KeyValueStorageGateway(final KeyValueStorageService localService,
            final List<Replica> replicas) {
        this.localService = localService;
        this.replicas = replicas;
        this.resolver = new ReplicaResolver(replicas);
        this.taskScheduler = new TaskScheduler();
    }

    public void start() {
        taskScheduler.start();
        replicas.forEach(Replica::start);
    }

    public void stop() {
        replicas.forEach(Replica::stop);
        taskScheduler.stop();
    }

    public byte[] getEntity(final String key, final ReplicationFactor rf)
            throws NoSuchElementException, DeletedEntityException, NotEnoughReplicasException {
        final ReplicaResponse localResponse = getEntityLocally(key);
        if (rf.from == 1 && localResponse.responseStatus == ResponseStatus.ACK) {
            if (localResponse.payloadStatus == PayloadStatus.FOUND) {
                return localResponse.payload;
            } else {
                throw new NoSuchElementException();
            }
        } else {
            return getEntityRemotely(key, rf, localResponse);
        }
    }

    private ReplicaResponse getEntityLocally(final String key) {
        try {
            return ReplicaResponse.entityFound(localService.getEntity(key.getBytes()));
        } catch (DeletedEntityException e) {
            log.warn("Entity is deleted - rethrowing...");
            throw e;
        } catch (NoSuchElementException e) {
            return ReplicaResponse.entityNotFound();
        } catch (Exception e) {
            log.error("Exception while trying to get entity locally", e);
            return ReplicaResponse.fail();
        }
    }

    private byte[] getEntityRemotely(final String key, final ReplicationFactor rf,
            final ReplicaResponse localResponse) {
        final List<ReplicaResponse> replicaResponses =
                askReplicas(replica -> replica.requestGetEntity(key), key, rf);
        if (localResponse.responseStatus == ResponseStatus.ACK) {
            replicaResponses.add(localResponse);
        }
        return decideOnGetEntityResponses(rf.ack, replicaResponses);
    }

    private List<ReplicaResponse> askReplicas(final Function<Replica, ReplicaResponse> request,
            final String key, final ReplicationFactor rf) {
        final List<Replica> chosenReplicas = Arrays
                .stream(resolver.chooseReplicasForKey(key.getBytes(), rf.from))
                .filter(localService::isNotSelfReplica)
                .limit(rf.from - 1) // since one request was already served locally
                .collect(Collectors.toList());
        taskScheduler.schedule(request, chosenReplicas);
        return taskScheduler.getNeededAckedResponses(rf.ack - 1);
    }

    private byte[] decideOnGetEntityResponses(final int requestedAcksCount,
            final List<ReplicaResponse> replicaResponses) {
        if (replicaResponses.size() >= requestedAcksCount) {
            byte[] result = findEntity(replicaResponses);
            if (result != null) {
                return result;
            } else {
                throw new NoSuchElementException();
            }
        } else {
            throw new NotEnoughReplicasException();
        }
    }

    @Nullable
    private byte[] findEntity(final List<ReplicaResponse> replicaResponses) {
        byte[] result = null;
        for (ReplicaResponse response : replicaResponses) {
            switch (response.payloadStatus) {
                case FOUND:
                    result = response.payload; // WARNING: No conflict resolution implemented!
                    break;
                case DELETED:
                    throw new DeletedEntityException();
            }
        }
        return result;
    }

    public void putEntity(final String key, final byte[] entity, final ReplicationFactor rf) {
        final ReplicaResponse localResponse = putEntityLocally(key, entity);
        if (rf.from != 1 || localResponse.responseStatus != ResponseStatus.ACK) {
            putEntityRemotely(key, entity, rf, localResponse);
        }
    }

    private ReplicaResponse putEntityLocally(final String key, final byte[] entity) {
        try {
            localService.putEntity(key.getBytes(), entity);
            return ReplicaResponse.success();
        } catch (Exception e) {
            log.warn("Exception while trying to put entity locally", e);
            return ReplicaResponse.fail();
        }
    }

    private void putEntityRemotely(final String key, final byte[] value, final ReplicationFactor rf,
            final ReplicaResponse localResponse) {
        final List<ReplicaResponse> replicaResponses =
                askReplicas(replica -> replica.requestPutEntity(key, value), key, rf);
        if (localResponse.responseStatus == ResponseStatus.ACK) {
            replicaResponses.add(localResponse);
        }
        if (replicaResponses.size() < rf.ack) {
            throw new NotEnoughReplicasException();
        }
    }

    public void deleteEntity(final String key, final ReplicationFactor rf) {
        final ReplicaResponse localResponse = deleteEntityLocally(key);
        if (rf.from != 1 || localResponse.responseStatus != ResponseStatus.ACK) {
            deleteEntityRemotely(key, rf, localResponse);
        }
    }

    private ReplicaResponse deleteEntityLocally(final String key) {
        try {
            localService.deleteEntity(key.getBytes());
            return ReplicaResponse.success();
        } catch (Exception e) {
            log.warn("Exception while trying to delete entity locally", e);
            return ReplicaResponse.fail();
        }
    }

    private void deleteEntityRemotely(final String key, final ReplicationFactor rf,
            final ReplicaResponse localResponse) {
        final List<ReplicaResponse> replicaResponses =
                askReplicas(replica -> replica.requestDeleteEntity(key), key, rf);
        if (localResponse.responseStatus == ResponseStatus.ACK) {
            replicaResponses.add(localResponse);
        }
        if (replicaResponses.size() < rf.ack) {
            throw new NotEnoughReplicasException();
        }
    }
}
