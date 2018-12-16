package ru.kspt.highload.service;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import one.nio.http.HttpClient;
import one.nio.http.HttpException;
import one.nio.http.Response;
import one.nio.net.ConnectionString;
import one.nio.pool.PoolException;
import ru.kspt.highload.dto.ReplicaResponse;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.*;

@Slf4j
@ToString(of = {"host", "port"})
@Accessors(fluent = true)
@AllArgsConstructor
@EqualsAndHashCode(of = {"host", "port"})
public class Replica {
    public static final String INTERNAL_REQUESTS_HTTP_HEADER = "No-Proxy";

    public static final String INTERNAL_REQUESTS_HTTP_HEADER_VALUE = "yes";

    private static final String URL_SCHEMA = "http://";

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    final String host;

    final int port;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Getter(lazy = true)
    private final HttpClient httpClient = createHttpClient();

    private HttpClient createHttpClient() {
        return new HttpClient(new ConnectionString(URL_SCHEMA + host + ":" + port),
                INTERNAL_REQUESTS_HTTP_HEADER + ":" + INTERNAL_REQUESTS_HTTP_HEADER_VALUE);
    }

    ReplicaResponse requestGetEntity(final String key) {
        final CompletableFuture<ReplicaResponse> response =
                CompletableFuture.supplyAsync(() -> getEntityFromReplica(key), executor);
        try {
            return response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException __) {
            log.warn("Get request to replica {} timed out (timeout = {})!", this, TIMEOUT);
            return ReplicaResponse.fail();
        } catch (InterruptedException __) {
            log.warn(Thread.currentThread().getName() + " was interrupted");
            Thread.currentThread().interrupt();
            return ReplicaResponse.fail();
        } catch (ExecutionException e) {
            log.error("Unexpected error while executing get request from replica" + this, e);
            return ReplicaResponse.fail();
        }
    }

    private ReplicaResponse getEntityFromReplica(final String key) {
        try {
            final Response response = httpClient().get("/v0/entity?id=" + key);
            return parseResponseWithEntity(response);
        } catch (InterruptedException __) {
            log.warn("{} interrupted", Thread.currentThread().getName());
            Thread.currentThread().interrupt();
            return ReplicaResponse.fail();
        } catch (HttpException | IOException | PoolException e) {
            log.warn("Request for entity from replica {} failed: {}", this, e.getMessage());
            return ReplicaResponse.fail();
        }
    }

    private ReplicaResponse parseResponseWithEntity(final Response response) {
        switch (response.getStatus()) {
            case 200: return ReplicaResponse.entityFound(response.getBody());
            case 204: return ReplicaResponse.entityDeleted();
            case 404: return ReplicaResponse.entityNotFound();
            default: return ReplicaResponse.fail();
        }
    }

    boolean requestPutEntity() {
        return false;
    }

    boolean requestDeleteEntity() {
        return false;
    }

    static Replica create(final String hostport) {
        final String[] parts = hostport.split(":");
        return new Replica(parts[0], Integer.valueOf(parts[1]));
    }
}
