package com.plover.plover_be.global.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationTest {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;

    @Autowired
    OpenApiDocumentationTest(MockMvc mockMvc, ObjectMapper objectMapper) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
    }

    @DisplayName("Pageable은 page, size, sort 쿼리로 노출되고 인증 사용자 ID는 숨긴다")
    @Test
    void pageable_is_exposed_as_individual_optional_query_parameters() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        JsonNode paths = objectMapper.readTree(document).path("paths");

        assertPageableParameters(paths, "/api/crews/{crewId}/plogging-records");
        assertPageableParameters(paths, "/api/plogging-sessions");
        assertLoginUserIdIsHidden(paths);
    }

    private void assertPageableParameters(JsonNode paths, String path) {
        JsonNode parameters = paths.path(path).path("get").path("parameters");
        List<JsonNode> values = new ArrayList<>();
        parameters.forEach(values::add);

        assertThat(values).extracting(parameter -> parameter.path("name").asText())
                .contains("page", "size", "sort")
                .doesNotContain("pageable", "userId");
        assertThat(values)
                .filteredOn(parameter -> List.of("page", "size", "sort")
                        .contains(parameter.path("name").asText()))
                .allSatisfy(parameter -> {
                    assertThat(parameter.path("in").asText()).isEqualTo("query");
                    assertThat(parameter.path("required").asBoolean(false)).isFalse();
                });
        JsonNode size = values.stream()
                .filter(parameter -> parameter.path("name").asText().equals("size"))
                .findFirst()
                .orElseThrow();
        assertThat(size.path("schema").path("default").asInt()).isEqualTo(20);
    }

    private void assertLoginUserIdIsHidden(JsonNode paths) {
        List<JsonNode> parameters = new ArrayList<>();
        paths.forEach(pathItem -> pathItem.forEach(operation ->
                operation.path("parameters").forEach(parameters::add)));

        assertThat(parameters)
                .filteredOn(parameter -> parameter.path("name").asText().equals("userId"))
                .isEmpty();
    }
}
