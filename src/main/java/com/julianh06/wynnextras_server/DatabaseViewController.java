package com.julianh06.wynnextras_server;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DatabaseViewController {

    private final UserRepository userRepository;

    public DatabaseViewController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/db")
    public String viewDatabase() {
        StringBuilder sb = new StringBuilder();

        for (User u : userRepository.findAll()) {
            String date = u.getUpdatedAt() != null
                    ? new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
                    .format(new java.util.Date(u.getUpdatedAt()))
                    : "N/A";

            sb.append(u.getUuid())
                    .append(" | ")
                    .append(u.getModVersion())
                    .append(" | ")
                    .append(u.getPlayerName())
                    .append(" | ")
                    .append(date)
                    .append("\n");
        }

        return sb.toString();
    }
}
