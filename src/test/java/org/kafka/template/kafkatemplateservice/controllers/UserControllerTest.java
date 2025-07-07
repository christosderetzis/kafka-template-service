package org.kafka.template.kafkatemplateservice.controllers;

import static org.junit.jupiter.api.Assertions.*;

import org.kafka.template.kafkatemplateservice.kafka.UserProducer;
import org.kafka.template.kafkatemplateservice.models.User;
import static org.mockito.Mockito.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserProducer userProducer;

    @InjectMocks
    private UserController userController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = new ObjectMapper();
    }

    @Test
    void createUser_ShouldReturnSuccessMessage() throws Exception {
        // Arrange
        UserController.UserDto userDto = UserController.UserDto.builder()
                .id(1)
                .name("John Doe")
                .email("john.doe@example.com")
                .age(30)
                .build();

        String userDtoJson = objectMapper.writeValueAsString(userDto);

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userDtoJson))
                .andExpect(status().isOk())
                .andExpect(content().string("User created successfully"));

        // Verify the interaction with the userProducer
        verify(userProducer, times(1)).sendUser(any(User.class));
    }
}

