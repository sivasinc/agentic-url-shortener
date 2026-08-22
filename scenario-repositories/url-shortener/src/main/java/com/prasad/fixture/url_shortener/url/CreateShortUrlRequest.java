package com.prasad.fixture.url_shortener.url;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.URL;

public record CreateShortUrlRequest(
        @NotBlank
        @URL(protocol = "http")
        String url
) {
}