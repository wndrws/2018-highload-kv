package ru.kspt.highload.service;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

class ReplicaResolverTest {
    private final Replica[] allReps = new Replica[] {
            Replica.create("example.com:8081"),
            Replica.create("example.com:8082"),
            Replica.create("example.com:8083") };

    private final ReplicaResolver replicaResolver = new ReplicaResolver(allReps);

    private final byte[] keyFor1stReplica = new byte[] {1, 2, 2};

    private final byte[] keyFor2ndReplica = new byte[] {1, 2, 3};

    private final byte[] keyFor3rdReplica = new byte[] {1, 2, 4};

    @ParameterizedTest(name = "[{index}] {0} replica(s)")
    @ValueSource(ints = {1, 2, 3})
    void testChooseReplicasForKey_StartingFrom1st(final int count) {
        // given
        final List<Replica> expectedReplicas = listReplicasStartingFrom(0).subList(0, count);
        // when
        final Replica[] replicas = replicaResolver.chooseReplicasForKey(keyFor1stReplica, count);
        // then
        Assertions.assertArrayEquals(expectedReplicas.toArray(), replicas);
    }

    @ParameterizedTest(name = "[{index}] {0} replica(s)")
    @ValueSource(ints = {1, 2, 3})
    void testChooseReplicasForKey_StartingFrom2nd(final int count) {
        // given
        final List<Replica> expectedReplicas = listReplicasStartingFrom(1).subList(0, count);
        // when
        final Replica[] replicas = replicaResolver.chooseReplicasForKey(keyFor2ndReplica, count);
        // then
        Assertions.assertArrayEquals(expectedReplicas.toArray(), replicas);
    }

    @ParameterizedTest(name = "[{index}] {0} replica(s)")
    @ValueSource(ints = {1, 2, 3})
    void testChooseReplicasForKey_StartingFrom3rd(final int count) {
        // given
        final List<Replica> expectedReplicas = listReplicasStartingFrom(2).subList(0, count);
        // when
        final Replica[] replicas = replicaResolver.chooseReplicasForKey(keyFor3rdReplica, count);
        // then
        Assertions.assertArrayEquals(expectedReplicas.toArray(), replicas);
    }

    private List<Replica> listReplicasStartingFrom(final int idx) {
        switch (idx) {
            case 0: return Lists.newArrayList(allReps[0], allReps[1], allReps[2]);
            case 1: return Lists.newArrayList(allReps[1], allReps[2], allReps[0]);
            case 2: return Lists.newArrayList(allReps[2], allReps[0], allReps[1]);
            default: throw new IllegalArgumentException();
        }
    }

    @Test
    void testChooseReplicasForKey_SameReplicaForSameHash() {
        // given
        final byte[] keyOne = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x1F};
        final byte[] keyTwo = new byte[]{(byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00};
        Assumptions.assumeFalse(Arrays.equals(keyOne, keyTwo));
        Assumptions.assumeTrue(Arrays.hashCode(keyOne) == Arrays.hashCode(keyTwo));
        // when
        final Replica[] replicasOne = replicaResolver.chooseReplicasForKey(keyOne, 1);
        final Replica[] replicasTwo = replicaResolver.chooseReplicasForKey(keyTwo, 1);
        // then
        Assertions.assertArrayEquals(replicasOne, replicasTwo);
    }
}
