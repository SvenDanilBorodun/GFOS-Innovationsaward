package com.gfos.ideaboard.security;

import at.favre.lib.crypto.bcrypt.BCrypt;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class PasswordUtil {

    // BCrypt-Kosten für Passwort-Hashing
    private static final int BCRYPT_COST = 12;

    // Hashen Sie das Klartextpasswort mit BCrypt
    public String hashPassword(String plainPassword) {
        return BCrypt.withDefaults().hashToString(BCRYPT_COST, plainPassword.toCharArray());
    }

    // Überprüfen Sie, ob das Klartextpasswort mit dem Hash übereinstimmt
    public boolean verifyPassword(String plainPassword, String hashedPassword) {
        BCrypt.Result result = BCrypt.verifyer().verify(plainPassword.toCharArray(), hashedPassword);
        return result.verified;
    }
}
