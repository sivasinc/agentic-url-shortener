package com.prasad.fixture.url_shortener.url;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortUrlServiceTest {

    private final ShortUrlRepository repository =
            mock(ShortUrlRepository.class);

    private final ShortCodeGenerator generator =
            mock(ShortCodeGenerator.class);

    private final Clock clock =
            Clock.fixed(
                    Instant.parse(
                            "2026-08-21T12:00:00Z"
                    ),
                    ZoneOffset.UTC
            );

    private final ShortUrlService service =
            new ShortUrlService(
                    repository,
                    generator,
                    clock
            );

    @Test
    void createsShortUrl() {
        when(generator.generate())
                .thenReturn("Ab12Cd34");

        when(
                repository.existsByShortCode(
                        "Ab12Cd34"
                )
        ).thenReturn(false);

        when(repository.save(any(ShortUrl.class)))
                .thenAnswer(
                        invocation ->
                                invocation.getArgument(0)
                );

        ShortUrl result =
                service.create(
                        "https://example.com"
                );

        assertThat(result.getShortCode())
                .isEqualTo("Ab12Cd34");

        assertThat(result.getOriginalUrl())
                .isEqualTo(
                        "https://example.com"
                );

        assertThat(result.getCreatedAt())
                .isEqualTo(clock.instant());

        verify(repository)
                .save(any(ShortUrl.class));
    }

    @Test
    void resolvesExistingShortUrl() {
        ShortUrl shortUrl =
                new ShortUrl(
                        java.util.UUID.randomUUID(),
                        "Ab12Cd34",
                        "https://example.com",
                        clock.instant()
                );

        when(
                repository.findByShortCode(
                        "Ab12Cd34"
                )
        ).thenReturn(Optional.of(shortUrl));

        assertThat(
                service.resolve("Ab12Cd34")
        ).isSameAs(shortUrl);
    }

    @Test
    void rejectsUnknownShortCode() {
        when(
                repository.findByShortCode(
                        "missing"
                )
        ).thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.resolve("missing")
        ).isInstanceOf(
                ShortUrlNotFoundException.class
        );
    }
}