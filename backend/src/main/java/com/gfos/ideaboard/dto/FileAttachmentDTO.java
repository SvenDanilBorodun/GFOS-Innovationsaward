package com.gfos.ideaboard.dto;

import com.gfos.ideaboard.entity.FileAttachment;
import java.time.LocalDateTime;

public class FileAttachmentDTO {

    private Long id;
    private String filename;
    private String originalName;
    private String mimeType;
    private Long fileSize;
    private LocalDateTime uploadedAt;

    public FileAttachmentDTO() {}

    public static FileAttachmentDTO fromEntity(FileAttachment attachment) {
        FileAttachmentDTO dto = new FileAttachmentDTO();
        dto.setId(attachment.getId());
        dto.setFilename(attachment.getFilename());
        dto.setOriginalName(attachment.getOriginalName());
        dto.setMimeType(attachment.getMimeType());
        dto.setFileSize(attachment.getFileSize());
        dto.setUploadedAt(attachment.getUploadedAt());
        return dto;
    }

    // Getters und Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }
}
