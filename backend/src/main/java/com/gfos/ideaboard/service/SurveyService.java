package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.SurveyDTO;
import com.gfos.ideaboard.entity.*;
import com.gfos.ideaboard.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class SurveyService {

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    public List<SurveyDTO> getSurveys(int page, int size, Long userId) {
        List<Survey> surveys = em.createQuery(
                "SELECT s FROM Survey s ORDER BY s.createdAt DESC", Survey.class)
                .setFirstResult(page * size)
                .setMaxResults(size)
                .getResultList();
        return surveys.stream()
                .map(s -> SurveyDTO.fromEntity(s, getUserVotedOptionIds(s.getId(), userId)))
                .collect(Collectors.toList());
    }

    public List<SurveyDTO> getActiveSurveys(Long userId) {
        List<Survey> surveys = em.createNamedQuery("Survey.findActive", Survey.class)
                .setMaxResults(10)
                .getResultList();
        return surveys.stream()
                .map(s -> SurveyDTO.fromEntity(s, getUserVotedOptionIds(s.getId(), userId)))
                .collect(Collectors.toList());
    }

    public SurveyDTO getSurveyById(Long id, Long userId) {
        Survey survey = em.find(Survey.class, id);
        if (survey == null) {
            throw ApiException.notFound("Survey not found");
        }
        return SurveyDTO.fromEntity(survey, getUserVotedOptionIds(id, userId));
    }

    @Transactional
    public SurveyDTO createSurvey(String question, String description, List<String> options,
                                   Boolean isAnonymous, Boolean allowMultipleVotes, Long creatorId) {
        if (options == null || options.size() < 2) {
            throw ApiException.badRequest("At least 2 options are required");
        }

        User creator = em.find(User.class, creatorId);
        if (creator == null) {
            throw ApiException.notFound("User not found");
        }

        Survey survey = new Survey();
        survey.setQuestion(question);
        survey.setDescription(description);
        survey.setCreator(creator);
        survey.setIsActive(true);
        survey.setIsAnonymous(isAnonymous != null ? isAnonymous : false);
        survey.setAllowMultipleVotes(allowMultipleVotes != null ? allowMultipleVotes : false);

        em.persist(survey);

        // Optionen hinzufügen
        for (int i = 0; i < options.size(); i++) {
            SurveyOption option = new SurveyOption();
            option.setOptionText(options.get(i));
            option.setDisplayOrder(i);
            survey.addOption(option);
            em.persist(option);
        }

        return SurveyDTO.fromEntity(survey, List.of());
    }

    @Transactional
    public SurveyDTO vote(Long surveyId, List<Long> optionIds, Long userId) {
        Survey survey = em.find(Survey.class, surveyId);
        if (survey == null) {
            throw ApiException.notFound("Survey not found");
        }

        if (!survey.getIsActive()) {
            throw ApiException.badRequest("Survey is closed");
        }

        User user = em.find(User.class, userId);
        if (user == null) {
            throw ApiException.notFound("User not found");
        }

        // Prüfen, ob bereits abgestimmt
        List<Long> existingVotes = getUserVotedOptionIds(surveyId, userId);
        if (!existingVotes.isEmpty() && !survey.getAllowMultipleVotes()) {
            throw ApiException.conflict("Already voted on this survey");
        }

        // Validieren, dass Optionen zu dieser Umfrage gehören
        List<Long> validOptionIds = survey.getOptions().stream()
                .map(SurveyOption::getId)
                .collect(Collectors.toList());

        for (Long optionId : optionIds) {
            if (!validOptionIds.contains(optionId)) {
                throw ApiException.badRequest("Invalid option ID: " + optionId);
            }

            // Prüfen, ob für diese Option bereits abgestimmt wurde
            if (existingVotes.contains(optionId)) {
                continue;
            }

            SurveyOption option = em.find(SurveyOption.class, optionId);

            SurveyVote vote = new SurveyVote();
            vote.setSurvey(survey);
            vote.setOption(option);
            vote.setUser(user);
            em.persist(vote);

            // Hinweis: vote_count und total_votes werden automatisch durch Datenbank-Trigger aktualisiert
            // Hier NICHT manuell inkrementieren, um Doppelzählung zu vermeiden
        }

        // Flushen, um sicherzustellen, dass Trigger vor der Rückgabe ausgeführt wurden
        em.flush();

        // Entitäten aktualisieren, um aktualisierte Zählungen von Datenbank-Triggern zu erhalten
        em.refresh(survey);
        for (SurveyOption opt : survey.getOptions()) {
            em.refresh(opt);
        }

        return SurveyDTO.fromEntity(survey, getUserVotedOptionIds(surveyId, userId));
    }

    @Transactional
    public void deleteSurvey(Long id, Long userId) {
        Survey survey = em.find(Survey.class, id);
        if (survey == null) {
            throw ApiException.notFound("Survey not found");
        }

        // Nur Ersteller oder Admin können löschen
        User user = em.find(User.class, userId);
        if (!survey.getCreator().getId().equals(userId) && user.getRole() != UserRole.ADMIN) {
            throw ApiException.forbidden("Not authorized to delete this survey");
        }

        em.remove(survey);
    }

    private List<Long> getUserVotedOptionIds(Long surveyId, Long userId) {
        if (userId == null) return List.of();

        return em.createQuery(
                "SELECT v.option.id FROM SurveyVote v WHERE v.survey.id = :surveyId AND v.user.id = :userId",
                Long.class)
                .setParameter("surveyId", surveyId)
                .setParameter("userId", userId)
                .getResultList();
    }
}
