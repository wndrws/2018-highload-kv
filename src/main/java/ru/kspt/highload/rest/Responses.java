package ru.kspt.highload.rest;

import lombok.experimental.UtilityClass;
import one.nio.http.Response;

@UtilityClass
class Responses {
    /**
     * HTTP code: 201
     */
    Response created() {
        return new Response(Response.CREATED, Response.EMPTY);
    }

    /**
     * HTTP code: 202
     */
    Response accepted() {
        return new Response(Response.ACCEPTED, Response.EMPTY);
    }

    /**
     * HTTP code: 204
     */
    Response noContent() {
        return new Response(Response.NO_CONTENT, Response.EMPTY);
    }

    /**
     * HTTP code: 400
     */
    Response badRequest() {
        return new Response(Response.BAD_REQUEST, Response.EMPTY);
    }

    /**
     * HTTP code: 404
     */
    Response notFound() {
        return new Response(Response.NOT_FOUND, Response.EMPTY);
    }

    /**
     * HTTP code: 405
     */
    Response methodNotAllowed() {
        return new Response(Response.METHOD_NOT_ALLOWED, Response.EMPTY);
    }

    /**
     * HTTP code: 500
     */
    Response internalServerError() {
        return new Response(Response.INTERNAL_ERROR, Response.EMPTY);
    }

    /**
     * HTTP code: 504
     */
    Response notEnoughReplicas() {
        return new Response("504 Not Enough Replicas", Response.EMPTY);
    }
}
