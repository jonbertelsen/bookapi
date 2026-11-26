package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.config.Populate;
import app.config.SecurityConfig;
import app.entities.Member;
import app.entities.Role;
import io.javalin.Javalin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/** Opt-in integration tests against local PostgreSQL. Only test-created records are deleted. */
@EnabledIfSystemProperty(named = "library.integration", matches = "true")
class LibraryApiTest {
    private static Javalin app;
    private static String base;

    @BeforeAll
    static void start() {
        Populate.populate(HibernateConfig.getEntityManagerFactory("library"));
        app = ApplicationConfig.startServer(0);
        base = "http://localhost:" + app.port() + "/api/v1";
    }

    @AfterAll
    static void stop() {
        if (app != null) ApplicationConfig.stopServer(app);
        HibernateConfig.close();
    }

    @Test
    void listsMatchFrontendShape() {
        given().get(base + "/books").then().statusCode(200).body("size()", greaterThan(0))
                .body("[0].id", instanceOf(String.class)).body("[0].title", notNullValue());
        given().get(base + "/members/1").then().statusCode(200).body("id", equalTo("1"));
        given().get(base + "/loans").then().statusCode(200)
                .body("[0].dueDate", matchesPattern("\\d{4}-\\d{2}-\\d{2}"));
    }

    @Test
    void loanLifecycleAndConflict() {
        String bookId = null, memberId = null, loanId = null;
        try {
            bookId = given().contentType("application/json")
                    .body(Map.of("title", "API integration test", "author", "Test"))
                    .post(base + "/books").then().statusCode(201).extract().path("id");
            memberId = given().contentType("application/json").body(Map.of("name", "API integration test"))
                    .post(base + "/members").then().statusCode(201).extract().path("id");
            var loan = Map.of("bookId", bookId, "memberId", memberId, "dueDate", "2026-12-10", "returned", false);
            loanId = given().contentType("application/json").body(loan).post(base + "/loans")
                    .then().statusCode(201).body("bookId", equalTo(bookId))
                    .body("memberId", equalTo(memberId)).body("dueDate", equalTo("2026-12-10"))
                    .extract().path("id");
            given().contentType("application/json").body(loan).post(base + "/loans").then().statusCode(409);
            given().delete(base + "/books/" + bookId).then().statusCode(409);
            given().contentType("application/json").body(Map.of("returned", true))
                    .patch(base + "/loans/" + loanId).then().statusCode(200)
                    .body("returned", equalTo(true)).body("bookId", equalTo(bookId))
                    .body("dueDate", equalTo("2026-12-10"));
            // Borrowing becomes possible again while returned loan history still exists.
            String nextLoanId = given().contentType("application/json").body(loan).post(base + "/loans")
                    .then().statusCode(201).extract().path("id");
            given().delete(base + "/loans/" + nextLoanId).then().statusCode(204);
            given().contentType("application/json").body(Map.of("title", "Updated title", "author", "Test"))
                    .put(base + "/books/" + bookId).then().statusCode(200).body("title", equalTo("Updated title"));
        } finally {
            if (loanId != null) given().delete(base + "/loans/" + loanId).then().statusCode(204);
            if (bookId != null) given().delete(base + "/books/" + bookId).then().statusCode(204);
            if (memberId != null) given().delete(base + "/members/" + memberId).then().statusCode(204);
        }
    }

    @Test
    void invalidRequestsReturnClientErrors() {
        given().contentType("application/json").body("null")
                .post(base + "/books").then().statusCode(400);
        given().contentType("application/json").body("")
                .post(base + "/members").then().statusCode(400);
        given().contentType("application/json").body(Map.of("title", " ", "author", "Test"))
                .post(base + "/books").then().statusCode(400);
        given().contentType("application/json").body("{broken")
                .post(base + "/books").then().statusCode(400);
        given().get(base + "/books/nonexistent-id").then().statusCode(404);
        given().contentType("application/json")
                .body(Map.of("bookId", "missing", "memberId", "1", "dueDate", "not-a-date", "returned", false))
                .post(base + "/loans").then().statusCode(400);
        given().contentType("application/json")
                .body(Map.of("bookId", "missing", "memberId", "1", "dueDate", "2026-12-10", "returned", false))
                .post(base + "/loans").then().statusCode(404);
    }

