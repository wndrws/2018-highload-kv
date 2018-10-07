package ru.kspt.highload;

import lombok.Value;

import java.util.Arrays;

@Value
public class Key {
    int hash;

    byte[] bytes;

    Key(final byte[] bytes) {
        this.hash = Arrays.hashCode(bytes);
        this.bytes = bytes;
    }
}
