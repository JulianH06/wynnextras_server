package com.julianh06.wynnextras_server;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DatabaseViewController {

    private final UserRepository userRepository;

    public DatabaseViewController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/db")
    public String viewDatabase(Model model) {
        model.addAttribute("users", userRepository.findAll());
        return "database";
    }
}