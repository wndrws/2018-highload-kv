package ru.kspt.highload.service;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import one.nio.http.HttpClient;
import one.nio.net.ConnectionString;

import java.time.Duration;

@ToString(exclude = "httpClient")
@Accessors(fluent = true)
@AllArgsConstructor
@EqualsAndHashCode(exclude = "httpClient")
public class Replica {
    private static final String URL_SCHEMA = "http://";

    private static final Duration TIMEOUT = Duration.ofSeconds(30);

    final String host;

    final int port;

    @Getter(lazy = true)
    private final HttpClient httpClient = createHttpClient();

    private HttpClient createHttpClient() {
        return new HttpClient(new ConnectionString(
                URL_SCHEMA + host + ":" + port + "?timeout=" + TIMEOUT.toMillis()));
    }

    static Replica create(final String hostport) {
        final String[] parts = hostport.split(":");
        return new Replica(parts[0], Integer.valueOf(parts[1]));
    }
}
