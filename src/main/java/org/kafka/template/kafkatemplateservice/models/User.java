package org.kafka.template.kafkatemplateservice.models;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @NotNull(message = "ID cannot be null")
    private Integer id;

    @NotNull(message = "Name cannot be null")
    private String name;

    private String email;

    private Integer age;
}
