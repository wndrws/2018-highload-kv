package ru.kspt.highload.dao;

import lombok.Value;

import java.util.Arrays;

@Value
class Key {
    int hash;

    byte[] bytes;

    Key(final byte[] bytes) {
        this.hash = Arrays.hashCode(bytes);
        this.bytes = bytes;
    }
}
