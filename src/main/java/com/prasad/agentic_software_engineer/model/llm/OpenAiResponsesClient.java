package com.prasad.agentic_software_engineer.model.llm;

import com.prasad.agentic_software_engineer.config.AgentModelProperties;
import com.prasad.agentic_software_engineer.model.ModelInvocationException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConditionalOnProperty(
        name = "agentic.model.provider",
        havingValue = "openai"
)
public class OpenAiResponsesClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final AgentModelProperties properties;

    public OpenAiResponsesClient(
            @Qualifier("openAiRestClient")
            RestClient restClient,
            ObjectMapper objectMapper,
            AgentModelProperties properties
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public <T> T generate(
            String instructions,
            String input,
            String schemaName,
            JsonNode schema,
            Class<T> responseType
    ) {
        validateConfiguration();

        Map<String, Object> format =
                new LinkedHashMap<>();

        format.put("type", "json_schema");
        format.put("name", schemaName);
        format.put("strict", true);
        format.put("schema", schema);

        Map<String, Object> text =
                Map.of("format", format);

        Map<String, Object> request =
                new LinkedHashMap<>();

        request.put("model", properties.model());
        request.put("instructions", instructions);
        request.put("input", input);
        request.put(
                "max_output_tokens",
                properties.maxOutputTokens()
        );
        request.put("store", false);
        request.put("text", text);

        try {
            JsonNode response = restClient.post()
                    .uri("/responses")
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);

            String outputText =
                    extractOutputText(response);

            return objectMapper.readValue(
                    outputText,
                    responseType
            );
        } catch (ModelInvocationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ModelInvocationException(
                    "Model invocation failed",
                    exception
            );
        }
    }

    String extractOutputText(JsonNode response) {
        if (response == null) {
            throw new ModelInvocationException(
                    "Model returned an empty response"
            );
        }

        JsonNode error = response.path("error");

        if (!error.isMissingNode() &&
                !error.isNull()) {
            throw new ModelInvocationException(
                    "Model returned an error: " +
                            error.path("message").asText(
                                    "unknown model error"
                            )
            );
        }

        for (JsonNode output :
                response.path("output")) {
            if (!"message".equals(
                    output.path("type").asText()
            )) {
                continue;
            }

            for (JsonNode content :
                    output.path("content")) {
                if ("output_text".equals(
                        content.path("type").asText()
                )) {
                    String text =
                            content.path("text").asText();

                    if (!text.isBlank()) {
                        return text;
                    }
                }

                if ("refusal".equals(
                        content.path("type").asText()
                )) {
                    throw new ModelInvocationException(
                            "Model refused the request"
                    );
                }
            }
        }

        throw new ModelInvocationException(
                "Model response contains no structured output text"
        );
    }

    private void validateConfiguration() {
        if (properties.apiKey().isBlank()) {
            throw new ModelInvocationException(
                    "MODEL_API_KEY is required when MODEL_PROVIDER=openai"
            );
        }
    }
}