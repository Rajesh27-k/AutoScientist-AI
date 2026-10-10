package com.autoscientist.util;

import com.autoscientist.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.UUID;

@Service
public class FileStorageService {

    @Value("${app.upload.dir:./uploads/datasets}")
    private String uploadDirStr;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        try {
            this.uploadPath = Paths.get(uploadDirStr).toAbsolutePath().normalize();
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directory: " + uploadDirStr, e);
        }
    }

    public String storeFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Uploaded file is empty.");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("Only CSV files are supported (.csv extension required).");
        }

        // Clean filename and generate UUID stored filename to prevent collisions and path traversal
        String storedFilename = UUID.randomUUID().toString() + ".csv";
        Path targetLocation = this.uploadPath.resolve(storedFilename).normalize();

        // Enforce path traversal protection
        if (!targetLocation.startsWith(this.uploadPath)) {
            throw new BadRequestException("Invalid path traversal attempt.");
        }

        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            return storedFilename;
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file: " + e.getMessage(), e);
        }
    }

    public Path loadFilePath(String storedFilename) {
        Path path = this.uploadPath.resolve(storedFilename).normalize();
        if (!path.startsWith(this.uploadPath) || !Files.exists(path)) {
            throw new BadRequestException("File does not exist or access is restricted.");
        }
        return path;
    }

    public InputStream getInputStream(String storedFilename) throws IOException {
        Path path = loadFilePath(storedFilename);
        return Files.newInputStream(path);
    }

    public boolean deleteFile(String storedFilename) {
        try {
            Path path = loadFilePath(storedFilename);
            return Files.deleteIfExists(path);
        } catch (Exception e) {
            return false;
        }
    }
}
