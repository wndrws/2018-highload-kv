package ru.kspt.highload.rest;

import one.nio.http.*;

import java.io.IOException;

public class KeyValueStorageHttpServer extends HttpServer {
    private final KeyValueStorageController controller;

    KeyValueStorageHttpServer(final HttpServerConfig config,
            final KeyValueStorageController controller)
    throws IOException {
        super(config);
        this.controller = controller;
    }

    @Path("/v0/status")
    public Response handleStatus(final Request request) {
        return controller.status();
    }

    @Path("/v0/entity")
    public Response handleEntity(final Request request,
            @Param("id") final String id,
            @Param("replicas") final String replicas) {
        return controller.entity(request, id, replicas);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session)
    throws IOException {
        session.sendResponse(Responses.badRequest());
    }
}
