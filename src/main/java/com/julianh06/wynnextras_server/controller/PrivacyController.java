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
                    <p>Your Minecraft session credentials are never sent to or stored on our server. During authentication, your game confirms your Minecraft session directly with Mojang, and our server checks whether that confirmation succeeded. If it did, our server creates a random, temporary WynnExtras session identifier. It is used only to authorize requests to our server, it does not provide access to your Minecraft account, and expires after two hours.</p>
                    <p>The only data our server collects is Minecraft UUID, Username, Mod Version, and last usage time to allow for basic usage statistics.</p>
                    <p>Users with version 0.19.0 and above are able to anonymize their data sent.</p>
                    <p>When anonymization is enabled, a random anonymous identifier is used instead and remains stable for 30 days. We store only this identifier, the 30-day period, mod version, activity timestamps, and heartbeat counts. This data is stored in separate tables and contains no Minecraft UUID, username, session, badge data, or personal aspects.</p>
                    <h2>Public profile data</h2>
                    <p>Users with version 0.19.0 and above can also disable badges and personal aspects uploads. Badge selections and personal aspects are returned publicly only when published.</p>
                    <p>Hiding them does not delete them, and they can be published again later. Send a DM to one of the Admins in the WynnExtras Discord server if you want to delete old aspect data.</p>
                    <h2>Legal</h2>
                    <p>Under the GDPR, you may request confirmation as to whether we process personal data concerning you and, if so, access to and a copy of that data. Where the legal requirements are met, you may also request correction or deletion of your data, restriction of its processing, object to its processing, or exercise your right to data portability.</p>
                    <p>To exercise any of these rights, send a DM to one of the Admins in the WynnExtras Discord server. Please include your Minecraft username and UUID so that we can locate your data. We may request additional information where necessary to verify your identity before disclosing or deleting personal data.</p>
                    <p>If you have any concerns about how your data is handled, please reach out to us on Discord and we will do our best to help. If you prefer, you can also contact a data protection supervisory authority.</p>
                    <p>You can join our Discord server at <a href="https://wynnextras.com/discord">wynnextras.com/discord</a>.</p>
                </body>
                </html>
                """;
    }
}