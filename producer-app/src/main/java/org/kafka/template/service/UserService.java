package org.kafka.template.service;

import org.kafka.template.dtos.GenericResponseDto;
import org.kafka.template.dtos.UserCreatedRequestDto;
import org.kafka.template.dtos.UserCreatedResponseDto;
import org.kafka.template.kafka.UserProducer;
import org.kafka.template.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserProducer userProducer;
    private final UserMapper userMapper;

    public UserService(UserProducer userProducer, UserMapper userMapper) {
        this.userProducer = userProducer;
        this.userMapper = userMapper;
    }

    public GenericResponseDto<UserCreatedResponseDto> sendUser(UserCreatedRequestDto userCreatedRequestDto) {
        var user = userMapper.toJsonSchema(userCreatedRequestDto);
        userProducer.sendUser(user);
        var responseDto = userMapper.toUserCreatedResponseDto(user);
        return GenericResponseDto.<UserCreatedResponseDto>builder()
                .data(responseDto)
                .status(org.kafka.template.enums.GenericResponseStatus.SUCCESS)
                .build();
    }
}
