package com.gfos.ideaboard.service;

import com.gfos.ideaboard.dto.FileAttachmentDTO;
import com.gfos.ideaboard.entity.FileAttachment;
import com.gfos.ideaboard.entity.Idea;
import com.gfos.ideaboard.entity.User;
import com.gfos.ideaboard.exception.ApiException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class FileService {

    @PersistenceContext(unitName = "IdeaBoardPU")
    private EntityManager em;

    private static final String UPLOAD_DIR = System.getProperty("com.sun.aas.instanceRoot", ".") + "/uploads";
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "image/jpeg", "image/png", "image/gif", "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain", "text/csv"
    );

    @Transactional
    public FileAttachmentDTO uploadFile(Long ideaId, String originalFilename, String mimeType,
                                         byte[] fileData, Long uploaderId) {
        Idea idea = em.find(Idea.class, ideaId);
        if (idea == null) {
            throw ApiException.notFound("Idee nicht gefunden");
        }

        User uploader = em.find(User.class, uploaderId);
        if (uploader == null) {
            throw ApiException.notFound("Benutzer nicht gefunden");
        }

        // Dateigröße validieren
        if (fileData.length > MAX_FILE_SIZE) {
            throw ApiException.badRequest("Dateigröße übersteigt Maximum (10MB)");
        }

        // Dateityp validieren
        if (!ALLOWED_TYPES.contains(mimeType)) {
            throw ApiException.badRequest("Dateityp nicht erlaubt: " + mimeType);
        }

        // Eindeutigen Dateinamen generieren
        String extension = getFileExtension(originalFilename);
        String storedFilename = UUID.randomUUID().toString() + extension;

        // Verzeichnisstruktur erstellen
        Path ideaUploadDir = Paths.get(UPLOAD_DIR, ideaId.toString());
        try {
            Files.createDirectories(ideaUploadDir);
        } catch (IOException e) {
            throw ApiException.serverError("Fehler beim Erstellen des Upload-Verzeichnisses");
        }

        // Datei speichern
        Path filePath = ideaUploadDir.resolve(storedFilename);
        try {
            Files.write(filePath, fileData);
        } catch (IOException e) {
            throw ApiException.serverError("Fehler beim Speichern der Datei");
        }

        // Datenbank-Datensatz erstellen
        FileAttachment attachment = new FileAttachment();
        attachment.setIdea(idea);
        attachment.setFilename(storedFilename);
        attachment.setOriginalName(originalFilename);
        attachment.setMimeType(mimeType);
        attachment.setFileSize((long) fileData.length);
        attachment.setFilePath(filePath.toString());
        attachment.setUploadedBy(uploader);

        em.persist(attachment);

        return FileAttachmentDTO.fromEntity(attachment);
    }

    public byte[] getFile(Long ideaId, Long fileId) {
        FileAttachment attachment = em.find(FileAttachment.class, fileId);
        if (attachment == null || !attachment.getIdea().getId().equals(ideaId)) {
            throw ApiException.notFound("Datei nicht gefunden");
        }

        Path filePath = Paths.get(UPLOAD_DIR, ideaId.toString(), attachment.getFilename());
        try {
            return Files.readAllBytes(filePath);
        } catch (IOException e) {
            throw ApiException.notFound("Datei nicht auf der Festplatte gefunden");
        }
    }

    public FileAttachment getFileAttachment(Long fileId) {
        FileAttachment attachment = em.find(FileAttachment.class, fileId);
        if (attachment == null) {
            throw ApiException.notFound("Datei nicht gefunden");
        }
        return attachment;
    }

    @Transactional
    public void deleteFile(Long ideaId, Long fileId, Long userId) {
        FileAttachment attachment = em.find(FileAttachment.class, fileId);
        if (attachment == null || !attachment.getIdea().getId().equals(ideaId)) {
            throw ApiException.notFound("Datei nicht gefunden");
        }

        Idea idea = attachment.getIdea();
        // Nur Autor oder Admin können Dateien löschen
        if (!idea.getAuthor().getId().equals(userId)) {
            throw ApiException.forbidden("Nicht berechtigt, diese Datei zu löschen");
        }

        // Aus Dateisystem löschen
        Path filePath = Paths.get(UPLOAD_DIR, ideaId.toString(), attachment.getFilename());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            // Protokollieren aber fortfahren - Datenbank-Datensatz sollte immer noch gelöscht werden
        }

        // Aus Datenbank löschen
        em.remove(attachment);
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) return "";
        return filename.substring(lastDot);
    }
}
