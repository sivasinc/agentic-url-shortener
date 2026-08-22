package com.prasad.agentic_software_engineer.workspace;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

@Component
public class FileHashService {

    public String sha256(Path file) {
        MessageDigest digest = digest();

        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = input.read(buffer)) != -1) {
                digest.update(
                        buffer,
                        0,
                        bytesRead
                );
            }

            return HexFormat.of().formatHex(
                    digest.digest()
            );
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to hash file: " + file,
                    exception
            );
        }
    }

    public Map<String, String> manifest(Path root) {
        Map<String, String> hashes = new TreeMap<>();

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .sorted()
                    .forEach(
                            file -> hashes.put(
                                    normalizeRelativePath(
                                            root.relativize(file)
                                    ),
                                    sha256(file)
                            )
                    );

            return Map.copyOf(hashes);
        } catch (IOException exception) {
            throw new WorkspaceException(
                    "Unable to create workspace manifest",
                    exception
            );
        }
    }

    private MessageDigest digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 is unavailable",
                    exception
            );
        }
    }

    private String normalizeRelativePath(Path path) {
        return path.toString().replace('\\', '/');
    }
}