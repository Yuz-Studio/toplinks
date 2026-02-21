package com.yuz.toplinks.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileStorageService {

    private final Path uploadDir;

    public FileStorageService(@Value("${file.upload-dir:uploads}") String uploadDir) throws IOException {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public String storeFile(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new IOException("File name is empty");
        }
        // Sanitize filename
        filename = Paths.get(filename).getFileName().toString();
        Path targetLocation = this.uploadDir.resolve(filename);
        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        return filename;
    }

    public List<String> listFiles() throws IOException {
        try (Stream<Path> stream = Files.list(this.uploadDir)) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());
        }
    }

    public Path getFilePath(String filename) {
        Path resolved = this.uploadDir.resolve(filename).normalize();
        if (!resolved.startsWith(this.uploadDir)) {
            throw new SecurityException("Access denied: path traversal attempt detected");
        }
        return resolved;
    }

    public Path getUploadDir() {
        return uploadDir;
    }
}
