package com.julianh06.wynnextras_server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Iterator;

@Service
public class WynncraftService {

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public String fetchUuid(String apiKey) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.wynncraft.com/v3/player/whoami"))
                    .header("Authorization", "Bearer " + apiKey)
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("Wynncraft API failed: " + response.statusCode());
            }

            JsonNode root = mapper.readTree(response.body());

            // Die UUID ist der Key des Objekts
            Iterator<String> fieldNames = root.fieldNames();

            if (!fieldNames.hasNext()) {
                throw new RuntimeException("Invalid JSON: no UUID key found");
            }

            return fieldNames.next(); // <-- DIE UUID

        } catch (Exception e) {
            throw new RuntimeException("Failed to validate Wynncraft API key", e);
        }
    }
}


