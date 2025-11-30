package com.julianh06.wynnextras_server;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("user")
public class UserController {
    private final UserRepository repo;
    private final WynncraftService wynncraftService;

    public UserController(UserRepository repo, WynncraftService wynncraftService) {
        this.repo = repo;
        this.wynncraftService = wynncraftService;
    }

//    @GetMapping("/all")
//    public List<User> getUsers() {
//        return repo.findAll();
//    }

    @GetMapping
    public ResponseEntity<User> getUser(
        @RequestHeader("playerUUID") String playerUUID,
        @RequestHeader("Wynncraft-Api-Key") String apiKey,
        @RequestHeader("RequestingUUID") String requestingUUID
    ) {
        boolean requestingUserExists = repo.existsById(requestingUUID);
        if (!requestingUserExists) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(null);
        }

        List<String> actualUuids = wynncraftService.fetchUuid(apiKey);

        boolean isAuthorized = false;
        for(String actualUuid : actualUuids) {
            if(requestingUUID.equalsIgnoreCase(actualUuid)) {
                isAuthorized = true;
                break;
            }
        }

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(null);
        }

        return repo.findById(playerUUID)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(null));
    }

    @PostMapping
    public ResponseEntity<?> saveUser(
            @RequestHeader("Wynncraft-Api-Key") String apiKey,
            @RequestBody User incoming
    ) {
        String expectedUuid = incoming.getUuid();
        List<String> actualUuids = wynncraftService.fetchUuid(apiKey);

        boolean isAuthorized = actualUuids.stream()
                .anyMatch(uuid -> uuid.equalsIgnoreCase(expectedUuid));

        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("UUID mismatch. You are not allowed to create or update this user.");
        }

        User existing = repo.findById(incoming.getUuid()).orElse(null);

        if (existing == null) {
            for (Aspect a : incoming.getAspects()) {
                a.setUser(incoming);
            }
            return ResponseEntity.ok(repo.save(incoming));
        }

        Map<String, Aspect> existingMap = new HashMap<>();
        for (Aspect a : existing.getAspects()) {
            existingMap.put(a.getName(), a);
        }

        List<Aspect> mergedAspects = new ArrayList<>();

        for (Aspect inc : incoming.getAspects()) {
            inc.setUser(existing);

            if (existingMap.containsKey(inc.getName())) {
                Aspect db = existingMap.get(inc.getName());
                db.setAmount(inc.getAmount());
                db.setRarity(inc.getRarity());
                db.setRequiredClass(inc.getRequiredClass());
                mergedAspects.add(db);
            } else {
                mergedAspects.add(inc);
            }
        }

        existing.setPlayerName(incoming.getPlayerName());
        existing.setUpdatedAt(incoming.getUpdatedAt());
        existing.setModVersion(incoming.getModVersion());
        existing.setAspects(mergedAspects);

        return ResponseEntity.ok(repo.save(existing));
    }
}
