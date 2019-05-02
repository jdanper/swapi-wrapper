package org.danielper.swapi.util;

import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.eventbus.ReplyException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class Http {
    public static void fail(final RoutingContext ctx, final int statusCode, final Throwable error) {
        final var failureResponse = new JsonObject().put("message", error.getMessage());

        end(ctx, statusCode, failureResponse);
    }

    public static void handleMsgResult(final RoutingContext ctx, final AsyncResult<Message<Object>> result) {
        if (result.failed()) {
            final var failure = (ReplyException) result.cause();
            fail(ctx, failure.failureCode(), result.cause());

            return;
        }

        final var responseJson = new JsonObject().put("data", result.result().body());

        end(ctx, 200, responseJson);
    }

    public static void end(final RoutingContext ctx, final int statusCode, final JsonObject data) {
        ctx.response().setStatusCode(statusCode).end(data.encode());
    }
}
