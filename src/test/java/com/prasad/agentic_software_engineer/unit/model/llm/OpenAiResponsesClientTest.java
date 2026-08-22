package com.prasad.agentic_software_engineer.unit.model.llm;

import com.prasad.agentic_software_engineer.config.AgentModelProperties;
import com.prasad.agentic_software_engineer.model.RequirementAnalysis;
import com.prasad.agentic_software_engineer.model.llm.ModelSchemas;
import com.prasad.agentic_software_engineer.model.llm.OpenAiResponsesClient;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAiResponsesClientTest {

    @Test
    void parsesStructuredResponseWithoutNetworkCall() {
        RestClient.Builder builder =
                RestClient.builder()
                        .baseUrl(
                                "https://api.openai.com/v1"
                        )
                        .defaultHeader(
                                "Authorization",
                                "Bearer test-key"
                        );

        MockRestServiceServer server =
                MockRestServiceServer
                        .bindTo(builder)
                        .build();

        server.expect(
                        once(),
                        requestTo(
                                "https://api.openai.com/v1/responses"
                        )
                )
                .andExpect(
                        header(
                                "Authorization",
                                "Bearer test-key"
                        )
                )
                .andRespond(
                        withSuccess(
                                """
                                {
                                  "id": "resp_test",
                                  "output": [
                                    {
                                      "type": "message",
                                      "content": [
                                        {
                                          "type": "output_text",
                                          "text": "{\\"normalizedRequirement\\":\\"Add analytics\\",\\"acceptanceCriteria\\":[\\"Analytics endpoint works\\"],\\"ambiguities\\":[],\\"assumptions\\":[],\\"risks\\":[],\\"requiresClarification\\":false}"
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """,
                                MediaType.APPLICATION_JSON
                        )
                );

        ObjectMapper objectMapper =
                new ObjectMapper();

        OpenAiResponsesClient client =
                new OpenAiResponsesClient(
                        builder.build(),
                        objectMapper,
                        new AgentModelProperties(
                                "openai",
                                URI.create(
                                        "https://api.openai.com/v1"
                                ),
                                "test-key",
                                "test-model",
                                Duration.ofSeconds(5),
                                1000
                        )
                );

        RequirementAnalysis result =
                client.generate(
                        "instructions",
                        "input",
                        "requirement_analysis",
                        new ModelSchemas(
                                objectMapper
                        ).requirementAnalysis(),
                        RequirementAnalysis.class
                );

        assertThat(result.normalizedRequirement())
                .isEqualTo("Add analytics");

        assertThat(result.requiresClarification())
                .isFalse();

        server.verify();
    }
}