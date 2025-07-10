package org.kafka.template.kafkatemplateservice.actors;

import org.kafka.template.kafkatemplateservice.DTOs.UserCreatedDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.reactive.server.WebTestClient;

@Component
public class WebActor {

    private WebTestClient webClient;

    public void setupWebTestClient(WebTestClient webTestClient) {
        this.webClient = webTestClient;
    }

    public void createUser(UserCreatedDto userCreatedDto) {
        webClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userCreatedDto)
                .exchange();
    }
}
