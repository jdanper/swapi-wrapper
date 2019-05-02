package org.danielper.swapi.module;

import io.vertx.circuitbreaker.CircuitBreaker;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.danielper.swapi.util.BusAddresses;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

public class PlanetVerticle extends AbstractVerticle {
    private final Logger log = LogManager.getLogger(PlanetVerticle.class);

    private EventBus bus;
    private CircuitBreaker breaker;
    private WebClient webClient;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        bus = vertx.eventBus();

        setupBreaker();
        webClient = WebClient.create(vertx);

        bus.consumer(BusAddresses.GET_ALL_PLANETS, this::getAllPlanets);
        bus.consumer(BusAddresses.NEW_PLANET, this::newPlanet);

        super.start(startFuture);
    }

    private void setupBreaker(){
        breaker = CircuitBreaker.create("swapi-breaker", vertx,
                new CircuitBreakerOptions()
                        .setMaxFailures(3)
                        .setTimeout(2000) // 2 seconds
                        .setResetTimeout(10000) // 10 seconds
        );
    }

    private final Function<JsonObject, JsonObject> fetchFilmCount = planet -> {
        breaker.execute(future ->
            webClient.get("https://swapi.co/api/planets")
                .addQueryParam("search", planet.getString("name"))
                .send(res -> {
                    if (res.failed()) {
                        log.info("Unable to fetch planet");
                        future.fail(res.cause());
                        return;
                    }

                    final var planetResults = res.result().bodyAsJsonObject().getJsonArray("results");

                    if (planetResults.isEmpty()) {
                        return;
                    }

                    final var filmsCount = planetResults.getJsonObject(0)
                        .getJsonArray("films")
                        .size();

                    planet.put("films_count", filmsCount);
                }));

        future.complete();
        return planet;
    };

    private void getAllPlanets(Message<JsonObject> msg) {
        bus.send(BusAddresses.QUERY_ALL_PLANETS, null, res -> {
            if (res.failed()) msg.fail(500, "Unable to query planets");

            msg.reply(res.result().body());
        });
    }

    private void newPlanet(Message<JsonObject> msg) {
        final var content = msg.body();

        if (!isPlanetValid(content)) {
            msg.fail(400, "Invalid content");
            return;
        }

        fetchFilmCount.andThen(planet ->
                bus.send(BusAddresses.INSERT_PLANET, planet, res -> {
                    if (res.failed()) {
                        msg.fail(500, "Unable to save the planet");
                        return;
                    }

                    msg.reply(res.result().body());
                })
        ).apply(content);
    }

    private boolean isPlanetValid(final JsonObject planet) {
        final var requiredProps = Arrays.asList("name", "climate", "terrain");

        return requiredProps.stream()
                .map(planet::getString)
                .filter(Objects::nonNull)
                .filter(str -> !str.isEmpty())
                .count() == requiredProps.size();
    }
}
