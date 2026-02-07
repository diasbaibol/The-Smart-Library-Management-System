package edu.aitu.library.data;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SchemaInitializer {

    public static void init(Connection conn) throws Exception {
        boolean oldAuto = conn.getAutoCommit();
        try {
            conn.setAutoCommit(false);

            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON;");
            }

            ensureUsers(conn);
            ensureBooks(conn);
            ensureLoans(conn);
            ensureReservations(conn);
            ensureIndexes(conn);

            conn.commit();
        } catch (Exception e) {
            try { conn.rollback(); } catch (Exception ignored) {}
            throw e;
        } finally {
            try { conn.setAutoCommit(oldAuto); } catch (Exception ignored) {}
        }
    }

    private static void ensureUsers(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    role TEXT NOT NULL
                );
            """);
        }
    }

    private static void ensureBooks(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS books (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    author TEXT NOT NULL,
                    available INTEGER NOT NULL CHECK (available IN (0,1))
                );
            """);
        }

        if (!columnExists(conn, "books", "available")) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE books ADD COLUMN available INTEGER;");
            }
        }

        try (Statement st = conn.createStatement()) {
            st.execute("UPDATE books SET available=1 WHERE available IS NULL;");
        }
    }

    private static void ensureLoans(Connection conn) throws Exception {
        if (!tableExists(conn, "loans")) {
            try (Statement st = conn.createStatement()) {
                st.execute("""
                    CREATE TABLE loans (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        user_id INTEGER NOT NULL,
                        book_id INTEGER NOT NULL,
                        loan_date TEXT NOT NULL,
                        due_date TEXT NOT NULL,
                        return_date TEXT NULL,
                        fine_cents INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY (user_id) REFERENCES users(id),
                        FOREIGN KEY (book_id) REFERENCES books(id)
                    );
                """);
            }
            return;
        }

        if (!columnExists(conn, "loans", "loan_date")) {
            throw new Exception("Existing loans table does not contain loan_date. Manual migration required.");
        }

        if (!columnExists(conn, "loans", "due_date")) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE loans ADD COLUMN due_date TEXT;");
            }
        }

        if (!columnExists(conn, "loans", "return_date")) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE loans ADD COLUMN return_date TEXT;");
            }
        }

        if (!columnExists(conn, "loans", "fine_cents")) {
            try (Statement st = conn.createStatement()) {
                st.execute("ALTER TABLE loans ADD COLUMN fine_cents INTEGER;");
            }
        }

        try (Statement st = conn.createStatement()) {
            st.execute("UPDATE loans SET fine_cents=0 WHERE fine_cents IS NULL;");
        }

        try (Statement st = conn.createStatement()) {
            st.execute("""
                UPDATE loans
                SET due_date =
                    CASE (SELECT role FROM users WHERE users.id = loans.user_id)
                        WHEN 'LIBRARIAN' THEN date(loans.loan_date, '+30 day')
                        ELSE date(loans.loan_date, '+14 day')
                    END
                WHERE due_date IS NULL;
            """);
        }
    }

    private static void ensureReservations(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE TABLE IF NOT EXISTS reservations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    book_id INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    created_at TEXT NOT NULL,
                    expires_at TEXT NOT NULL,
                    fulfilled_loan_id INTEGER NULL,
                    FOREIGN KEY(user_id) REFERENCES users(id),
                    FOREIGN KEY(book_id) REFERENCES books(id),
                    FOREIGN KEY(fulfilled_loan_id) REFERENCES loans(id)
                );
            """);
        }
    }

    private static void ensureIndexes(Connection conn) throws Exception {
        try (Statement st = conn.createStatement()) {
            st.execute("""
                CREATE INDEX IF NOT EXISTS idx_res_active_book
                ON reservations(book_id, status);
            """);
        }
    }

    private static boolean tableExists(Connection conn, String table) throws Exception {
        String sql = "SELECT name FROM sqlite_master WHERE type='table' AND name=? LIMIT 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private static boolean columnExists(Connection conn, String table, String column) throws Exception {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("PRAGMA table_info(" + table + ");")) {
            while (rs.next()) {
                String col = rs.getString("name");
                if (column.equalsIgnoreCase(col)) return true;
            }
            return false;
        }
    }
}
