package org.danielper.swapi.module;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.danielper.swapi.util.BusAddresses;

public class SwapiVerticle extends AbstractVerticle {

    private final Logger log = LogManager.getLogger(PlanetVerticle.class);

    private final String API_HOST = "swapi.co";
    private final String PLANETS_ENDPOINT = "api/planets";

    private CircuitBreaker breaker;

    private WebClient webClient;

    @Override
    public void start(Future<Void> startFuture) {
        final var bus = vertx.eventBus();

        bus.consumer(BusAddresses.FETCH_FILM_COUNT, this::fetchFilmCount);
        bus.consumer(BusAddresses.GET_PLANETS_SWAPI, this::getAllPlanets);

        setupBreaker();

        webClient = WebClient.create(vertx);

        startFuture.complete();
    }

    private void setupBreaker() {
        breaker = CircuitBreaker.create("swapi-breaker", vertx,
            new CircuitBreakerOptions()
                .setMaxFailures(3)
                .setTimeout(2000) // 2 seconds
                .setResetTimeout(10000) // 10 seconds
        );
    }

    private void fetchFilmCount(Message<String> msg) {
        final var planetName = msg.body();

        breaker.<Integer>execute(future ->
            webClient.get(API_HOST, PLANETS_ENDPOINT)
                .addQueryParam("search", planetName)
                .send(res -> {
                    if (res.failed()) {
                        future.fail(res.cause());
                        return;
                    }

                    extractFilmCount(future, res.result().bodyAsJsonObject());
                })
        ).setHandler(res -> {
            if (res.failed()) {
                log.info("Unable to fetch planet", res.cause());
                msg.fail(500, res.cause().getMessage());
                return;
            }

            msg.reply(res.result());
        });
    }

    private void extractFilmCount(final Future<Integer> future, final JsonObject content) {
        final var searchResult = content.getJsonArray("results");

        if (searchResult.isEmpty()) {
            future.complete(0);
            return;
        }

        final var filmCount = searchResult.getJsonObject(0)
            .getJsonArray("films")
            .size();

        future.complete(filmCount);
    }

    private void getAllPlanets(Message<String> msg) {
        final var page = msg.body();

        breaker.<JsonObject>execute(future ->
            webClient.get(API_HOST, PLANETS_ENDPOINT)
                .addQueryParam("page", page)
                .send(res -> {
                    if (res.failed()) {
                        future.fail(res.cause());
                        return;
                    }

                    future.complete(res.result().bodyAsJsonObject());
                })
        ).setHandler(res -> {
            if (res.failed()) {
                log.info("Unable to list planets from remote API", res.cause());
                msg.fail(500, res.cause().getMessage());
                return;
            }

            msg.reply(res.result());
        });
    }
}
