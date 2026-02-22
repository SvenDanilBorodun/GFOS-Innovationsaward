package com.gfos.ideaboard.dto;

/**
 * Antwort-DTO für das Umschalten von Checklistenelementen.
 * Enthält Informationen über automatische Statusübergänge.
 */
public class ChecklistToggleResponse {

    private ChecklistItemDTO item;
    private boolean transitionedToInProgress;
    private boolean allTodosCompleted;

    public ChecklistToggleResponse() {}

    public ChecklistToggleResponse(ChecklistItemDTO item, boolean transitionedToInProgress, boolean allTodosCompleted) {
        this.item = item;
        this.transitionedToInProgress = transitionedToInProgress;
        this.allTodosCompleted = allTodosCompleted;
    }

    // Getters und Setters
    public ChecklistItemDTO getItem() {
        return item;
    }

    public void setItem(ChecklistItemDTO item) {
        this.item = item;
    }

    public boolean isTransitionedToInProgress() {
        return transitionedToInProgress;
    }

    public void setTransitionedToInProgress(boolean transitionedToInProgress) {
        this.transitionedToInProgress = transitionedToInProgress;
    }

    public boolean isAllTodosCompleted() {
        return allTodosCompleted;
    }

    public void setAllTodosCompleted(boolean allTodosCompleted) {
        this.allTodosCompleted = allTodosCompleted;
    }
}
