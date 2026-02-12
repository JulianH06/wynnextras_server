package com.julianh06.wynnextras_server;

import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import com.julianh06.wynnextras_server.repository.WynnExtrasUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@SpringBootApplication
@RestController
public class WynnextrasServerApplication {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private WynnExtrasUserRepository wynnExtrasUserRepository;

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

		List<WynnExtrasUser> allUsers = wynnExtrasUserRepository.findActiveUsersSince(Instant.ofEpochSecond(0));
		sb.append("Total entries: <br>").append(allUsers.size());

		for (WynnExtrasUser u : allUsers) {
			String date = u.getLastSeen() != null
					? new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
					.format(new java.util.Date(u.getLastSeen().toEpochMilli()))
					: "N/A";

			sb.append(u.getUsername())
					.append(" | ")
					.append(date)
					.append(" | ")
					.append(u.getModVersion())
					.append("<br>");
		}

		sb.append("<br> Time sorted: <br>");

		allUsers.sort((u1, u2) -> {
			if (u1.getLastSeen() == null && u2.getLastSeen() == null) return 0;
			if (u1.getLastSeen() == null) return -1;
			if (u2.getLastSeen() == null) return 1;
			return u1.getLastSeen().compareTo(u2.getLastSeen());
		});

		for (WynnExtrasUser u : allUsers) {
			String date = u.getLastSeen() != null
					? new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
					.format(new java.util.Date(u.getLastSeen().toEpochMilli()))
					: "N/A";

			sb.append(u.getUsername())
					.append(" | ")
					.append(date)
					.append(" | ")
					.append(u.getModVersion())
					.append("<br>");
		}

		return sb.toString();
	}
}