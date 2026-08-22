package com.prasad.fixture.url_shortener.url;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class ShortCodeGenerator {

    private static final char[] ALPHABET =
            "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz"
                    .toCharArray();

    private static final int CODE_LENGTH = 8;

    private final SecureRandom random =
            new SecureRandom();

    public String generate() {
        char[] result =
                new char[CODE_LENGTH];

        for (int index = 0;
             index < result.length;
             index++) {
            result[index] =
                    ALPHABET[
                            random.nextInt(
                                    ALPHABET.length
                            )
                            ];
        }

        return new String(result);
    }
}