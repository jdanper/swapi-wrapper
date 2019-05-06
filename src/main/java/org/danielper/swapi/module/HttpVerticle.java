package org.danielper.swapi.module;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.danielper.swapi.util.BusAddresses;

import static org.danielper.swapi.util.Http.getQueryStr;
import static org.danielper.swapi.util.Http.handleMsgResult;

public class HttpVerticle extends AbstractVerticle {
    private final Logger log = LogManager.getLogger(HttpVerticle.class);

    private EventBus bus;

    @Override
    public void start() throws Exception {
        bus = vertx.eventBus();

        setupRoutes();

        super.start();
    }

    private void setupRoutes(){
        vertx.createHttpServer()
                .requestHandler(getRoutes())
                .listen(8080);
    }

    private Router getRoutes(){
        final var router =  Router.router(vertx);
        router.route().handler(BodyHandler.create());

        router.get("/health").handler(this::healthCheckHandler);

        router.get("/planets").handler(this::planetsHandler);
        router.post("/planets").handler(this::newPlanet);

        router.get("/planets/swapi").handler(this::getPlanetsFromSwapi);

        return router;
    }

    private void healthCheckHandler(RoutingContext ctx){
        ctx.response()
                .end(new JsonObject().put("message", "OK").encode());
    }

    private void planetsHandler(RoutingContext ctx){
        bus.send(BusAddresses.GET_ALL_PLANETS, null, res -> handleMsgResult(ctx, res));
    }

    private void newPlanet(RoutingContext ctx){
        bus.send(BusAddresses.NEW_PLANET, ctx.getBodyAsJson(), res -> handleMsgResult(ctx, res));
    }

    private void getPlanetsFromSwapi(RoutingContext ctx) {
        bus.send(BusAddresses.GET_PLANETS_SWAPI, getQueryStr(ctx, "page").orElse("1"), res -> handleMsgResult(ctx, res));
    }
}
