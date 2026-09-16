package com.msbatchproducer.msbatchproducer.service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.*;
@Service
public class UploadStorage {
    private final Path directory;
    public UploadStorage(@Value("${app.upload.directory:./data/uploads}") String directory) { this.directory = Path.of(directory); }
    public record StoredUpload(String id, String path, String format) {}
    public StoredUpload store(MultipartFile file) throws IOException {
        String name = Objects.requireNonNullElse(file.getOriginalFilename(), "").toLowerCase(Locale.ROOT);
        String format = name.endsWith(".zip") ? "zip" : name.endsWith(".xml") ? "xml" : "";
        if (format.isEmpty() || file.isEmpty()) throw new IllegalArgumentException("Adjunta un archivo .zip o .xml no vacío");
        if (file.getSize() > 25L * 1024 * 1024) throw new IllegalArgumentException("Máximo 25 MiB por carga");
        Files.createDirectories(directory);
        String id = UUID.randomUUID().toString();
        Path destination = directory.resolve(id + "." + format).toAbsolutePath();
        try (var in = file.getInputStream(); var out = Files.newOutputStream(destination, StandardOpenOption.CREATE_NEW)) {
            byte[] buffer = new byte[8192]; long total = 0; int n;
            while ((n = in.read(buffer)) != -1) {
                total += n;
                if (total > 25L * 1024 * 1024) throw new IOException("Carga demasiado grande");
                out.write(buffer, 0, n);
            }
        } catch (Exception e) { Files.deleteIfExists(destination); throw e; }
        return new StoredUpload(id, destination.toString(), format);
    }
}
