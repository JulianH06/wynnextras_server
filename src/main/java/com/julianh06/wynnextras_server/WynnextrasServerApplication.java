package com.julianh06.wynnextras_server;

import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import com.julianh06.wynnextras_server.repository.WynnExtrasUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
		sb.append("Total entries:").append(allUsers.size()).append("<br><br>");

		List<WynnExtrasUser> allUsersCopy = new ArrayList<>(allUsers);
		allUsersCopy.sort(Comparator.comparing(WynnExtrasUser::getCreatedAt));
		printUsers(sb, allUsersCopy);

		sb.append("<br> Time sorted: <br> <br>");

		allUsers.sort((u1, u2) -> {
			if (u1.getLastSeen() == null && u2.getLastSeen() == null) return 0;
			if (u1.getLastSeen() == null) return -1;
			if (u2.getLastSeen() == null) return 1;
			return u1.getLastSeen().compareTo(u2.getLastSeen());
		});

		printUsers(sb, allUsers);

		return sb.toString();
	}

	private void printUsers(StringBuilder sb, List<WynnExtrasUser> allUsersCopy) {
		for (WynnExtrasUser u : allUsersCopy) {
			String date = u.getLastSeen() != null
					? new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
					.format(new java.util.Date(u.getLastSeen().toEpochMilli()))
					: "N/A";

			String created = u.getCreatedAt() != null
					? new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss")
					.format(new java.util.Date(u.getCreatedAt().toEpochMilli()))
					: "N/A";

			sb.append(u.getUsername())
					.append(" | ")
					.append(created)
					.append(" | ")
					.append(date)
					.append(" | ")
					.append(u.getModVersion())
					.append("<br>");
		}
	}
}