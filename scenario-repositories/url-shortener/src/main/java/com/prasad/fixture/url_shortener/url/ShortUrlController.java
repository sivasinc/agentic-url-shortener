package com.prasad.fixture.url_shortener.url;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class ShortUrlController {

    private final ShortUrlService service;

    public ShortUrlController(
            ShortUrlService service
    ) {
        this.service = service;
    }

    @PostMapping("/api/v1/urls")
    public ResponseEntity<ShortUrlResponse> create(
            @Valid
            @RequestBody
            CreateShortUrlRequest request,
            HttpServletRequest servletRequest
    ) {
        ShortUrl shortUrl =
                service.create(request.url());

        String baseUrl =
                servletRequest.getRequestURL()
                        .toString()
                        .replace(
                                servletRequest.getRequestURI(),
                                ""
                        );

        ShortUrlResponse response =
                new ShortUrlResponse(
                        shortUrl.getId(),
                        shortUrl.getShortCode(),
                        shortUrl.getOriginalUrl(),
                        baseUrl +
                                "/" +
                                shortUrl.getShortCode(),
                        shortUrl.getCreatedAt()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(
            @PathVariable
            String shortCode
    ) {
        ShortUrl shortUrl =
                service.resolve(shortCode);

        return ResponseEntity
                .status(HttpStatus.FOUND)
                .location(
                        URI.create(
                                shortUrl.getOriginalUrl()
                        )
                )
                .build();
    }
}