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
