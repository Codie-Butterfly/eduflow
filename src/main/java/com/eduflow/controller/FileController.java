package com.eduflow.controller;

import com.eduflow.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Tag(name = "Files", description = "File upload and management endpoints")
public class FileController {

    private final FileService fileService;

    @Value("${file.upload.dir:uploads}")
    private String uploadDir;

    @PostMapping("/v1/files/upload")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload a single file")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {

        String fileUrl = fileService.uploadFile(file, folder);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "File uploaded successfully");
        response.put("url", fileUrl);
        response.put("originalName", file.getOriginalFilename());
        response.put("size", file.getSize());
        response.put("contentType", file.getContentType());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/v1/files/upload-multiple")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Upload multiple files")
    public ResponseEntity<Map<String, Object>> uploadMultipleFiles(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "folder", defaultValue = "general") String folder) {

        List<String> fileUrls = fileService.uploadFiles(files, folder);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", files.size() + " file(s) uploaded successfully");
        response.put("urls", fileUrls);
        response.put("count", files.size());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/v1/files")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a file")
    public ResponseEntity<Map<String, Object>> deleteFile(@RequestParam("url") String fileUrl) {
        fileService.deleteFile(fileUrl);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "File deleted successfully");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/v1/files/config")
    @Operation(summary = "Get file upload configuration")
    public ResponseEntity<Map<String, Object>> getUploadConfig() {
        return ResponseEntity.ok(fileService.getUploadConfig());
    }

    @GetMapping("/files/{folder}/{filename:.+}")
    @Operation(summary = "Serve uploaded file")
    public ResponseEntity<Resource> serveFile(
            @PathVariable String folder,
            @PathVariable String filename) {

        try {
            Path filePath = Paths.get(uploadDir, folder, filename);
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                String contentType = determineContentType(filename);

                return ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(contentType))
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                                "inline; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    private String determineContentType(String filename) {
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls" -> "application/vnd.ms-excel";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "ppt" -> "application/vnd.ms-powerpoint";
            case "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation";
            case "txt" -> "text/plain";
            case "csv" -> "text/csv";
            case "zip" -> "application/zip";
            case "rar" -> "application/x-rar-compressed";
            default -> "application/octet-stream";
        };
    }
}