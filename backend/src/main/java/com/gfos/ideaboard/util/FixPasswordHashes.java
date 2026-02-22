package com.gfos.ideaboard.util;

import at.favre.lib.crypto.bcrypt.BCrypt;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class FixPasswordHashes {

    private static final int BCRYPT_COST = 12;
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/ideaboard";
    private static final String DB_USER = "ideaboard_user";
    private static final String DB_PASSWORD = "ideaboard123";

    public static void main(String[] args) {
        try {
            // Generieren Sie korrekte Hashes
            String adminHash = BCrypt.withDefaults().hashToString(BCRYPT_COST, "admin123".toCharArray());
            String userHash = BCrypt.withDefaults().hashToString(BCRYPT_COST, "password123".toCharArray());

            System.out.println("Verbindung zur Datenbank wird hergestellt...");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);

            // Aktualisieren Sie das Admin-Passwort
            String updateAdmin = "UPDATE users SET password_hash = ? WHERE username = ?";
            PreparedStatement stmt1 = conn.prepareStatement(updateAdmin);
            stmt1.setString(1, adminHash);
            stmt1.setString(2, "admin");
            int count1 = stmt1.executeUpdate();
            System.out.println("Admin-Passwort aktualisiert: " + count1 + " Zeile(n)");

            // Aktualisieren Sie Test-Benutzer-Passwörter
            String updateUsers = "UPDATE users SET password_hash = ? WHERE username IN (?, ?, ?)";
            PreparedStatement stmt2 = conn.prepareStatement(updateUsers);
            stmt2.setString(1, userHash);
            stmt2.setString(2, "jsmith");
            stmt2.setString(3, "mwilson");
            stmt2.setString(4, "tjohnson");
            int count2 = stmt2.executeUpdate();
            System.out.println("Test-Benutzer-Passwörter aktualisiert: " + count2 + " Zeile(n)");

            // Überprüfen Sie
            String query = "SELECT username, role FROM users ORDER BY username";
            PreparedStatement stmt3 = conn.prepareStatement(query);
            ResultSet rs = stmt3.executeQuery();

            System.out.println("\nBenutzer in der Datenbank:");
            while (rs.next()) {
                System.out.println("  - " + rs.getString("username") + " (" + rs.getString("role") + ")");
            }

            rs.close();
            stmt1.close();
            stmt2.close();
            stmt3.close();
            conn.close();

            System.out.println("\n✓ Passwort-Hashes erfolgreich aktualisiert!");
            System.out.println("\nSie können sich jetzt anmelden mit:");
            System.out.println("  admin / admin123");
            System.out.println("  jsmith / password123");
            System.out.println("  mwilson / password123");
            System.out.println("  tjohnson / password123");

        } catch (Exception e) {
            System.err.println("Fehler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
