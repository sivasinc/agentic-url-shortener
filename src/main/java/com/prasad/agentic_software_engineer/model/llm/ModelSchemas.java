package com.prasad.agentic_software_engineer.model.llm;

import com.prasad.agentic_software_engineer.model.ModelInvocationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class ModelSchemas {

    private final ObjectMapper objectMapper;

    public JsonNode requirementAnalysis() {
        return schema("""
                {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "normalizedRequirement": {"type": "string"},
                    "acceptanceCriteria": {
                      "type": "array",
                      "items": {"type": "string"}
                    },
                    "ambiguities": {
                      "type": "array",
                      "items": {"type": "string"}
                    },
                    "assumptions": {
                      "type": "array",
                      "items": {"type": "string"}
                    },
                    "risks": {
                      "type": "array",
                      "items": {"type": "string"}
                    },
                    "requiresClarification": {"type": "boolean"}
                  },
                  "required": [
                    "normalizedRequirement",
                    "acceptanceCriteria",
                    "ambiguities",
                    "assumptions",
                    "risks",
                    "requiresClarification"
                  ]
                }
                """);
    }

    public JsonNode engineeringPlan() {
        return schema("""
                {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "rationale": {"type": "string"},
                    "tasks": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "properties": {
                          "id": {"type": "string"},
                          "name": {"type": "string"},
                          "description": {"type": "string"},
                          "dependencyIds": {
                            "type": "array",
                            "items": {"type": "string"}
                          },
                          "parallelizable": {"type": "boolean"},
                          "approvalRequired": {"type": "boolean"}
                        },
                        "required": [
                          "id",
                          "name",
                          "description",
                          "dependencyIds",
                          "parallelizable",
                          "approvalRequired"
                        ]
                      }
                    },
                    "risks": {
                      "type": "array",
                      "items": {"type": "string"}
                    },
                    "tradeOffs": {
                      "type": "array",
                      "items": {"type": "string"}
                    }
                  },
                  "required": [
                    "rationale",
                    "tasks",
                    "risks",
                    "tradeOffs"
                  ]
                }
                """);
    }

    public JsonNode patchProposal() {
        return schema("""
                {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "summary": {"type": "string"},
                    "changes": {
                      "type": "array",
                      "items": {
                        "type": "object",
                        "additionalProperties": false,
                        "properties": {
                          "type": {
                            "type": "string",
                            "enum": ["CREATE", "UPDATE", "DELETE"]
                          },
                          "path": {"type": "string"},
                          "expectedSha256": {
                            "type": ["string", "null"]
                          },
                          "content": {
                            "type": ["string", "null"]
                          },
                          "rationale": {"type": "string"}
                        },
                        "required": [
                          "type",
                          "path",
                          "expectedSha256",
                          "content",
                          "rationale"
                        ]
                      }
                    },
                    "assumptions": {
                      "type": "array",
                      "items": {"type": "string"}
                    },
                    "risks": {
                      "type": "array",
                      "items": {"type": "string"}
                    }
                  },
                  "required": [
                    "summary",
                    "changes",
                    "assumptions",
                    "risks"
                  ]
                }
                """);
    }

    public JsonNode documentationProposal() {
        return schema("""
                {
                  "type": "object",
                  "additionalProperties": false,
                  "properties": {
                    "readmeSection": {"type": "string"},
                    "architectureSummary": {"type": "string"},
                    "limitations": {
                      "type": "array",
                      "items": {"type": "string"}
                    }
                  },
                  "required": [
                    "readmeSection",
                    "architectureSummary",
                    "limitations"
                  ]
                }
                """);
    }

    private JsonNode schema(String json) {
        try {
            return objectMapper.readTree(json);
        } catch (JacksonException exception) {
            throw new ModelInvocationException(
                    "Invalid internal model schema",
                    exception
            );
        }
    }
}