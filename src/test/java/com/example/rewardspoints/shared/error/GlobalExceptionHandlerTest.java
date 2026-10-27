package com.example.rewardspoints.shared.error;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the handler in isolation with a stub controller, so it stays testable (and movable into a
 * shared library) without any feature module or database.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.StubController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.StubController.class})
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void businessException_usesItsCodeAndCategoryStatus() throws Exception {
        mockMvc.perform(get("/stub/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("missing thing"));
    }

    @Test
    void customErrorCode_fromAnyModule_mapsByCategory() throws Exception {
        mockMvc.perform(get("/stub/business-rule"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("STUB_RULE_BROKEN"));
    }

    @Test
    void bodyValidation_returns400WithFieldErrors() throws Exception {
        mockMvc.perform(post("/stub/body").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"\", \"nick\": \"toolong\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors.length()").value(2))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.errors[?(@.field == 'nick')]").exists());
    }

    @Test
    void unreadableBody_returns400Malformed_withoutLeakingParserDetails() throws Exception {
        mockMvc.perform(post("/stub/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.detail").value("Malformed or unreadable request body"));
    }

    @Test
    void pathTypeMismatch_returns400Malformed() throws Exception {
        mockMvc.perform(get("/stub/items/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON_MALFORMED_REQUEST"));
    }

    @Test
    void optimisticLockFailure_returns409() throws Exception {
        mockMvc.perform(get("/stub/stale"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("COMMON_CONCURRENT_MODIFICATION"));
    }

    @Test
    void unexpectedException_returns500_withoutLeakingMessage() throws Exception {
        mockMvc.perform(get("/stub/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("COMMON_INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    @Test
    void springMvcExceptions_getTheSameShapeAndACode() throws Exception {
        mockMvc.perform(delete("/stub/boom"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("COMMON_METHOD_NOT_ALLOWED"));

        mockMvc.perform(post("/stub/body").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("COMMON_UNSUPPORTED_MEDIA_TYPE"));

        mockMvc.perform(get("/stub/no-such-route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COMMON_RESOURCE_NOT_FOUND"));
    }

    enum StubErrorCode implements ErrorCode {
        RULE_BROKEN;

        @Override
        public String code() {
            return "STUB_" + name();
        }

        @Override
        public ErrorCategory category() {
            return ErrorCategory.BUSINESS_RULE;
        }
    }

    static class StubRuleException extends BusinessException {
        StubRuleException() {
            super(StubErrorCode.RULE_BROKEN, "rule broken");
        }
    }

    record Body(@NotBlank String name, @Size(max = 3) String nick) {
    }

    @RestController
    static class StubController {
        @GetMapping("/stub/not-found")
        void notFound() {
            throw new NotFoundException("missing thing");
        }

        @GetMapping("/stub/business-rule")
        void businessRule() {
            throw new StubRuleException();
        }

        @PostMapping("/stub/body")
        void body(@Valid @RequestBody Body body) {
        }

        @GetMapping("/stub/items/{id}")
        void item(@PathVariable UUID id) {
        }

        @GetMapping("/stub/stale")
        void stale() {
            throw new OptimisticLockingFailureException("stale");
        }

        @GetMapping("/stub/boom")
        void boom() {
            throw new IllegalStateException("secret internals");
        }
    }
}
