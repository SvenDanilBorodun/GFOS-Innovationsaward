package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.UserDTO;
import com.gfos.ideaboard.entity.User;
import com.gfos.ideaboard.entity.UserRole;
import com.gfos.ideaboard.exception.ApiException;
import com.gfos.ideaboard.security.PasswordUtil;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class UserService {

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @Inject
    private PasswordUtil passwordUtil;

    public User findById(Long id) {
        return em.find(User.class, id);
    }

    public UserDTO getUserById(Long id) {
        User user = findById(id);
        if (user == null) {
            throw ApiException.notFound("User not found");
        }
        return UserDTO.fromEntity(user);
    }

    public List<UserDTO> getAllUsers() {
        List<User> users = em.createNamedQuery("User.findActive", User.class)
                .getResultList();
        return users.stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public UserDTO updateUser(Long id, String firstName, String lastName, String email) {
        User user = findById(id);
        if (user == null) {
            throw ApiException.notFound("User not found");
        }

        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        if (email != null && !email.equals(user.getEmail())) {
            // Prüfen, ob E-Mail bereits vergeben ist
            if (isEmailTaken(email, id)) {
                throw ApiException.conflict("Email already in use");
            }
            user.setEmail(email);
        }

        em.merge(user);
        return UserDTO.fromEntity(user);
    }

    @Transactional
    public void updateRole(Long id, UserRole role) {
        User user = findById(id);
        if (user == null) {
            throw ApiException.notFound("User not found");
        }
        user.setRole(role);
        em.merge(user);
    }

    @Transactional
    public void setUserActive(Long id, boolean isActive) {
        User user = findById(id);
        if (user == null) {
            throw ApiException.notFound("User not found");
        }
        user.setIsActive(isActive);
        em.merge(user);
    }

    @Transactional
    public void addXp(Long userId, int xpPoints) {
        User user = findById(userId);
        if (user != null) {
            user.addXp(xpPoints);
            em.merge(user);
        }
    }

    private boolean isEmailTaken(String email, Long excludeUserId) {
        Long count = em.createQuery(
                "SELECT COUNT(u) FROM User u WHERE u.email = :email AND u.id != :userId", Long.class)
                .setParameter("email", email)
                .setParameter("userId", excludeUserId)
                .getSingleResult();
        return count > 0;
    }

    public List<UserDTO> getLeaderboard(int limit) {
        List<User> users = em.createQuery(
                "SELECT u FROM User u WHERE u.isActive = true ORDER BY u.xpPoints DESC, u.level DESC", User.class)
                .setMaxResults(limit)
                .getResultList();
        return users.stream()
                .map(UserDTO::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = findById(userId);
        if (user == null) {
            throw ApiException.notFound("User not found");
        }

        if (!passwordUtil.verifyPassword(oldPassword, user.getPasswordHash())) {
            throw ApiException.badRequest("Current password is incorrect");
        }

        if (newPassword == null || newPassword.length() < 6) {
            throw ApiException.badRequest("New password must be at least 6 characters");
        }

        user.setPasswordHash(passwordUtil.hashPassword(newPassword));
        em.merge(user);
    }
}
