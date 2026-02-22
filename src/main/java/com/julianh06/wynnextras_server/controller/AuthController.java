package com.julianh06.wynnextras_server.controller;

import com.julianh06.wynnextras_server.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.julianh06.wynnextras_server.service.AuthService.SESSION_DURATION_MS;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<?> authenticate(@RequestBody AuthRequest request) {
        String token = authService.createSessionAfterMojangVerify(
            request.getUsername(),
            request.getServerId()
        );

        if (token == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Authentication failed");
        }

        return ResponseEntity.ok(new AuthResponse(token, System.currentTimeMillis() + SESSION_DURATION_MS));
    }

    public static class AuthRequest {
        private String username;
        private String serverId;

        public String getUsername() { return username; }
        public String getServerId() { return serverId; }

        public void setUsername(String username) { this.username = username; }
        public void setServerId(String serverId) { this.serverId = serverId; }
    }

    public static class AuthResponse {
        private final String token;
        private final long expiresIn;

        public AuthResponse(String token, long expiresIn) {
            this.token = token;
            this.expiresIn = expiresIn;
        }

        public String getToken() { return token; }

        public long getExpiresIn() { return expiresIn; }
    }
}