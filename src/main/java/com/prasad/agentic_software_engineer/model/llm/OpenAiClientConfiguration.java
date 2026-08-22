package com.prasad.agentic_software_engineer.model.llm;

import com.prasad.agentic_software_engineer.config.AgentModelProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

@Configuration
@ConditionalOnProperty(
        name = "agentic.model.provider",
        havingValue = "openai"
)
public class OpenAiClientConfiguration {

    @Bean
    @Qualifier("openAiRestClient")
    RestClient openAiRestClient(
            AgentModelProperties properties
    ) {
        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(properties.timeout())
                .build();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        httpClient
                );

        requestFactory.setReadTimeout(
                properties.timeout()
        );

        return RestClient.builder()
                .baseUrl(
                        properties.baseUrl().toString()
                )
                .requestFactory(requestFactory)
                .defaultHeader(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + properties.apiKey()
                )
                .defaultHeader(
                        HttpHeaders.CONTENT_TYPE,
                        "application/json"
                )
                .build();
    }
}