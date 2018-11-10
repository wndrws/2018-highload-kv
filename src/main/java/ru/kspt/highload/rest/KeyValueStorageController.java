package ru.kspt.highload.rest;

import lombok.extern.slf4j.Slf4j;
import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import ru.kspt.highload.service.KeyValueStorageService;

import java.io.IOException;
import java.util.NoSuchElementException;

import static one.nio.http.Request.METHOD_DELETE;
import static one.nio.http.Request.METHOD_GET;
import static one.nio.http.Request.METHOD_PUT;

@Slf4j
public class KeyValueStorageController {
    private final KeyValueStorageService service;

    private final HttpServer httpServer;

    public KeyValueStorageController(final KeyValueStorageService service, final int port)
    throws IOException {
        this.service = service;
        this.httpServer = new KeyValueStorageHttpServer(createConfig(port), this);
    }

    public void startHttpServer() {
        httpServer.start();
    }

    public void stopHttpServer() {
        httpServer.stop();
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

    Response entity(final Request request, final String id, final String replicas) {
        if (isBadParameter(id)) {
            return Responses.badRequest();
        }
        switch (request.getMethod()) {
            case METHOD_GET: return handleGetEntity(id);
            case METHOD_PUT: return handlePutEntity(id, request);
            case METHOD_DELETE: return handleDeleteEntity(id);
            default: return Responses.methodNotAllowed();
        }
    }

    private boolean isBadParameter(final String param) {
        return param == null || param.isEmpty();
    }

    private Response handleGetEntity(final String entityId) {
        try {
            byte[] value = service.getEntity(entityId.getBytes());
            return Response.ok(value);
        } catch (NoSuchElementException ex) {
            return Responses.notFound();
        } catch (Exception ex) {
            log.error("Exception occurred during GET", ex);
            return Responses.internalServerError();
        }
    }

    private Response handlePutEntity(final String entityId, final Request request) {
        try {
            service.putEntity(entityId.getBytes(), request.getBody());
            return Responses.created();
        } catch (Exception ex) {
            log.error("Exception occurred during PUT", ex);
            return Responses.internalServerError();
        }
    }

    private Response handleDeleteEntity(final String entityId) {
        try {
            service.deleteEntity(entityId.getBytes());
            return Responses.accepted();
        } catch (Exception ex) {
            log.error("Exception occurred during DELETE", ex);
            return Responses.internalServerError();
        }
    }
}
