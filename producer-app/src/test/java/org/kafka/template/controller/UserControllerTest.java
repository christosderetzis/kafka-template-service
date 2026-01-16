package org.kafka.template.controller;

import org.kafka.template.dtos.GenericResponseDto;
import org.kafka.template.dtos.UserCreatedRequestDto;
import org.kafka.template.dtos.UserCreatedResponseDto;
import org.kafka.template.enums.GenericResponseStatus;
import org.kafka.template.kafka.UserProducer;
import org.kafka.template.models.User;
import static org.mockito.Mockito.*;

import org.kafka.template.service.UserService;
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
    private UserService userService;

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
        UserCreatedRequestDto userDto = UserCreatedRequestDto
                .builder()
                .id(1)
                .name("John Doe")
                .email("john.doe@example.com")
                .age(30)
                .build();

        String userDtoJson = objectMapper.writeValueAsString(userDto);

        UserCreatedResponseDto responseDto= UserCreatedResponseDto
                .builder()
                .id(1)
                .name("John Doe")
                .email("john.doe@example.com")
                .age(30)
                .build();

        GenericResponseDto<UserCreatedResponseDto> userCreatedResponseDto = GenericResponseDto.<UserCreatedResponseDto>builder()
                .data(responseDto)
                .status(GenericResponseStatus.SUCCESS)
                .build();

        String userCreatedResponseDtoJson = objectMapper.writeValueAsString(userCreatedResponseDto);

        when(userService.sendUser(any(UserCreatedRequestDto.class)))
                .thenReturn(userCreatedResponseDto);

        // Act & Assert
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userDtoJson))
                .andExpect(status().isOk())
                .andExpect(content().json(userCreatedResponseDtoJson));

        // Verify the interaction with the userProducer
        verify(userService, times(1)).sendUser(any(UserCreatedRequestDto.class));
    }
}