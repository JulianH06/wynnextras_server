# WynnExtras Backend Development Guide

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Database Schema](#database-schema)
4. [API Endpoints](#api-endpoints)
5. [Authentication Flow](#authentication-flow)
6. [Adding New Features](#adding-new-features)
7. [Code Structure](#code-structure)
8. [Common Patterns](#common-patterns)
9. [Testing](#testing)
10. [Deployment](#deployment)
11. [Future Feature Ideas](#future-feature-ideas)

---

## Overview

WynnExtras backend is a Spring Boot 3.5.7 REST API that provides:
- **Personal aspect tracking** - Players sync their aspect collections
- **Raid loot pool crowdsourcing** - Community determines weekly raid loot
- **Gambit crowdsourcing** - Community determines daily gambit locations
- **Leaderboards** - Top players by maxed aspects
- **Player discovery** - Browse all players with aspects
- **Secure authentication** - Mojang sessionserver verification

**Tech Stack:**
- Spring Boot 3.5.7
- Java 21
- PostgreSQL 16
- JPA/Hibernate
- Maven

---

## Architecture

### Package Structure
```
com.julianh06.wynnextras_server/
├── controller/     # REST endpoints (HTTP request handlers)
│   ├── PersonalAspectController.java
│   ├── LootPoolController.java
│   └── GambitController.java
├── service/        # Business logic
│   ├── MojangAuthService.java
│   ├── LootPoolService.java
│   └── GambitService.java
├── repository/     # Database access (JPA repositories)
│   ├── PersonalAspectRepository.java
│   ├── RaidLootPoolSubmissionRepository.java
│   ├── RaidLootPoolApprovedRepository.java
│   ├── GambitSubmissionRepository.java
│   ├── GambitApprovedRepository.java
│   └── VerifiedUserRepository.java
├── entity/         # Database entities (JPA models)
│   ├── PersonalAspect.java
│   ├── RaidLootPoolSubmission.java
│   ├── RaidLootPoolApproved.java
│   ├── GambitSubmission.java
│   ├── GambitApproved.java
│   └── VerifiedUser.java
├── dto/            # Data transfer objects (API request/response)
│   ├── PersonalAspectDto.java
│   ├── LootPoolSubmissionDto.java
│   └── GambitSubmissionDto.java
└── util/           # Utility classes
    └── TimeUtils.java
```

### Design Pattern: MVC + Service Layer
- **Controllers** - Handle HTTP requests, validate input, return responses
- **Services** - Business logic, authentication, consensus algorithms
- **Repositories** - Database queries using Spring Data JPA
- **Entities** - Database table models with JPA annotations
- **DTOs** - Clean API contracts, separate from database entities

---

## Database Schema

### Personal Aspects (`personal_aspect`)
Stores each player's aspect collection.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGSERIAL | Primary key |
| `player_uuid` | VARCHAR(32) | Player UUID (no dashes, lowercase) |
| `player_name` | VARCHAR(16) | Player username |
| `aspect_name` | VARCHAR(255) | Aspect name (e.g., "Radiant Gem") |
| `rarity` | VARCHAR(50) | Rarity tier |
| `amount` | INT | Count (0-3, where 3 = maxed) |
| `mod_version` | VARCHAR(50) | WynnExtras mod version |
| `updated_at` | TIMESTAMP | Last sync time |

**Indexes:**
- `(player_uuid, aspect_name)` - Fast lookups per player
- `player_uuid` - List player's aspects
- `amount` - Leaderboard queries (maxed aspects)

### Raid Loot Pool Submissions (`raid_loot_pool_submission`)
Stores individual player submissions for raid loot pools.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGSERIAL | Primary key |
| `raid_type` | VARCHAR(50) | Raid name (e.g., "The Nameless Anomaly") |
| `username` | VARCHAR(16) | Submitter username |
| `aspects_json` | TEXT | JSON array of aspects (sorted by name) |
| `week_identifier` | VARCHAR(10) | Week ID (e.g., "2026-W04") |
| `submitted_at` | TIMESTAMP | Submission time |

**Indexes:**
- `(raid_type, week_identifier)` - Query submissions for current week

### Raid Loot Pool Approved (`raid_loot_pool_approved`)
Stores approved/locked raid loot pools.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGSERIAL | Primary key |
| `raid_type` | VARCHAR(50) | Raid name |
| `aspects_json` | TEXT | Approved aspects JSON |
| `week_identifier` | VARCHAR(10) | Week ID |
| `submission_count` | INT | Number of matching submissions |
| `locked` | BOOLEAN | Locked (10+ submissions) |

**Indexes:**
- `(raid_type, week_identifier)` - Unique constraint, fetch approved pool

### Gambit Submissions (`gambit_submission`)
Stores individual gambit location submissions.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGSERIAL | Primary key |
| `gambits_json` | TEXT | JSON array of gambits (sorted by name) |
| `username` | VARCHAR(16) | Submitter username |
| `day_identifier` | VARCHAR(10) | Day ID (e.g., "2026-01-27") |
| `submitted_at` | TIMESTAMP | Submission time |

**Indexes:**
- `day_identifier` - Query submissions for today

### Gambit Approved (`gambit_approved`)
Stores approved/locked daily gambits.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGSERIAL | Primary key |
| `gambits_json` | TEXT | Approved gambits JSON |
| `day_identifier` | VARCHAR(10) | Day ID |
| `locked` | BOOLEAN | Locked (2+ submissions) |

**Indexes:**
- `day_identifier` - Unique constraint, fetch approved gambits

### Verified Users (`verified_user`)
Whitelist of trusted players who get instant approval.

| Column | Type | Description |
|--------|------|-------------|
| `id` | BIGSERIAL | Primary key |
| `username` | VARCHAR(16) | Trusted username |

---

## API Endpoints

### Personal Aspects

#### `POST /user` - Upload Aspects
Upload player's aspect collection (authenticated).

**Headers:**
- `Username: string` - Minecraft username
- `Server-ID: string` - Mojang authentication token

**Request:**
```json
{
  "playerName": "Notch",
  "modVersion": "0.12.1-TEST1",
  "aspects": [
    {
      "name": "Radiant Gem",
      "rarity": "Rare",
      "amount": 3
    }
  ]
}
```

**Response (Success):**
```json
{
  "status": "success",
  "message": "Aspects uploaded successfully"
}
```

**Response (Auth Failed):**
```json
{
  "status": "error",
  "message": "Authentication failed - invalid session"
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Missing headers or invalid data
- `401 Unauthorized` - Authentication failed

---

#### `GET /user?playerUuid=xxx` - Get Player's Aspects
Retrieve a specific player's aspects (no auth required).

**Query Parameters:**
- `playerUuid: string` - Player UUID (with or without dashes)

**Response:**
```json
{
  "playerUuid": "069a79f444e94726a5befca90e38aaf5",
  "playerName": "Notch",
  "modVersion": "0.12.1-TEST1",
  "updatedAt": 1738072800000,
  "aspects": [
    {
      "name": "Radiant Gem",
      "rarity": "Rare",
      "amount": 3
    }
  ]
}
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Invalid UUID format
- `404 Not Found` - No aspects found for player

---

#### `GET /user/list` - List All Players
Get all players who have aspects in the database (no auth required).

**Response:**
```json
[
  {
    "playerUuid": "069a79f444e94726a5befca90e38aaf5",
    "playerName": "Notch",
    "modVersion": "0.12.1-TEST1",
    "lastUpdated": 1738072800000,
    "aspectCount": 47
  }
]
```

**Sorted by:** Most recently updated first

**Status Codes:**
- `200 OK` - Success

---

#### `GET /user/leaderboard?limit=5` - Leaderboard
Get top players with most maxed aspects (amount = 3).

**Query Parameters:**
- `limit: int` - Number of entries (default: 5, max: 100)

**Response:**
```json
[
  {
    "playerUuid": "069a79f444e94726a5befca90e38aaf5",
    "playerName": "Notch",
    "maxAspectCount": 47
  }
]
```

**Status Codes:**
- `200 OK` - Success
- `400 Bad Request` - Invalid limit

---

### Raid Loot Pools

#### `POST /raid/loot-pool` - Submit Loot Pool
Submit raid loot pool for current week (authenticated).

**Headers:**
- `Username: string`
- `Server-ID: string`

**Request:**
```json
{
  "raidType": "The Nameless Anomaly",
  "aspects": [
    {
      "name": "Radiant Gem",
      "rarity": "Rare"
    }
  ]
}
```

**Response (Approved):**
```json
{
  "aspects": [...]
}
```

**Response (Not Approved):**
```json
null
```

**Approval Logic:**
- **Verified user** → Instant approval
- **3+ matching submissions** → Approved (unlocked)
- **10+ matching submissions** → Approved + Locked

**Status Codes:**
- `200 OK` - Success (check if body is null or has data)
- `401 Unauthorized` - Auth failed

---

#### `GET /raid/loot-pool?raidType=xxx` - Get Approved Pool
Get approved loot pool for a raid in current week (no auth).

**Query Parameters:**
- `raidType: string` - Raid name

**Response:**
```json
{
  "aspects": [...]
}
```

**Status Codes:**
- `200 OK` - Approved pool found
- `204 No Content` - No approved pool yet

---

### Gambits

#### `POST /gambit` - Submit Gambits
Submit daily gambit locations (authenticated).

**Headers:**
- `Username: string`
- `Server-ID: string`

**Request:**
```json
{
  "gambits": [
    {
      "name": "Gambit Location A"
    }
  ]
}
```

**Response:** Same as loot pool (approved data or null)

**Approval Logic:**
- **2+ matching submissions** → Approved + Locked

**Status Codes:**
- `200 OK` - Success
- `401 Unauthorized` - Auth failed

---

#### `GET /gambit` - Get Approved Gambits
Get approved gambits for today (no auth).

**Response:**
```json
{
  "gambits": [...]
}
```

**Status Codes:**
- `200 OK` - Approved gambits found
- `204 No Content` - No approved gambits yet

---

## Authentication Flow

### How Mojang Sessionserver Auth Works

This is the **industry-standard** authentication used by Minecraft multiplayer servers.

#### Step 1: Client Authenticates (Mod Side)
```java
// Client generates random serverId
String serverId = UUID.randomUUID().toString();

// Client calls Mojang: "I want to authenticate"
MinecraftSessionService sessionService = ...;
sessionService.joinServer(playerUuid, accessToken, serverId);
```

#### Step 2: Client Sends Headers
```http
POST /user
Username: Notch
Server-ID: abc-123-def-456
```

#### Step 3: Backend Verifies (Server Side)
```java
// Backend calls Mojang: "Did 'Notch' authenticate with 'abc-123-def-456'?"
String url = "https://sessionserver.mojang.com/session/minecraft/hasJoined" +
    "?username=Notch&serverId=abc-123-def-456";

// Mojang responds:
// 200 OK + {"id": "069a79f...", "name": "Notch"}  -> Valid
// 204 No Content                                   -> Invalid
```

#### Step 4: Backend Uses Verified UUID
Backend **ignores** what client claimed and uses the UUID **from Mojang**.

### Security Features
- ✅ **No session IDs exposed** - serverId is one-time use
- ✅ **Replay attack prevention** - serverId expires after 30 seconds
- ✅ **Cryptographic verification** - Only Mojang can verify
- ✅ **No passwords transmitted** - Uses existing Minecraft session
- ✅ **Zero user input** - Completely automatic

### Implementation: MojangAuthService.java
```java
@Service
public class MojangAuthService {
    private final Map<String, Long> usedServerIds = new ConcurrentHashMap<>();

    public AuthResult verifyPlayer(String username, String serverId) {
        // 1. Check replay attack
        if (serverIdWasRecentlyUsed(serverId)) {
            return AuthResult.error("Token already used");
        }

        // 2. Call Mojang
        HttpResponse<String> response = httpClient.send(request, ...);

        // 3. Parse response
        if (response.statusCode() == 200) {
            JsonNode json = objectMapper.readTree(response.body());
            String verifiedUuid = json.get("id").asText();
            String verifiedUsername = json.get("name").asText();

            // 4. Mark token as used
            usedServerIds.put(serverId, System.currentTimeMillis());

            return AuthResult.success(verifiedUuid, verifiedUsername);
        }

        return AuthResult.error("Authentication failed");
    }
}
```

---

## Adding New Features

### Example: Add Aspect History Tracking

#### Step 1: Create Entity
```java
@Entity
@Table(name = "aspect_history")
public class AspectHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String playerUuid;

    @Column(nullable = false)
    private String aspectName;

    @Column(nullable = false)
    private int oldAmount;

    @Column(nullable = false)
    private int newAmount;

    @Column(nullable = false)
    private Instant changedAt;

    // Constructors, getters, setters...
}
```

#### Step 2: Create Repository
```java
@Repository
public interface AspectHistoryRepository extends JpaRepository<AspectHistory, Long> {
    List<AspectHistory> findByPlayerUuidOrderByChangedAtDesc(String playerUuid);

    @Query("SELECT h FROM AspectHistory h WHERE h.playerUuid = :uuid AND h.aspectName = :name ORDER BY h.changedAt DESC")
    List<AspectHistory> findHistoryForAspect(String uuid, String name);
}
```

#### Step 3: Create DTO
```java
public class AspectHistoryDto {
    public static class HistoryEntry {
        private String aspectName;
        private int oldAmount;
        private int newAmount;
        private long changedAt;

        // Constructor, getters, setters...
    }
}
```

#### Step 4: Add Controller Endpoint
```java
@GetMapping("/history")
public ResponseEntity<?> getHistory(@RequestParam String playerUuid) {
    String normalized = playerUuid.replace("-", "").toLowerCase();
    List<AspectHistory> history = aspectHistoryRepo.findByPlayerUuidOrderByChangedAtDesc(normalized);

    List<AspectHistoryDto.HistoryEntry> entries = history.stream()
        .map(h -> new AspectHistoryDto.HistoryEntry(
            h.getAspectName(), h.getOldAmount(), h.getNewAmount(), h.getChangedAt().toEpochMilli()
        ))
        .collect(Collectors.toList());

    return ResponseEntity.ok(entries);
}
```

#### Step 5: Update Business Logic
```java
// In PersonalAspectController.uploadAspects():
if (existing.isPresent()) {
    PersonalAspect pa = existing.get();
    int oldAmount = pa.getAmount();
    pa.setAmount(aspect.getAmount());

    // Track history if changed
    if (oldAmount != aspect.getAmount()) {
        AspectHistory history = new AspectHistory(
            verifiedUuid, aspect.getName(), oldAmount, aspect.getAmount()
        );
        aspectHistoryRepo.save(history);
    }

    personalAspectRepo.save(pa);
}
```

#### Step 6: Database Migration
```sql
CREATE TABLE aspect_history (
    id BIGSERIAL PRIMARY KEY,
    player_uuid VARCHAR(32) NOT NULL,
    aspect_name VARCHAR(255) NOT NULL,
    old_amount INT NOT NULL,
    new_amount INT NOT NULL,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_aspect_history_player ON aspect_history(player_uuid, changed_at DESC);
```

---

## Code Structure

### Entity Pattern
```java
@Entity
@Table(name = "table_name")
public class MyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String someField;

    @Column(nullable = false)
    private Instant createdAt;

    // JPA requires no-arg constructor
    public MyEntity() {}

    // Constructor for business logic
    public MyEntity(String someField) {
        this.someField = someField;
        this.createdAt = Instant.now();
    }

    // Getters and setters...
}
```

### Repository Pattern
```java
@Repository
public interface MyRepository extends JpaRepository<MyEntity, Long> {
    // Spring Data JPA generates implementation automatically

    // Method name queries (Spring parses the name)
    Optional<MyEntity> findBySomeField(String field);
    List<MyEntity> findByCreatedAtAfter(Instant date);

    // Custom JPQL queries
    @Query("SELECT e FROM MyEntity e WHERE e.someField LIKE %:search%")
    List<MyEntity> searchByField(@Param("search") String search);
}
```

### Controller Pattern
```java
@RestController
@RequestMapping("/api")
public class MyController {
    @Autowired
    private MyRepository repo;

    @Autowired
    private MyService service;

    @GetMapping("/items")
    public ResponseEntity<?> getItems() {
        try {
            List<MyEntity> items = repo.findAll();
            return ResponseEntity.ok(items);
        } catch (Exception e) {
            logger.error("Error fetching items", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to fetch items");
        }
    }

    @PostMapping("/items")
    @Transactional
    public ResponseEntity<?> createItem(@RequestBody MyDto dto) {
        // Validate input
        if (dto.getSomeField() == null) {
            return ResponseEntity.badRequest().body("Field required");
        }

        // Business logic in service
        MyEntity entity = service.processAndSave(dto);

        return ResponseEntity.ok(entity);
    }
}
```

### Service Pattern
```java
@Service
public class MyService {
    @Autowired
    private MyRepository repo;

    @Transactional
    public MyEntity processAndSave(MyDto dto) {
        // Business logic here
        MyEntity entity = new MyEntity(dto.getSomeField());
        return repo.save(entity);
    }
}
```

---

## Common Patterns

### Time-Based Identifiers
```java
// TimeUtils.java patterns

// Week identifier (Friday 19:00 CET to Friday 19:00 CET)
String weekId = TimeUtils.getWeekIdentifier();  // "2026-W04"

// Day identifier (19:00 CET to 19:00 CET)
String dayId = TimeUtils.getDayIdentifier();    // "2026-01-27"

// Next reset times
ZonedDateTime nextReset = TimeUtils.getNextLootPoolReset();  // Next Friday 19:00
ZonedDateTime nextReset = TimeUtils.getNextGambitReset();    // Tomorrow 19:00
```

### JSON Storage in Database
```java
// Serialize to JSON
String json = objectMapper.writeValueAsString(listOfObjects);
entity.setDataJson(json);

// Deserialize from JSON
List<MyDto> objects = objectMapper.readValue(
    entity.getDataJson(),
    objectMapper.getTypeFactory().constructCollectionType(List.class, MyDto.class)
);
```

### Consensus Algorithm Pattern
```java
@Transactional
private MyDto checkAndApprove(String key, String dataJson) {
    // 1. Get all submissions for this key
    List<Submission> all = submissionRepo.findByKey(key);

    // 2. Filter to matching submissions
    List<Submission> matching = all.stream()
        .filter(s -> s.getDataJson().equals(dataJson))
        .collect(Collectors.toList());

    int count = matching.size();

    // 3. Check thresholds
    if (count >= LOCK_THRESHOLD) {
        // Lock and approve
        saveApproved(key, dataJson, true);
        return deserialize(dataJson);
    } else if (count >= APPROVE_THRESHOLD) {
        // Approve (unlocked)
        saveApproved(key, dataJson, false);
        return deserialize(dataJson);
    }

    // Not enough submissions yet
    return null;
}
```

### UUID Normalization
```java
// Always normalize UUIDs: no dashes, lowercase
String normalized = uuid.replace("-", "").toLowerCase();

// Validate UUID format
if (!normalized.matches("[0-9a-f]{32}")) {
    throw new IllegalArgumentException("Invalid UUID");
}
```

---

## Testing

### Manual Testing with curl

```bash
# 1. Upload aspects (requires Mojang auth - test with mod)
curl -X POST http://localhost:8080/user \
  -H "Content-Type: application/json" \
  -H "Username: Notch" \
  -H "Server-ID: abc-123" \
  -d '{
    "playerName": "Notch",
    "modVersion": "0.12.1-TEST1",
    "aspects": [
      {"name": "Radiant Gem", "rarity": "Rare", "amount": 3}
    ]
  }'

# 2. Get player aspects
curl http://localhost:8080/user?playerUuid=069a79f444e94726a5befca90e38aaf5

# 3. Get player list
curl http://localhost:8080/user/list

# 4. Get leaderboard
curl http://localhost:8080/user/leaderboard?limit=10

# 5. Get raid loot pool
curl http://localhost:8080/raid/loot-pool?raidType=The%20Nameless%20Anomaly

# 6. Get gambits
curl http://localhost:8080/gambit
```

### Integration Testing Strategy

1. **Test database separately** - Use H2 in-memory database for tests
2. **Mock external services** - Mock MojangAuthService for tests
3. **Test repositories** - Use `@DataJpaTest`
4. **Test controllers** - Use `@WebMvcTest`
5. **Test full stack** - Use `@SpringBootTest`

Example test:
```java
@SpringBootTest
@AutoConfigureMockMvc
class PersonalAspectControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MojangAuthService mojangAuth;

    @Test
    void testUploadAspects() throws Exception {
        when(mojangAuth.verifyPlayer(anyString(), anyString()))
            .thenReturn(AuthResult.success("uuid", "Notch"));

        mockMvc.perform(post("/user")
                .header("Username", "Notch")
                .header("Server-ID", "abc-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{...}"))
            .andExpect(status().isOk());
    }
}
```

---

## Deployment

### Local Development
```bash
# 1. Start PostgreSQL
docker run -d -p 5432:5432 \
  -e POSTGRES_DB=wynnextras \
  -e POSTGRES_USER=wynnextras \
  -e POSTGRES_PASSWORD=password \
  postgres:16

# 2. Configure application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/wynnextras
spring.datasource.username=wynnextras
spring.datasource.password=password

# 3. Run application
./mvnw spring-boot:run
```

### Production Deployment
```bash
# 1. Build JAR
./mvnw clean package -DskipTests

# 2. Run with production profile
java -jar target/wynnextras_server-0.0.1-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --spring.datasource.url=jdbc:postgresql://db:5432/wynnextras \
  --spring.datasource.username=$DB_USER \
  --spring.datasource.password=$DB_PASS
```

### Docker Deployment
```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
```

---

## Future Feature Ideas

### 🎯 High Priority Features

#### 1. Aspect Statistics Endpoint
**Purpose:** Global statistics about aspect collection across all players

**Endpoint:** `GET /user/statistics`

**Response:**
```json
{
  "totalPlayers": 1547,
  "totalAspects": 73542,
  "totalMaxedAspects": 8291,
  "rarestAspects": [
    {
      "name": "Legendary Aspect",
      "ownedBy": 3,
      "maxedBy": 1,
      "percentOwned": 0.19
    }
  ],
  "mostCommonAspects": [...],
  "averageAspectsPerPlayer": 47.5,
  "averageMaxedPerPlayer": 5.4
}
```

**Implementation:**
- Add `StatisticsService.java`
- Complex aggregation queries
- Cache results (update every 5 minutes)

**Use Case:**
- Show stats on aspects screen
- "You have more aspects than 85% of players"
- Track progress toward global goals

---

#### 2. Aspect Comparison Endpoint
**Purpose:** Compare two players' aspect collections

**Endpoint:** `GET /user/compare?uuid1=xxx&uuid2=yyy`

**Response:**
```json
{
  "player1": {
    "uuid": "...",
    "name": "Player1",
    "totalAspects": 47,
    "maxedAspects": 12
  },
  "player2": {
    "uuid": "...",
    "name": "Player2",
    "totalAspects": 35,
    "maxedAspects": 8
  },
  "onlyPlayer1Has": [
    {"name": "Rare Aspect", "amount": 2}
  ],
  "onlyPlayer2Has": [
    {"name": "Epic Aspect", "amount": 1}
  ],
  "bothHave": [
    {
      "name": "Common Aspect",
      "player1Amount": 3,
      "player2Amount": 2
    }
  ],
  "similarity": 0.73
}
```

**Use Case:**
- Compare with friends
- See who has better collection
- Trade opportunities

---

#### 3. Aspect History/Changelog
**Purpose:** Track aspect changes over time per player

**Endpoint:** `GET /user/history?playerUuid=xxx`

**Response:**
```json
[
  {
    "aspectName": "Radiant Gem",
    "oldAmount": 2,
    "newAmount": 3,
    "changedAt": 1738072800000,
    "note": "Maxed!"
  }
]
```

**Implementation:**
- New table: `aspect_history`
- Trigger on aspect amount change
- Store old/new values

**Use Case:**
- Track progress over time
- See when aspects were maxed
- Achievement notifications

---

#### 4. Aspect Search/Filter
**Purpose:** Search aspects by name, rarity, ownership

**Endpoint:** `GET /aspects/search?query=gem&rarity=rare&minOwned=5`

**Response:**
```json
{
  "results": [
    {
      "name": "Radiant Gem",
      "rarity": "Rare",
      "ownedByCount": 147,
      "maxedByCount": 23,
      "topOwners": [
        {"name": "Player1", "amount": 3}
      ]
    }
  ]
}
```

**Use Case:**
- Find specific aspects
- See who owns rare aspects
- Market research

---

#### 5. Personal Goals/Wishlist
**Purpose:** Let players set aspect collection goals

**Endpoints:**
- `POST /user/goals` - Add goal
- `GET /user/goals?playerUuid=xxx` - Get goals
- `DELETE /user/goals/{id}` - Remove goal

**Request:**
```json
{
  "aspectName": "Radiant Gem",
  "targetAmount": 3,
  "notes": "Need for build"
}
```

**Response:**
```json
{
  "goals": [
    {
      "id": 1,
      "aspectName": "Radiant Gem",
      "currentAmount": 2,
      "targetAmount": 3,
      "progress": 0.67,
      "completed": false
    }
  ]
}
```

**Use Case:**
- Track collection goals
- Motivation system
- Achievement tracking

---

### 🚀 Advanced Features

#### 6. Aspect Trading System
**Purpose:** Facilitate aspect trading between players

**Tables:**
- `trade_offer` - Active trade offers
- `trade_history` - Completed trades

**Endpoints:**
- `POST /trade/offer` - Create offer
- `GET /trade/offers` - Browse offers
- `POST /trade/accept` - Accept trade
- `DELETE /trade/offer/{id}` - Cancel offer

**Use Case:**
- Player economy
- Trade rare aspects
- Market pricing

---

#### 7. Aspect Price Tracking
**Purpose:** Track aspect value over time based on trades

**Endpoint:** `GET /aspects/price-history?name=xxx`

**Response:**
```json
{
  "aspectName": "Radiant Gem",
  "currentPrice": 45.5,
  "priceHistory": [
    {"date": "2026-01-20", "price": 42.0},
    {"date": "2026-01-27", "price": 45.5}
  ],
  "recentTrades": [
    {
      "price": 50,
      "quantity": 1,
      "date": "2026-01-27"
    }
  ]
}
```

**Use Case:**
- Market analysis
- Investment decisions
- Track value trends

---

#### 8. Social Features - Friends System
**Purpose:** Add/follow friends to see their collections

**Endpoints:**
- `POST /friends/add` - Send friend request
- `GET /friends/list` - Get friends
- `GET /friends/feed` - See friends' recent activity

**Response:**
```json
{
  "friends": [
    {
      "uuid": "...",
      "name": "Friend1",
      "aspectCount": 47,
      "recentActivity": [
        {
          "type": "maxed_aspect",
          "aspectName": "Radiant Gem",
          "timestamp": 1738072800000
        }
      ]
    }
  ]
}
```

**Use Case:**
- Social competition
- Share achievements
- Motivate friends

---

#### 9. Achievement System
**Purpose:** Track collection milestones

**Endpoint:** `GET /user/achievements?playerUuid=xxx`

**Response:**
```json
{
  "achievements": [
    {
      "id": "collector_10",
      "name": "Collector I",
      "description": "Own 10 different aspects",
      "progress": 10,
      "target": 10,
      "completed": true,
      "unlockedAt": 1738072800000,
      "reward": "Badge"
    },
    {
      "id": "maxed_1",
      "name": "Perfectionist I",
      "description": "Max your first aspect",
      "progress": 1,
      "target": 1,
      "completed": true
    }
  ],
  "totalPoints": 250,
  "rank": "Master Collector"
}
```

**Implementation:**
- Achievement definitions in database
- Check on aspect upload
- Award points/badges

**Use Case:**
- Gamification
- Long-term goals
- Show off progress

---

#### 10. Daily/Weekly Challenges
**Purpose:** Time-limited collection challenges

**Endpoint:** `GET /challenges`

**Response:**
```json
{
  "daily": {
    "challenge": "Collect 3 aspects today",
    "progress": 2,
    "target": 3,
    "reward": "50 points",
    "endsAt": 1738108800000
  },
  "weekly": {
    "challenge": "Max 2 aspects this week",
    "progress": 1,
    "target": 2,
    "reward": "Achievement badge",
    "endsAt": 1738368000000
  }
}
```

**Use Case:**
- Daily engagement
- Bonus rewards
- Limited-time events

---

#### 11. Aspect Drop Rate Tracking
**Purpose:** Track where aspects drop from (community data)

**Endpoint:** `POST /aspects/drop-report`

**Request:**
```json
{
  "aspectName": "Radiant Gem",
  "source": "Raid: The Nameless Anomaly",
  "sourceType": "raid"
}
```

**Endpoint:** `GET /aspects/drop-info?name=xxx`

**Response:**
```json
{
  "aspectName": "Radiant Gem",
  "dropSources": [
    {
      "source": "Raid: The Nameless Anomaly",
      "reportCount": 147,
      "confidence": 0.95
    }
  ],
  "estimatedDropRate": 0.15
}
```

**Use Case:**
- Wiki information
- Farming guides
- Drop rate transparency

---

#### 12. Backup/Export System
**Purpose:** Let players backup their collection

**Endpoints:**
- `GET /user/export?playerUuid=xxx` - Export to JSON
- `POST /user/import` - Import backup (authenticated)

**Response:**
```json
{
  "version": "1.0",
  "exportedAt": 1738072800000,
  "playerUuid": "...",
  "playerName": "Notch",
  "aspects": [...]
}
```

**Use Case:**
- Data portability
- Backup before risky operations
- Transfer between mods

---

#### 13. Notification System
**Purpose:** Notify players of events

**Endpoint:** `GET /user/notifications?playerUuid=xxx`

**Response:**
```json
{
  "notifications": [
    {
      "id": 1,
      "type": "milestone",
      "title": "Achievement Unlocked!",
      "message": "You maxed your 10th aspect!",
      "timestamp": 1738072800000,
      "read": false
    },
    {
      "type": "friend_activity",
      "message": "Friend1 just maxed Radiant Gem",
      "read": false
    }
  ]
}
```

**Use Case:**
- Achievement notifications
- Friend activity
- Trade offers

---

#### 14. API Rate Limiting
**Purpose:** Prevent abuse

**Implementation:**
```java
@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, RateLimit> limits = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, ...) {
        String clientId = getClientId(request);
        RateLimit limit = limits.computeIfAbsent(clientId, k -> new RateLimit());

        if (!limit.allowRequest()) {
            response.setStatus(429);
            return false;
        }

        return true;
    }
}
```

**Use Case:**
- Prevent spam
- Fair usage
- Server protection

---

#### 15. Admin Dashboard Endpoints
**Purpose:** Moderate and monitor the system

**Endpoints:**
- `GET /admin/stats` - Server statistics
- `GET /admin/recent-uploads` - Recent aspect uploads
- `DELETE /admin/user/{uuid}` - Remove user data
- `POST /admin/verified-users` - Add verified user

**Authentication:** Separate admin auth (API key or OAuth)

**Use Case:**
- Moderation
- System health
- User management

---

### 💡 Simple Additions

#### 16. Health Check Endpoint
```java
@GetMapping("/health")
public ResponseEntity<String> health() {
    return ResponseEntity.ok("OK");
}
```

#### 17. Version Endpoint
```java
@GetMapping("/version")
public ResponseEntity<Map<String, String>> version() {
    return ResponseEntity.ok(Map.of(
        "version", "1.0.0",
        "buildDate", "2026-01-27"
    ));
}
```

#### 18. Aspect Count Endpoint
```java
@GetMapping("/aspects/count")
public ResponseEntity<Map<String, Long>> aspectCount() {
    long totalAspects = personalAspectRepo.count();
    long uniqueAspects = personalAspectRepo.countDistinctAspectNames();
    return ResponseEntity.ok(Map.of(
        "total", totalAspects,
        "unique", uniqueAspects
    ));
}
```

---

## Summary

### When to Add Backend Features
- ✅ **Data needs to be shared** across players (leaderboards, crowdsourcing)
- ✅ **Data needs to persist** beyond local client
- ✅ **Requires authentication** to verify player ownership
- ✅ **Community features** (trading, friends, comparisons)
- ✅ **Analytics and statistics** across all players
- ❌ **Local-only features** (client-side settings, UI preferences)
- ❌ **Wynncraft game data** (use Wynncraft API instead)

### Best Practices
1. **Always authenticate writes** - Use MojangAuthService
2. **Public reads** - Most GET endpoints don't need auth
3. **Normalize UUIDs** - No dashes, lowercase
4. **Use time identifiers** - Week/day IDs for time-based data
5. **Index queries** - Add database indexes for common queries
6. **Cache expensive queries** - Use Spring Cache for statistics
7. **Validate input** - Check nulls, formats, ranges
8. **Log everything** - Use SLF4J logger
9. **Handle errors gracefully** - Return meaningful error messages
10. **Document as you go** - Update this file with new features

### Next Steps
1. Implement high-priority features (statistics, comparison, history)
2. Add caching layer (Redis or Spring Cache)
3. Add rate limiting
4. Set up monitoring (Prometheus + Grafana)
5. Write integration tests
6. Set up CI/CD pipeline
7. Create admin dashboard

---

**Last Updated:** 2026-01-27
**Version:** 1.0.0
**Author:** WynnExtras Development Team
