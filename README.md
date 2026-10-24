# FinanceBudge

> An app with gradle and jetty combined to show how the tech stack of Gradle with Jetty runs.


### Initialization

- Build war file

    ```bash
    ./gradlew war
    ```

- Copy war file to web app

    ```bash
    ./gradlew copyWars
    ```
- Start jetty

    ```bash
    ./gradlew startJetty
    ```

startJetty depends on copyWars, which in turn depends on war. So you can just execute startJetty if you just need to compile the web application.

Then you can find the files being served at `http://localhost:8080/FinanceBudge/hello`.


## OpenSearch

Start opensearch server:
```bash
docker run -d --name opensearch-dev \
  -p 9200:9200 -p 9600:9600 \
  -e "discovery.type=single-node" \
  -e "plugins.security.disabled=true" \
  -e "OPENSEARCH_JAVA_OPTS=-Xms512m -Xmx512m" \
  opensearchproject/opensearch:2.19.0
```
Test OpenSearch server works:
```bash
curl http://localhost:9200
```

Start Jetty Server : 
```bash
./gradlew startJetty
```

Then navigate to `http://localhost:8080/FinanceBudge/console`.
This is a page for demo-ing opensearch. 

Before pushing a log in the log file, start the logstash server:
```bash
docker run  --name logstash-dev   --network host   -v /home/shaunak/NetSuite/Applications/GradleJetty/logs:/home/shaunak/NetSuite/Applications/GradleJetty/logs   -v /home/shaunak/NetSuite/Applications/GradleJetty/logstash/app-logs.conf:/usr/share/logstash/pipeline/logstash.conf   opensearchproject/logstash-oss-with-opensearch-output-plugin:latest
```

When a POST query with log is executed:
    - The log file is appended to.
    - Logstash notes this new file (tails every few seconds) and then adds these lines to the OpenSearch index.
    - OpenSearch notes this log and adds it to the index.

You can write a log entry to an opensearch console, and then query the opensearch using the search box in "Search Logs".

It queries the message part of the logs.

## Kafka

Start a local Kafka broker (official image, KRaft mode, no Zookeeper needed):
```bash
docker run -d --name kafka-dev -p 9092:9092 apache/kafka:3.9.0
```
Test it's up:
```bash
docker exec kafka-dev /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list
```

Start Jetty Server:
```bash
./gradlew startJetty
```

Then navigate to `http://localhost:8080/FinanceBudge/kafka/console`.
This is a page for demo-ing Kafka.

