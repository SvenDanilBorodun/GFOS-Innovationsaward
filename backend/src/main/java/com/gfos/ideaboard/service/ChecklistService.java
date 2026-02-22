package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.ChecklistItemDTO;
import com.gfos.ideaboard.dto.ChecklistToggleResponse;
import com.gfos.ideaboard.entity.ChecklistItem;
import com.gfos.ideaboard.entity.Idea;
import com.gfos.ideaboard.entity.IdeaStatus;
import com.gfos.ideaboard.entity.User;
import com.gfos.ideaboard.entity.UserRole;
import com.gfos.ideaboard.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class ChecklistService {

    private static final int MAX_TITLE_LENGTH = 200;

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    public List<ChecklistItemDTO> getChecklistByIdea(Long ideaId) {
        List<ChecklistItem> items = em.createNamedQuery("ChecklistItem.findByIdea", ChecklistItem.class)
                .setParameter("ideaId", ideaId)
                .getResultList();

        return items.stream()
                .map(ChecklistItemDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Prüft, ob der Benutzer die Checkliste bearbeiten darf.
     * Erlaubt für: Autor der Idee, PROJECT_MANAGER oder ADMIN
     */
    private boolean canEditChecklist(Idea idea, Long currentUserId) {
        User currentUser = em.find(User.class, currentUserId);
        if (currentUser == null) return false;

        // Autor kann bearbeiten
        if (idea.getAuthor().getId().equals(currentUserId)) return true;

        // PM/Admin kann bearbeiten
        return currentUser.getRole() == UserRole.PROJECT_MANAGER ||
               currentUser.getRole() == UserRole.ADMIN;
    }

    /**
     * Prüft, ob die Checkliste bearbeitet werden kann (nicht bei COMPLETED Status)
     */
    private void validateChecklistEditable(Idea idea) {
        if (idea.getStatus() == IdeaStatus.COMPLETED) {
            throw ApiException.badRequest("Checkliste kann bei abgeschlossenen Ideen nicht bearbeitet werden");
        }
    }

    @Transactional
    public ChecklistItemDTO createChecklistItem(Long ideaId, String title, Long currentUserId) {
        // Eingabe validieren
        if (title == null || title.trim().isEmpty()) {
            throw ApiException.badRequest("Checklistenelement-Titel ist erforderlich");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw ApiException.badRequest("Checklistenelement-Titel muss " + MAX_TITLE_LENGTH + " Zeichen oder weniger sein");
        }

        Idea idea = em.find(Idea.class, ideaId);
        if (idea == null) {
            throw ApiException.notFound("Idee nicht gefunden");
        }

        // Prüfe Bearbeitungsberechtigung (Autor, PM oder Admin)
        if (!canEditChecklist(idea, currentUserId)) {
            throw ApiException.forbidden("Nicht berechtigt, Checklistenelemente hinzuzufügen");
        }

        // Prüfe, ob die Idee nicht abgeschlossen ist
        validateChecklistEditable(idea);

        // Nächste Ordinalposition abrufen
        Integer maxPosition = em.createQuery(
                "SELECT COALESCE(MAX(c.ordinalPosition), -1) FROM ChecklistItem c WHERE c.idea.id = :ideaId", Integer.class)
                .setParameter("ideaId", ideaId)
                .getSingleResult();

        ChecklistItem item = new ChecklistItem();
        item.setIdea(idea);
        item.setTitle(title.trim());
        item.setIsCompleted(false);
        item.setOrdinalPosition(maxPosition + 1);

        em.persist(item);

        // Ideenfortschritt basierend auf der Checkliste aktualisieren
        updateIdeaProgress(idea);

        return ChecklistItemDTO.fromEntity(item);
    }

    @Transactional
    public ChecklistToggleResponse toggleChecklistItem(Long ideaId, Long itemId, Long currentUserId) {
        Idea idea = em.find(Idea.class, ideaId);
        if (idea == null) {
            throw ApiException.notFound("Idee nicht gefunden");
        }

        // Prüfe Bearbeitungsberechtigung (Autor, PM oder Admin)
        if (!canEditChecklist(idea, currentUserId)) {
            throw ApiException.forbidden("Nicht berechtigt, Checklistenelemente zu aktualisieren");
        }

        // Prüfe, ob die Idee nicht abgeschlossen ist
        validateChecklistEditable(idea);

        ChecklistItem item = em.find(ChecklistItem.class, itemId);
        if (item == null) {
            throw ApiException.notFound("Checklistenelement nicht gefunden");
        }

        // Prüfe, ob das Element zu dieser Idee gehört
        if (!item.getIdea().getId().equals(ideaId)) {
            throw ApiException.badRequest("Checklistenelement gehört nicht zu dieser Idee");
        }

        // Fertigstellungsstatus umschalten
        item.setIsCompleted(!item.getIsCompleted());
        em.merge(item);

        // Ideenfortschritt basierend auf Checklistenfertigstellung aktualisieren
        // und Statusübergänge prüfen
        StatusTransitionResult result = updateIdeaProgressWithTransitions(idea);

        return new ChecklistToggleResponse(
            ChecklistItemDTO.fromEntity(item),
            result.transitionedToInProgress,
            result.allTodosCompleted
        );
    }

    @Transactional
    public void deleteChecklistItem(Long ideaId, Long itemId, Long currentUserId) {
        Idea idea = em.find(Idea.class, ideaId);
        if (idea == null) {
            throw ApiException.notFound("Idee nicht gefunden");
        }

        // Prüfe Bearbeitungsberechtigung (Autor, PM oder Admin)
        if (!canEditChecklist(idea, currentUserId)) {
            throw ApiException.forbidden("Nicht berechtigt, Checklistenelemente zu löschen");
        }

        // Prüfe, ob die Idee nicht abgeschlossen ist
        validateChecklistEditable(idea);

        ChecklistItem item = em.find(ChecklistItem.class, itemId);
        if (item == null) {
            throw ApiException.notFound("Checklistenelement nicht gefunden");
        }

        // Prüfe, ob das Element zu dieser Idee gehört
        if (!item.getIdea().getId().equals(ideaId)) {
            throw ApiException.badRequest("Checklistenelement gehört nicht zu dieser Idee");
        }

        em.remove(item);

        // Ideenfortschritt aktualisieren
        updateIdeaProgress(idea);
    }

    @Transactional
    public ChecklistItemDTO updateChecklistItem(Long ideaId, Long itemId, String title, Long currentUserId) {
        Idea idea = em.find(Idea.class, ideaId);
        if (idea == null) {
            throw ApiException.notFound("Idee nicht gefunden");
        }

        // Prüfe Bearbeitungsberechtigung (Autor, PM oder Admin)
        if (!canEditChecklist(idea, currentUserId)) {
            throw ApiException.forbidden("Nicht berechtigt, Checklistenelemente zu aktualisieren");
        }

        // Prüfe, ob die Idee nicht abgeschlossen ist
        validateChecklistEditable(idea);

        if (title == null || title.trim().isEmpty()) {
            throw ApiException.badRequest("Checklistenelement-Titel ist erforderlich");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw ApiException.badRequest("Checklistenelement-Titel muss " + MAX_TITLE_LENGTH + " Zeichen oder weniger sein");
        }

        ChecklistItem item = em.find(ChecklistItem.class, itemId);
        if (item == null) {
            throw ApiException.notFound("Checklistenelement nicht gefunden");
        }

        // Prüfe, ob das Element zu dieser Idee gehört
        if (!item.getIdea().getId().equals(ideaId)) {
            throw ApiException.badRequest("Checklistenelement gehört nicht zu dieser Idee");
        }

        item.setTitle(title.trim());
        em.merge(item);

        return ChecklistItemDTO.fromEntity(item);
    }

    /**
     * Aktualisiert den Fortschrittsprozentsatz der Idee basierend auf der Checklistenfertigstellung.
     * Fortschritt = (fertiggestellte Elemente / Gesamtelemente) * 100
     */
    private void updateIdeaProgress(Idea idea) {
        List<ChecklistItem> items = em.createNamedQuery("ChecklistItem.findByIdea", ChecklistItem.class)
                .setParameter("ideaId", idea.getId())
                .getResultList();

        if (items.isEmpty()) {
            // Keine Checklistenelemente, Fortschritt nicht ändern
            return;
        }

        long completedCount = items.stream()
                .filter(ChecklistItem::getIsCompleted)
                .count();

        int progressPercentage = (int) Math.round((double) completedCount / items.size() * 100);
        idea.setProgressPercentage(progressPercentage);
        em.merge(idea);
    }

    /**
     * Aktualisiert den Fortschritt und behandelt automatische Statusübergänge.
     * @return StatusTransitionResult mit Informationen über Statusänderungen
     */
    private StatusTransitionResult updateIdeaProgressWithTransitions(Idea idea) {
        List<ChecklistItem> items = em.createNamedQuery("ChecklistItem.findByIdea", ChecklistItem.class)
                .setParameter("ideaId", idea.getId())
                .getResultList();

        if (items.isEmpty()) {
            return new StatusTransitionResult(false, false);
        }

        long completedCount = items.stream()
                .filter(ChecklistItem::getIsCompleted)
                .count();

        int progressPercentage = (int) Math.round((double) completedCount / items.size() * 100);
        idea.setProgressPercentage(progressPercentage);

        boolean transitionedToInProgress = false;
        boolean allTodosCompleted = false;

        // Automatischer Übergang: CONCEPT -> IN_PROGRESS wenn erstes Todo erledigt
        if (idea.getStatus() == IdeaStatus.CONCEPT && completedCount > 0) {
            idea.setStatus(IdeaStatus.IN_PROGRESS);
            transitionedToInProgress = true;
        }

        // Prüfen, ob alle Todos erledigt sind (für Frontend-Bestätigungsdialog)
        if (completedCount == items.size() && !items.isEmpty()) {
            allTodosCompleted = true;
        }

        em.merge(idea);

        return new StatusTransitionResult(transitionedToInProgress, allTodosCompleted);
    }

    /**
     * Ergebnis der Statusübergangsprüfung
     */
    private static class StatusTransitionResult {
        final boolean transitionedToInProgress;
        final boolean allTodosCompleted;

        StatusTransitionResult(boolean transitionedToInProgress, boolean allTodosCompleted) {
            this.transitionedToInProgress = transitionedToInProgress;
            this.allTodosCompleted = allTodosCompleted;
        }
    }
}