    @Test
    void corsAllowsReactDevServer() {
        given().header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "PATCH")
                .header("Access-Control-Request-Headers", "content-type")
                .options(base + "/loans/1").then().statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:5173"));
    }
    @Test
    void loginReturnsJwtAndAuthenticatedMember() {
        String token = given().contentType("application/json")
                .body(Map.of("username", "1", "password", "1234"))
                .post(base + "/auth/login").then().statusCode(200)
                .header("Cache-Control", equalTo("no-store"))
                .body("tokenType", equalTo("Bearer")).body("expiresIn", equalTo(3600))
                .body("member.id", equalTo("1")).body("member.username", equalTo("1"))
                .body("member", not(hasKey("password"))).body("member", not(hasKey("passwordHash")))
                .extract().path("token");
        given().header("Authorization", "Bearer " + token).get(base + "/auth/me")
                .then().statusCode(200).body("id", equalTo("1"))
                .body("username", equalTo("1")).body("$", not(hasKey("passwordHash")));
        given().get(base + "/auth/me").then().statusCode(401);
        given().header("Authorization", "Bearer invalid").get(base + "/auth/me").then().statusCode(401);
        given().contentType("application/json").body(Map.of("username", "1", "password", "wrong"))
                .post(base + "/auth/login").then().statusCode(401).body("message", equalTo("Invalid username or password"));
        given().contentType("application/json").body(Map.of("username", "does-not-exist", "password", "1234"))
                .post(base + "/auth/login").then().statusCode(401).body("message", equalTo("Invalid username or password"));
        given().contentType("application/json").body(Map.of("username", "1"))
                .post(base + "/auth/login").then().statusCode(400);
        given().contentType("application/json").body(Map.of("username", "", "password", "secret-not-to-echo"))
                .post(base + "/auth/login").then().statusCode(400)
                .body(not(containsString("secret-not-to-echo")));
        given().contentType("application/json").body(Map.of("username", "2", "password", "1234"))
                .post(base + "/auth/login").then().statusCode(200).body("member.id", equalTo("2"));
        given().header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .header("Access-Control-Request-Headers", "authorization")
                .options(base + "/auth/me").then().statusCode(200)
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:5173"));
    }

    @Test
    void newMemberGetsDefaultPasswordAndHashIsNeverReturned() {
        String id = null;
        try {
            id = given().contentType("application/json").body(Map.of("name", "Security test"))
                    .post(base + "/members").then().statusCode(201)
                    .body("$", not(hasKey("passwordHash"))).extract().path("id");
            String token = given().contentType("application/json").body(Map.of("username", id, "password", "1234"))
                    .post(base + "/auth/login").then().statusCode(200).extract().path("token");
            given().header("Authorization", "Bearer " + token).get(base + "/auth/me")
                    .then().statusCode(200).body("id", equalTo(id));
            try (var em = HibernateConfig.getEntityManagerFactory("library").createEntityManager()) {
                var member = em.find(Member.class, id);
                Assertions.assertTrue(member.getPasswordHash().startsWith("$2a$12$"));
                Assertions.assertTrue(member.verifyPassword("1234"));
                String hash = member.getPasswordHash();
                SecurityConfig.initializeMembers(HibernateConfig.getEntityManagerFactory("library"));
                em.refresh(member);
                Assertions.assertEquals(hash, member.getPasswordHash(), "Restart must preserve password hashes");
            }
            given().delete(base + "/members/" + id).then().statusCode(204);
            id = null;
            given().header("Authorization", "Bearer " + token).get(base + "/auth/me").then().statusCode(401);
        } finally {
            if (id != null) given().delete(base + "/members/" + id).then().statusCode(204);
        }
    }

    @Test
    void seededRolesAppearInLoginAndProfile() {
        for (String[] expected : new String[][]{{"1", "user"}, {"2", "admin"}}) {
            String token = given().contentType("application/json")
                    .body(Map.of("username", expected[0], "password", "1234"))
                    .post(base + "/auth/login").then().statusCode(200)
                    .body("member.roles", contains(expected[1])).extract().path("token");
            Assertions.assertEquals(java.util.List.of(expected[1]),
                    com.auth0.jwt.JWT.decode(token).getClaim("roles").asList(String.class));
            given().header("Authorization", "Bearer " + token).get(base + "/auth/me")
                    .then().statusCode(200).body("roles", contains(expected[1]));
        }
    }

    @Test
    void multipleRolesPersistAndMemberRequestsCannotGrantRoles() {
        String id = null;
        try {
            // Roles are output-only: the public member API cannot grant admin access.
            id = given().contentType("application/json")
                    .body(Map.of("name", "Multiple roles test", "roles", java.util.List.of("admin")))
                    .post(base + "/members").then().statusCode(201)
                    .body("roles", contains("user")).extract().path("id");
            String originalToken = given().contentType("application/json")
                    .body(Map.of("username", id, "password", "1234"))
                    .post(base + "/auth/login").then().statusCode(200).extract().path("token");
            var emf = HibernateConfig.getEntityManagerFactory("library");
            try (var em = emf.createEntityManager()) {
                em.getTransaction().begin();
                var member = em.find(Member.class, id);
                member.addRole(em.find(Role.class, "admin"));
                em.getTransaction().commit();
            }
            SecurityConfig.initializeMembers(emf);
            String bothRolesToken = given().contentType("application/json")
                    .body(Map.of("username", id, "password", "1234"))
                    .post(base + "/auth/login").then().statusCode(200)
                    .body("member.roles", containsInAnyOrder("user", "admin")).extract().path("token");
            given().header("Authorization", "Bearer " + bothRolesToken).get(base + "/auth/me")
                    .then().statusCode(200).body("roles", containsInAnyOrder("user", "admin"));
            try (var em = emf.createEntityManager()) {
                em.getTransaction().begin();
                em.find(Member.class, id).removeRole("user");
                em.getTransaction().commit();
            }
            // A token with a role that has since been removed no longer grants access.
            given().header("Authorization", "Bearer " + originalToken).get(base + "/auth/me")
                    .then().statusCode(403);
            given().header("Authorization", "Bearer " + bothRolesToken).get(base + "/auth/me")
                    .then().statusCode(200).body("roles", contains("admin"));
            given().delete(base + "/members/" + id).then().statusCode(204);
            id = null;
            try (var em = emf.createEntityManager()) {
                Assertions.assertNotNull(em.find(Role.class, "user"));
                Assertions.assertNotNull(em.find(Role.class, "admin"));
            }
        } finally {
            if (id != null) given().delete(base + "/members/" + id).then().statusCode(204);
        }
    }

}
