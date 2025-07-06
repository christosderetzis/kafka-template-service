package org.kafka.template.kafkatemplateservice.actors;

import org.kafka.template.kafkatemplateservice.controllers.UserController;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.test.web.reactive.server.WebTestClient;

@Component
public class WebActor {

    private WebTestClient webClient;

    public void setupWebTestClient(WebTestClient webTestClient) {
        // Configure the WebTestClient as needed

        this.webClient = webTestClient;
    }


    public void createUser(UserController.UserDto userDto) {
        webClient.post()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange();
    }

    public void updateUser(UserController.UserDto userDto) {
        webClient.put()
                .uri("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange();
    }
}
