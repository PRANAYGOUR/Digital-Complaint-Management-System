package com.dcs.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Simple JDBC connector utility that leverages Spring Boot's auto-configured DataSource.
 *
 * Your datasource settings come from application.properties or environment variables.
 * Use this helper when you need a raw JDBC Connection alongside JPA.
 */
@Component
public class JdbcConnector {
    private final DataSource dataSource;

    @Autowired
    public JdbcConnector(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Obtain a JDBC Connection from the configured DataSource.
     * Caller is responsible for closing the connection (try-with-resources recommended).
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}