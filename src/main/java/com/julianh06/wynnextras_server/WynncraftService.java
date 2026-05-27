package com.julianh06.wynnextras_server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class WynncraftService {
    private static final URI CLASSES_URI = URI.create("https://api.wynncraft.com/v3/classes");
    private static final URI ONLINE_PLAYERS_URI = URI.create("https://api.wynncraft.com/v3/player?identifier=uuid");
    private static final String ASPECTS_URL_TEMPLATE = "https://api.wynncraft.com/v3/aspects/%s";

    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public record OnlinePlayerSample(Set<String> playerUuids, int totalOnlinePlayers) {}

    public List<String> fetchUuid(String apiKey) {
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

            List<String> uuids = new ArrayList<>();
            Iterator<String> fieldNames = root.fieldNames();

            while (fieldNames.hasNext()) {
                uuids.add(fieldNames.next());
            }

            if (uuids.isEmpty()) {
                throw new RuntimeException("Invalid JSON: no UUIDs found");
            }

            return uuids;

        } catch (Exception e) {
            throw new RuntimeException("Failed to validate Wynncraft API key", e);
        }
    }

    public Set<String> fetchCurrentAspectNames() {
        try {
            Set<String> classTrees = fetchClassTrees();
            Set<String> aspectNames = new LinkedHashSet<>();

            for (String classTree : classTrees) {
                JsonNode root = fetchJson(URI.create(ASPECTS_URL_TEMPLATE.formatted(classTree)));
                if (!root.isArray()) {
                    throw new RuntimeException("Invalid JSON: aspects response is not a list for class tree " + classTree);
                }

                for (JsonNode aspect : root) {
                    JsonNode name = aspect.get("name");
                    if (name != null && name.isTextual() && !name.asText().isBlank()) {
                        aspectNames.add(name.asText());
                    }
                }
            }

            if (aspectNames.isEmpty()) {
                throw new RuntimeException("Invalid JSON: no aspect names found");
            }

            return aspectNames;

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch current Wynncraft aspect names", e);
        }
    }

    public Set<String> fetchOnlinePlayerUuids() {
        return fetchOnlinePlayerSample().playerUuids();
    }

    public OnlinePlayerSample fetchOnlinePlayerSample() {
        try {
            JsonNode root = fetchJson(ONLINE_PLAYERS_URI);
            JsonNode players = root.path("players");
            if (!players.isObject()) {
                throw new RuntimeException("Invalid JSON: players response is not an object");
            }

            Set<String> uuids = new LinkedHashSet<>();
            Iterator<String> fieldNames = players.fieldNames();
            while (fieldNames.hasNext()) {
                String uuid = fieldNames.next().replace("-", "").toLowerCase();
                if (uuid.matches("[0-9a-f]{32}")) {
                    uuids.add(uuid);
                }
            }

            return new OnlinePlayerSample(uuids, root.path("total").asInt(uuids.size()));

        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch online Wynncraft players", e);
        }
    }

    private Set<String> fetchClassTrees() throws Exception {
        JsonNode root = fetchJson(CLASSES_URI);
        Set<String> classTrees = new LinkedHashSet<>();
        Iterator<String> fieldNames = root.fieldNames();

        while (fieldNames.hasNext()) {
            classTrees.add(fieldNames.next());
        }

        if (classTrees.isEmpty()) {
            throw new RuntimeException("Invalid JSON: no class trees found");
        }

        return classTrees;
    }

    private JsonNode fetchJson(URI uri) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Wynncraft API failed for " + uri + ": " + response.statusCode());
        }

        return mapper.readTree(response.body());
    }
}