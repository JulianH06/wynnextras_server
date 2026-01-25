package com.julianh06.wynnextras_server;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Date;
import java.util.List;

@Controller
public class DatabaseViewController {

    private final UserRepository userRepository;

    public DatabaseViewController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/db")
    public String viewDatabase(Model model) {
        List<User> users = userRepository.findAll();
        for(User u : users) {
            if(u.getUpdatedAt() != null) {
                u.setUpdatedAtDate(new Date(u.getUpdatedAt()));
            }
        }

        model.addAttribute("users", users);
        return "database";
    }
}