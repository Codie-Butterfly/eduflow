package com.eduflow.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface FileService {

    /**
     * Uploads a file and returns the file URL
     */
    String uploadFile(MultipartFile file, String folder);

    /**
     * Uploads multiple files and returns their URLs
     */
    List<String> uploadFiles(List<MultipartFile> files, String folder);

    /**
     * Deletes a file by its URL or path
     */
    void deleteFile(String fileUrl);

    /**
     * Returns the maximum allowed file size in bytes
     */
    long getMaxFileSize();

    /**
     * Returns the list of allowed file extensions
     */
    List<String> getAllowedExtensions();

    /**
     * Returns upload configuration info
     */
    Map<String, Object> getUploadConfig();
}