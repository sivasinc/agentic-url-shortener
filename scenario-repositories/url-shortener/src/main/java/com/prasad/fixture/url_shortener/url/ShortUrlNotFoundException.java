package com.prasad.fixture.url_shortener.url;

public class ShortUrlNotFoundException
        extends RuntimeException {

    public ShortUrlNotFoundException(
            String shortCode
    ) {
        super(
                "Short URL does not exist: " +
                        shortCode
        );
    }
}