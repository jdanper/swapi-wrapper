package org.danielper.swapi;

import io.vertx.core.Vertx;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.danielper.swapi.module.HttpVerticle;
import org.danielper.swapi.module.PlanetVerticle;
import org.danielper.swapi.module.RepositoryVerticle;
import org.danielper.swapi.module.SwapiVerticle;

public class App {
    private static final Logger log = LogManager.getLogger(App.class);

    public static void main(String... args) {
        final var vertx = Vertx.vertx();

        vertx.deployVerticle(new RepositoryVerticle(), res -> {
            if (res.succeeded()) {
                log.info("Database connected");
            }else{
                log.fatal(res.cause().getMessage());
                System.exit(-1);
            }
        });

        vertx.deployVerticle(new SwapiVerticle(), res -> {
            if (res.succeeded()) {
                log.info("Remove api consumer started");
            }else{
                log.fatal(res.cause().getMessage());
                System.exit(-1);
            }
        });

        vertx.deployVerticle(new PlanetVerticle(), res -> {
            if (res.succeeded()) {
                log.info("Planets handler started");
            }else {
                log.fatal(res.cause().getMessage());
                System.exit(-1);
            }
        });

        vertx.deployVerticle(new HttpVerticle(), res -> {
            if (res.succeeded()) {
                log.info("Http server started");
            }else {
                log.fatal(res.cause().getMessage());
                System.exit(-1);
            }
        });
    }
}
