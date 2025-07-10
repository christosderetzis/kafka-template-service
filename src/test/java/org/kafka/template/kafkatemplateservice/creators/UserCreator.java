package org.kafka.template.kafkatemplateservice.creators;

import com.github.javafaker.Faker;
import java.util.Locale;
import org.kafka.template.kafkatemplateservice.models.User;

public class UserCreator {

    static Faker faker = new Faker(Locale.US);

    public static User createRandomUser() {
        return User.builder()
                .id(faker.number().numberBetween(1, 1000))
                .name(faker.name().fullName())
                .email(faker.internet().emailAddress())
                .age(faker.number().numberBetween(18, 65))
                .build();
    }
}
