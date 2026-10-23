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

