package com.oracle.svm.graalvisor.jdbc;

import java.sql.Connection;

public class ConnectionWrapper {

    private final Connection connection;
    private final String mapKey;

    ConnectionWrapper(Connection connection, String mapKey) {
        this.connection = connection;
        this.mapKey = mapKey;
    }

    Connection getConnection() {
        return this.connection;
    }

    String getMapKey() {
        return this.mapKey;
    }
}
