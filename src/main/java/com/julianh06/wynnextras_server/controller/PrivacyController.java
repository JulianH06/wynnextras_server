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
                    <p>Data listed during OAuth2 setup is only used for ingame API calls like the profile and guild viewer and is not send to our server.</p>
                    <p>The only data our server collects is Minecraft UUID, Username, Mod Version, and last usage time to allow for basic usage statistics.</p>
                </body>
                </html>
                """;
    }
}