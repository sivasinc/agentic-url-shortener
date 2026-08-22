package com.prasad.agentic_software_engineer.model;

import java.util.Objects;

public record ProposedFileChange(
        FileChangeType type,
        String path,
        String expectedSha256,
        String content,
        String rationale
) {

    public ProposedFileChange {
        type = Objects.requireNonNull(type);
        path = Objects.requireNonNull(path).trim();
        rationale = Objects.requireNonNull(
                rationale
        ).trim();

        expectedSha256 = normalizeNullable(
                expectedSha256
        );

        content = normalizeNullable(content);

        if (path.isBlank()) {
            throw new IllegalArgumentException(
                    "File-change path cannot be blank"
            );
        }

        if (rationale.isBlank()) {
            throw new IllegalArgumentException(
                    "File-change rationale cannot be blank"
            );
        }

        if (type == FileChangeType.CREATE &&
                content == null) {
            throw new IllegalArgumentException(
                    "CREATE requires file content"
            );
        }

        if (type == FileChangeType.UPDATE &&
                (content == null ||
                        expectedSha256 == null)) {
            throw new IllegalArgumentException(
                    "UPDATE requires content and expected hash"
            );
        }

        if (type == FileChangeType.DELETE &&
                expectedSha256 == null) {
            throw new IllegalArgumentException(
                    "DELETE requires expected hash"
            );
        }
    }

    private static String normalizeNullable(
            String value
    ) {
        if (value == null) {
            return null;
        }

        return value.trim();
    }
}