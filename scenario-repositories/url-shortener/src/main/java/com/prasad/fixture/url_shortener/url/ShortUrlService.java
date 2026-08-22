package com.prasad.fixture.url_shortener.url;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
public class ShortUrlService {

    private static final int MAX_ATTEMPTS = 10;

    private final ShortUrlRepository repository;
    private final ShortCodeGenerator generator;
    private final Clock clock;

    @Autowired
    public ShortUrlService(
            ShortUrlRepository repository,
            ShortCodeGenerator generator
    ) {
        this(
                repository,
                generator,
                Clock.systemUTC()
        );
    }

    ShortUrlService(
            ShortUrlRepository repository,
            ShortCodeGenerator generator,
            Clock clock
    ) {
        this.repository = repository;
        this.generator = generator;
        this.clock = clock;
    }

    @Transactional
    public ShortUrl create(String originalUrl) {
        for (int attempt = 0;
             attempt < MAX_ATTEMPTS;
             attempt++) {
            String code = generator.generate();

            if (!repository.existsByShortCode(code)) {
                return repository.save(
                        new ShortUrl(
                                UUID.randomUUID(),
                                code,
                                originalUrl,
                                clock.instant()
                        )
                );
            }
        }

        throw new IllegalStateException(
                "Unable to generate a unique short code"
        );
    }

    @Transactional(readOnly = true)
    public ShortUrl resolve(String shortCode) {
        return repository.findByShortCode(shortCode)
                .orElseThrow(
                        () ->
                                new ShortUrlNotFoundException(
                                        shortCode
                                )
                );
    }
}