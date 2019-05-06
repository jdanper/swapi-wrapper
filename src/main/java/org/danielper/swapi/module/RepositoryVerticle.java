package org.danielper.swapi.module;

import com.datastax.driver.core.Row;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.danielper.swapi.database.PlanetsDb;
import org.danielper.swapi.util.BusAddresses;

import java.util.function.Function;
import java.util.stream.Collectors;

public class RepositoryVerticle extends AbstractVerticle {
    private final Logger log = LogManager.getLogger(RepositoryVerticle.class);

    private PlanetsDb db;

    private Function<Row, JsonObject> mapRowToPlanetJson = row -> new JsonObject()
            .put("name", row.getString("name"))
            .put("climate", row.getString("climate"))
            .put("terrain", row.getString("terrain"))
            .put("film_count", row.getInt("film_count"));

    @Override
    public void start(Future<Void> startFuture) {
        try {
            final var bus = vertx.eventBus();

            db = new PlanetsDb();

            bus.consumer(BusAddresses.QUERY_ALL_PLANETS, this::getAll);
            bus.consumer(BusAddresses.INSERT_PLANET, this::insert);

            startFuture.complete();
        } catch (Exception ex) {
            startFuture.fail(ex);
        }
    }

    private void getAll(Message<Void> msg){
        final var planets = db.getAll()
                .all()
                .stream()
                .map(mapRowToPlanetJson)
                .collect(Collectors.toList());

        msg.reply(new JsonArray(planets));
    }

    private void insert(Message<JsonObject> msg){
        final var id = db.insert(msg.body());

        msg.reply(new JsonObject().put("id",id.toString()));
    }
}
