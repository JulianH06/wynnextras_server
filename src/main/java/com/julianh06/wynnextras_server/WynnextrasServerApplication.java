package com.julianh06.wynnextras_server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@SpringBootApplication
@RestController
public class WynnextrasServerApplication {
	@Autowired
	private UserRepository userRepository;

	public static void main(String[] args) {
		SpringApplication.run(WynnextrasServerApplication.class, args);
	}

	@GetMapping
	public String helloWorld() {
		return "Fick dich Muecke!";
	}

	@GetMapping("/db")
	public String viewDatabase() {
		StringBuilder sb = new StringBuilder();

		List<User> users = userRepository.findAll();
		users.sort((u1, u2) -> {
			if (u1.getUpdatedAt() == null && u2.getUpdatedAt() == null) return 0;
			if (u1.getUpdatedAt() == null) return -1;
			if (u2.getUpdatedAt() == null) return 1;
			return u1.getUpdatedAt().compareTo(u2.getUpdatedAt());
		});

		for (User u : users) {
			String date = u.getUpdatedAt() != null
					? new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
					.format(new java.util.Date(u.getUpdatedAt()))
					: "N/A";

			sb.append(u.getPlayerName())
					.append(" | ")
					.append(date)
					.append(" | ")
					.append(u.getModVersion())
					.append("<br>");
		}

		sb.append("<br>Total entries: ").append(users.size());

		return sb.toString();
	}
}
