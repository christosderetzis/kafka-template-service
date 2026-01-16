package org.kafka.template.service;

import ch.qos.logback.classic.Level;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kafka.template.base.BaseLogTest;
import org.kafka.template.dtos.GenericResponseDto;
import org.kafka.template.dtos.UserCreatedRequestDto;
import org.kafka.template.dtos.UserCreatedResponseDto;
import org.kafka.template.enums.GenericResponseStatus;
import org.kafka.template.kafka.UserProducer;
import org.kafka.template.mapper.UserMapper;
import org.kafka.template.models.User;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest extends BaseLogTest {

    @Mock
    private UserProducer userProducer;

    @Mock
    private UserMapper userMapper;

    private UserService userService;

    @BeforeEach
    void setUp() {
        userService = new UserService(userProducer, userMapper);
    }

    @Test
    void sendUser_shouldSendUserAndReturnSuccessResponse() {
        // given
        UserCreatedRequestDto requestDto = UserCreatedRequestDto.builder().build();
        User user = new User(); // replace with actual schema type
        UserCreatedResponseDto responseDto = UserCreatedResponseDto.builder().build();

        // mocking
        when(userMapper.toJsonSchema(requestDto)).thenReturn(user);
        when(userMapper.toUserCreatedResponseDto(user)).thenReturn(responseDto);

        // when
        GenericResponseDto<UserCreatedResponseDto> result =
                userService.sendUser(requestDto);

        // then
        verify(userMapper).toJsonSchema(requestDto);
        verify(userProducer).sendUser(user);
        verify(userMapper).toUserCreatedResponseDto(user);

        assertNotNull(result);
        assertEquals(GenericResponseStatus.SUCCESS, result.getStatus());
        assertEquals(responseDto, result.getData());
    }
}