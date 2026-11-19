package com.hygienetracker.api.controller;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.hygienetracker.api.auth.AppTokenService;
import com.hygienetracker.api.auth.AuthUser;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * {@code GET /api/me/stats} end to end over HTTP: the real security filter chain, controller,
 * service and a Testcontainers PostgreSQL with the Flyway schema (#94, PRD-5).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatsApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AppTokenService tokens;

    @Autowired
    private Clock clock;

    /** Users created by a test, deleted afterwards (their showers cascade). */
    private final List<Long> createdUsers = new ArrayList<>();

    @BeforeEach
    void pointRestAssuredAtTheServer() {
        RestAssured.port = port;
    }

    @AfterEach
    void deleteCreatedUsers() {
        createdUsers.forEach(id -> jdbc.update("DELETE FROM users WHERE id = ?", id));
    }

    @Test
    void lastShowerThreeDaysAgoGivesThreeDaysAndSixPointsLost() {
        long userId = createUser("three-days@example.com");
        logShower(userId, today().minusDays(3));

        given().auth().oauth2(tokenFor(userId))
        .when().get("/api/me/stats")
        .then()
                .statusCode(200)
                .contentType(ContentType.JSON)
                .body("daysSinceLastShower", equalTo(3))
                .body("pointsLostToMissedDays", equalTo(6))
                .body("streak", equalTo(0))
                .body("score", equalTo(4));
    }

    @Test
    void neverShoweredGivesZerosAndNullDaysSinceLastShower() {
        long userId = createUser("never@example.com");

        given().auth().oauth2(tokenFor(userId))
        .when().get("/api/me/stats")
        .then()
                .statusCode(200)
                .body("streak", equalTo(0))
                .body("score", equalTo(0))
                .body("daysSinceLastShower", nullValue())
                .body("pointsLostToMissedDays", equalTo(0));
    }

    @Test
    void noTokenGets401() {
        given()
        .when().get("/api/me/stats")
        .then()
                .statusCode(401);
    }

    @Test
    void countsOnlyTheSignedInUsersShowers() {
        long me = createUser("me@example.com");
        long someoneElse = createUser("someone-else@example.com");
        logShower(someoneElse, today());
        logShower(someoneElse, today().minusDays(1));

        given().auth().oauth2(tokenFor(me))
        .when().get("/api/me/stats")
        .then()
                .statusCode(200)
                .body("streak", equalTo(0))
                .body("score", equalTo(0))
                .body("daysSinceLastShower", nullValue());
    }

    @Test
    void responseHasExactlyTheFieldsOfTheStatsSchema() {
        long userId = createUser("schema@example.com");
        logShower(userId, today());

        given().auth().oauth2(tokenFor(userId))
        .when().get("/api/me/stats")
        .then()
                .statusCode(200)
                .body("keySet().sort()", equalTo(
                        List.of("daysSinceLastShower", "pointsLostToMissedDays", "score", "streak")));
    }

    private LocalDate today() {
        return LocalDate.now(clock);
    }

    private long createUser(String email) {
        long id = jdbc.queryForObject("INSERT INTO users (email) VALUES (?) RETURNING id", Long.class, email);
        createdUsers.add(id);
        return id;
    }

    private void logShower(long userId, LocalDate day) {
        jdbc.update("""
                INSERT INTO activity_logs (user_id, type_id, local_date, minutes)
                VALUES (?, (SELECT id FROM activity_types WHERE name = 'SHOWER'), ?, 10)
                """, userId, day);
    }

    private String tokenFor(long userId) {
        return tokens.signIn(AuthUser.newUser(userId, "user" + userId + "@example.com")).token();
    }
}
