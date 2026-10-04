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

