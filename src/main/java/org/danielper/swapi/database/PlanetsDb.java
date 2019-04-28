package org.danielper.swapi.database;

import com.datastax.driver.core.*;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.utils.UUIDs;
import io.vertx.core.json.JsonObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.danielper.swapi.util.Secrets;

import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PlanetsDb {
    private final Logger log = LogManager.getLogger(PlanetsDb.class);

    private final String CASSANDRA_PLANETS_USER = Secrets.getOrDefault("CASSANDRA_USER", "cassandra");
    
    private final String PLANETS_KEYSPACE = System.getenv().getOrDefault("PLANETS_KEYSPACE", "planets");

    private final String CASSANDRA_PLANETS_PWD = Secrets.getOrDefault("CASSANDRA_PASSWORD", "cassandra");

    private final String CASSANDRA_HOSTS = System.getenv().getOrDefault("CASSANDRA_HOSTS", "localhost");

    private final String TABLE_NAME = "planet";

    private Session session;

    public PlanetsDb(){
        connect();
    }

    private void connect() {
            final var authProvider =
                    new PlainTextAuthProvider(CASSANDRA_PLANETS_USER, CASSANDRA_PLANETS_PWD);

            final var cluster = Cluster.builder()
                    .addContactPoints(CASSANDRA_HOSTS.split(","))
                    .withAuthProvider(authProvider)
                    .build();

            session = cluster.connect(PLANETS_KEYSPACE);
    }

    public ResultSet getAll(){
        final var stmt = new SimpleStatement("SELECT * FROM " + TABLE_NAME)
                .setKeyspace(PLANETS_KEYSPACE);

        return session.execute(stmt);
    }

    public UUID insert(final JsonObject data) {
        final var id = UUIDs.timeBased();

        final var insert = QueryBuilder.insertInto(PLANETS_KEYSPACE, TABLE_NAME)
                .value("id", id)
                .values(data.fieldNames().toArray(new String[0]), data.getMap().values().toArray());

        try {
            final var resultSet = session.execute(insert);
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }

        return id;
    }

    public <T> List<T> runQuery(final String query, final Function<Row, T> mapper){
        final var resultSet = session.execute(query);

        return resultSet.all()
                .stream()
                .map(mapper)
                .collect(Collectors.toList());
    }
}
