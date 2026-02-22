package com.gfos.ideaboard.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * Datenbankvalidierungsprogramm, um sicherzustellen, dass Seed-Daten korrekt sind.
 * Führen Sie dies nach der Datenbankinitialisierung aus, um zu überprüfen, dass alles funktioniert.
 */
public class ValidateDatabase {

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/ideaboard";
    private static final String DB_USER = "ideaboard_user";
    private static final String DB_PASSWORD = "ideaboard123";

    // Erwartete Test-Anmeldedaten
    private static final Map<String, String> TEST_CREDENTIALS = new HashMap<>() {{
        put("admin", "admin123");
        put("jsmith", "password123");
        put("mwilson", "password123");
        put("tjohnson", "password123");
    }};

    private static int testsRun = 0;
    private static int testsPassed = 0;
    private static int testsFailed = 0;

    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  DATENBANKVALIDIERUNG");
        System.out.println("========================================\n");

        try {
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("✓ Datenbankverbindung erfolgreich\n");

            validateUserPasswords(conn);
            validateTableCounts(conn);
            validateDataIntegrity(conn);

            conn.close();

            System.out.println("\n========================================");
            System.out.println("  VALIDIERUNGSZUSAMMENFASSUNG");
            System.out.println("========================================");
            System.out.println("Durchgeführte Tests:    " + testsRun);
            System.out.println("Bestandene Tests:       " + testsPassed + " ✓");
            System.out.println("Fehlgeschlagene Tests: " + testsFailed + " ✗");

            if (testsFailed == 0) {
                System.out.println("\n✓ ALLE VALIDIERUNGSPRÜFUNGEN BESTANDEN!");
                System.out.println("  Die Datenbank ist einsatzbereit.");
                System.exit(0);
            } else {
                System.out.println("\n✗ VALIDIERUNG FEHLGESCHLAGEN!");
                System.out.println("  Bitte überprüfen Sie die obigen Fehler.");
                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println("✗ FATALER FEHLER: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void validateUserPasswords(Connection conn) throws Exception {
        System.out.println("--- Benutzerauthentifizierung testen ---");

        String query = "SELECT username, password_hash, role FROM users WHERE username = ?";
        PreparedStatement stmt = conn.prepareStatement(query);

        for (Map.Entry<String, String> entry : TEST_CREDENTIALS.entrySet()) {
            String username = entry.getKey();
            String expectedPassword = entry.getValue();

            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();

            testsRun++;
            if (rs.next()) {
                String passwordHash = rs.getString("password_hash");
                String role = rs.getString("role");

                BCrypt.Result result = BCrypt.verifyer().verify(
                    expectedPassword.toCharArray(),
                    passwordHash
                );

                if (result.verified) {
                    System.out.println("  ✓ " + username + " (" + role + "): Passwort verifiziert");
                    testsPassed++;
                } else {
                    System.out.println("  ✗ " + username + " (" + role + "): Passwortverifikation FEHLGESCHLAGEN");
                    System.out.println("    Erwartet: " + expectedPassword);
                    System.out.println("    Hash stimmt nicht überein!");
                    testsFailed++;
                }
            } else {
                System.out.println("  ✗ " + username + ": Benutzer NICHT in der Datenbank gefunden");
                testsFailed++;
            }
            rs.close();
        }
        stmt.close();
        System.out.println();
    }

    private static void validateTableCounts(Connection conn) throws Exception {
        System.out.println("--- Überprüfung der Tabellenzahl ---");

        // Benutzer und Abzeichen werden als Seed-Daten erwartet
        String[] requiredTables = { "users", "badges" };

        // Diese Tabellen sollten existieren, aber können leer sein
        String[] optionalTables = {
            "ideas", "comments", "likes",
            "surveys", "survey_options", "idea_tags"
        };

        PreparedStatement stmt;

        // Erforderliche Tabellen mit Daten prüfen
        for (String table : requiredTables) {
            testsRun++;
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                if (count > 0) {
                    System.out.println("  ✓ " + table + ": " + count + " Zeilen");
                    testsPassed++;
                } else {
                    System.out.println("  ✗ " + table + ": LEER (erwartet Seed-Daten)");
                    testsFailed++;
                }
            }
            rs.close();
            stmt.close();
        }

        // Optionale Tabellen nur auf Existenz prüfen
        for (String table : optionalTables) {
            testsRun++;
            stmt = conn.prepareStatement("SELECT COUNT(*) FROM " + table);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("  ✓ " + table + ": " + count + " Zeilen (Tabelle existiert)");
                testsPassed++;
            }
            rs.close();
            stmt.close();
        }
        System.out.println();
    }

    private static void validateDataIntegrity(Connection conn) throws Exception {
        System.out.println("--- Überprüfung der Datenintegrität ---");

        // Überprüfen Sie, ob der Admin-Benutzer existiert und aktiv ist
        testsRun++;
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT is_active, role FROM users WHERE username = 'admin'"
        );
        ResultSet rs = stmt.executeQuery();
        if (rs.next() && rs.getBoolean("is_active") && "ADMIN".equals(rs.getString("role"))) {
            System.out.println("  ✓ Admin-Benutzer existiert und ist aktiv");
            testsPassed++;
        } else {
            System.out.println("  ✗ Admin-Benutzer fehlt oder ist inaktiv");
            testsFailed++;
        }
        rs.close();
        stmt.close();

        // Überprüfen Sie, ob Ideen gültige Autoren haben (falls vorhanden)
        testsRun++;
        stmt = conn.prepareStatement(
            "SELECT COUNT(*) FROM ideas i LEFT JOIN users u ON i.author_id = u.id WHERE u.id IS NULL"
        );
        rs = stmt.executeQuery();
        if (rs.next() && rs.getInt(1) == 0) {
            System.out.println("  ✓ Alle Ideen haben gültige Autoren");
            testsPassed++;
        } else {
            System.out.println("  ✗ Einige Ideen haben ungültige Autorverweise");
            testsFailed++;
        }
        rs.close();
        stmt.close();

        // Überprüfen Sie, ob Abzeichen definiert sind
        testsRun++;
        stmt = conn.prepareStatement("SELECT COUNT(*) FROM badges WHERE is_active = true");
        rs = stmt.executeQuery();
        if (rs.next() && rs.getInt(1) >= 3) {
            System.out.println("  ✓ Abzeichensystem konfiguriert (" + rs.getInt(1) + " Abzeichen)");
            testsPassed++;
        } else {
            System.out.println("  ✗ Unzureichende Abzeichen konfiguriert (mindestens 3 erforderlich)");
            testsFailed++;
        }
        rs.close();
        stmt.close();

        System.out.println();
    }
}
