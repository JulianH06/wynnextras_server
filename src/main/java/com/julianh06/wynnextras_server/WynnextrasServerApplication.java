package com.julianh06.wynnextras_server;

import com.julianh06.wynnextras_server.entity.ActiveUserSnapshot;
import com.julianh06.wynnextras_server.entity.GuildUserSnapshot;
import com.julianh06.wynnextras_server.entity.VersionUsageSnapshot;
import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import com.julianh06.wynnextras_server.entity.WynncraftUsageSnapshot;
import com.julianh06.wynnextras_server.repository.ActiveUserSnapshotRepository;
import com.julianh06.wynnextras_server.repository.DailyUserActivityRepository;
import com.julianh06.wynnextras_server.repository.GuildUserSnapshotRepository;
import com.julianh06.wynnextras_server.repository.VersionUsageSnapshotRepository;
import com.julianh06.wynnextras_server.repository.WynnExtrasUserRepository;
import com.julianh06.wynnextras_server.repository.WynncraftUsageSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

@SpringBootApplication
@EnableScheduling
@RestController
public class WynnextrasServerApplication {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private WynnExtrasUserRepository wynnExtrasUserRepository;

	@Autowired
	private ActiveUserSnapshotRepository activeUserSnapshotRepository;

	@Autowired
	private GuildUserSnapshotRepository guildUserSnapshotRepository;

	@Autowired
	private DailyUserActivityRepository dailyUserActivityRepository;

	@Autowired
	private VersionUsageSnapshotRepository versionUsageSnapshotRepository;

	@Autowired
	private WynncraftUsageSnapshotRepository wynncraftUsageSnapshotRepository;

	public static void main(String[] args) {
		SpringApplication.run(WynnextrasServerApplication.class, args);
	}

	@GetMapping
	public String helloWorld() {
		return "Fick dich Muecke!";
	}

