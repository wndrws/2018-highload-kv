package ru.kspt.highload.service;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.List;

@AllArgsConstructor
class ReplicaResolver {
    private final List<Replica> allReplicas;

    Replica[] chooseReplicasForKey(final byte[] keyBytes, final int replicasCount) {
        final Replica[] replicas = new Replica[replicasCount];
        final int startIdx = Arrays.hashCode(keyBytes) % allReplicas.size();
        for (int j = replicasCount - 1; j >= 0; j--) {
            replicas[j] = allReplicas.get((startIdx + j) % allReplicas.size());
        }
        return replicas;
    }
}
