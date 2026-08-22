package com.prasad.fixture.url_shortener.url;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "short_urls")
public class ShortUrl {

    @Id
    private UUID id;

    @Column(
            name = "short_code",
            nullable = false,
            unique = true,
            length = 12
    )
    private String shortCode;

    @Column(
            name = "original_url",
            nullable = false,
            length = 2048
    )
    private String originalUrl;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private Instant createdAt;

    protected ShortUrl() {
        // Required by JPA.
    }

    public ShortUrl(
            UUID id,
            String shortCode,
            String originalUrl,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id);
        this.shortCode = requireText(
                shortCode,
                "Short code"
        );
        this.originalUrl = requireText(
                originalUrl,
                "Original URL"
        );
        this.createdAt =
                Objects.requireNonNull(createdAt);
    }

    public UUID getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    private static String requireText(
            String value,
            String field
    ) {
        Objects.requireNonNull(value);

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " cannot be blank"
            );
        }

        return value.trim();
    }
}