	@GetMapping("/db")
	public ResponseEntity<String> viewDatabase() {
		List<WynnExtrasUser> allUsers = wynnExtrasUserRepository.findActiveUsersSince(Instant.ofEpochSecond(0));

		ZoneId utc = ZoneId.of("UTC");
		DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(utc);

		List<WynnExtrasUser> sorted = new ArrayList<>(allUsers);
		sorted.sort(Comparator.comparing(u -> u.getCreatedAt() != null ? u.getCreatedAt() : Instant.EPOCH));

		// ── Chart 1: Cumulative users ─────────────────────────────────────
		Map<String, Integer> createdPerDay = new LinkedHashMap<>();
		for (WynnExtrasUser u : sorted) {
			if (u.getCreatedAt() == null) continue;
			createdPerDay.merge(dayFmt.format(u.getCreatedAt()), 1, Integer::sum);
		}
		StringBuilder c1l = new StringBuilder(), c1d = new StringBuilder(), c1s = new StringBuilder();
		int cum = 0;
		for (Map.Entry<String, Integer> e : createdPerDay.entrySet()) {
			cum += e.getValue();
			if (c1l.length() > 0) { c1l.append(","); c1d.append(","); c1s.append(","); }
			c1l.append('"').append(e.getKey()).append('"');
			c1d.append(cum);
			c1s.append(e.getValue());
		}

		// ── Chart 2: New users per week ───────────────────────────────────
		Map<String, Integer> newPerWeek = new LinkedHashMap<>();
		for (WynnExtrasUser u : sorted) {
			if (u.getCreatedAt() == null) continue;
			LocalDate monday = LocalDate.ofInstant(u.getCreatedAt(), utc).with(java.time.DayOfWeek.MONDAY);
			newPerWeek.merge(monday.toString(), 1, Integer::sum);
		}
		StringBuilder c2l = new StringBuilder(), c2d = new StringBuilder();
		for (Map.Entry<String, Integer> e : newPerWeek.entrySet()) {
			if (c2l.length() > 0) { c2l.append(","); c2d.append(","); }
			c2l.append('"').append(e.getKey()).append('"');
			c2d.append(e.getValue());
		}

		// ── Chart 3: Daily last-seen + 7-day rolling average ─────────────
		Map<String, Integer> lastSeenMap = new LinkedHashMap<>();
		for (WynnExtrasUser u : allUsers) {
			if (u.getLastSeen() == null) continue;
			lastSeenMap.merge(dayFmt.format(u.getLastSeen()), 1, Integer::sum);
		}
		List<String> lsDays = new ArrayList<>(lastSeenMap.keySet());
		Collections.sort(lsDays);
		StringBuilder c3l = new StringBuilder(), c3bar = new StringBuilder(), c3avg = new StringBuilder();
		for (int i = 0; i < lsDays.size(); i++) {
			if (c3l.length() > 0) { c3l.append(","); c3bar.append(","); c3avg.append(","); }
			c3l.append('"').append(lsDays.get(i)).append('"');
			c3bar.append(lastSeenMap.get(lsDays.get(i)));
			int start = Math.max(0, i - 6);
			int sum = 0;
			for (int j = start; j <= i; j++) sum += lastSeenMap.get(lsDays.get(j));
			c3avg.append(String.format("%.1f", (double) sum / (i - start + 1)).replace(',', '.'));
		}

		// ── Chart 4: Mod version distribution ────────────────────────────
		String[] pieColors = {
			"rgba(0,200,255,0.7)","rgba(0,229,160,0.7)","rgba(255,180,0,0.7)",
			"rgba(255,69,96,0.7)","rgba(180,100,255,0.7)","rgba(255,140,50,0.7)",
			"rgba(50,200,120,0.7)","rgba(100,160,255,0.7)","rgba(255,220,50,0.7)","rgba(200,80,160,0.7)"
		};
		Map<String, Integer> versionMap = new LinkedHashMap<>();
		for (WynnExtrasUser u : allUsers) {
			if (u.getModVersion() == null || u.getModVersion().isBlank()) continue;
			versionMap.merge(u.getModVersion(), 1, Integer::sum);
		}
		List<Map.Entry<String, Integer>> vEntries = new ArrayList<>(versionMap.entrySet());
		vEntries.sort((a, b) -> b.getValue() - a.getValue());
		StringBuilder c4l = new StringBuilder(), c4d = new StringBuilder();
		for (Map.Entry<String, Integer> e : vEntries) {
			if (c4l.length() > 0) { c4l.append(","); c4d.append(","); }
			c4l.append('"').append(e.getKey()).append('"');
			c4d.append(e.getValue());
		}

		// ── Charts 4b/4c: Version distribution — active last 7 / 14 days ──
		Instant now = Instant.now();
		Instant cutoff7  = now.minus(7,  java.time.temporal.ChronoUnit.DAYS);
		Instant cutoff14 = now.minus(14, java.time.temporal.ChronoUnit.DAYS);

		Map<String, Integer> versionMap7  = new LinkedHashMap<>();
		Map<String, Integer> versionMap14 = new LinkedHashMap<>();
		for (WynnExtrasUser u : allUsers) {
			if (u.getModVersion() == null || u.getModVersion().isBlank() || u.getLastSeen() == null) continue;
			if (u.getLastSeen().isAfter(cutoff7))  versionMap7.merge(u.getModVersion(),  1, Integer::sum);
			if (u.getLastSeen().isAfter(cutoff14)) versionMap14.merge(u.getModVersion(), 1, Integer::sum);
		}
		List<Map.Entry<String, Integer>> vEntries7  = new ArrayList<>(versionMap7.entrySet());
		List<Map.Entry<String, Integer>> vEntries14 = new ArrayList<>(versionMap14.entrySet());
		vEntries7.sort((a, b)  -> b.getValue() - a.getValue());
		vEntries14.sort((a, b) -> b.getValue() - a.getValue());

		StringBuilder c4bl = new StringBuilder(), c4bd = new StringBuilder(), c4bColors = new StringBuilder();
		StringBuilder c4cl = new StringBuilder(), c4cd = new StringBuilder(), c4cColors = new StringBuilder();
		int vi7 = 0;
		for (Map.Entry<String, Integer> e : vEntries7) {
			if (c4bl.length() > 0) { c4bl.append(","); c4bd.append(","); c4bColors.append(","); }
			c4bl.append('"').append(e.getKey()).append('"');
			c4bd.append(e.getValue());
			c4bColors.append('"').append(pieColors[vi7 % pieColors.length]).append('"');
			vi7++;
		}
		int vi14 = 0;
		for (Map.Entry<String, Integer> e : vEntries14) {
			if (c4cl.length() > 0) { c4cl.append(","); c4cd.append(","); c4cColors.append(","); }
			c4cl.append('"').append(e.getKey()).append('"');
			c4cd.append(e.getValue());
			c4cColors.append('"').append(pieColors[vi14 % pieColors.length]).append('"');
			vi14++;
		}

		// ── Chart 5: Hour-of-day activity ─────────────────────────────────
		int[] hours = new int[24];
		for (WynnExtrasUser u : allUsers) {
			if (u.getLastSeen() == null) continue;
			hours[u.getLastSeen().atZone(utc).getHour()]++;
		}
		StringBuilder c5d = new StringBuilder();
		for (int i = 0; i < 24; i++) {
			if (i > 0) c5d.append(",");
			c5d.append(hours[i]);
		}

		// ── Chart 6: Daily active-user snapshots ─────────────────────────
		List<ActiveUserSnapshot> activeSnapshots = activeUserSnapshotRepository.findTop90ByOrderBySnapshotDateDesc();
		Collections.reverse(activeSnapshots);
		StringBuilder c6l = new StringBuilder(), c6d1 = new StringBuilder(), c6d3 = new StringBuilder(), c6d5 = new StringBuilder();
		StringBuilder c6d7 = new StringBuilder(), c6d10 = new StringBuilder(), c6d14 = new StringBuilder();
		for (ActiveUserSnapshot s : activeSnapshots) {
			appendCsv(c6l, jsQuote(s.getSnapshotDate().toString()));
			appendCsv(c6d1, Long.toString(s.getActive1d()));
			appendCsv(c6d3, Long.toString(s.getActive3d()));
			appendCsv(c6d5, Long.toString(s.getActive5d()));
			appendCsv(c6d7, Long.toString(s.getActive7d()));
			appendCsv(c6d10, Long.toString(s.getActive10d()));
			appendCsv(c6d14, Long.toString(s.getActive14d()));
		}

		// ── Chart 6b: Wynncraft-wide daily usage percentage ──────────────
		List<WynncraftUsageSnapshot> usageSnapshots = wynncraftUsageSnapshotRepository.findTop90ByOrderBySnapshotDateDesc();
		Collections.reverse(usageSnapshots);
		StringBuilder c6bl = new StringBuilder(), c6bpct = new StringBuilder(), c6bUsers = new StringBuilder();
		StringBuilder c6bTotal = new StringBuilder(), c6bSamples = new StringBuilder();
		WynncraftUsageSnapshot latestUsageSnapshot = null;
		for (WynncraftUsageSnapshot s : usageSnapshots) {
			appendCsv(c6bl, jsQuote(s.getSnapshotDate().toString()));
			appendCsv(c6bUsers, Long.toString(s.getWynnExtrasUsers()));
			appendCsv(c6bTotal, Long.toString(s.getUniquePlayers()));
			appendCsv(c6bSamples, Long.toString(s.getSampleCount()));
			if (s.getErrorMessage() != null) {
				appendCsv(c6bpct, "null");
			} else {
				appendCsv(c6bpct, formatNumber(s.getUsagePercent(), 2));
				latestUsageSnapshot = s;
			}
		}

		// ── Chart 7/8: Daily heartbeat volume + retention/churn ──────────
		List<Object[]> heartbeatRows = dailyUserActivityRepository.findDailyHeartbeatStats();
		StringBuilder c7l = new StringBuilder(), c7unique = new StringBuilder(), c7heartbeats = new StringBuilder();
		StringBuilder c8l = new StringBuilder(), c8new = new StringBuilder(), c8returned = new StringBuilder(), c8d1 = new StringBuilder();
		for (Object[] row : heartbeatRows) {
			LocalDate day = (LocalDate) row[0];
			long uniqueUsers = ((Number) row[1]).longValue();
			long heartbeatCount = ((Number) row[2]).longValue();
			appendCsv(c7l, jsQuote(day.toString()));
			appendCsv(c7unique, Long.toString(uniqueUsers));
			appendCsv(c7heartbeats, Long.toString(heartbeatCount));
			appendCsv(c8l, jsQuote(day.toString()));
			appendCsv(c8new, Long.toString(dailyUserActivityRepository.countFirstSeenOnDate(day)));
			appendCsv(c8returned, Long.toString(dailyUserActivityRepository.countReturnedAfterGap(day, day.minusDays(7))));
			appendCsv(c8d1, Long.toString(dailyUserActivityRepository.countCohortReturnedOnDate(day.minusDays(1), day)));
		}

		LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
		long newToday = dailyUserActivityRepository.countFirstSeenOnDate(todayUtc);
		long returnedToday = dailyUserActivityRepository.countReturnedAfterGap(todayUtc, todayUtc.minusDays(7));
		long inactive7 = allUsers.stream().filter(u -> u.getLastSeen() == null || !u.getLastSeen().isAfter(now.minus(7, java.time.temporal.ChronoUnit.DAYS))).count();
		long inactive14 = allUsers.stream().filter(u -> u.getLastSeen() == null || !u.getLastSeen().isAfter(now.minus(14, java.time.temporal.ChronoUnit.DAYS))).count();
		long inactive30 = allUsers.stream().filter(u -> u.getLastSeen() == null || !u.getLastSeen().isAfter(now.minus(30, java.time.temporal.ChronoUnit.DAYS))).count();

		// ── Chart 9: Version adoption timeline ───────────────────────────
		List<VersionUsageSnapshot> versionSnapshots = versionUsageSnapshotRepository.findTop1000ByOrderBySnapshotDateAscModVersionAsc();
		Set<String> versionDates = new LinkedHashSet<>();
		Set<String> versions = new LinkedHashSet<>();
		Map<String, Map<String, VersionUsageSnapshot>> versionByDateVersion = new LinkedHashMap<>();
		for (VersionUsageSnapshot s : versionSnapshots) {
			String day = s.getSnapshotDate().toString();
			versionDates.add(day);
			versions.add(s.getModVersion());
			versionByDateVersion.computeIfAbsent(day, ignored -> new LinkedHashMap<>()).put(s.getModVersion(), s);
		}
		StringBuilder c9l = new StringBuilder();
		StringBuilder c9AllDatasets = new StringBuilder(), c9Active1dDatasets = new StringBuilder(), c9Active3dDatasets = new StringBuilder();
		StringBuilder c9Active7dDatasets = new StringBuilder(), c9Active14dDatasets = new StringBuilder();
		for (String day : versionDates) appendCsv(c9l, jsQuote(day));
		int versionColorIndex = 0;
		for (String version : versions) {
			String color = pieColors[versionColorIndex % pieColors.length];
			StringBuilder allData = new StringBuilder(), active1dData = new StringBuilder(), active3dData = new StringBuilder();
			StringBuilder active7dData = new StringBuilder(), active14dData = new StringBuilder();
			for (String day : versionDates) {
				VersionUsageSnapshot s = versionByDateVersion.getOrDefault(day, Map.of()).get(version);
				appendCsv(allData, Long.toString(s == null ? 0L : s.getUserCount()));
				appendCsv(active1dData, Long.toString(s == null ? 0L : s.getActive1dCount()));
				appendCsv(active3dData, Long.toString(s == null ? 0L : s.getActive3dCount()));
				appendCsv(active7dData, Long.toString(s == null ? 0L : s.getActive7dCount()));
				appendCsv(active14dData, Long.toString(s == null ? 0L : s.getActive14dCount()));
			}
			appendTimelineDataset(c9AllDatasets, version, allData, color);
			appendTimelineDataset(c9Active1dDatasets, version, active1dData, color);
			appendTimelineDataset(c9Active3dDatasets, version, active3dData, color);
			appendTimelineDataset(c9Active7dDatasets, version, active7dData, color);
			appendTimelineDataset(c9Active14dDatasets, version, active14dData, color);
			versionColorIndex++;
		}

		// ── Charts 10/11: Guild active + adoption snapshots ──────────────
		List<GuildUserSnapshot> guildSnapshots = guildUserSnapshotRepository.findTop1000ByOrderBySnapshotDateAscGuildTagAsc();
		Set<String> guildDates = new LinkedHashSet<>();
		Set<String> guildTags = new LinkedHashSet<>();
		Map<String, Map<String, GuildUserSnapshot>> guildByDateTag = new LinkedHashMap<>();
		for (GuildUserSnapshot s : guildSnapshots) {
			String day = s.getSnapshotDate().toString();
			guildDates.add(day);
			guildTags.add(s.getGuildTag());
			guildByDateTag.computeIfAbsent(day, ignored -> new LinkedHashMap<>()).put(s.getGuildTag(), s);
		}
		StringBuilder c10l = new StringBuilder(), c10datasets = new StringBuilder(), c11datasets = new StringBuilder();
		for (String day : guildDates) appendCsv(c10l, jsQuote(day));
		int guildColorIndex = 0;
		for (String tag : guildTags) {
			String color = pieColors[guildColorIndex % pieColors.length].replace("0.7", "0.9");
			StringBuilder activeData = new StringBuilder();
			StringBuilder adoptionData = new StringBuilder();
			for (String day : guildDates) {
				GuildUserSnapshot s = guildByDateTag.getOrDefault(day, Map.of()).get(tag);
				appendCsv(activeData, s == null || s.getErrorMessage() != null ? "null" : Integer.toString(s.getActive7d()));
				if (s == null || s.getErrorMessage() != null || s.getMemberCount() == 0) {
					appendCsv(adoptionData, "null");
				} else {
					double pct = (double) s.getWynnExtrasUsersTotal() * 100.0 / s.getMemberCount();
					appendCsv(adoptionData, String.format("%.1f", pct).replace(',', '.'));
				}
			}
			if (c10datasets.length() > 0) { c10datasets.append(","); c11datasets.append(","); }
			c10datasets.append("{ label:").append(jsQuote(tag)).append(", data:[").append(activeData)
					.append("], borderColor:").append(jsQuote(color)).append(", backgroundColor:'transparent', borderWidth:2, pointRadius:1, tension:0.25 }");
			c11datasets.append("{ label:").append(jsQuote(tag)).append(", data:[").append(adoptionData)
					.append("], borderColor:").append(jsQuote(color)).append(", backgroundColor:'transparent', borderWidth:2, pointRadius:1, tension:0.25 }");
			guildColorIndex++;
		}

		// ── HTML ──────────────────────────────────────────────────────────
		StringBuilder sb = new StringBuilder();
		StringBuilder c4colors = new StringBuilder();
		int vi = 0;
		for (Map.Entry<String, Integer> e : vEntries) {
			if (c4colors.length() > 0) c4colors.append(",");
			c4colors.append('"').append(pieColors[vi % pieColors.length]).append('"');
			vi++;
		}

		sb.append("""
				<!DOCTYPE html>
				<html lang="en">
				<head>
				<meta charset="UTF-8">
				<meta name="viewport" content="width=device-width, initial-scale=1.0">
				<title>WynnExtras DB</title>
				<script src="https://cdn.jsdelivr.net/npm/chart.js@4/dist/chart.umd.min.js"></script>
				<style>
				  * { box-sizing: border-box; margin: 0; padding: 0; }
				  body { background: #0a0c0f; color: #c8d8e8; font-family: 'Share Tech Mono', monospace; padding: 32px; }
				  h1 { color: #00c8ff; font-size: 20px; letter-spacing: 2px; margin-bottom: 4px; }
				  .subtitle { color: #4a6080; font-size: 12px; margin-bottom: 32px; }
				  .grid { display: grid; gap: 20px; max-width: 1200px; }
				  .grid-2 { grid-template-columns: 1fr 1fr; }
				  .card { background: #111419; border: 1px solid #1e2530; border-radius: 10px; padding: 20px; }
				  .card-title { color: #4a6080; font-size: 10px; letter-spacing: 3px; text-transform: uppercase; margin-bottom: 16px; }
				  .card-header { display:flex; align-items:center; justify-content:space-between; gap:12px; margin-bottom:16px; }
				  .card-header .card-title { margin-bottom:0; }
				  .timeline-controls { display:flex; flex-wrap:wrap; gap:6px; justify-content:flex-end; }
				  .timeline-chip { font-family:'Share Tech Mono',monospace; font-size:10px; padding:5px 9px; border-radius:4px; border:1px solid #2a3545; background:transparent; color:#4a6080; cursor:pointer; white-space:nowrap; }
				  .timeline-chip.tchip-active { border-color:#00c8ff; color:#00c8ff; background:rgba(0,200,255,0.08); }
				  .guild-chart-controls { display:flex; flex-wrap:wrap; gap:6px; justify-content:flex-end; }
				  .guild-chart-chip { font-family:'Share Tech Mono',monospace; font-size:10px; padding:5px 9px; border-radius:4px; border:1px solid #2a3545; background:transparent; color:#4a6080; cursor:pointer; white-space:nowrap; }
				  .guild-chart-chip.gchip-active { border-color:#00c8ff; color:#00c8ff; background:rgba(0,200,255,0.08); }
				  .metric-grid { display:grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap:12px; max-width:1200px; margin-bottom:20px; }
				  .metric { background:#111419; border:1px solid #1e2530; border-radius:8px; padding:14px; }
				  .metric-label { color:#4a6080; font-size:9px; letter-spacing:2px; text-transform:uppercase; margin-bottom:8px; }
				  .metric-value { color:#c8d8e8; font-size:22px; }
				  .user-list { font-size: 12px; line-height: 1.9; margin-top: 32px; max-width: 1200px; color: #8aa0b8; }
				  @media (max-width: 900px) { .metric-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); } }
				  @media (max-width: 700px) { .grid-2 { grid-template-columns: 1fr; } .card-header { align-items:flex-start; flex-direction:column; } .timeline-controls, .guild-chart-controls { justify-content:flex-start; } }
				</style>
				</head>
				<body>
				""");

		long activeUsers7  = allUsers.stream().filter(u -> u.getLastSeen() != null && u.getLastSeen().isAfter(cutoff7)).count();
		long activeUsers14 = allUsers.stream().filter(u -> u.getLastSeen() != null && u.getLastSeen().isAfter(cutoff14)).count();

		sb.append("<h1>WynnExtras DB</h1>");
		sb.append("<p class=\"subtitle\">Total users: ").append(allUsers.size())
				.append(" &nbsp;·&nbsp; Active (7d): ").append(activeUsers7)
				.append(" &nbsp;·&nbsp; Active (14d): ").append(activeUsers14)
				.append("</p>");
		sb.append("<div class=\"metric-grid\">");
		sb.append("<div class=\"metric\"><div class=\"metric-label\">New today UTC</div><div class=\"metric-value\">").append(newToday).append("</div></div>");
		sb.append("<div class=\"metric\"><div class=\"metric-label\">Returned today</div><div class=\"metric-value\">").append(returnedToday).append("</div></div>");
		sb.append("<div class=\"metric\"><div class=\"metric-label\">Inactive > 7d</div><div class=\"metric-value\">").append(inactive7).append("</div></div>");
		sb.append("<div class=\"metric\"><div class=\"metric-label\">Inactive > 14d</div><div class=\"metric-value\">").append(inactive14).append("</div></div>");
		sb.append("<div class=\"metric\"><div class=\"metric-label\">Inactive > 30d</div><div class=\"metric-value\">").append(inactive30).append("</div></div>");
		sb.append("<div class=\"metric\"><div class=\"metric-label\">Wynncraft daily usage</div><div class=\"metric-value\">")
				.append(latestUsageSnapshot == null ? "n/a" : formatNumber(latestUsageSnapshot.getUsagePercent(), 2) + "%")
				.append("</div></div>");
		sb.append("</div>");
		sb.append("<div class=\"grid\" style=\"max-width:1200px\">");

		// Chart 1
		sb.append("<div class=\"card\"><div class=\"card-title\">Cumulative unique users</div><canvas id=\"c1\" height=\"70\"></canvas></div>");
		sb.append("<div class=\"card\"><div class=\"card-title\">Cumulative unique users slope (new users/day)</div><canvas id=\"c1s\" height=\"70\"></canvas></div>");

		// Chart 2
		sb.append("<div class=\"card\"><div class=\"card-title\">New users per week</div><canvas id=\"c2\" height=\"70\"></canvas></div>");

		// Chart 3
		sb.append("<div class=\"card\"><div class=\"card-title\">Daily activity (last-seen) + 7-day rolling average</div><canvas id=\"c3\" height=\"70\"></canvas></div>");

		// Snapshot/activity charts
		sb.append("<div class=\"card\"><div class=\"card-title\">Active users snapshots (daily 01:00 UTC)</div><canvas id=\"c6\" height=\"80\"></canvas></div>");
		sb.append("<div class=\"card\"><div class=\"card-title\">WynnExtras usage of active Wynncraft players (snapshot day UTC)</div><canvas id=\"c6b\" height=\"80\"></canvas></div>");
		sb.append("<div class=\"grid grid-2\">");
		sb.append("<div class=\"card\"><div class=\"card-title\">Heartbeat volume per day (UTC)</div><canvas id=\"c7\" height=\"130\"></canvas></div>");
		sb.append("<div class=\"card\"><div class=\"card-title\">New / returned users per day (UTC)</div><canvas id=\"c8\" height=\"130\"></canvas></div>");
		sb.append("</div>");
		sb.append("""
				<div class="card">
				  <div class="card-header">
				    <div class="card-title">Version adoption timeline</div>
				    <div class="timeline-controls">
				      <button class="timeline-chip tchip-active" data-p="all" onclick="setVersionTimelinePeriod('all')">Alltime</button>
				      <button class="timeline-chip" data-p="active1d" onclick="setVersionTimelinePeriod('active1d')">Active 1D</button>
				      <button class="timeline-chip" data-p="active3d" onclick="setVersionTimelinePeriod('active3d')">Active 3D</button>
				      <button class="timeline-chip" data-p="active7d" onclick="setVersionTimelinePeriod('active7d')">Active 7D</button>
				      <button class="timeline-chip" data-p="active14d" onclick="setVersionTimelinePeriod('active14d')">Active 14D</button>
				    </div>
				  </div>
				  <canvas id="c9" height="90"></canvas>
				</div>
				""");
		sb.append("""
				<div class="card">
				  <div class="card-header">
				    <div id="guild-chart-title" class="card-title">Tracked guilds - active last 7 days</div>
				    <div class="guild-chart-controls">
				      <button id="guild-chart-mode" class="guild-chart-chip gchip-active" onclick="toggleGuildChartMode()">Mode: Total</button>
				      <button id="guild-chart-toggle-all" class="guild-chart-chip" onclick="toggleGuildDatasets()">Hide all</button>
				    </div>
				  </div>
				  <canvas id="c10" height="150"></canvas>
				</div>
				""");

		// Charts 4+5 side by side
		sb.append("<div class=\"grid grid-2\">");
		sb.append("<div class=\"card\"><div class=\"card-title\">Mod version distribution (all time)</div><canvas id=\"c4\"></canvas></div>");
		sb.append("<div class=\"card\"><div class=\"card-title\">Activity by hour of day (UTC)</div><canvas id=\"c5\" height=\"130\"></canvas></div>");
		sb.append("</div>");

		// Charts 4b+4c side by side
		sb.append("<div class=\"grid grid-2\">");
		sb.append("<div class=\"card\"><div class=\"card-title\">Mod version distribution — active last 7 days</div><canvas id=\"c4b\"></canvas></div>");
		sb.append("<div class=\"card\"><div class=\"card-title\">Mod version distribution — active last 14 days</div><canvas id=\"c4c\"></canvas></div>");
		sb.append("</div>");

		sb.append("</div>"); // grid

		sb.append("<script>\nconst opts = (extra={}) => ({ responsive:true, plugins:{ legend:{ labels:{ color:'#c8d8e8', font:{size:11} } } }, scales:{ x:{ ticks:{color:'#4a6080',maxTicksLimit:14}, grid:{color:'#1e2530'} }, y:{ beginAtZero:true, ticks:{color:'#4a6080'}, grid:{color:'#1e2530'} } }, ...extra });\n");
		sb.append("const doughnutOpts = { responsive:true, plugins:{ legend:{ position:'right', labels:{ color:'#c8d8e8', font:{size:11}, padding:12 } }, tooltip:{ callbacks:{ label: function(ctx){ const total=ctx.dataset.data.reduce((a,b)=>a+b,0); const pct=total>0?((ctx.raw/total)*100).toFixed(1):'0.0'; return ' '+ctx.label+': '+ctx.raw+' ('+pct+'%)'; } } } } };\n");

		// Chart 1 script
		sb.append("new Chart(document.getElementById('c1'),{ type:'line', data:{ labels:[").append(c1l)
				.append("], datasets:[{ label:'Total', data:[").append(c1d)
				.append("], borderColor:'#00c8ff', backgroundColor:'rgba(0,200,255,0.08)', borderWidth:2, pointRadius:1, fill:true, tension:0.3 }] }, options:opts() });\n");
		sb.append("new Chart(document.getElementById('c1s'),{ type:'bar', data:{ labels:[").append(c1l)
				.append("], datasets:[{ label:'New users/day', data:[").append(c1s)
				.append("], backgroundColor:'rgba(255,180,0,0.38)', borderColor:'#ffb400', borderWidth:1 }] }, options:opts() });\n");

		// Chart 2 script
		sb.append("new Chart(document.getElementById('c2'),{ type:'bar', data:{ labels:[").append(c2l)
				.append("], datasets:[{ label:'New users', data:[").append(c2d)
				.append("], backgroundColor:'rgba(0,229,160,0.5)', borderColor:'#00e5a0', borderWidth:1 }] }, options:opts() });\n");

		// Chart 3 script
		sb.append("new Chart(document.getElementById('c3'),{ type:'bar', data:{ labels:[").append(c3l)
				.append("], datasets:[ { label:'Last-seen per day', data:[").append(c3bar)
				.append("], backgroundColor:'rgba(0,200,255,0.25)', borderColor:'rgba(0,200,255,0.5)', borderWidth:1, order:2 }, { label:'7-day average', data:[").append(c3avg)
				.append("], type:'line', borderColor:'#ffb400', backgroundColor:'transparent', borderWidth:2, pointRadius:0, tension:0.3, order:1 } ] }, options:opts() });\n");

		// Chart 4 script — pie chart
		sb.append("new Chart(document.getElementById('c4'),{ type:'doughnut', data:{ labels:[").append(c4l)
				.append("], datasets:[{ data:[").append(c4d)
				.append("], backgroundColor:[").append(c4colors)
				.append("], borderColor:'#111419', borderWidth:2 }] }, options:doughnutOpts });\n");

		// Chart 4b script — active last 7 days
		sb.append("new Chart(document.getElementById('c4b'),{ type:'doughnut', data:{ labels:[").append(c4bl)
				.append("], datasets:[{ data:[").append(c4bd)
				.append("], backgroundColor:[").append(c4bColors)
				.append("], borderColor:'#111419', borderWidth:2 }] }, options:doughnutOpts });\n");

		// Chart 4c script — active last 14 days
		sb.append("new Chart(document.getElementById('c4c'),{ type:'doughnut', data:{ labels:[").append(c4cl)
				.append("], datasets:[{ data:[").append(c4cd)
				.append("], backgroundColor:[").append(c4cColors)
				.append("], borderColor:'#111419', borderWidth:2 }] }, options:doughnutOpts });\n");

		// Chart 5 script
		String[] hourLabels = new String[24];
		for (int i = 0; i < 24; i++) hourLabels[i] = '"' + String.format("%02d:00", i) + '"';
		sb.append("new Chart(document.getElementById('c5'),{ type:'bar', data:{ labels:[").append(String.join(",", hourLabels))
				.append("], datasets:[{ label:'Last-seen count', data:[").append(c5d)
				.append("], backgroundColor:'rgba(255,69,96,0.45)', borderColor:'#ff4560', borderWidth:1 }] }, options:opts() });\n");

		// Chart 6 script
		sb.append("new Chart(document.getElementById('c6'),{ type:'line', data:{ labels:[").append(c6l)
				.append("], datasets:[")
				.append("{ label:'1d', data:[").append(c6d1).append("], borderColor:'#00c8ff', backgroundColor:'transparent', borderWidth:2, pointRadius:1, tension:0.25 },")
				.append("{ label:'3d', data:[").append(c6d3).append("], borderColor:'#00e5a0', backgroundColor:'transparent', borderWidth:2, pointRadius:1, tension:0.25 },")
				.append("{ label:'5d', data:[").append(c6d5).append("], borderColor:'#ffb400', backgroundColor:'transparent', borderWidth:2, pointRadius:1, tension:0.25 },")
				.append("{ label:'7d', data:[").append(c6d7).append("], borderColor:'#ff4560', backgroundColor:'transparent', borderWidth:2, pointRadius:1, tension:0.25 },")
				.append("{ label:'10d', data:[").append(c6d10).append("], borderColor:'#b464ff', backgroundColor:'transparent', borderWidth:2, pointRadius:1, tension:0.25 },")
				.append("{ label:'14d', data:[").append(c6d14).append("], borderColor:'#64a0ff', backgroundColor:'transparent', borderWidth:2, pointRadius:1, tension:0.25 }")
				.append("] }, options:opts() });\n");

		sb.append("const c6bUsers=[").append(c6bUsers).append("]; const c6bTotal=[").append(c6bTotal).append("]; const c6bSamples=[").append(c6bSamples).append("];\n");
		sb.append("new Chart(document.getElementById('c6b'),{ type:'line', data:{ labels:[").append(c6bl)
				.append("], datasets:[{ label:'WynnExtras usage %', data:[").append(c6bpct)
				.append("], borderColor:'#00e5a0', backgroundColor:'rgba(0,229,160,0.08)', borderWidth:2, pointRadius:2, fill:true, tension:0.25 }] }, options:opts({ scales:{ x:{ ticks:{color:'#4a6080',maxTicksLimit:14}, grid:{color:'#1e2530'} }, y:{ beginAtZero:true, suggestedMax:10, ticks:{color:'#4a6080', callback:v=>v+'%'}, grid:{color:'#1e2530'} } }, plugins:{ legend:{ labels:{ color:'#c8d8e8', font:{size:11} } }, tooltip:{ callbacks:{ label:function(ctx){ const i=ctx.dataIndex; if (ctx.raw == null) return ' snapshot error'; return ' '+ctx.raw.toFixed(2)+'% ('+c6bUsers[i]+' / '+c6bTotal[i]+' players, '+c6bSamples[i]+' samples)'; } } } } }) });\n");

		// Chart 7 script
		sb.append("new Chart(document.getElementById('c7'),{ type:'bar', data:{ labels:[").append(c7l)
				.append("], datasets:[{ label:'Unique users', data:[").append(c7unique)
				.append("], backgroundColor:'rgba(0,200,255,0.35)', borderColor:'#00c8ff', borderWidth:1 }, { label:'Heartbeats', data:[")
				.append(c7heartbeats)
				.append("], type:'line', borderColor:'#00e5a0', backgroundColor:'transparent', borderWidth:2, pointRadius:0, tension:0.25 }] }, options:opts() });\n");

		// Chart 8 script
		sb.append("new Chart(document.getElementById('c8'),{ type:'bar', data:{ labels:[").append(c8l)
				.append("], datasets:[{ label:'New users', data:[").append(c8new)
				.append("], backgroundColor:'rgba(0,229,160,0.38)', borderColor:'#00e5a0', borderWidth:1 }, { label:'Returned after 7d gap', data:[")
				.append(c8returned)
				.append("], backgroundColor:'rgba(255,180,0,0.38)', borderColor:'#ffb400', borderWidth:1 }, { label:'D1 retained', data:[")
				.append(c8d1)
				.append("], type:'line', borderColor:'#ff4560', backgroundColor:'transparent', borderWidth:2, pointRadius:0, tension:0.25 }] }, options:opts() });\n");

		// Chart 9 script
		sb.append("const versionTimelineData = {")
				.append("all:{ labels:[").append(c9l).append("], datasets:[").append(c9AllDatasets).append("] },")
				.append("active1d:{ labels:[").append(c9l).append("], datasets:[").append(c9Active1dDatasets).append("] },")
				.append("active3d:{ labels:[").append(c9l).append("], datasets:[").append(c9Active3dDatasets).append("] },")
				.append("active7d:{ labels:[").append(c9l).append("], datasets:[").append(c9Active7dDatasets).append("] },")
				.append("active14d:{ labels:[").append(c9l).append("], datasets:[").append(c9Active14dDatasets).append("] }")
				.append("};\n")
				.append("const versionTimelineOptions = opts();\n")
				.append("versionTimelineOptions.scales.x.stacked = true;\n")
				.append("versionTimelineOptions.scales.y.stacked = true;\n")
				.append("const copyVersionTimelineData = p => ({ labels:[...versionTimelineData[p].labels], datasets:versionTimelineData[p].datasets.map(d => ({...d, data:[...d.data]})) });\n")
				.append("const versionTimelineChart = new Chart(document.getElementById('c9'),{ type:'bar', data:copyVersionTimelineData('all'), options:versionTimelineOptions });\n")
				.append("function setVersionTimelinePeriod(p) { versionTimelineChart.data = copyVersionTimelineData(p); document.querySelectorAll('.timeline-chip').forEach(c => c.classList.toggle('tchip-active', c.dataset.p === p)); versionTimelineChart.update(); }\n");

		// Chart 10 script
		sb.append("const guildTimelineData = {")
				.append("total:{ title:'Tracked guilds - active last 7 days', yLabel:'Active users', labels:[").append(c10l).append("], datasets:[").append(c10datasets).append("] },")
				.append("percent:{ title:'Tracked guilds - WynnExtras adoption %', yLabel:'Adoption %', labels:[").append(c10l).append("], datasets:[").append(c11datasets).append("] }")
				.append("};\n")
				.append("let guildChartMode = 'total';\n")
				.append("const guildDatasetVisible = {};\n")
				.append("function copyGuildTimelineData(mode) { return { labels:[...guildTimelineData[mode].labels], datasets:guildTimelineData[mode].datasets.map(d => ({...d, data:[...d.data], hidden:guildDatasetVisible[d.label] === false})) }; }\n")
				.append("const guildOptions = opts();\n")
				.append("guildOptions.plugins.legend.onClick = function(e, legendItem, legend) { const chart = legend.chart; const i = legendItem.datasetIndex; const dataset = chart.data.datasets[i]; const nextVisible = !chart.isDatasetVisible(i); dataset.hidden = !nextVisible; guildDatasetVisible[dataset.label] = nextVisible; chart.update(); updateGuildToggleButton(); };\n")
				.append("guildOptions.scales.y.title = { display:true, text:guildTimelineData.total.yLabel, color:'#4a6080' };\n")
				.append("const guildChart = new Chart(document.getElementById('c10'),{ type:'line', data:copyGuildTimelineData('total'), options:guildOptions });\n")
				.append("function captureGuildDatasetVisibility() { guildChart.data.datasets.forEach((d, i) => guildDatasetVisible[d.label] = guildChart.isDatasetVisible(i)); }\n")
				.append("function setGuildChartMode(mode) { captureGuildDatasetVisibility(); guildChartMode = mode; guildChart.data = copyGuildTimelineData(mode); document.getElementById('guild-chart-title').textContent = guildTimelineData[mode].title; document.getElementById('guild-chart-mode').textContent = mode === 'total' ? 'Mode: Total' : 'Mode: Percent'; guildChart.options.scales.y.title.text = guildTimelineData[mode].yLabel; if (mode === 'percent') { guildChart.options.scales.y.max = 100; } else { delete guildChart.options.scales.y.max; } guildChart.update(); updateGuildToggleButton(); }\n")
				.append("function toggleGuildChartMode() { setGuildChartMode(guildChartMode === 'total' ? 'percent' : 'total'); }\n")
				.append("function updateGuildToggleButton() { const anyVisible = guildChart.data.datasets.some((d, i) => guildChart.isDatasetVisible(i)); document.getElementById('guild-chart-toggle-all').textContent = anyVisible ? 'Hide all' : 'Show all'; }\n")
				.append("function toggleGuildDatasets() { const anyVisible = guildChart.data.datasets.some((d, i) => guildChart.isDatasetVisible(i)); const nextVisible = !anyVisible; guildChart.data.datasets.forEach(d => { d.hidden = !nextVisible; guildDatasetVisible[d.label] = nextVisible; }); guildChart.update(); updateGuildToggleButton(); }\n");

		sb.append("""
				// --- Guild Lookup ---
				const RANKS = ['OWNER','CHIEF','STRATEGIST','CAPTAIN','RECRUITER','RECRUIT'];
				let guildData = null;
				let guildPeriod = 'all';

				async function lookupGuild() {
				  const tag = document.getElementById('guild-tag').value.trim();
				  if (!tag) return;
				  const btn = document.getElementById('guild-btn');
				  const out = document.getElementById('guild-result');
				  btn.disabled = true;
				  out.innerHTML = '<span style="color:#4a6080">Loading...</span>';
				  try {
				    const r = await fetch('/admin/guild-lookup?tag=' + encodeURIComponent(tag));
				    const data = await r.json();
				    if (!r.ok) { out.innerHTML = '<span style="color:#ff4560">Error: ' + (data.error || r.status) + '</span>'; return; }
				    guildData = data;
				    renderGuild();
				  } catch(e) {
				    out.innerHTML = '<span style="color:#ff4560">Network error: ' + e.message + '</span>';
				  } finally {
				    btn.disabled = false;
				  }
				}

				function setGuildPeriod(p) {
				  guildPeriod = p;
				  document.querySelectorAll('.guild-chip').forEach(c => c.classList.toggle('gchip-active', c.dataset.p === p));
				  if (guildData) renderGuild();
				}

				function renderGuild() {
				  const now = Date.now();
				  const cutoff = guildPeriod === '7d' ? now - 7*86400*1000 : guildPeriod === '14d' ? now - 14*86400*1000 : 0;
				  const members = guildData.members;
				  const isActiveUser = m => m.isUser && (cutoff === 0 || (m.lastSeen && m.lastSeen > cutoff));
				  const activeCount = members.filter(isActiveUser).length;
				  const total = members.length;
				  const pct = total > 0 ? ((activeCount / total) * 100).toFixed(1) : '0.0';
				  const periodLabel = guildPeriod === 'all' ? 'all time' : 'last ' + guildPeriod;
				  let html = '<div class="guild-summary">' + activeCount + ' / ' + total + ' members use WynnExtras (' + pct + '%) — ' + periodLabel + '</div>';
				  for (const rank of RANKS) {
				    const group = members.filter(m => m.rank === rank);
				    if (!group.length) continue;
				    html += '<div class="guild-rank-header">' + rank + '</div>';
				    html += '<div class="guild-rank-group">';
				    for (const m of group) {
				      const active = isActiveUser(m);
				      const dateStr = m.lastSeen ? new Date(m.lastSeen).toLocaleDateString('en-US') : '—';
				      html += '<div class="guild-row' + (active ? ' guild-row-user' : '') + '">';
				      html += '<span class="guild-status">' + (active ? '✓' : (m.isUser ? '~' : '✗')) + '</span>';
				      html += '<span class="guild-name">' + m.name + '</span>';
				      html += '<span class="guild-ver">' + (m.modVersion || '—') + '</span>';
				      html += '<span class="guild-date">' + dateStr + '</span>';
				      html += '</div>';
				    }
				    html += '</div>';
				  }
				  document.getElementById('guild-result').innerHTML = html;
				}

				document.getElementById('guild-tag').addEventListener('keydown', e => { if (e.key === 'Enter') lookupGuild(); });
				""");
		sb.append("</script>\n");

		// Guild Lookup section (between charts and user list)
		sb.append("""
				<div style="margin-top:32px;max-width:1200px">
				  <div style="font-family:'Share Tech Mono',monospace;font-size:10px;letter-spacing:3px;text-transform:uppercase;color:#4a6080;margin-bottom:12px">Guild Lookup</div>
				  <div style="display:flex;gap:8px;align-items:center;margin-bottom:12px">
				    <input id="guild-tag" type="text" placeholder="Guild tag e.g. SEQ" style="font-family:'Share Tech Mono',monospace;font-size:13px;background:#111419;border:1px solid #2a3545;border-radius:6px;padding:8px 12px;color:#c8d8e8;outline:none;width:200px" />
				    <button id="guild-btn" onclick="lookupGuild()" style="font-family:'Share Tech Mono',monospace;font-size:12px;padding:8px 18px;border-radius:6px;border:1px solid #00c8ff;background:rgba(0,200,255,0.08);color:#00c8ff;cursor:pointer">Lookup</button>
				    <div style="display:flex;gap:6px;margin-left:8px">
				      <button class="guild-chip gchip-active" data-p="all" onclick="setGuildPeriod('all')" style="font-family:'Share Tech Mono',monospace;font-size:11px;padding:4px 10px;border-radius:4px;border:1px solid #2a3545;background:transparent;color:#4a6080;cursor:pointer">All time</button>
				      <button class="guild-chip" data-p="7d" onclick="setGuildPeriod('7d')" style="font-family:'Share Tech Mono',monospace;font-size:11px;padding:4px 10px;border-radius:4px;border:1px solid #2a3545;background:transparent;color:#4a6080;cursor:pointer">7 days</button>
				      <button class="guild-chip" data-p="14d" onclick="setGuildPeriod('14d')" style="font-family:'Share Tech Mono',monospace;font-size:11px;padding:4px 10px;border-radius:4px;border:1px solid #2a3545;background:transparent;color:#4a6080;cursor:pointer">14 days</button>
				    </div>
				  </div>
				  <style>
				    .guild-chip.gchip-active { border-color:#00c8ff !important; color:#00c8ff !important; background:rgba(0,200,255,0.08) !important; }
				    .guild-summary { font-family:'Share Tech Mono',monospace; font-size:12px; color:#00e5a0; margin-bottom:12px; }
				    .guild-rank-header { font-family:'Share Tech Mono',monospace; font-size:10px; letter-spacing:2px; color:#4a6080; padding:8px 0 4px; border-top:1px solid #1e2530; margin-top:4px; }
				    .guild-rank-group { display:flex; flex-direction:column; gap:2px; margin-bottom:4px; }
				    .guild-row { display:grid; grid-template-columns:24px 200px 100px 1fr; gap:8px; font-family:'Share Tech Mono',monospace; font-size:12px; color:#4a6080; padding:3px 0; }
				    .guild-row-user { color:#c8d8e8; }
				    .guild-status { font-weight:bold; }
				    .guild-row-user .guild-status { color:#00e5a0; }
				    .guild-row:not(.guild-row-user) .guild-status { color:#ff4560; }
				    .guild-row.guild-row-inactive .guild-status { color:#ffb400; }
				  </style>
				  <div id="guild-result"></div>
				</div>
				""");

		sb.append("<div class=\"user-list\">");
		printUsers(sb, sorted);
		sb.append("</div></body></html>");

		return ResponseEntity.ok()
				.header("Content-Type", "text/html; charset=UTF-8")
				.body(sb.toString());
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

	private static void appendCsv(StringBuilder sb, String value) {
		if (sb.length() > 0) sb.append(",");
		sb.append(value);
	}

	private static void appendTimelineDataset(StringBuilder sb, String label, StringBuilder data, String color) {
		if (sb.length() > 0) sb.append(",");
		sb.append("{ label:").append(jsQuote(label))
				.append(", data:[").append(data)
				.append("], backgroundColor:").append(jsQuote(color))
				.append(", borderColor:").append(jsQuote(color))
				.append(", borderWidth:0, barPercentage:1.0, categoryPercentage:1.0, stack:'versions' }");
	}

	private static String jsQuote(String value) {
		if (value == null) return "null";
		return "\"" + value
				.replace("\\", "\\\\")
				.replace("\"", "\\\"")
				.replace("\n", "\\n")
				.replace("\r", "\\r") + "\"";
	}

	private static String formatNumber(double value, int decimals) {
		return String.format(Locale.US, "%." + decimals + "f", value);
	}

	@GetMapping("/admin/panel")
	public ResponseEntity<String> adminPanel() {
		String html = """
				<!DOCTYPE html>
				<html lang="de">
				<head>
				<meta charset="UTF-8">
				<meta name="viewport" content="width=device-width, initial-scale=1.0">
				<title>WynnExtras Admin</title>
				<style>
				  @import url('https://fonts.googleapis.com/css2?family=Share+Tech+Mono&family=Outfit:wght@300;400;600;700&display=swap');
				
				  :root {
				    --bg: #0a0c0f;
				    --surface: #111419;
				    --border: #1e2530;
				    --border-bright: #2a3545;
				    --text: #c8d8e8;
				    --muted: #4a6080;
				    --accent: #00c8ff;
				    --accent-dim: rgba(0,200,255,0.08);
				    --danger: #ff4560;
				    --danger-dim: rgba(255,69,96,0.08);
				    --warn: #ffb400;
				    --warn-dim: rgba(255,180,0,0.08);
				    --success: #00e5a0;
				    --success-dim: rgba(0,229,160,0.1);
				    --mono: 'Share Tech Mono', monospace;
				    --sans: 'Outfit', sans-serif;
				  }
				
				  * { box-sizing: border-box; margin: 0; padding: 0; }
				
				  body {
				    background: var(--bg);
				    color: var(--text);
				    font-family: var(--sans);
				    min-height: 100vh;
				    padding: 0;
				  }
				
				  /* Grid background */
				  body::before {
				    content: '';
				    position: fixed;
				    inset: 0;
				    background-image:
				      linear-gradient(var(--border) 1px, transparent 1px),
				      linear-gradient(90deg, var(--border) 1px, transparent 1px);
				    background-size: 40px 40px;
				    opacity: 0.4;
				    pointer-events: none;
				    z-index: 0;
				  }
				
				  .layout {
				    position: relative;
				    z-index: 1;
				    max-width: 860px;
				    margin: 0 auto;
				    padding: 40px 24px 80px;
				  }
				
				  /* Header */
				  header {
				    display: flex;
				    align-items: center;
				    gap: 16px;
				    margin-bottom: 48px;
				    padding-bottom: 24px;
				    border-bottom: 1px solid var(--border-bright);
				  }
				
				  .logo-mark {
				    width: 40px;
				    height: 40px;
				    background: var(--accent-dim);
				    border: 1px solid var(--accent);
				    border-radius: 8px;
				    display: flex;
				    align-items: center;
				    justify-content: center;
				    color: var(--accent);
				    font-family: var(--mono);
				    font-size: 18px;
				    flex-shrink: 0;
				  }
				
				  header h1 {
				    font-family: var(--mono);
				    font-size: 20px;
				    color: var(--accent);
				    letter-spacing: 2px;
				    text-transform: uppercase;
				  }
				
				  header p {
				    font-size: 12px;
				    color: var(--muted);
				    font-family: var(--mono);
				    margin-top: 2px;
				  }
				
				  .header-right {
				    margin-left: auto;
				    display: flex;
				    align-items: center;
				    gap: 8px;
				    font-family: var(--mono);
				    font-size: 11px;
				    color: var(--muted);
				  }
				
				  .status-dot {
				    width: 7px;
				    height: 7px;
				    border-radius: 50%;
				    background: var(--success);
				    box-shadow: 0 0 6px var(--success);
				    animation: pulse 2s infinite;
				  }
				
				  @keyframes pulse {
				    0%, 100% { opacity: 1; }
				    50% { opacity: 0.4; }
				  }
				
				  /* Section */
				  .section {
				    margin-bottom: 32px;
				  }
				
				  .section-header {
				    display: flex;
				    align-items: center;
				    gap: 10px;
				    margin-bottom: 16px;
				  }
				
				  .section-label {
				    font-family: var(--mono);
				    font-size: 10px;
				    letter-spacing: 3px;
				    text-transform: uppercase;
				    color: var(--muted);
				  }
				
				  .section-line {
				    flex: 1;
				    height: 1px;
				    background: var(--border);
				  }
				
				  /* Card */
				  .card {
				    background: var(--surface);
				    border: 1px solid var(--border);
				    border-radius: 10px;
				    padding: 20px;
				    margin-bottom: 12px;
				    transition: border-color 0.2s;
				  }
				
				  .card:hover {
				    border-color: var(--border-bright);
				  }
				
				  .card-title {
				    font-size: 13px;
				    font-weight: 600;
				    color: var(--text);
				    margin-bottom: 4px;
				  }
				
				  .card-desc {
				    font-size: 12px;
				    color: var(--muted);
				    margin-bottom: 16px;
				    line-height: 1.5;
				  }
				
				  .card-actions {
				    display: flex;
				    flex-wrap: wrap;
				    gap: 8px;
				  }
				
				  /* Buttons */
				  .btn {
				    font-family: var(--mono);
				    font-size: 12px;
				    padding: 8px 16px;
				    border-radius: 6px;
				    border: 1px solid;
				    cursor: pointer;
				    transition: all 0.15s;
				    letter-spacing: 0.5px;
				    display: inline-flex;
				    align-items: center;
				    gap: 6px;
				  }
				
				  .btn:active { transform: scale(0.97); }
				  .btn:disabled { opacity: 0.4; cursor: not-allowed; transform: none; }
				
				  .btn-danger {
				    background: var(--danger-dim);
				    border-color: var(--danger);
				    color: var(--danger);
				  }
				  .btn-danger:hover:not(:disabled) {
				    background: rgba(255,69,96,0.18);
				    box-shadow: 0 0 12px rgba(255,69,96,0.2);
				  }
				
				  .btn-warn {
				    background: var(--warn-dim);
				    border-color: var(--warn);
				    color: var(--warn);
				  }
				  .btn-warn:hover:not(:disabled) {
				    background: rgba(255,180,0,0.18);
				    box-shadow: 0 0 12px rgba(255,180,0,0.2);
				  }
				
				  .btn-accent {
				    background: var(--accent-dim);
				    border-color: var(--accent);
				    color: var(--accent);
				  }
				  .btn-accent:hover:not(:disabled) {
				    background: rgba(0,200,255,0.15);
				    box-shadow: 0 0 12px rgba(0,200,255,0.2);
				  }
				
				  .btn-ghost {
				    background: transparent;
				    border-color: var(--border-bright);
				    color: var(--muted);
				  }
				  .btn-ghost:hover:not(:disabled) {
				    border-color: var(--text);
				    color: var(--text);
				  }
				
				  /* Spinner */
				  .spin {
				    display: inline-block;
				    width: 12px;
				    height: 12px;
				    border: 2px solid currentColor;
				    border-top-color: transparent;
				    border-radius: 50%;
				    animation: spin 0.6s linear infinite;
				  }
				  @keyframes spin { to { transform: rotate(360deg); } }
				
				  /* Toast log */
				  #log {
				    position: fixed;
				    bottom: 24px;
				    right: 24px;
				    z-index: 100;
				    display: flex;
				    flex-direction: column;
				    gap: 8px;
				    max-width: 380px;
				    pointer-events: none;
				  }
				
				  .toast {
				    font-family: var(--mono);
				    font-size: 12px;
				    padding: 10px 16px;
				    border-radius: 8px;
				    border: 1px solid;
				    backdrop-filter: blur(8px);
				    animation: slideIn 0.2s ease;
				    pointer-events: auto;
				  }
				  .toast.ok  { background: rgba(0,229,160,0.1);   border-color: var(--success); color: var(--success); }
				  .toast.err { background: rgba(255,69,96,0.1);   border-color: var(--danger);  color: var(--danger); }
				  .toast.inf { background: rgba(0,200,255,0.08);  border-color: var(--accent);  color: var(--accent); }
				
				  @keyframes slideIn {
				    from { opacity: 0; transform: translateX(20px); }
				    to   { opacity: 1; transform: translateX(0); }
				  }
				
				  /* Confirm modal */
				  #modal-overlay {
				    display: none;
				    position: fixed;
				    inset: 0;
				    background: rgba(0,0,0,0.75);
				    backdrop-filter: blur(4px);
				    z-index: 200;
				    align-items: center;
				    justify-content: center;
				  }
				  #modal-overlay.open { display: flex; }
				
				  #modal {
				    background: var(--surface);
				    border: 1px solid var(--border-bright);
				    border-radius: 12px;
				    padding: 28px;
				    max-width: 420px;
				    width: 90%;
				    font-family: var(--sans);
				  }
				
				  #modal h2 {
				    font-size: 16px;
				    font-weight: 600;
				    margin-bottom: 8px;
				    color: var(--danger);
				    font-family: var(--mono);
				    letter-spacing: 1px;
				  }
				
				  #modal p {
				    font-size: 13px;
				    color: var(--text);
				    margin-bottom: 24px;
				    line-height: 1.6;
				  }
				
				  #modal code {
				    font-family: var(--mono);
				    background: rgba(0,200,255,0.07);
				    border: 1px solid var(--border-bright);
				    border-radius: 4px;
				    padding: 1px 6px;
				    font-size: 12px;
				    color: var(--accent);
				  }
				
				  .modal-btns {
				    display: flex;
				    gap: 10px;
				    justify-content: flex-end;
				  }
				
				  /* Aspect input area */
				  .input-row {
				    display: flex;
				    gap: 8px;
				    align-items: center;
				  }
				
				  input[type="text"] {
				    font-family: var(--mono);
				    font-size: 12px;
				    background: var(--bg);
				    border: 1px solid var(--border-bright);
				    border-radius: 6px;
				    padding: 8px 12px;
				    color: var(--text);
				    flex: 1;
				    outline: none;
				    transition: border-color 0.15s;
				  }
				
				  input[type="text"]:focus {
				    border-color: var(--accent);
				  }
				
				  input[type="text"]::placeholder {
				    color: var(--muted);
				  }

				  .stale-preview {
				    margin-top: 14px;
				    padding: 12px;
				    border: 1px solid var(--border);
				    border-radius: 6px;
				    background: var(--bg);
				    font-family: var(--mono);
				    font-size: 12px;
				    color: var(--muted);
				  }

				  .stale-preview.empty {
				    color: var(--success);
				    border-color: rgba(0,229,160,0.35);
				    background: var(--success-dim);
				  }

				  .stale-summary {
				    color: var(--text);
				    margin-bottom: 10px;
				  }

				  .stale-list {
				    display: flex;
				    flex-direction: column;
				    gap: 6px;
				    max-height: 220px;
				    overflow: auto;
				  }

				  .stale-row {
				    display: flex;
				    justify-content: space-between;
				    gap: 12px;
				    padding: 6px 8px;
				    border: 1px solid var(--border);
				    border-radius: 4px;
				    color: var(--text);
				  }

				  .stale-count {
				    color: var(--warn);
				    white-space: nowrap;
				  }
				
				  /* Raid chips */
				  .chip-group {
				    display: flex;
				    flex-wrap: wrap;
				    gap: 6px;
				    margin-bottom: 12px;
				  }
				
				  .chip {
				    font-family: var(--mono);
				    font-size: 11px;
				    padding: 4px 10px;
				    border-radius: 4px;
				    border: 1px solid var(--border-bright);
				    background: transparent;
				    color: var(--muted);
				    cursor: pointer;
				    transition: all 0.15s;
				  }
				
				  .chip.active, .chip:hover {
				    border-color: var(--warn);
				    color: var(--warn);
				    background: var(--warn-dim);
				  }
				
				  .chip-run {
				    border-color: var(--border-bright);
				  }
				  .chip-run.active, .chip-run:hover {
				    border-color: var(--accent);
				    color: var(--accent);
				    background: var(--accent-dim);
				  }
				
				  /* Divider */
				  hr {
				    border: none;
				    border-top: 1px solid var(--border);
				    margin: 24px 0;
				  }
				</style>
				</head>
				<body>
				
				<div class="layout">
				  <header>
				    <div class="logo-mark">W</div>
				    <div>
				      <h1>WynnExtras Admin</h1>
				      <p>wynnextras.com // internal panel</p>
				    </div>
				    <div class="header-right">
				      <span class="status-dot"></span>
				      <span id="server-status">connected</span>
				    </div>
				  </header>
				
				  <!-- LOOT POOL: RAIDS -->
				  <div class="section">
				    <div class="section-header">
				      <span class="section-label">Raid Loot Pool</span>
				      <div class="section-line"></div>
				    </div>

				    <div class="card">
				      <div class="card-title">Wipe Raid Pool — einzelner Raid</div>
				      <div class="card-desc">
				        Löscht den approved pool + alle submissions für diesen Raid in der aktuellen Woche.
				        Nützlich wenn ein neuer Pool früher als normal erscheint.
				      </div>
				      <div class="chip-group" id="raid-chips">
				        <button class="chip" data-val="NOTG">NOTG</button>
				        <button class="chip" data-val="NOL">NOL</button>
				        <button class="chip" data-val="TCC">TCC</button>
				        <button class="chip" data-val="TNA">TNA</button>
				      </div>
				      <div class="card-actions">
				        <button class="btn btn-warn" onclick="wipeRaid()">
				          <span>⚡</span> Ausgewählte wipen
				        </button>
				      </div>
				    </div>
				
				    <div class="card">
				      <div class="card-title">Wipe Raid Pool — alle Raids</div>
				      <div class="card-desc">Löscht alle approved pools + submissions aller Raids der aktuellen Woche.</div>
				      <div class="card-actions">
				        <button class="btn btn-danger" onclick="confirm('Wipe ALLE Raid Loot Pools?', () => doWipe('/admin/loot-pool/raid', 'Alle Raid Pools gewiped'))">
				          ✕ Alle Raids wipen
				        </button>
				      </div>
				    </div>
				  </div>
				
				  <!-- LOOT POOL: LOOTRUNS -->
				  <div class="section">
				    <div class="section-header">
				      <span class="section-label">Lootrun Loot Pool</span>
				      <div class="section-line"></div>
				    </div>
				
				    <div class="card">
				      <div class="card-title">Wipe Lootrun Pool — einzelne Zone</div>
				      <div class="card-desc">Löscht den approved pool + alle submissions für diese Lootrun-Zone.</div>
				      <div class="chip-group" id="lootrun-chips">
				        <button class="chip chip-run" data-val="SE">SE</button>
				        <button class="chip chip-run" data-val="SI">SI</button>
				        <button class="chip chip-run" data-val="MH">MH</button>
				        <button class="chip chip-run" data-val="CORK">CORK</button>
				        <button class="chip chip-run" data-val="COTL">COTL</button>
				      </div>
				      <div class="card-actions">
				        <button class="btn btn-accent" onclick="wipeLootrun()">
				          <span>⚡</span> Ausgewählte wipen
				        </button>
				      </div>
				    </div>
				
				    <div class="card">
				      <div class="card-title">Wipe Lootrun Pool — alle Zonen</div>
				      <div class="card-desc">Löscht alle Lootrun approved pools + submissions.</div>
				      <div class="card-actions">
				        <button class="btn btn-danger" onclick="confirm('Wipe ALLE Lootrun Loot Pools?', () => doWipe('/admin/loot-pool/lootrun', 'Alle Lootrun Pools gewiped'))">
				          ✕ Alle Lootruns wipen
				        </button>
				      </div>
				    </div>
				  </div>
				
				  <!-- ASPECTS -->
				  <div class="section">
				    <div class="section-header">
				      <span class="section-label">Aspects</span>
				      <div class="section-line"></div>
				    </div>
				
				    <div class="card">
				      <div class="card-title">Stale Aspects aus Wynn API wipen</div>
				      <div class="card-desc">
				        Holt die aktuellen Aspect-Namen aus Wynncraft und zeigt alle DB-Namen, die dort nicht mehr existieren. Gelöscht wird erst nach Preview und Bestätigung.
				      </div>
				      <div class="card-actions">
				        <button class="btn btn-accent" id="stale-preview-btn" onclick="previewStaleAspects()">Preview laden</button>
				        <button class="btn btn-danger" id="stale-delete-btn" onclick="deleteStaleAspects()" disabled>Stale Aspects löschen</button>
				      </div>
				      <div id="stale-aspect-preview" class="stale-preview">
				        Noch keine Preview geladen.
				      </div>
				    </div>
				
				    <div class="card">
				      <div class="card-title">Umbenannte Aspects wipen (bekannte)</div>
				      <div class="card-desc">
				        Diese Aspects wurden in Wynn umbenannt — die alten Namen werden bei allen Usern gelöscht.
				      </div>
				      <div class="card-actions" id="known-aspect-btns">
				        <button class="btn btn-warn" onclick="wipeAspect('Aspect of Upkeep Charges')">
				          Aspect of Upkeep Charges
				        </button>
				        <button class="btn btn-warn" onclick="wipeAspect('Aspect of Flickering Transmission')">
				          Aspect of Flickering Transmission
				        </button>
				        <button class="btn btn-warn" onclick="wipeAspect(`Riftwalker's Embodiment of Chronal Control`)">
				          Riftwalker's Embodiment of Chronal Control
				        </button>
				      </div>
				    </div>
				
				    <div class="card">
				      <div class="card-title">Custom Aspect-Name wipen</div>
				      <div class="card-desc">
				        Beliebigen Aspect-Namen bei allen Usern löschen. Exakt so eingeben wie er in der DB steht.
				      </div>
				      <div class="input-row">
				        <input type="text" id="custom-aspect" placeholder="z.B. Aspect of Something Old" />
				        <button class="btn btn-danger" onclick="wipeCustomAspect()">✕ Wipen</button>
				      </div>
				    </div>
				
				    <div class="card">
				      <div class="card-title">Wipe alle Aspects eines Spielers</div>
				      <div class="card-desc">
				        UUID des Spielers (ohne Bindestriche, 32 Zeichen hex).
				      </div>
				      <div class="input-row">
				        <input type="text" id="player-uuid" placeholder="z.B. 069a79f444e94726a5befca90e38aaf5" maxlength="36" />
				        <button class="btn btn-danger" onclick="wipePlayerAspects()">✕ Wipen</button>
				      </div>
				    </div>
				  </div>
				
				  <!-- MISC -->
				  <div class="section">
				    <div class="section-header">
				      <span class="section-label">Sonstiges</span>
				      <div class="section-line"></div>
				    </div>
				    <div class="card">
				      <div class="card-title">Verified Users neu laden</div>
				      <div class="card-desc">Lädt die Verified-User-Liste neu aus der Datei (VERIFIED_USERS.md).</div>
				      <div class="card-actions">
				        <button class="btn btn-accent" onclick="reloadVerified()">↻ Reload</button>
				      </div>
				    </div>
				    <div class="card">
				      <div class="card-title">Wynncraft Usage Snapshot auslösen</div>
				      <div class="card-desc">
				        Holt sofort die aktuelle Wynncraft-Online-Liste und berechnet den Usage-Snapshot für den aktuellen UTC-Tag neu.
				      </div>
				      <div class="card-actions">
				        <button class="btn btn-accent" id="usage-snapshot-btn" onclick="captureUsageSnapshot()">Snapshot auslösen</button>
				      </div>
				    </div>
				  </div>
				</div>
				
				<!-- Confirm Modal -->
				<div id="modal-overlay">
				  <div id="modal">
				    <h2>⚠ BESTÄTIGEN</h2>
				    <p id="modal-text"></p>
				    <div class="modal-btns">
				      <button class="btn btn-ghost" onclick="closeModal()">Abbrechen</button>
				      <button class="btn btn-danger" id="modal-confirm-btn" onclick="runConfirmed()">Bestätigen</button>
				    </div>
				  </div>
				</div>
				
				<!-- Toast log -->
				<div id="log"></div>
				
				<script>
				  // --- State ---
				  let selectedRaid = null;
				  let selectedLootrun = null;
				  let pendingAction = null;
				  let stalePreview = null;
				
				  // --- Chip selection ---
				  document.querySelectorAll('#raid-chips .chip').forEach(c => {
				    c.addEventListener('click', () => {
				      document.querySelectorAll('#raid-chips .chip').forEach(x => x.classList.remove('active'));
				      if (selectedRaid === c.dataset.val) {
				        selectedRaid = null;
				      } else {
				        c.classList.add('active');
				        selectedRaid = c.dataset.val;
				      }
				    });
				  });
				
				  document.querySelectorAll('#lootrun-chips .chip').forEach(c => {
				    c.addEventListener('click', () => {
				      document.querySelectorAll('#lootrun-chips .chip').forEach(x => x.classList.remove('active'));
				      if (selectedLootrun === c.dataset.val) {
				        selectedLootrun = null;
				      } else {
				        c.classList.add('active');
				        selectedLootrun = c.dataset.val;
				      }
				    });
				  });
				
				  // --- Confirm modal ---
				  function confirm(msg, action) {
				    document.getElementById('modal-text').innerHTML = msg;
				    pendingAction = action;
				    document.getElementById('modal-overlay').classList.add('open');
				  }
				
				  function closeModal() {
				    document.getElementById('modal-overlay').classList.remove('open');
				    pendingAction = null;
				  }
				
				  function runConfirmed() {
				    const action = pendingAction;
				    closeModal();
				    if (action) action();
				  }
				
				  // Close on overlay click
				  document.getElementById('modal-overlay').addEventListener('click', e => {
				    if (e.target === document.getElementById('modal-overlay')) closeModal();
				  });
				
				  // --- Toast ---
				  function toast(msg, type = 'inf') {
				    const el = document.createElement('div');
				    el.className = `toast ${type}`;
				    el.textContent = msg;
				    document.getElementById('log').appendChild(el);
				    setTimeout(() => el.remove(), 4000);
				  }
				
				  // --- API helper ---
				  async function api(method, path, params = {}) {
				    const url = new URL(path, window.location.origin);
				    if (method === 'DELETE' || method === 'GET') {
				      Object.entries(params).forEach(([k, v]) => url.searchParams.set(k, v));
				    }
				    const res = await fetch(url.toString(), {
				      method,
				      headers: { 'Content-Type': 'application/json' },
				      body: method === 'POST' ? JSON.stringify(params) : undefined,
				    });
				    const text = await res.text();
				    return { ok: res.ok, status: res.status, text };
				  }
				
				  async function doWipe(path, successMsg, params = {}) {
				    toast('Wipe läuft...', 'inf');
				    try {
				      const r = await api('DELETE', path, params);
				      if (r.ok) {
				        toast('✓ ' + successMsg, 'ok');
				      } else {
				        toast('✗ Fehler ' + r.status + ': ' + r.text, 'err');
				      }
				    } catch (e) {
				      toast('✗ Netzwerkfehler: ' + e.message, 'err');
				    }
				  }
				
				  // --- Actions ---
				  function wipeRaid() {
				    if (!selectedRaid) { toast('Bitte erst einen Raid auswählen', 'err'); return; }
				    confirm(
				      `Loot Pool für <code>${selectedRaid}</code> wirklich wipen?<br><br>Approved pool + alle Submissions werden gelöscht.`,
				      () => doWipe('/admin/loot-pool/raid', `Raid Pool gewiped: ${selectedRaid}`, { raidType: selectedRaid })
				    );
				  }
				
				  function wipeLootrun() {
				    if (!selectedLootrun) { toast('Bitte erst eine Zone auswählen', 'err'); return; }
				    confirm(
				      `Loot Pool für Lootrun <code>${selectedLootrun}</code> wirklich wipen?`,
				      () => doWipe('/admin/loot-pool/lootrun', `Lootrun Pool gewiped: ${selectedLootrun}`, { lootrunType: selectedLootrun })
				    );
				  }
				
				  function wipeAspect(name) {
				    confirm(
				      `Aspect <code>${name}</code> bei <strong>allen Usern</strong> löschen?`,
				      () => doWipe('/admin/aspects', `Aspect gewiped: ${name}`, { aspectName: name })
				    );
				  }

				  async function previewStaleAspects() {
				    const previewBtn = document.getElementById('stale-preview-btn');
				    const deleteBtn = document.getElementById('stale-delete-btn');
				    previewBtn.disabled = true;
				    previewBtn.innerHTML = '<span class="spin"></span> Lade...';
				    deleteBtn.disabled = true;
				    stalePreview = null;
				    toast('Preview läuft...', 'inf');

				    try {
				      const r = await api('GET', '/admin/aspects/stale-preview');
				      if (!r.ok) {
				        toast('✗ Fehler ' + r.status + ': ' + r.text, 'err');
				        return;
				      }

				      const data = JSON.parse(r.text);
				      stalePreview = data;
				      renderStaleAspectPreview(data);
				      deleteBtn.disabled = data.staleAspectCount === 0;
				      toast(`✓ Preview: ${data.staleAspectCount} alte Namen, ${data.totalStaleRows} Einträge`, 'ok');
				    } catch (e) {
				      toast('✗ ' + e.message, 'err');
				    } finally {
				      previewBtn.disabled = false;
				      previewBtn.textContent = 'Preview laden';
				    }
				  }

				  function deleteStaleAspects() {
				    if (!stalePreview || stalePreview.staleAspectCount === 0) {
				      toast('Erst Preview mit alten Aspects laden', 'err');
				      return;
				    }

				    confirm(
				      `${stalePreview.staleAspectCount} alte Aspect-Namen mit insgesamt <strong>${stalePreview.totalStaleRows}</strong> DB-Einträgen löschen?<br><br>Die aktuelle Wynncraft-Liste wird vor dem Löschen erneut geladen.`,
				      async () => {
				        const deleteBtn = document.getElementById('stale-delete-btn');
				        deleteBtn.disabled = true;
				        toast('Stale-Wipe läuft...', 'inf');

				        try {
				          const r = await api('DELETE', '/admin/aspects/stale', { confirm: 'true' });
				          if (r.ok) {
				            const data = JSON.parse(r.text);
				            stalePreview = {
				              ...data,
				              staleAspectCount: 0,
				              totalStaleRows: 0,
				              staleAspects: []
				            };
				            renderStaleAspectPreview(stalePreview);
				            toast(`✓ Stale Aspects gewiped: ${data.deleted} Einträge`, 'ok');
				          } else {
				            toast('✗ Fehler ' + r.status + ': ' + r.text, 'err');
				          }
				        } catch (e) {
				          toast('✗ ' + e.message, 'err');
				        }
				      }
				    );
				  }

				  function renderStaleAspectPreview(data) {
				    const el = document.getElementById('stale-aspect-preview');
				    const rows = data.staleAspects || [];
				    el.classList.toggle('empty', rows.length === 0);

				    if (rows.length === 0) {
				      el.innerHTML = `Keine stale Aspects gefunden. Aktuelle Wynncraft-Namen: ${data.currentAspectCount || 0}.`;
				      return;
				    }

				    const visibleRows = rows.slice(0, 100);
				    const overflow = rows.length > visibleRows.length
				      ? `<div class="stale-row"><span>... ${rows.length - visibleRows.length} weitere</span><span class="stale-count"></span></div>`
				      : '';

				    el.innerHTML = `
				      <div class="stale-summary">${data.staleAspectCount} alte Namen, ${data.totalStaleRows} DB-Einträge. Aktuelle Wynncraft-Namen: ${data.currentAspectCount}.</div>
				      <div class="stale-list">
				        ${visibleRows.map(row => `
				          <div class="stale-row">
				            <span>${escapeHtml(row.aspectName)}</span>
				            <span class="stale-count">${row.entryCount}x</span>
				          </div>
				        `).join('')}
				        ${overflow}
				      </div>
				    `;
				  }

				  function escapeHtml(value) {
				    return String(value)
				      .replace(/&/g, '&amp;')
				      .replace(/</g, '&lt;')
				      .replace(/>/g, '&gt;')
				      .replace(/"/g, '&quot;')
				      .replace(/'/g, '&#039;');
				  }
				
				  function wipeCustomAspect() {
				    const name = document.getElementById('custom-aspect').value.trim();
				    if (!name) { toast('Aspect-Name eingeben', 'err'); return; }
				    confirm(
				      `Aspect <code>${name}</code> bei <strong>allen Usern</strong> löschen?`,
				      async () => {
				        await doWipe('/admin/aspects', `Aspect gewiped: ${name}`, { aspectName: name });
				        document.getElementById('custom-aspect').value = '';
				      }
				    );
				  }
				
				  function wipePlayerAspects() {
				    let uuid = document.getElementById('player-uuid').value.trim().replace(/-/g, '').toLowerCase();
				    if (!/^[0-9a-f]{32}$/.test(uuid)) { toast('Ungültige UUID (32 hex Zeichen erwartet)', 'err'); return; }
				    confirm(
				      `Alle Aspects für UUID <code>${uuid}</code> löschen?`,
				      async () => {
				        toast('Wipe läuft...', 'inf');
				        try {
				          const r = await api('DELETE', '/admin/aspects/player', { playerUuid: uuid });
				          if (r.ok) {
				            toast('✓ Player Aspects gewiped', 'ok');
				            document.getElementById('player-uuid').value = '';
				          } else {
				            toast('✗ Fehler ' + r.status + ': ' + r.text, 'err');
				          }
				        } catch(e) {
				          toast('✗ ' + e.message, 'err');
				        }
				      }
				    );
				  }
				
				  async function reloadVerified() {
				    toast('Reload...', 'inf');
				    try {
				      const r = await api('POST', '/admin/reload-verified-users');
				      if (r.ok) {
				        const data = JSON.parse(r.text);
				        toast(`✓ Verified users geladen: ${data.verifiedUserCount}`, 'ok');
				      } else {
				        toast('✗ Fehler: ' + r.text, 'err');
				      }
					    } catch(e) {
					      toast('✗ ' + e.message, 'err');
					    }
					  }

					  async function captureUsageSnapshot() {
					    const btn = document.getElementById('usage-snapshot-btn');
					    btn.disabled = true;
					    btn.innerHTML = '<span class="spin"></span> Läuft...';
					    toast('Wynncraft Usage Snapshot läuft...', 'inf');

					    try {
					      const r = await api('POST', '/admin/wynncraft-usage/snapshot');
					      if (r.ok) {
					        const data = JSON.parse(r.text);
					        toast(`✓ Snapshot ${data.snapshotDate}: ${data.usagePercent.toFixed(2)}% (${data.wynnExtrasUsers}/${data.uniquePlayers})`, 'ok');
					      } else {
					        toast('✗ Fehler ' + r.status + ': ' + r.text, 'err');
					      }
					    } catch(e) {
					      toast('✗ ' + e.message, 'err');
					    } finally {
					      btn.disabled = false;
					      btn.textContent = 'Snapshot auslösen';
					    }
					  }
					</script>
				</body>
				</html>
        """;
		return ResponseEntity.ok()
				.header("Content-Type", "text/html; charset=UTF-8")
				.body(html);
	}
}
