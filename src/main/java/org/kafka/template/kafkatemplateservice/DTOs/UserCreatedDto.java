package org.kafka.template.kafkatemplateservice.DTOs;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class UserCreatedDto {
    private Integer id;
    private String name;
    private String email;
    private Integer age;
}
