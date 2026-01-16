package org.kafka.template.mapper;

import org.kafka.template.entity.UserEntity;
import org.kafka.template.models.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", source = "id")
    UserEntity toUserEntity(User user);
}