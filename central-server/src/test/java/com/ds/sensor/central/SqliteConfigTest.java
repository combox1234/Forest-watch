package com.ds.sensor.central;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class SqliteConfigTest {

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("Verify SQLite WAL Mode and Busy Timeout")
    public void testSqlitePragmasAndWriteAccess() throws Exception {
        assertNotNull(dataSource, "DataSource bean must not be null");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // Check journal_mode
            try (ResultSet rs = stmt.executeQuery("PRAGMA journal_mode;")) {
                assertTrue(rs.next(), "PRAGMA journal_mode must return a row");
                String journalMode = rs.getString(1);
                System.out.println("Current journal_mode: " + journalMode);
                assertEquals("wal", journalMode.toLowerCase(), "SQLite journal_mode should be WAL");
            }

            // Check busy_timeout
            try (ResultSet rs = stmt.executeQuery("PRAGMA busy_timeout;")) {
                assertTrue(rs.next(), "PRAGMA busy_timeout must return a row");
                int busyTimeout = rs.getInt(1);
                System.out.println("Current busy_timeout: " + busyTimeout);
                assertEquals(10000, busyTimeout, "SQLite busy_timeout must be 10000ms");
            }

            // Verify write access to SQLite database
            stmt.execute("CREATE TABLE IF NOT EXISTS _write_probe (id INTEGER PRIMARY KEY, ts TEXT);");
            stmt.execute("INSERT INTO _write_probe (ts) VALUES (datetime('now'));");
            try (ResultSet rs = stmt.executeQuery("SELECT count(*) FROM _write_probe;")) {
                assertTrue(rs.next());
                assertTrue(rs.getInt(1) > 0, "Must be able to write and read from SQLite table");
            }
            stmt.execute("DROP TABLE _write_probe;");
        }
    }
}
