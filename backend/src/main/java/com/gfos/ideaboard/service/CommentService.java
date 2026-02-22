package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.CommentDTO;
import com.gfos.ideaboard.entity.*;
import com.gfos.ideaboard.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class CommentService {

    private static final int MAX_COMMENT_LENGTH = 200;
    private static final int XP_FOR_COMMENT = 5;

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @Inject
    private UserService userService;

    @Inject
    private NotificationService notificationService;

    @Inject
    private GamificationService gamificationService;

    public List<CommentDTO> getCommentsByIdea(Long ideaId, Long currentUserId) {
        List<Comment> comments = em.createNamedQuery("Comment.findByIdea", Comment.class)
                .setParameter("ideaId", ideaId)
                .getResultList();
        return comments.stream()
                .map(comment -> CommentDTO.fromEntity(comment, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentDTO createComment(Long ideaId, String content, Long authorId) {
        if (content == null || content.trim().isEmpty()) {
            throw ApiException.badRequest("Kommentarinhalt ist erforderlich");
        }
        if (content.length() > MAX_COMMENT_LENGTH) {
            throw ApiException.badRequest("Kommentar muss " + MAX_COMMENT_LENGTH + " Zeichen oder weniger sein");
        }

        Idea idea = em.find(Idea.class, ideaId);
        if (idea == null) {
            throw ApiException.notFound("Idee nicht gefunden");
        }

        User author = em.find(User.class, authorId);
        if (author == null) {
            throw ApiException.notFound("Benutzer nicht gefunden");
        }

        Comment comment = new Comment();
        comment.setIdea(idea);
        comment.setAuthor(author);
        comment.setContent(content.trim());
        em.persist(comment);

        // Hinweis: comment_count wird automatisch durch Datenbank-Trigger aktualisiert
        // NICHT manuell inkrementieren, um Doppelzählung zu vermeiden

        // XP vergeben und Abzeichen prüfen
        gamificationService.awardXpForComment(authorId);

        // Ideenschöpfer benachrichtigen (falls nicht auf eigener Idee kommentiert)
        if (!idea.getAuthor().getId().equals(authorId)) {
            notificationService.notifyComment(idea, author, content);
        }

        return CommentDTO.fromEntity(comment, authorId);
    }

    @Transactional
    public void deleteComment(Long commentId, Long currentUserId) {
        Comment comment = em.find(Comment.class, commentId);
        if (comment == null) {
            throw ApiException.notFound("Kommentar nicht gefunden");
        }

        // Berechtigung prüfen
        User currentUser = em.find(User.class, currentUserId);
        if (!comment.getAuthor().getId().equals(currentUserId) &&
            currentUser.getRole() != UserRole.ADMIN) {
            throw ApiException.forbidden("Nicht berechtigt, diesen Kommentar zu löschen");
        }

        // Hinweis: comment_count wird automatisch durch Datenbank-Trigger aktualisiert
        // NICHT manuell dekrementieren, um Doppelzählung zu vermeiden

        em.remove(comment);
    }

    @Transactional
    public void addReaction(Long commentId, String emoji, Long userId) {
        Comment comment = em.find(Comment.class, commentId);
        if (comment == null) {
            throw ApiException.notFound("Kommentar nicht gefunden");
        }

        User user = em.find(User.class, userId);
        if (user == null) {
            throw ApiException.notFound("Benutzer nicht gefunden");
        }

        // Prüfe, ob bereits mit diesem Emoji reagiert
        CommentReaction existing = findReaction(commentId, userId, emoji);
        if (existing != null) {
            throw ApiException.conflict("Bereits mit diesem Emoji reagiert");
        }

        CommentReaction reaction = new CommentReaction();
        reaction.setComment(comment);
        reaction.setUser(user);
        reaction.setEmoji(emoji);
        em.persist(reaction);

        // Hinweis: reaction_count wird automatisch durch Datenbank-Trigger aktualisiert
        // NICHT manuell inkrementieren, um Doppelzählung zu vermeiden

        // Kommentarautor benachrichtigen
        if (!comment.getAuthor().getId().equals(userId)) {
            notificationService.notifyReaction(comment, user, emoji);
        }
    }

    @Transactional
    public void removeReaction(Long commentId, String emoji, Long userId) {
        CommentReaction reaction = findReaction(commentId, userId, emoji);
        if (reaction == null) {
            throw ApiException.notFound("Reaktion nicht gefunden");
        }

        // Hinweis: reaction_count wird automatisch durch Datenbank-Trigger aktualisiert
        // NICHT manuell dekrementieren, um Doppelzählung zu vermeiden

        em.remove(reaction);
    }

    private CommentReaction findReaction(Long commentId, Long userId, String emoji) {
        try {
            return em.createNamedQuery("CommentReaction.findByCommentAndUserAndEmoji", CommentReaction.class)
                    .setParameter("commentId", commentId)
                    .setParameter("userId", userId)
                    .setParameter("emoji", emoji)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}
