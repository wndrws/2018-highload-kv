package ru.kspt.highload.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import one.nio.http.*;
import one.nio.server.AcceptorConfig;
import ru.mail.polis.KVDao;
import ru.mail.polis.KVService;

import java.io.IOException;
import java.util.NoSuchElementException;

import static one.nio.http.Request.*;

@Slf4j
public class KeyValueStorageService extends HttpServer implements KVService {
    private final static String STATUS_ENDPOINT = "/v0/status";

    private final static String ENTITY_ENDPOINT = "/v0/entity";

    private final KVDao storage;

    public KeyValueStorageService(final HttpServerConfig config, final KVDao storage)
    throws IOException {
        super(config);
        this.storage = storage;
    }

    public static KeyValueStorageService create(final KVDao dao, final int port)
    throws IOException {
        return new KeyValueStorageService(createConfig(port), dao);
    }

    private static HttpServerConfig createConfig(final int port) {
        final AcceptorConfig acceptorConfig = new AcceptorConfig();
        acceptorConfig.port = port;
        HttpServerConfig config = new HttpServerConfig();
        config.acceptors = new AcceptorConfig[]{acceptorConfig};
        return config;
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) {
        switch (request.getPath()) {
            case STATUS_ENDPOINT:
                reportStatus(session);
                break;
            case ENTITY_ENDPOINT:
                handleEntityRequest(request, session);
                break;
            default:
                sendNotFound(session);
        }
    }

    @SneakyThrows
    private void reportStatus(final HttpSession session) {
        session.sendResponse(Response.ok("Ready to work!"));
    }

    @SneakyThrows
    private void sendNotFound(final HttpSession session) {
        session.sendResponse(new Response(Response.NOT_FOUND, Response.EMPTY));
    }

    private void handleEntityRequest(final Request request, final HttpSession session) {
        if (isBad(request)) {
            sendBadRequest(session);
            return;
        }
        switch (request.getMethod()) {
            case METHOD_GET:
                handleGetEntity(request, session);
                break;
            case METHOD_PUT:
                handlePutEntity(request, session);
                break;
            case METHOD_DELETE:
                handleDeleteEntity(request, session);
                break;
            default:
                sendMethodNotAllowed(session);
        }
    }

    private boolean isBad(final Request request) {
        return extractKeyFrom(request).isEmpty();
    }

    private String extractKeyFrom(final Request request) {
        final String keyString = request.getParameter("id=");
        return keyString == null ? "" : keyString;
    }

    @SneakyThrows
    private void sendBadRequest(final HttpSession session) {
        session.sendResponse(new Response(Response.BAD_REQUEST, Response.EMPTY));
    }

    private void handleGetEntity(final Request request, final HttpSession session) {
        try {
            byte[] value = storage.get(extractKeyFrom(request).getBytes());
            session.sendResponse(Response.ok(value));
        } catch (NoSuchElementException ex) {
            sendNotFound(session);
        } catch (Exception ex) {
            log.error("Exception occurred during GET", ex);
            sendServerError(session);
        }
    }

    private void handlePutEntity(final Request request, final HttpSession session) {
        try {
            storage.upsert(extractKeyFrom(request).getBytes(), request.getBody());
            session.sendResponse(new Response(Response.CREATED, Response.EMPTY));
        } catch (Exception ex) {
            log.error("Exception occurred during PUT", ex);
            sendServerError(session);
        }
    }

    private void handleDeleteEntity(final Request request, final HttpSession session) {
        try {
            storage.remove(extractKeyFrom(request).getBytes());
            session.sendResponse(new Response(Response.ACCEPTED, Response.EMPTY));
        } catch (IOException ex) {
            log.error("Exception occurred during DELETE", ex);
            sendServerError(session);
        }
    }

    @SneakyThrows
    private void sendMethodNotAllowed(final HttpSession session) {
        session.sendResponse(new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY));
    }

    @SneakyThrows
    private void sendServerError(final HttpSession session) {
        session.sendResponse(new Response(Response.INTERNAL_ERROR, Response.EMPTY));
    }
}
