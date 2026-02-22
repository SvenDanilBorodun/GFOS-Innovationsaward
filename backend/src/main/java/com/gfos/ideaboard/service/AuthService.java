package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.AuthRequest;
import com.gfos.ideaboard.dto.AuthResponse;
import com.gfos.ideaboard.dto.RegisterRequest;
import com.gfos.ideaboard.dto.UserDTO;
import com.gfos.ideaboard.entity.User;
import com.gfos.ideaboard.entity.UserRole;
import com.gfos.ideaboard.exception.ApiException;
import com.gfos.ideaboard.security.JwtUtil;
import com.gfos.ideaboard.security.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

@ApplicationScoped
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @Inject
    private JwtUtil jwtUtil;

    @Inject
    private PasswordUtil passwordUtil;

    @Transactional
    public AuthResponse login(AuthRequest request) {
        logger.debug("Anmeldeversuch für Benutzername: {}", request.getUsername());

        User user = findByUsername(request.getUsername());

        if (user == null) {
            logger.warn("Anmeldung fehlgeschlagen: Benutzer nicht gefunden - {}", request.getUsername());
            throw ApiException.unauthorized("Ungültiger Benutzername oder Passwort");
        }

        if (!user.getIsActive()) {
            logger.warn("Anmeldung fehlgeschlagen: Konto deaktiviert - {}", request.getUsername());
            throw ApiException.unauthorized("Konto ist deaktiviert");
        }

        if (!passwordUtil.verifyPassword(request.getPassword(), user.getPasswordHash())) {
            logger.warn("Anmeldung fehlgeschlagen: Ungültiges Passwort für Benutzer - {}", request.getUsername());
            throw ApiException.unauthorized("Ungültiger Benutzername oder Passwort");
        }

        logger.info("Anmeldung erfolgreich für Benutzer: {} ({})", request.getUsername(), user.getRole());

        // Letzte Anmeldung aktualisieren
        user.setLastLogin(LocalDateTime.now());
        em.merge(user);

        return createAuthResponse(user);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Prüfe, ob Benutzername bereits existiert
        if (findByUsername(request.getUsername()) != null) {
            throw ApiException.conflict("Benutzername existiert bereits");
        }

        // Prüfe, ob E-Mail bereits existiert
        if (findByEmail(request.getEmail()) != null) {
            throw ApiException.conflict("E-Mail existiert bereits");
        }

        // Erstelle neuen Benutzer
        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordUtil.hashPassword(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(UserRole.EMPLOYEE);
        user.setXpPoints(0);
        user.setLevel(1);
        user.setIsActive(true);

        em.persist(user);
        em.flush();

        return createAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (!jwtUtil.isTokenValid(refreshToken) || !jwtUtil.isRefreshToken(refreshToken)) {
            throw ApiException.unauthorized("Ungültiger Aktualisierungstoken");
        }

        Long userId = jwtUtil.getUserIdFromToken(refreshToken);
        User user = em.find(User.class, userId);

        if (user == null || !user.getIsActive()) {
            throw ApiException.unauthorized("Benutzer nicht gefunden oder inaktiv");
        }

        return createAuthResponse(user);
    }

    private AuthResponse createAuthResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        return new AuthResponse(
                accessToken,
                refreshToken,
                UserDTO.fromEntity(user),
                jwtUtil.getAccessTokenExpiration()
        );
    }

    private User findByUsername(String username) {
        try {
            return em.createNamedQuery("User.findByUsername", User.class)
                    .setParameter("username", username)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    private User findByEmail(String email) {
        try {
            return em.createNamedQuery("User.findByEmail", User.class)
                    .setParameter("email", email)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
