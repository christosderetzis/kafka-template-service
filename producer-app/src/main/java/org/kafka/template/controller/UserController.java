package org.kafka.template.controller;

import org.kafka.template.dtos.GenericResponseDto;
import org.kafka.template.dtos.UserCreatedRequestDto;
import org.kafka.template.dtos.UserCreatedResponseDto;
import org.kafka.template.kafka.UserProducer;
import org.kafka.template.models.User;
import org.kafka.template.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/users")
    public GenericResponseDto<UserCreatedResponseDto> createUser(@RequestBody UserCreatedRequestDto userCreatedRequestDto) {
        return userService.sendUser(userCreatedRequestDto);
    }
}
