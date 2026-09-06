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
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>Privacy Notice</title>
                    <style>
                        body { font-family: system-ui, sans-serif; max-width: 720px; margin: 48px auto; padding: 0 20px; line-height: 1.55; color: #1f2937; }
                        h1 { font-size: 1.8rem; margin-bottom: 1rem; }
                        h2 { font-size: 1.25rem; margin-top: 2rem; }
                        li { margin: .45rem 0; }
                        .updated { color: #6b7280; }
                    </style>
                </head>
                <body>
                    <h1>Privacy Notice</h1>
                    <p class="updated">Last updated: September 6, 2026</p>

                    <p>Data from OAuth2 setup is only used for in-game API calls such as the profile and guild viewers. It is not sent to our server.</p>

                    <h2>Authentication with the WynnExtras server</h2>
                    <p>Your Minecraft session credentials are never sent to or stored on our server. During authentication, your game confirms your Minecraft session directly with Mojang, and our server checks whether that confirmation succeeded. If it did, our server creates a random, temporary WynnExtras session identifier. It is used only to authorize requests to our server, it does not provide access to your Minecraft account, and expires after two hours.</p>

                    <h2>Usage telemetry</h2>
                    <p>Starting with WynnExtras 0.19.0, you can choose one of three telemetry modes in the privacy settings:</p>
                    <ul>
                        <li><strong>On:</strong> The server stores your Minecraft UUID, username, mod version, first and last usage timestamps, and heartbeat counts. Daily activity derived from these heartbeats is used for basic usage statistics.</li>
                        <li><strong>Anonymize:</strong> Heartbeats contain a pseudonymous, installation-specific identifier instead of your Minecraft UUID and username. The identifier changes for each 30-day UTC period. The server stores the identifier, period, mod version, first and last activity timestamps, and heartbeat counts in separate tables that are not linked to UUID-based data.</li>
                        <li><strong>Off:</strong> No usage telemetry is sent.</li>
                    </ul>
                    <p>Your telemetry mode applies only to usage statistics. Other enabled features may still authenticate and exchange the optional data described below.</p>

                    <h2>Optional WynnExtras data</h2>
                    <ul>
                        <li><strong>Badge:</strong> Your Minecraft UUID, username, selected badge icon and color, publication status, and badge activity time. Published badges are returned to other users; hidden badges are not.</li>
                        <li><strong>Aspects:</strong> Your Minecraft UUID, username, mod version, aspect names, rarities, amounts, update time, and publication status. Published aspects can be viewed by other users and included in the aspect leaderboard.</li>
                        <li><strong>Achievements:</strong> Your Minecraft UUID, username, mod version, achievement unlocks and progress, and update time. Uploaded achievements can be viewed by other users and included in achievement lists and leaderboards.</li>
                        <li><strong>Daily gambits:</strong> Crowdsourced gambit names and descriptions, the submission day and time, and your verified username. The username is used to validate submissions. Approved gambit data is shared with other users.</li>
                    </ul>
                    <p>The privacy settings let you hide your badge and aspects, stop future achievement uploads, and stop crowdsourcing daily gambits. Hiding a badge or aspects does not delete the stored data, and it can be published again later. Disabling achievement uploads or gambit crowdsourcing also does not delete data submitted previously.</p>

                    <h2>Optional downloads</h2>
                    <p>You can separately stop WynnExtras from fetching player badges, achievements, player aspects and the aspect leaderboard, crowdsourced gambits, custom profile titles, and loot pool, lootrun, and gambit reset times. These settings stop future requests for the selected data; they do not delete server-side data. Some data may require a game restart to be fetched again after re-enabling a request.</p>

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
