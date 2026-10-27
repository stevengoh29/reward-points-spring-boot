package com.example.rewardspoints.user.api;

import com.example.rewardspoints.user.infrastructure.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full-stack sample: real controller, service, JPA, Flyway migrations and PostgreSQL (Testcontainers).
 * Requires a running Docker daemon.
 */
@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
class UserControllerIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_thenGetById() throws Exception {
        UUID id = createUser("Alice@Example.com", "Alice");

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.displayName").value("Alice"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getUser_unknownId_returns404ProblemDetail() throws Exception {
        mockMvc.perform(get("/users/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"));
    }

    @Test
    void findUsers_filtersAndPaginates() throws Exception {
        createUser("alice@example.com", "Alice");
        createUser("alicia@example.com", "Alicia");
        createUser("bob@example.com", "Bob");

        mockMvc.perform(get("/users").param("email", "ALIC").param("size", "1").param("sort", "email,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].email").value("alice@example.com"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(false));
    }

    @Test
    void findUsers_withoutFilters_returnsEveryone() throws Exception {
        createUser("alice@example.com", "Alice");
        createUser("bob@example.com", "Bob");

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void updateUser_changesDisplayNameAndStatus() throws Exception {
        UUID id = createUser("alice@example.com", "Alice");

        mockMvc.perform(patch("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"displayName": "Alice B", "status": "SUSPENDED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice B"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(jsonPath("$.displayName").value("Alice B"))
                .andExpect(jsonPath("$.status").value("SUSPENDED"));
    }

    @Test
    void updateUser_unknownId_returns404() throws Exception {
        mockMvc.perform(patch("/users/{id}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"x\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateUser_displayNameTooLong_returns400() throws Exception {
        UUID id = createUser("alice@example.com", "Alice");

        mockMvc.perform(patch("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"" + "x".repeat(101) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_softDeletes_andUserDisappears() throws Exception {
        UUID id = createUser("alice@example.com", "Alice");

        mockMvc.perform(delete("/users/{id}", id)).andExpect(status().isNoContent());

        mockMvc.perform(get("/users/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/users")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void deleteUser_unknownId_returns404() throws Exception {
        mockMvc.perform(delete("/users/{id}", UUID.randomUUID())).andExpect(status().isNotFound());
    }

    @Test
    void createUser_duplicateEmail_returns409_caseInsensitive() throws Exception {
        createUser("alice@example.com", "Alice");

        postUser("{\"email\": \"ALICE@example.com\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void createUser_invalidInput_returns400() throws Exception {
        postUser("{}").andExpect(status().isBadRequest());
        postUser("{\"email\": \"\"}").andExpect(status().isBadRequest());
        postUser("{\"email\": \"   \"}").andExpect(status().isBadRequest());
        postUser("{\"email\": \"not-an-email\"}").andExpect(status().isBadRequest());
        postUser("{\"email\": \"" + "a".repeat(320) + "@example.com\"}").andExpect(status().isBadRequest());
        postUser("{\"email\": \"a@example.com\", \"displayName\": \"" + "x".repeat(101) + "\"}")
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/users")).andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void createUser_alwaysStartsActive_evenIfStatusIsSent() throws Exception {
        postUser("{\"email\": \"a@example.com\", \"status\": \"SUSPENDED\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void getUser_doesNotExposeInternalFields() throws Exception {
        UUID id = createUser("alice@example.com", "Alice");

        mockMvc.perform(get("/users/{id}", id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.version").doesNotExist())
                .andExpect(jsonPath("$.createdBy").doesNotExist())
                .andExpect(jsonPath("$.deletedAt").doesNotExist());
    }

    @Test
    void createUser_withoutDisplayName_isAllowed() throws Exception {
        postUser("{\"email\": \"a@example.com\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deletedUser_emailCanRegisterAgain_andSecondDeleteIs404() throws Exception {
        UUID id = createUser("alice@example.com", "Alice");
        mockMvc.perform(delete("/users/{id}", id)).andExpect(status().isNoContent());
        mockMvc.perform(delete("/users/{id}", id)).andExpect(status().isNotFound());

        UUID again = createUser("alice@example.com", "Alice 2");
        assertThat(again).isNotEqualTo(id);
    }

    @Test
    void updateUser_emptyBody_changesNothing() throws Exception {
        UUID id = createUser("alice@example.com", "Alice");

        mockMvc.perform(patch("/users/{id}", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Alice"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void updateUser_suspendThenReactivate() throws Exception {
        UUID id = createUser("alice@example.com", "Alice");

        patchStatus(id, "SUSPENDED").andExpect(jsonPath("$.status").value("SUSPENDED"));
        patchStatus(id, "ACTIVE").andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void findUsers_filtersByStatusAndDisplayName_andExcludesDeleted() throws Exception {
        UUID alice = createUser("alice@example.com", "Alice");
        UUID bob = createUser("bob@example.com", "Bob");
        UUID carol = createUser("carol@example.com", "Carol");
        patchStatus(bob, "SUSPENDED");
        mockMvc.perform(delete("/users/{id}", carol)).andExpect(status().isNoContent());

        mockMvc.perform(get("/users").param("status", "SUSPENDED"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].email").value("bob@example.com"));
        mockMvc.perform(get("/users").param("displayName", "ali"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(alice.toString()));
        mockMvc.perform(get("/users")).andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void malformedUuid_returns400() throws Exception {
        mockMvc.perform(get("/users/{id}", "not-a-uuid")).andExpect(status().isBadRequest());
        mockMvc.perform(delete("/users/{id}", "not-a-uuid")).andExpect(status().isBadRequest());
    }

    private ResultActions postUser(String json) throws Exception {
        return mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions patchStatus(UUID id, String status) throws Exception {
        return mockMvc.perform(patch("/users/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    private UUID createUser(String email, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                java.util.Map.of("email", email, "displayName", displayName))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("id").asText()).isNotBlank();
        return UUID.fromString(body.get("id").asText());
    }
}
