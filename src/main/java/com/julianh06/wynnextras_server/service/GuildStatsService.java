package com.julianh06.wynnextras_server.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import com.julianh06.wynnextras_server.repository.WynnExtrasUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GuildStatsService {
    private static final String[] RANKS = {"owner", "chief", "strategist", "captain", "recruiter", "recruit"};

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    @Autowired
    private WynnExtrasUserRepository wynnExtrasUserRepository;

    public GuildLookupResult lookupGuild(String tag) throws GuildLookupException, InterruptedException {
        try {
            String url = "https://api.wynncraft.com/v3/guild/prefix/" + URLEncoder.encode(tag.trim(), StandardCharsets.UTF_8);
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 404) {
                throw new GuildLookupException(404, "Guild not found");
            }
            if (resp.statusCode() != 200) {
                throw new GuildLookupException(502, "Wynncraft API error: " + resp.statusCode());
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(resp.body());

            String guildName = root.path("name").asText();
            String guildPrefix = root.path("prefix").asText();

            JsonNode membersNode = root.path("members");
            List<String> allUuids = new ArrayList<>();
            List<GuildMember> members = new ArrayList<>();

            for (String rank : RANKS) {
                JsonNode rankNode = membersNode.path(rank);
                if (rankNode.isMissingNode() || !rankNode.isObject()) continue;
                rankNode.fields().forEachRemaining(entry -> {
                    String playerName = entry.getKey();
                    String rawUuid = entry.getValue().path("uuid").asText();
                    String uuid = rawUuid.replace("-", "").toLowerCase();
                    allUuids.add(uuid);
                    members.add(new GuildMember(playerName, rank.toUpperCase(), uuid));
                });
            }

            List<WynnExtrasUser> dbUsers = wynnExtrasUserRepository.findAllById(allUuids);
            Map<String, WynnExtrasUser> userByUuid = new HashMap<>();
            for (WynnExtrasUser u : dbUsers) {
                userByUuid.put(u.getUuid(), u);
            }

            for (GuildMember member : members) {
                WynnExtrasUser user = userByUuid.get(member.uuid);
                if (user != null) {
                    member.isUser = true;
                    member.lastSeen = user.getLastSeen();
                    member.modVersion = user.getModVersion();
                }
            }

            return new GuildLookupResult(guildName, guildPrefix, members);
        } catch (GuildLookupException | InterruptedException e) {
            throw e;
        } catch (Exception e) {
            throw new GuildLookupException(502, "Error: " + e.getMessage());
        }
    }

    public GuildSnapshotStats buildSnapshotStats(String tag, Instant snapshotInstant) throws GuildLookupException, InterruptedException {
        GuildLookupResult lookup = lookupGuild(tag);
        GuildSnapshotStats stats = new GuildSnapshotStats();
        stats.guildTag = lookup.guildPrefix;
        stats.guildName = lookup.guildName;
        stats.memberCount = lookup.members.size();

        Instant cutoff1 = snapshotInstant.minus(1, ChronoUnit.DAYS);
        Instant cutoff3 = snapshotInstant.minus(3, ChronoUnit.DAYS);
        Instant cutoff5 = snapshotInstant.minus(5, ChronoUnit.DAYS);
        Instant cutoff7 = snapshotInstant.minus(7, ChronoUnit.DAYS);
        Instant cutoff10 = snapshotInstant.minus(10, ChronoUnit.DAYS);
        Instant cutoff14 = snapshotInstant.minus(14, ChronoUnit.DAYS);

        for (GuildMember member : lookup.members) {
            if (!member.isUser) continue;
            stats.wynnExtrasUsersTotal++;
            if (member.lastSeen == null) continue;
            if (member.lastSeen.isAfter(cutoff1)) stats.active1d++;
            if (member.lastSeen.isAfter(cutoff3)) stats.active3d++;
            if (member.lastSeen.isAfter(cutoff5)) stats.active5d++;
            if (member.lastSeen.isAfter(cutoff7)) stats.active7d++;
            if (member.lastSeen.isAfter(cutoff10)) stats.active10d++;
            if (member.lastSeen.isAfter(cutoff14)) stats.active14d++;
        }

        return stats;
    }

    public static class GuildLookupException extends Exception {
        private final int statusCode;

        public GuildLookupException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }

    public static class GuildLookupResult {
        private final String guildName;
        private final String guildPrefix;
        private final List<GuildMember> members;

        public GuildLookupResult(String guildName, String guildPrefix, List<GuildMember> members) {
            this.guildName = guildName;
            this.guildPrefix = guildPrefix;
            this.members = members;
        }

        public Map<String, Object> toResponseMap() {
            Map<String, Object> result = new HashMap<>();
            result.put("guildName", guildName);
            result.put("guildPrefix", guildPrefix);
            result.put("members", members.stream().map(GuildMember::toResponseMap).toList());
            return result;
        }
    }

    public static class GuildMember {
        private final String name;
        private final String rank;
        private final String uuid;
        private boolean isUser;
        private Instant lastSeen;
        private String modVersion;

        public GuildMember(String name, String rank, String uuid) {
            this.name = name;
            this.rank = rank;
            this.uuid = uuid;
        }

        public Map<String, Object> toResponseMap() {
            Map<String, Object> member = new HashMap<>();
            member.put("name", name);
            member.put("rank", rank);
            member.put("uuid", uuid);
            member.put("isUser", isUser);
            member.put("lastSeen", lastSeen != null ? lastSeen.toEpochMilli() : null);
            member.put("modVersion", modVersion);
            return member;
        }
    }

    public static class GuildSnapshotStats {
        public String guildTag;
        public String guildName;
        public int memberCount;
        public int wynnExtrasUsersTotal;
        public int active1d;
        public int active3d;
        public int active5d;
        public int active7d;
        public int active10d;
        public int active14d;
    }
}
