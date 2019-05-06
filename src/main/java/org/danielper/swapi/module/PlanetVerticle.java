package org.danielper.swapi.module;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import org.danielper.swapi.util.BusAddresses;

import java.util.Arrays;
import java.util.Objects;

public class PlanetVerticle extends AbstractVerticle {

    private EventBus bus;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        bus = vertx.eventBus();

        bus.consumer(BusAddresses.GET_ALL_PLANETS, this::getAllPlanets);
        bus.consumer(BusAddresses.NEW_PLANET, this::newPlanet);

        super.start(startFuture);
    }

    private void getAllPlanets(Message<JsonObject> msg) {
        bus.send(BusAddresses.QUERY_ALL_PLANETS, null, res -> {
            if (res.failed()) msg.fail(500, "Unable to query planets");

            msg.reply(res.result().body());
        });
    }

    private void newPlanet(Message<JsonObject> msg) {
        final var planet = msg.body();

        if (!isPlanetValid(planet)) {
            msg.fail(400, "Invalid content");
            return;
        }

        bus.<Integer>send(BusAddresses.FETCH_FILM_COUNT, planet.getString("name"), countResult -> {
            if (countResult.failed()) {
                msg.fail(500, countResult.cause().getMessage());
                return;
            }

            planet.put("film_count", countResult.result().body());

            bus.<JsonObject>send(BusAddresses.INSERT_PLANET, planet, res -> {
                if (res.failed()) {
                    msg.fail(500, "Unable to save the planet");
                    return;
                }

                msg.reply(res.result().body());
            });
        });
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