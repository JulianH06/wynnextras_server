package com.julianh06.wynnextras_server.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrivacyController {
    @GetMapping(value = "/privacy", produces = MediaType.TEXT_HTML_VALUE)
    public String privacy() {
        return """
                <!doctype html>
                <html lang="de">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Privacy Notice</title>
                    <style>
                        body { font-family: system-ui, sans-serif; max-width: 720px; margin: 48px auto; padding: 0 20px; line-height: 1.55; color: #1f2937; }
                        h1 { font-size: 1.8rem; margin-bottom: 1rem; }
                    </style>
                </head>
                <body>
                    <h1>Privacy Notice</h1>
                    <p>Data from OAuth2 setup is only used for in-game API calls such as the profile and guild viewers. It is not sent to our server.</p>
                    <p>Your Minecraft session credentials are never sent to or stored on our server. During authentication, your game confirms your Minecraft session directly with Mojang. Our server then asks Mojang whether the confirmation was successful and creates a separate login token that expires after two hours.</p>
                    <p>The only data our server collects is Minecraft UUID, Username, Mod Version, and last usage time to allow for basic usage statistics.</p>
                    <p>Users with version 0.19.0 and above are able to anonymize their data sent.</p>
                    <p>When anonymization is enabled, a random anonymous identifier is used instead and remains stable for 30 days. We store only this period identifier, the 30-day period, mod version, activity timestamps, and heartbeat counts. This data is stored in separate tables and contains no Minecraft UUID, username, session, badge data, or personal aspects.</p>
                    <h2>Public profile data</h2>
                    <p>Users with version 0.19.0 can also disable badges and personal aspects uploads. Badge selections and personal aspects are returned publicly only when published.</p>
                    <p>Hiding them does not delete them, and they can be published again later. Send a DM to one of the Admins in the WynnExtras Discord server if you want to delete old aspect data.</p>
                    <p>You can go to https://wynnextras.com/discord to join our Discord server.</p>
                </body>
                </html>
                """;
    }
}