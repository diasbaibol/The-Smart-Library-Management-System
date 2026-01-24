package edu.aitu.library.data;

import java.sql.Connection;
import java.sql.Statement;

public class SchemaInitializer {

    public static void init(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    role TEXT NOT NULL
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    author TEXT NOT NULL,
                    available INTEGER NOT NULL CHECK (available IN (0,1))
                );
            """);

            st.execute("""
                CREATE TABLE IF NOT EXISTS loans (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    book_id INTEGER NOT NULL,
                    loan_date TEXT NOT NULL,
                    return_date TEXT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id),
                    FOREIGN KEY (book_id) REFERENCES books(id)
                );
            """);
        }
    }
}
