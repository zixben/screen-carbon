package com.lks.dto;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lks.bean.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserResponseTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializedUserResponseDoesNotExposeSensitiveUserFields() throws JsonProcessingException {
        User user = new User();
        user.setId(123);
        user.setFullName("Test User");
        user.setUsername("test-user");
        user.setEmail("test@example.com");
        user.setPassword("$2a$10$sensitiveHash");
        user.setRecoveryToken("sensitiveToken");
        user.setCode("12345");
        user.setRole("USER");

        String json = objectMapper.writeValueAsString(UserResponse.from(user));

        assertTrue(json.contains("\"username\":\"test-user\""));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("sensitiveHash"));
        assertFalse(json.contains("recoveryToken"));
        assertFalse(json.contains("sensitiveToken"));
        assertFalse(json.contains("\"code\""));
    }

    @Test
    void registrationRequestTracksRoleInjectionButIgnoresCsrfFormField() throws JsonProcessingException {
        UserRegistrationRequest request = objectMapper.readValue("""
                {
                  "fullName": "Test User",
                  "username": "test-user",
                  "email": "test@example.com",
                  "password": "Password!",
                  "_csrf": "token",
                  "role": "ADMIN"
                }
                """, UserRegistrationRequest.class);

        assertFalse(request.getUnsupportedFields().containsKey("_csrf"));
        assertTrue(request.getUnsupportedFields().containsKey("role"));
    }
}
