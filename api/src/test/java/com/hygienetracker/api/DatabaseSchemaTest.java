package com.hygienetracker.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Checks that the Flyway migrations build the schema in docs/erd.md on real PostgreSQL:
 * the tables exist, the constraints reject bad data, and deletes cascade as documented.
 * Each test runs in a transaction that is rolled back, so tests don't affect each other.
 */
@SpringBootTest
@Transactional
class DatabaseSchemaTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void allMigrationsAppliedSuccessfully() {
        List<String> versions = jdbc.queryForList(
                "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank",
                String.class);

        assertThat(versions).containsExactly("1", "2", "3");
    }

    @Test
    void showerActivityTypeIsSeeded() {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM activity_types WHERE name = 'SHOWER'", Integer.class);

        assertThat(count).isEqualTo(1);
    }

    @Test
    void newUserDefaultsToUserRole() {
        long userId = insertUser("new@example.com");

        String role = jdbc.queryForObject("SELECT role FROM users WHERE id = ?", String.class, userId);

        assertThat(role).isEqualTo("USER");
    }

    @Test
    void rejectsUserWithoutEmail() {
        assertThatThrownBy(() -> jdbc.update("INSERT INTO users (display_name) VALUES ('No Email')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsDuplicateEmail() {
        insertUser("same@example.com");

        assertThatThrownBy(() -> insertUser("same@example.com"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsUnknownRole() {
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO users (email, role) VALUES ('owner@example.com', 'OWNER')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void rejectsSameProviderAccountLinkedTwice() {
        long first = insertUser("first@example.com");
        long second = insertUser("second@example.com");
        insertIdentity(first, "GITHUB", "12345");

        assertThatThrownBy(() -> insertIdentity(second, "GITHUB", "12345"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingUserDeletesTheirIdentitiesShowersAndBans() {
        long userId = insertUser("leaving@example.com");
        long adminId = insertUser("admin@example.com");
        insertIdentity(userId, "GOOGLE", "google-sub-1");
        long logId = insertShower(userId);
        jdbc.update("INSERT INTO bans (user_id, admin_id) VALUES (?, ?)", userId, adminId);

        jdbc.update("DELETE FROM users WHERE id = ?", userId);

        assertThat(countWhere("identities", "user_id", userId)).isZero();
        assertThat(countWhere("activity_logs", "user_id", userId)).isZero();
        assertThat(countWhere("shower_details", "log_id", logId)).isZero();
        assertThat(countWhere("bans", "user_id", userId)).isZero();
    }

    @Test
    void deletingAdminKeepsBansTheyIssued() {
        long bannedId = insertUser("banned@example.com");
        long adminId = insertUser("admin@example.com");
        jdbc.update("INSERT INTO bans (user_id, admin_id, reason) VALUES (?, ?, 'spam')", bannedId, adminId);

        jdbc.update("DELETE FROM users WHERE id = ?", adminId);

        Long remainingAdmin = jdbc.queryForObject(
                "SELECT admin_id FROM bans WHERE user_id = ?", Long.class, bannedId);
        assertThat(remainingAdmin).isNull();
    }

    @Test
    void userCanOnlyBeBannedOnce() {
        long bannedId = insertUser("banned@example.com");
        jdbc.update("INSERT INTO bans (user_id) VALUES (?)", bannedId);

        assertThatThrownBy(() -> jdbc.update("INSERT INTO bans (user_id) VALUES (?)", bannedId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private long insertUser(String email) {
        return jdbc.queryForObject(
                "INSERT INTO users (email) VALUES (?) RETURNING id", Long.class, email);
    }

    private void insertIdentity(long userId, String provider, String providerUserId) {
        jdbc.update(
                "INSERT INTO identities (user_id, provider, provider_user_id) VALUES (?, ?, ?)",
                userId, provider, providerUserId);
    }

    private long insertShower(long userId) {
        long logId = jdbc.queryForObject("""
                INSERT INTO activity_logs (user_id, type_id, local_date, minutes)
                VALUES (?, (SELECT id FROM activity_types WHERE name = 'SHOWER'), CURRENT_DATE, 10)
                RETURNING id
                """, Long.class, userId);
        jdbc.update("INSERT INTO shower_details (log_id, shampoo) VALUES (?, TRUE)", logId);
        return logId;
    }

    private int countWhere(String table, String column, long value) {
        return jdbc.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, value);
    }
}
