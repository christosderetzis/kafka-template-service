package org.kafka.template.dtos;

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
