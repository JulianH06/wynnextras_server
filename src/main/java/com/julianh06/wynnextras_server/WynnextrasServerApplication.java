package com.julianh06.wynnextras_server;

import com.julianh06.wynnextras_server.entity.WynnExtrasUser;
import com.julianh06.wynnextras_server.repository.WynnExtrasUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
		StringBuilder c1l = new StringBuilder(), c1d = new StringBuilder();
		int cum = 0;
		for (Map.Entry<String, Integer> e : createdPerDay.entrySet()) {
			cum += e.getValue();
			if (c1l.length() > 0) { c1l.append(","); c1d.append(","); }
			c1l.append('"').append(e.getKey()).append('"');
			c1d.append(cum);
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

		// ── HTML ──────────────────────────────────────────────────────────
		StringBuilder sb = new StringBuilder();
		sb.append("""
				<!DOCTYPE html>
				<html lang="de">
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
				  .user-list { font-size: 12px; line-height: 1.9; margin-top: 32px; max-width: 1200px; color: #8aa0b8; }
				  @media (max-width: 700px) { .grid-2 { grid-template-columns: 1fr; } }
				</style>
				</head>
				<body>
				""");

		sb.append("<h1>WynnExtras DB</h1>");
		sb.append("<p class=\"subtitle\">Total users: ").append(allUsers.size()).append("</p>");
		sb.append("<div class=\"grid\" style=\"max-width:1200px\">");

		// Chart 1
		sb.append("<div class=\"card\"><div class=\"card-title\">Kumulierte unique user</div><canvas id=\"c1\" height=\"70\"></canvas></div>");

		// Chart 2
		sb.append("<div class=\"card\"><div class=\"card-title\">Neue user pro woche</div><canvas id=\"c2\" height=\"70\"></canvas></div>");

		// Chart 3
		sb.append("<div class=\"card\"><div class=\"card-title\">Tägliche aktivität (last-seen) + 7-Tage-Schnitt</div><canvas id=\"c3\" height=\"70\"></canvas></div>");

		// Charts 4+5 side by side
		sb.append("<div class=\"grid grid-2\">");
		sb.append("<div class=\"card\"><div class=\"card-title\">Mod-versionsverteilung</div><canvas id=\"c4\" height=\"130\"></canvas></div>");
		sb.append("<div class=\"card\"><div class=\"card-title\">Aktivität nach Uhrzeit (UTC)</div><canvas id=\"c5\" height=\"130\"></canvas></div>");
		sb.append("</div>");

		sb.append("</div>"); // grid

		sb.append("<script>\nconst opts = (extra={}) => ({ responsive:true, plugins:{ legend:{ labels:{ color:'#c8d8e8', font:{size:11} } } }, scales:{ x:{ ticks:{color:'#4a6080',maxTicksLimit:14}, grid:{color:'#1e2530'} }, y:{ beginAtZero:true, ticks:{color:'#4a6080'}, grid:{color:'#1e2530'} } }, ...extra });\n");

		// Chart 1 script
		sb.append("new Chart(document.getElementById('c1'),{ type:'line', data:{ labels:[").append(c1l)
				.append("], datasets:[{ label:'Gesamt', data:[").append(c1d)
				.append("], borderColor:'#00c8ff', backgroundColor:'rgba(0,200,255,0.08)', borderWidth:2, pointRadius:1, fill:true, tension:0.3 }] }, options:opts() });\n");

		// Chart 2 script
		sb.append("new Chart(document.getElementById('c2'),{ type:'bar', data:{ labels:[").append(c2l)
				.append("], datasets:[{ label:'Neue user', data:[").append(c2d)
				.append("], backgroundColor:'rgba(0,229,160,0.5)', borderColor:'#00e5a0', borderWidth:1 }] }, options:opts() });\n");

		// Chart 3 script
		sb.append("new Chart(document.getElementById('c3'),{ type:'bar', data:{ labels:[").append(c3l)
				.append("], datasets:[ { label:'Last-seen pro Tag', data:[").append(c3bar)
				.append("], backgroundColor:'rgba(0,200,255,0.25)', borderColor:'rgba(0,200,255,0.5)', borderWidth:1, order:2 }, { label:'7-Tage-Schnitt', data:[").append(c3avg)
				.append("], type:'line', borderColor:'#ffb400', backgroundColor:'transparent', borderWidth:2, pointRadius:0, tension:0.3, order:1 } ] }, options:opts() });\n");

		// Chart 4 script
		sb.append("new Chart(document.getElementById('c4'),{ type:'bar', data:{ labels:[").append(c4l)
				.append("], datasets:[{ label:'User', data:[").append(c4d)
				.append("], backgroundColor:'rgba(255,180,0,0.45)', borderColor:'#ffb400', borderWidth:1 }] }, options:opts({ indexAxis:'y', scales:{ x:{ beginAtZero:true, ticks:{color:'#4a6080'}, grid:{color:'#1e2530'} }, y:{ ticks:{color:'#4a6080'}, grid:{color:'#1e2530'} } } }) });\n");

		// Chart 5 script
		String[] hourLabels = new String[24];
		for (int i = 0; i < 24; i++) hourLabels[i] = '"' + String.format("%02d:00", i) + '"';
		sb.append("new Chart(document.getElementById('c5'),{ type:'bar', data:{ labels:[").append(String.join(",", hourLabels))
				.append("], datasets:[{ label:'Last-seen count', data:[").append(c5d)
				.append("], backgroundColor:'rgba(255,69,96,0.45)', borderColor:'#ff4560', borderWidth:1 }] }, options:opts() });\n");

		sb.append("</script>\n");

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
				    closeModal();
				    if (pendingAction) pendingAction();
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
				</script>
				</body>
				</html>
        """;
		return ResponseEntity.ok()
				.header("Content-Type", "text/html; charset=UTF-8")
				.body(html);
	}
}