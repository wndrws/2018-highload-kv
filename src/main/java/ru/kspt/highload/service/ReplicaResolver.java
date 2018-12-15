package ru.kspt.highload.service;

import lombok.AllArgsConstructor;

import java.util.Arrays;

@AllArgsConstructor
public class ReplicaResolver {
    private final Replica[] allReplicas;

    Replica[] chooseReplicasForKey(final byte[] keyBytes, final int replicasCount) {
        final Replica[] replicas = new Replica[replicasCount];
        final int startIdx = Arrays.hashCode(keyBytes) % allReplicas.length;
        for (int j = replicasCount - 1; j >= 0; j--) {
            replicas[j] = allReplicas[(startIdx + j) % allReplicas.length];
        }
        return replicas;
    }
}
