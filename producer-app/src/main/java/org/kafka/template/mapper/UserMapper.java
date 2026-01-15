package org.kafka.template.mapper;

import org.kafka.template.dtos.UserCreatedRequestDto;
import org.kafka.template.dtos.UserCreatedResponseDto;
import org.kafka.template.models.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toJsonSchema(UserCreatedRequestDto userCreatedRequestDto);

    UserCreatedResponseDto toUserCreatedResponseDto(User user);
}
