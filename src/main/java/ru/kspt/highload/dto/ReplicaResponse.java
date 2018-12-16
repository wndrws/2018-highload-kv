package ru.kspt.highload.dto;

import lombok.Value;
import org.jetbrains.annotations.Nullable;

@Value
public class ReplicaResponse {
    public ResponseStatus responseStatus;

    public PayloadStatus payloadStatus;

    @Nullable
    public byte[] payload;

    public static ReplicaResponse fail() {
        return new ReplicaResponse(ResponseStatus.NACK, PayloadStatus.NOT_USED, null);
    }

    public static ReplicaResponse success() {
        return new ReplicaResponse(ResponseStatus.ACK, PayloadStatus.NOT_USED, null);
    }

    public static ReplicaResponse entityDeleted() {
        return new ReplicaResponse(ResponseStatus.ACK, PayloadStatus.DELETED, null);
    }

    public static ReplicaResponse entityNotFound() {
        return new ReplicaResponse(ResponseStatus.ACK, PayloadStatus.NOT_FOUND, null);
    }

    public static ReplicaResponse entityFound(final byte[] payload) {
        return new ReplicaResponse(ResponseStatus.ACK, PayloadStatus.FOUND, payload);
    }
}
