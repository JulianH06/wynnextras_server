package com.julianh06.wynnextras_server.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;

@RestController
@RequestMapping("/api")
public class ProfileTitleController {
    private final HashMap<String, String> profileTitles;

    public ProfileTitleController() {
        profileTitles = new HashMap<>();

        profileTitles.put("JulianH06", "WynnExtras Team Member");
        profileTitles.put("Teslanator", "WynnExtras Team Member");
        profileTitles.put("pat_crafter07", "WynnExtras Team Member");

        profileTitles.put("Mikecraft1224", "WynnExtras Contributor");
        profileTitles.put("elwood24", "WynnExtras Contributor");
        profileTitles.put("LegendaryVirus", "WynnExtras Contributor");
        profileTitles.put("BaltrazYT", "WynnExtras Contributor");
        profileTitles.put("LookingForSleep", "WynnExtras Contributor");
        profileTitles.put("SidOfThe7Cs", "WynnExtras Contributor");
        profileTitles.put("drzxm", "WynnExtras Contributor");
        profileTitles.put("theoplegends", "WynnExtras Contributor");
        profileTitles.put("Tabytac", "WynnExtras Contributor");
        profileTitles.put("Zatzou", "WynnExtras Contributor");
        profileTitles.put("Rafii2198", "WynnExtras Contributor");

        profileTitles.put("Muecke3001", "Fick dich Muecke!");
        profileTitles.put("Colossal_Rat", "rat");
        profileTitles.put("Bnnui", "Bunny");
        profileTitles.put("Hotaga", "Hotaga Hotaga Hotaga");
    }

    ProfileTitleController(HashMap<String, String> profileTitles) {
        this.profileTitles = profileTitles;
    }

    @GetMapping(value = "/profile-titles", produces = MediaType.APPLICATION_JSON_VALUE)
    public HashMap<String, String> getProfileTitles() {
        HashMap<String, String> validProfileTitles = new HashMap<>();
        profileTitles.forEach((username, title) -> {
            if (username != null && !username.isBlank() && title != null && !title.isBlank()) {
                validProfileTitles.put(username, title);
            }
        });
        return validProfileTitles;
    }
}