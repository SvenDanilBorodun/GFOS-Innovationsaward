package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.AuditLogDTO;
import com.gfos.ideaboard.entity.AuditAction;
import com.gfos.ideaboard.entity.AuditLog;
import com.gfos.ideaboard.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class AuditService {

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    @Transactional
    public void log(Long userId, AuditAction action, String entityType, Long entityId,
                    String oldValue, String newValue) {
        AuditLog log = new AuditLog();
        if (userId != null) {
            log.setUser(em.find(User.class, userId));
        }
        log.setAction(action);
        log.setEntityType(entityType);
        log.setEntityId(entityId);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        em.persist(log);
    }

    public List<AuditLogDTO> getRecentLogs(int limit) {
        List<AuditLog> logs = em.createNamedQuery("AuditLog.findRecent", AuditLog.class)
                .setMaxResults(limit)
                .getResultList();
        return logs.stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
    }

    public List<AuditLogDTO> getLogsByEntity(String entityType, Long entityId) {
        List<AuditLog> logs = em.createNamedQuery("AuditLog.findByEntity", AuditLog.class)
                .setParameter("entityType", entityType)
                .setParameter("entityId", entityId)
                .getResultList();
        return logs.stream()
                .map(AuditLogDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
