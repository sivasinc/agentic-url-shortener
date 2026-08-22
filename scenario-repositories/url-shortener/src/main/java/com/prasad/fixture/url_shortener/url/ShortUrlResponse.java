package com.prasad.fixture.url_shortener.url;

import java.time.Instant;
import java.util.UUID;

public record ShortUrlResponse(
        UUID id,
        String shortCode,
        String originalUrl,
        String shortUrl,
        Instant createdAt
) {
}