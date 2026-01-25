package com.julianh06.wynnextras_server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

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
					.append("<br>");
		}

		return sb.toString();
	}
}
