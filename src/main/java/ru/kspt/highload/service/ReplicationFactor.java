package ru.kspt.highload.service;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Value
public class ReplicationFactor {
    private final static char DELIMITER = '/';

    public int ack;

    public int from;

    @Nullable
    public static ReplicationFactor parse(final String replicas) {
        final int delimIdx = replicas.indexOf(DELIMITER);
        if (delimIdx == -1) {
            return null;
        } else {
            final int ack = Integer.valueOf(replicas.substring(0, delimIdx));
            final int from = Integer.valueOf(replicas.substring(delimIdx + 1));
            return (ack < 1 || ack > from) ? null : new ReplicationFactor(ack, from);
        }
    }

    public static ReplicationFactor quorum(final int from) {
        return new ReplicationFactor(from / 2 + 1, from);
    }
}
