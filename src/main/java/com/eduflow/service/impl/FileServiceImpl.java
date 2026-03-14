package com.eduflow.service.impl;

import com.eduflow.exception.BadRequestException;
import com.eduflow.service.FileService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;

@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @Value("${file.upload.max-size:5242880}")  // 5MB default
    private long maxFileSize;

    @Value("${file.upload.base-url:}")
    private String baseUrl;

    @Value("${server.port:8099}")
    private String serverPort;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            // Images
            "jpg", "jpeg", "png", "gif", "webp", "svg",
            // Documents
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
            // Text
            "txt", "csv",
            // Archives
            "zip", "rar"
    );

    private static final Map<String, List<String>> ALLOWED_CONTENT_TYPES = Map.of(
            "image", Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp", "image/svg+xml"),
            "document", Arrays.asList(
                    "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            ),
            "text", Arrays.asList("text/plain", "text/csv"),
            "archive", Arrays.asList("application/zip", "application/x-rar-compressed")
    );

    @PostConstruct
    public void init() {
        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                log.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }
        } catch (IOException e) {
            log.error("Could not create upload directory: {}", e.getMessage());
            throw new RuntimeException("Could not create upload directory", e);
        }
    }

    @Override
    public String uploadFile(MultipartFile file, String folder) {
        validateFile(file);

        String originalFilename = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));
        String extension = getFileExtension(originalFilename);
        String uniqueFilename = UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = Paths.get(uploadDir, folder);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            Path targetPath = targetDir.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

            String fileUrl = buildFileUrl(folder, uniqueFilename);
            log.info("File uploaded successfully: {} -> {}", originalFilename, fileUrl);

            return fileUrl;
        } catch (IOException e) {
            log.error("Failed to upload file {}: {}", originalFilename, e.getMessage());
            throw new RuntimeException("Failed to upload file: " + originalFilename, e);
        }
    }

    @Override
    public List<String> uploadFiles(List<MultipartFile> files, String folder) {
        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            urls.add(uploadFile(file, folder));
        }
        return urls;
    }

    @Override
    public void deleteFile(String fileUrl) {
        try {
            // Extract relative path from URL
            String relativePath = fileUrl;
            if (fileUrl.contains("/files/")) {
                relativePath = fileUrl.substring(fileUrl.indexOf("/files/") + 7);
            }

            Path filePath = Paths.get(uploadDir, relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File deleted: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }
        } catch (IOException e) {
            log.error("Failed to delete file {}: {}", fileUrl, e.getMessage());
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    @Override
    public long getMaxFileSize() {
        return maxFileSize;
    }

    @Override
    public List<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }

    @Override
    public Map<String, Object> getUploadConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("maxFileSize", maxFileSize);
        config.put("maxFileSizeFormatted", formatFileSize(maxFileSize));
        config.put("allowedExtensions", ALLOWED_EXTENSIONS);
        config.put("allowedContentTypes", ALLOWED_CONTENT_TYPES);
        return config;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("File is empty or not provided");
        }

        if (file.getSize() > maxFileSize) {
            throw new BadRequestException(String.format(
                    "File size exceeds maximum allowed size. Max: %s, Got: %s",
                    formatFileSize(maxFileSize),
                    formatFileSize(file.getSize())
            ));
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("File name is required");
        }

        String extension = getFileExtension(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException(String.format(
                    "File type not allowed: .%s. Allowed types: %s",
                    extension,
                    String.join(", ", ALLOWED_EXTENSIONS)
            ));
        }

        // Validate content type
        String contentType = file.getContentType();
        boolean validContentType = ALLOWED_CONTENT_TYPES.values().stream()
                .flatMap(List::stream)
                .anyMatch(type -> type.equals(contentType));

        if (!validContentType && contentType != null) {
            log.warn("Potentially suspicious content type: {} for file: {}", contentType, originalFilename);
        }
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            throw new BadRequestException("File must have an extension");
        }
        return filename.substring(lastDotIndex + 1);
    }

    private String buildFileUrl(String folder, String filename) {
        String base = baseUrl;
        if (base == null || base.isBlank()) {
            base = "http://localhost:" + serverPort;
        }
        return base + "/files/" + folder + "/" + filename;
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}