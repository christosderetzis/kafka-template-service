package org.kafka.template.kafkatemplateservice.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @NotNull
    private int id;

    @NotNull
    private String name;

    private String email;

    private Integer age;
}