When the webapp deploys, `KafkaLifecycleListener` starts a background consumer thread (`KafkaConsumerService`)
subscribed to the `app-events` topic. When you submit the "Produce" form:
- The page POSTs to `/kafka/produce`, which sends the key/value as one record to the `app-events` topic on the broker.
- The broker assigns it a partition and offset and returns those to the page immediately (that's "sent").
- Completely separately, the background consumer thread polls the broker, picks up the record, and buffers it in memory.
- The page polls `/kafka/messages` every 2s and shows whatever the consumer has buffered so far (that's "consumed").

The point of the demo: the producer and consumer never call each other directly, only the broker. You can override
the broker address/topic with the `KAFKA_BOOTSTRAP_SERVERS` / `KAFKA_TOPIC` env vars (see `KafkaConfig`).

## Cassandra

Start a local single-node Cassandra:
```bash
docker run -d --name cassandra-dev -p 9042:9042 cassandra:5.0
```
Cassandra takes 30-60s to finish booting before the CQL port is ready. Test it's up:
```bash
docker exec cassandra-dev cqlsh -e "DESCRIBE KEYSPACES"
```
Don't start/redeploy Jetty until that command succeeds - `CassandraLifecycleListener` connects and creates the
keyspace/table on webapp startup, so it needs the node to already be accepting connections.

Start Jetty Server:
```bash
./gradlew startJetty
```

Then navigate to `http://localhost:8080/FinanceBudge/cassandra/console`.
This is a page for demo-ing Cassandra.

The `events` table is keyed by `(source, created_at)`: `source` is the **partition key** (decides which node(s)
own the row - with a 1-node cluster that's moot, but it's the same design you'd use on a real cluster), and
`created_at` is a **clustering column** (sorts rows within a partition, newest first). Submitting the "Insert"
form POSTs to `/cassandra/events`. Searching only takes a `source`, not a free-text query - `/cassandra/events/search`
requires it and returns every row under that partition, already sorted - that's deliberate: Cassandra can only
serve lookups efficiently by partition key, there's no general cross-field filtering the way SQL has.

You can override the connection with the `CASSANDRA_HOST` / `CASSANDRA_PORT` / `CASSANDRA_DATACENTER` env vars
(see `CassandraConfig`) - the defaults match the official Cassandra image's out-of-the-box single-node config.

## Cross-cluster search (CCS)

A second, separate single-node OpenSearch cluster (`cluster-b`) that `opensearch-dev` can search into without copying any data. Start it:
```bash
cd opensearch-ccs && docker compose up -d
```
Join it to `opensearch-dev`'s network and register it as a remote (one-time per `opensearch-dev` restart):
```bash
docker network connect opensearch-ccs_ccs-net opensearch-dev
curl -X PUT "http://localhost:9200/_cluster/settings" -H "Content-Type: application/json" -d \
  '{"persistent": {"cluster.remote.cluster-b.seeds": ["cluster-b:9300"]}}'
```

On `http://localhost:8080/FinanceBudge/console`:
- The "Target cluster" dropdown on the write form picks where a log entry goes. `local` uses the normal file → Logstash path; `cluster-b` skips both and indexes straight into cluster-b's own `app-logs` (CCS has no write path - remote aliases are search-only).
- The "Also search cluster-b" checkbox on the search form calls `/logs/search/ccs`, which queries `app-logs,cluster-b:app-logs` in one request and tags each hit's origin cluster.

CCS is explicit per query, not "search everything": an index pattern like `cluster-b:app-logs` only touches clusters you actually name. Override the alias/host/port with `CCS_REMOTE_CLUSTER_ALIAS` / `CCS_REMOTE_HOST` / `CCS_REMOTE_PORT` (see `CrossClusterConfig`).

## JUnit Parameterized Tests

`@ParameterizedTest` replaces `@Test` on a method and tells JUnit to run it multiple times, once per set of
arguments. It always needs exactly one **source** annotation alongside it to say where those arguments come from:

| Annotation | Supplies |
|---|---|
| `@ValueSource` | a flat list of literals (one primitive/String/Class array) |
| `@EnumSource` | all (or a filtered subset) of an enum's constants |
| `@CsvSource` | multiple arguments per run, inline as CSV strings |
| `@CsvFileSource` | same as `@CsvSource`, but rows read from a `.csv` file on the classpath |
| `@MethodSource` | arguments generated by a static factory method - needed when the data isn't a simple literal |
| `@NullSource` / `@EmptySource` | adds a single `null` / empty-string run, often combined with `@ValueSource` via `@NullAndEmptySource` |
| `@ArgumentsSource` | a custom `ArgumentsProvider` class, for anything the built-in ones can't express |

**`@ValueSource`** feeds the test a list of literal values, one per run:
```java
@ParameterizedTest
@ValueSource(strings = {"INFO", "WARN", "ERROR"})
void acceptsKnownLogLevels(String level) {
    assertTrue(LogEntry.isValidLevel(level));
}
```

**`@EnumSource`** feeds the test every constant of an enum (or a filtered subset), instead of hand-typing the
literals - it stays correct automatically if a constant is added later:
```java
@ParameterizedTest
@EnumSource(LogLevel.class)
void allLevelsHaveAColor(LogLevel level) {
    assertNotNull(level.color());
}
```

Both require the `junit-jupiter-params` artifact on the classpath alongside `junit-jupiter-api`.

