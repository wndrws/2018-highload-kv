package ru.kspt.highload.rest;

import lombok.extern.slf4j.Slf4j;
import one.nio.http.HttpServer;
import one.nio.http.HttpServerConfig;
import one.nio.http.Request;
import one.nio.http.Response;
import one.nio.server.AcceptorConfig;
import ru.kspt.highload.DeletedEntityException;
import ru.kspt.highload.NotEnoughReplicasException;
import ru.kspt.highload.service.KeyValueStorageGateway;
import ru.kspt.highload.service.KeyValueStorageService;
import ru.kspt.highload.service.Replica;
import ru.kspt.highload.service.ReplicationFactor;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import static one.nio.http.Request.*;

@Slf4j
public class KeyValueStorageController {
    private final KeyValueStorageGateway gateway;

    private final HttpServer httpServer;

    private final List<Replica> replicas;

    public KeyValueStorageController(final KeyValueStorageService service, final int port,
            final List<Replica> replicas) throws IOException {
        this.httpServer = new KeyValueStorageHttpServer(createConfig(port), this);
        this.replicas = replicas;
        this.gateway = new KeyValueStorageGateway(service, replicas);
    }

    public void startHttpServer() {
        gateway.start();
        httpServer.start();
    }

    public void stopHttpServer() {
        httpServer.stop();
        gateway.stop();
    }

    private static HttpServerConfig createConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        return config;
    }

    Response status() {
        return Response.ok("Ready to work!");
    }

    Response entity(final Request request, final String id, final String replicasParam,
            final boolean isInternal) {
        if (isBadParameter(id)) {
            return Responses.badRequest();
        }
        try {
            final ReplicationFactor replicationFactor =
                    isInternal ? ReplicationFactor.single() : parseReplicationFactor(replicasParam);
            return handleEntity(request, id, replicationFactor);
        } catch (IllegalArgumentException __) {
            return Responses.badRequest();
        } catch (DeletedEntityException __) {
            return isInternal ? Responses.noContent() : Responses.notFound();
        } catch (NotEnoughReplicasException __) {
            return Responses.notEnoughReplicas();
        } catch (Exception ex) {
            log.error("Unexpected exception occurred!", ex);
            return Responses.internalServerError();
        }
    }

    private static boolean isBadParameter(final String param) {
        return param == null || param.isEmpty();
    }

    private ReplicationFactor parseReplicationFactor(final String replicasParam) {
        if (isBadParameter(replicasParam)) {
            return ReplicationFactor.quorum(replicas.size());
        } else {
            final ReplicationFactor result = ReplicationFactor.parse(replicasParam);
            if (result == null || result.from > replicas.size()) {
                throw new IllegalArgumentException();
            } else return result;
        }
    }

    private Response handleEntity(final Request request, final String id,
            final ReplicationFactor rf) throws IOException {
        switch (request.getMethod()) {
            case METHOD_GET: return handleGetEntity(id, rf);
            case METHOD_PUT: return handlePutEntity(id, request, rf);
            case METHOD_DELETE: return handleDeleteEntity(id, rf);
            default: return Responses.methodNotAllowed();
        }
    }

    private Response handleGetEntity(final String entityId, final ReplicationFactor rf) {
        try {
            byte[] value = gateway.getEntity(entityId, rf);
            return Response.ok(value);
        } catch (NoSuchElementException ex) {
            return Responses.notFound();
        }
    }

    private Response handlePutEntity(final String entityId, final Request request,
            final ReplicationFactor rf) {
        gateway.putEntity(entityId, request.getBody(), rf);
        return Responses.created();
    }

    private Response handleDeleteEntity(final String entityId, final ReplicationFactor rf) {
        gateway.deleteEntity(entityId, rf);
        return Responses.accepted();
    }
}
