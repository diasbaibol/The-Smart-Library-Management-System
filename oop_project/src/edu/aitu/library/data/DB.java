package edu.aitu.library.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DB {
    private final String url;

    public DB(String url) {
        this.url = url;
    }

    public Connection getConnection() throws SQLException {
        // For SQLite: "jdbc:sqlite:library.db"
        return DriverManager.getConnection(url);
    }
}

