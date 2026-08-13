package com.julianh06.wynnextras_server.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
public class DiscordController {
    private static final URI DISCORD_INVITE = URI.create("https://discord.gg/UbC6vZDaD5");

    @GetMapping("/discord")
    public ResponseEntity<Void> discord() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(DISCORD_INVITE)
                .build();
    }
}
