package com.julianh06.wynnextras_server.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ResetTimesController {
    private static final ResetConfig CONFIG = new ResetConfig(
            new ResetTime("FRIDAY", 17, 0, "UTC"),  // raid aspect lootpool
            new ResetTime("FRIDAY", 18, 0, "UTC"),  // lootrun lootpool
            new ResetTime(null,     17, 0, "UTC")   // gambit (täglich, day=null)
    );

    @GetMapping("/reset-times")
    public ResetConfig getResetTimes() {
        return CONFIG;
    }

    public record ResetTime(String day, int hour, int minute, String timezone) {}
    public record ResetConfig(ResetTime lootpool_reset, ResetTime lootrun_reset, ResetTime gambit_reset) {}
}