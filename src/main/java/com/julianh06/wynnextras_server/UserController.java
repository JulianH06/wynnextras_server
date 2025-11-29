package com.julianh06.wynnextras_server;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
            @RequestBody User user
    ) {
        String expectedUuid = user.getUuid();
        List<String> actualUuids = wynncraftService.fetchUuid(apiKey);

        boolean isAuthorized = false;
        for(String actualUuid : actualUuids) {
            if(expectedUuid.equalsIgnoreCase(actualUuid)) {
                isAuthorized = true;
                break;
            }
        }
        if (isAuthorized) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("UUID mismatch. You are not allowed to create this user.");
        }

        for (Aspect aspect : user.getAspects()) {
            aspect.setUser(user);
        }
        return ResponseEntity.ok(repo.save(user));
    }
}
