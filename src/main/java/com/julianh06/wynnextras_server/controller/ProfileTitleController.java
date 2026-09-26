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

        String teamMemberTitle = "WynnExtras Team Member";
        profileTitles.put("JulianH06", teamMemberTitle);
        profileTitles.put("Teslanator", teamMemberTitle);
        profileTitles.put("pat_crafter07", teamMemberTitle);

        String contributorTitle = "WynnExtras Contributor";
        profileTitles.put("Mikecraft1224", contributorTitle);
        profileTitles.put("elwood24", contributorTitle);
        profileTitles.put("LegendaryVirus", contributorTitle);
        profileTitles.put("BaltrazYT", contributorTitle);
        profileTitles.put("LookingForSleep", contributorTitle);
        profileTitles.put("SidOfThe7Cs", contributorTitle);
        profileTitles.put("drzxm", contributorTitle);
        profileTitles.put("theoplegends", contributorTitle);
        profileTitles.put("Tabytac", contributorTitle);
        profileTitles.put("Zatzou", contributorTitle);
        profileTitles.put("Rafii2198", contributorTitle);
        profileTitles.put("ValentineX", contributorTitle);
        profileTitles.put("Tapu_Zuko", contributorTitle);

        profileTitles.put("Muecke3001", "Fick dich Muecke!");
        profileTitles.put("Colossal_Rat", "rat");
        profileTitles.put("Bnnui", "Bunny");
        profileTitles.put("Hotaga", "Hotaga Hotaga Hotaga");
        profileTitles.put("LoubiOP", "Arschloch");
        profileTitles.put("NEMQNJAA", "Bolesnik");
        profileTitles.put("Shisouhan", "Mr. Poi");
        profileTitles.put("Crafterbags", "MR POI OPENED ME");
        profileTitles.put("mcpro_gamer", "Moves HQ with mind");
        profileTitles.put("1wolvesgaming", "The real seq trial guy");
        profileTitles.put("xStefke", "#1 TNA Coach");
        profileTitles.put("ysosweet", "sleepy");
        profileTitles.put("mrhmar", "hammy");
        profileTitles.put("kablob", "kablud");
        profileTitles.put("a3pki", "stupid cat");
        profileTitles.put("shironappa", "500");
        profileTitles.put("cela41", "cat");
        profileTitles.put("superkat1403", "#1 bomb buyer");
        profileTitles.put("reyzhia", "French proffa");
        profileTitles.put("kubawu_", "Mines ore with mind");
        profileTitles.put("zmiksowany", "Hatsune Miku");
        profileTitles.put("9abag9", "APSOOO");
        profileTitles.put("6Steezoo9", "Drunk Bunny");
        profileTitles.put("Harnasiov", "Russian Duck");
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