package org.kafka.template.kafkatemplateservice.controllers;

import lombok.Builder;
import lombok.Getter;
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
    public String createUser(@RequestBody UserDto userDto) {
        User user = User.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .email(userDto.getEmail())
                .age(userDto.getAge())
                .build();

        userProducer.sendUser(user);
        return "User created successfully";
    }

    @Builder
    @Getter
    public static class UserDto {
        private Integer id;
        private String name;
        private String email;
        private Integer age;

        // Getters and setters
    }
}
