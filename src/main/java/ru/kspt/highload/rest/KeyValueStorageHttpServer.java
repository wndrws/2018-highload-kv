package ru.kspt.highload.rest;

import one.nio.http.*;

import java.io.IOException;

import static ru.kspt.highload.service.Replica.INTERNAL_REQUESTS_HTTP_HEADER;
import static ru.kspt.highload.service.Replica.INTERNAL_REQUESTS_HTTP_HEADER_VALUE;

public class KeyValueStorageHttpServer extends HttpServer {
    private final KeyValueStorageController controller;

    KeyValueStorageHttpServer(final HttpServerConfig config,
            final KeyValueStorageController controller) throws IOException {
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
            @Param("replicas") final String replicas,
            @Header(INTERNAL_REQUESTS_HTTP_HEADER) final String internal) {
        final boolean isInternal =
                internal != null && internal.equals(INTERNAL_REQUESTS_HTTP_HEADER_VALUE);
        return controller.entity(request, id, replicas, isInternal);
    }

    @Override
    public void handleDefault(final Request request, final HttpSession session) throws IOException {
        session.sendResponse(Responses.badRequest());
    }
}
