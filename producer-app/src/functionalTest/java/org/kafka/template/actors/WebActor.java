package org.kafka.template.actors;

import org.kafka.template.dtos.GenericResponseDto;
import org.kafka.template.dtos.UserCreatedRequestDto;
import org.kafka.template.dtos.UserCreatedResponseDto;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.reactive.server.WebTestClient;

@Component
public class WebActor {

    private WebTestClient webClient;

    public void setupWebTestClient(WebTestClient webTestClient) {
        this.webClient = webTestClient;
    }

    public WebTestClient.ResponseSpec createUser(UserCreatedRequestDto userCreatedRequestDto) {
        return webClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userCreatedRequestDto)
                .exchange();
    }
}
