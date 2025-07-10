package org.kafka.template.kafkatemplateservice.controllers;

import org.kafka.template.kafkatemplateservice.DTOs.UserCreatedDto;
import org.kafka.template.kafkatemplateservice.kafka.UserProducer;
import org.kafka.template.kafkatemplateservice.models.User;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UserController {

    private final UserProducer userProducer;

    public UserController(UserProducer userProducer) {
        this.userProducer = userProducer;
    }

    @PostMapping("/users")
    public String createUser(@RequestBody UserCreatedDto userCreatedDto) {
        User user = User.builder()
                .id(userCreatedDto.getId())
                .name(userCreatedDto.getName())
                .email(userCreatedDto.getEmail())
                .age(userCreatedDto.getAge())
                .build();

        userProducer.sendUser(user);
        return "User created successfully";
    }
}
