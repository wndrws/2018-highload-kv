package ru.kspt.highload.service;

import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
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
import java.util.function.Supplier;

@Slf4j
@ToString(of = {"host", "port"})
@Accessors(fluent = true)
@RequiredArgsConstructor
@EqualsAndHashCode(of = {"host", "port"})
public class Replica {
    public static final String INTERNAL_REQUESTS_HTTP_HEADER = "No-Proxy";

    public static final String INTERNAL_REQUESTS_HTTP_HEADER_VALUE = "yes";

    private static final String URL_SCHEMA = "http://";

    private static final Duration TIMEOUT = Duration.ofSeconds(1);

    final String host;

    final int port;

    private ExecutorService executor;

    private HttpClient httpClient;

    private HttpClient createHttpClient() {
        return new HttpClient(new ConnectionString(URL_SCHEMA + host + ":" + port),
                INTERNAL_REQUESTS_HTTP_HEADER + ": " + INTERNAL_REQUESTS_HTTP_HEADER_VALUE);
    }

    void start() {
        httpClient = createHttpClient();
        executor = Executors.newSingleThreadExecutor();
    }

    void stop() {
        if (executor != null) {
            executor.shutdownNow();
        }
        if (httpClient != null) {
            httpClient.close();
        }
    }

    ReplicaResponse requestGetEntity(final String key) {
        return makeRequest(() -> getEntityFromReplica(key), "GET");
    }

    private ReplicaResponse makeRequest(final Supplier<ReplicaResponse> requestCall, final String name) {
        assert executor != null;
        final CompletableFuture<ReplicaResponse> response =
                CompletableFuture.supplyAsync(requestCall, executor);
        try {
            return response.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException __) {
            log.warn("{} request to replica {} timed out (timeout = {})!", name, this, TIMEOUT);
            return ReplicaResponse.fail();
        } catch (InterruptedException __) {
            log.warn(Thread.currentThread().getName() + " was interrupted");
            Thread.currentThread().interrupt();
            return ReplicaResponse.fail();
        } catch (ExecutionException e) {
            log.error("Unexpected error executing " + name + " request to the replica " + this, e);
            return ReplicaResponse.fail();
        }
    }

    private ReplicaResponse getEntityFromReplica(final String key) {
        assert httpClient != null;
        try {
            final Response response = httpClient.get(makeEntityEndpoint(key));
            return parseResponseWithEntity(response);
        } catch (InterruptedException __) {
            log.warn("{} interrupted", Thread.currentThread().getName());
            Thread.currentThread().interrupt();
            return ReplicaResponse.fail();
        } catch (HttpException | IOException | PoolException e) {
            log.warn("GET request to the replica {} failed: {}", this, e.getMessage());
            return ReplicaResponse.fail();
        }
    }

    private String makeEntityEndpoint(String key) {
        return "/v0/entity?id=" + key;
    }

    private ReplicaResponse parseResponseWithEntity(final Response response) {
        switch (response.getStatus()) {
            case 200: return ReplicaResponse.entityFound(response.getBody());
            case 204: return ReplicaResponse.entityDeleted();
            case 404: return ReplicaResponse.entityNotFound();
            default: return ReplicaResponse.fail();
        }
    }

    ReplicaResponse requestPutEntity(final String key, final byte[] value) {
        return makeRequest(() -> putEntityToReplica(key, value), "PUT");
    }

    private ReplicaResponse putEntityToReplica(final String key, final byte[] value) {
        assert httpClient != null;
        try {
            final Response response = httpClient.put(makeEntityEndpoint(key), value);
            return response.getStatus() == 201 ? ReplicaResponse.success() : ReplicaResponse.fail();
        } catch (InterruptedException __) {
            log.warn("{} interrupted", Thread.currentThread().getName());
            Thread.currentThread().interrupt();
            return ReplicaResponse.fail();
        } catch (HttpException | IOException | PoolException e) {
            log.warn("PUT request to the replica {} failed: {}", this, e.getMessage());
            return ReplicaResponse.fail();
        }
    }

    ReplicaResponse requestDeleteEntity(final String key) {
        return makeRequest(() -> deleteEntityFromReplica(key), "DELETE");
    }

    private ReplicaResponse deleteEntityFromReplica(final String key) {
        assert httpClient != null;
        try {
            final Response response = httpClient.delete(makeEntityEndpoint(key));
            return response.getStatus() == 202 ? ReplicaResponse.success() : ReplicaResponse.fail();
        } catch (InterruptedException __) {
            log.warn("{} interrupted", Thread.currentThread().getName());
            Thread.currentThread().interrupt();
            return ReplicaResponse.fail();
        } catch (HttpException | IOException | PoolException e) {
            log.warn("DELETE request to the replica {} failed: {}", this, e.getMessage());
            return ReplicaResponse.fail();
        }
    }

    static Replica create(final String hostport) {
        final String[] parts = hostport.split(":");
        return new Replica(parts[0], Integer.valueOf(parts[1]));
    }
}
