package com.gfos.ideaboard.integration;

import at.favre.lib.crypto.bcrypt.BCrypt;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify authentication system works correctly.
 * This test connects to the actual database to verify:
 * - User accounts exist
 * - Password hashes are correct
 * - BCrypt verification works
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AuthenticationIntegrationTest {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/ideaboard";
    private static final String DB_USER = "ideaboard_user";
    private static final String DB_PASSWORD = "ideaboard123";

    private static final Map<String, String> TEST_CREDENTIALS = new HashMap<>() {{
        put("admin", "admin123");
        put("jsmith", "password123");
        put("mwilson", "password123");
        put("tjohnson", "password123");
    }};

    private static Connection connection;

    @BeforeAll
    static void setUp() throws Exception {
        connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        assertNotNull(connection, "Database connection should be established");
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    @Order(1)
    @DisplayName("Database connection should be successful")
    void testDatabaseConnection() {
        assertDoesNotThrow(() -> {
            assertNotNull(connection);
            assertFalse(connection.isClosed());
        });
    }

    @Test
    @Order(2)
    @DisplayName("Admin user should exist and have correct password")
    void testAdminLogin() throws Exception {
        String query = "SELECT username, password_hash, role, is_active FROM users WHERE username = ?";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, "admin");
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "Admin user should exist in database");
        assertEquals("admin", rs.getString("username"));
        assertEquals("ADMIN", rs.getString("role"));
        assertTrue(rs.getBoolean("is_active"), "Admin should be active");

        String passwordHash = rs.getString("password_hash");
        BCrypt.Result result = BCrypt.verifyer().verify("admin123".toCharArray(), passwordHash);
        assertTrue(result.verified, "Admin password 'admin123' should verify successfully");

        rs.close();
        stmt.close();
    }

    @Test
    @Order(3)
    @DisplayName("All test users should exist with correct passwords")
    void testAllUserPasswords() throws Exception {
        String query = "SELECT username, password_hash, is_active FROM users WHERE username = ?";
        PreparedStatement stmt = connection.prepareStatement(query);

        for (Map.Entry<String, String> entry : TEST_CREDENTIALS.entrySet()) {
            String username = entry.getKey();
            String expectedPassword = entry.getValue();

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "User '" + username + "' should exist in database");
            assertTrue(rs.getBoolean("is_active"), "User '" + username + "' should be active");

            String passwordHash = rs.getString("password_hash");
            BCrypt.Result result = BCrypt.verifyer().verify(
                expectedPassword.toCharArray(),
                passwordHash
            );
            assertTrue(result.verified,
                "Password for user '" + username + "' should verify successfully. " +
                "Expected: '" + expectedPassword + "'"
            );

            rs.close();
        }
        stmt.close();
    }

    @Test
    @Order(4)
    @DisplayName("Wrong password should fail verification")
    void testWrongPasswordFails() throws Exception {
        String query = "SELECT password_hash FROM users WHERE username = ?";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, "admin");
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next());
        String passwordHash = rs.getString("password_hash");

        BCrypt.Result result = BCrypt.verifyer().verify("wrongpassword".toCharArray(), passwordHash);
        assertFalse(result.verified, "Wrong password should NOT verify");

        rs.close();
        stmt.close();
    }

    @Test
    @Order(5)
    @DisplayName("Users table should have required columns")
    void testUserTableStructure() throws Exception {
        String query = "SELECT id, username, email, password_hash, first_name, last_name, " +
                      "role, is_active, xp_points, level FROM users WHERE username = ?";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, "admin");
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next(), "Should be able to query all required columns");
        assertNotNull(rs.getLong("id"));
        assertNotNull(rs.getString("username"));
        assertNotNull(rs.getString("email"));
        assertNotNull(rs.getString("password_hash"));
        assertNotNull(rs.getString("role"));
        assertNotNull(rs.getBoolean("is_active"));
        assertNotNull(rs.getInt("xp_points"));
        assertNotNull(rs.getInt("level"));

        rs.close();
        stmt.close();
    }

    @Test
    @Order(6)
    @DisplayName("BCrypt hashes should use correct cost factor")
    void testBCryptCostFactor() throws Exception {
        String query = "SELECT password_hash FROM users WHERE username = ?";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, "admin");
        ResultSet rs = stmt.executeQuery();

        assertTrue(rs.next());
        String passwordHash = rs.getString("password_hash");

        // BCrypt hash format: $2a$12$... where 12 is the cost factor
        assertTrue(passwordHash.startsWith("$2a$12$"),
            "Password hash should use BCrypt cost factor 12");

        rs.close();
        stmt.close();
    }
}
