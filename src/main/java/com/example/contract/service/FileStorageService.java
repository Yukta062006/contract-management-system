package com.example.contract.service;

import com.example.contract.exception.InvalidFileException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Service responsible for managing local filesystem operations for uploaded contract documents.
 * Ensures security checks such as path traversal prevention and unique filename generation.
 */
@Service
public class FileStorageService {

    private final Path fileStorageLocation;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    /**
     * Initializes the storage directory on application startup.
     */
    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.fileStorageLocation);
        } catch (IOException ex) {
            throw new RuntimeException("Could not create storage directory at: " + this.fileStorageLocation, ex);
        }
    }

    /**
     * Stores an uploaded multipart file to the local filesystem with a unique filename.
     *
     * @param file The uploaded multipart file
     * @return The stored file's relative path string
     * @throws InvalidFileException if the file is empty, filename is unsafe, or storage fails
     */
    public Path storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Cannot store an empty or null file.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new InvalidFileException("File must have a valid filename.");
        }

        // Clean path to prevent directory traversal
        String cleanedFilename = StringUtils.cleanPath(originalFilename);
        if (cleanedFilename.contains("..") || cleanedFilename.contains("/") || cleanedFilename.contains("\\")) {
            throw new InvalidFileException("Filename contains invalid path sequence: " + originalFilename);
        }

        // Generate a secure, unique filename using UUID
        String uniqueFileName = UUID.randomUUID() + "_" + cleanedFilename;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(uniqueFileName).normalize();

            // Guard against path traversal escaping target directory
            if (!targetLocation.startsWith(this.fileStorageLocation)) {
                throw new InvalidFileException("Cannot store file outside current storage directory.");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetLocation, StandardCopyOption.REPLACE_EXISTING);
            }

            return targetLocation;
        } catch (IOException ex) {
            throw new InvalidFileException("Failed to store file " + originalFilename + ". Error: " + ex.getMessage(), ex);
        }
    }

    /**
     * Deletes a stored file from disk. Primarily used to clean up documents when parsing fails.
     *
     * @param filePath The absolute or relative Path to the file to be deleted
     */
    public void deleteFile(Path filePath) {
        if (filePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException ignored) {
            // Logged or ignored safely during cleanup routines
        }
    }

    /**
     * Deletes a file given its string path.
     *
     * @param filePathString Path string of the file to delete
     */
    public void deleteFile(String filePathString) {
        if (filePathString == null || filePathString.isBlank()) {
            return;
        }
        deleteFile(Paths.get(filePathString));
    }

    public Path getFileStorageLocation() {
        return fileStorageLocation;
    }
}
