package com.myapp.cassandra;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Forces CassandraSessionProvider to initialize (connect, create keyspace/table) as soon as the
 * webapp deploys instead of lazily on the first request, and closes the session cleanly on undeploy.
 */
@WebListener
public class CassandraLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        CassandraSessionProvider.getSession();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        CassandraSessionProvider.getSession().close();
    }
}
