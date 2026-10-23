package com.myapp.kafka;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/** Starts the background consumer when the webapp deploys, stops it when it undeploys. */
@WebListener
public class KafkaLifecycleListener implements ServletContextListener {

    private KafkaConsumerService consumerService;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        consumerService = new KafkaConsumerService();
        Thread consumerThread = new Thread(consumerService, "kafka-console-consumer");
        consumerThread.setDaemon(true);
        consumerThread.start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (consumerService != null) {
            consumerService.shutdown();
        }
    }
}
