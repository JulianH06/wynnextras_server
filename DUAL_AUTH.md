# Dual Authentication Support

## Overview

The WynnExtras backend now supports **two authentication methods** to ensure backward compatibility with old mod versions while providing secure authentication for new versions.

## Supported Authentication Methods

### 1. Mojang Sessionserver Authentication (New Mod - v0.12.x+)

**Used by:** WynnExtras 1.21.11 (v0.12.x and newer)

**How it works:**
- Client generates random `serverId`
- Client calls Mojang: `joinServer(uuid, accessToken, serverId)`
- Client sends headers: `Username`, `Server-ID`
- Backend calls Mojang: `hasJoined(username, serverId)`
- Mojang returns verified UUID and username
- Backend uses verified UUID (ignores client claims)

**Security:**
- ✅ Cryptographically secure
- ✅ No session IDs exposed
- ✅ Replay attack prevention (30-second expiry)
- ✅ Industry standard (used by all Minecraft servers)

**Headers:**
```http
Username: Notch
Server-ID: abc-123-def-456
```

---

### 2. Wynncraft API Key Authentication (Old Mod - v0.11.x)

**Used by:** WynnExtras 1.21.4 (v0.11.x and older)

**How it works:**
- Client sends Wynncraft API key in header
- Client sends UUID (in body for aspects, in header for loot pools/gambits)
- Backend calls Wynncraft API with the key
- Wynncraft returns player data if key is valid
- Backend verifies the key belongs to the claimed UUID

**Security:**
- ✅ Validates against Wynncraft API
- ✅ Verifies key ownership
- ✅ Cached for 5 minutes to reduce API calls
- ⚠️ API key could be stolen if exposed (but same risk as original mod)

**Headers (Personal Aspects):**
```http
Wynncraft-Api-Key: your-api-key-here
Content-Type: application/json
```

**Body must include:**
```json
{
  "uuid": "069a79f444e94726a5befca90e38aaf5",
  "playerName": "Notch",
  "aspects": [...],
  "modVersion": "0.11.3"
}
```

**Headers (Loot Pools/Gambits):**
```http
Wynncraft-Api-Key: your-api-key-here
Player-UUID: 069a79f444e94726a5befca90e38aaf5
Content-Type: application/json
```

---

## Endpoint Support

### Personal Aspects: `POST /user`

**New Mod:**
```bash
curl -X POST http://wynnextras.com/user \
  -H "Content-Type: application/json" \
  -H "Username: Notch" \
  -H "Server-ID: abc-123-def" \
  -d '{
    "playerName": "Notch",
    "modVersion": "0.12.1-TEST1",
    "aspects": [
      {"name": "Radiant Gem", "rarity": "Rare", "amount": 3}
    ]
  }'
```

**Old Mod:**
```bash
curl -X POST http://wynnextras.com/user \
  -H "Content-Type: application/json" \
  -H "Wynncraft-Api-Key: your-key-here" \
  -d '{
    "uuid": "069a79f444e94726a5befca90e38aaf5",
    "playerName": "Notch",
    "modVersion": "0.11.3",
    "aspects": [
      {"name": "Radiant Gem", "rarity": "Rare", "amount": 3}
    ],
    "updatedAt": 1738072800000
  }'
```

---

### Loot Pools: `POST /lootpool/{raidType}`

**New Mod:**
```bash
curl -X POST http://wynnextras.com/lootpool/TNA \
  -H "Content-Type: application/json" \
  -H "Username: Notch" \
  -H "Server-ID: abc-123-def" \
  -d '{
    "aspects": [
      {"name": "Radiant Gem", "rarity": "Rare", "requiredClass": "Warrior"}
    ]
  }'
```

**Old Mod (if implemented in future):**
```bash
curl -X POST http://wynnextras.com/lootpool/TNA \
  -H "Content-Type: application/json" \
  -H "Wynncraft-Api-Key: your-key-here" \
  -H "Player-UUID: 069a79f444e94726a5befca90e38aaf5" \
  -d '{
    "aspects": [
      {"name": "Radiant Gem", "rarity": "Rare", "requiredClass": "Warrior"}
    ]
  }'
```

---

### Gambits: `POST /gambit`

**New Mod:**
```bash
curl -X POST http://wynnextras.com/gambit \
  -H "Content-Type: application/json" \
  -H "Username: Notch" \
  -H "Server-ID: abc-123-def" \
  -d '{
    "gambits": [
      {"name": "Gambit Location A"}
    ]
  }'
```

**Old Mod (if implemented in future):**
```bash
curl -X POST http://wynnextras.com/gambit \
  -H "Content-Type: application/json" \
  -H "Wynncraft-Api-Key: your-key-here" \
  -H "Player-UUID: 069a79f444e94726a5befca90e38aaf5" \
  -d '{
    "gambits": [
      {"name": "Gambit Location A"}
    ]
  }'
```

---

## Implementation Details

### Authentication Detection Logic

```java
// Determine which auth method is being used
boolean hasMojangAuth = username != null && !username.trim().isEmpty()
                     && serverId != null && !serverId.trim().isEmpty();

boolean hasWynnAuth = wynnApiKey != null && !wynnApiKey.trim().isEmpty();

if (hasMojangAuth) {
    // Use Mojang Sessionserver auth
    MojangAuthService.AuthResult result = mojangAuth.verifyPlayer(username, serverId);
    verifiedUuid = result.getUuid();
    verifiedUsername = result.getUsername();

} else if (hasWynnAuth) {
    // Use Wynncraft API key auth
    WynnAPIKeyService.AuthResult result = wynnApiKeyService.validateApiKey(wynnApiKey, claimedUuid);
    verifiedUuid = result.getUuid();
    verifiedUsername = result.getUsername();

} else {
    // No valid auth provided
    return 400 Bad Request;
}
```

---

### WynnAPIKeyService

**Location:** `service/WynnAPIKeyService.java`

**Key Features:**
- Calls Wynncraft API: `https://api.wynncraft.com/v3/player/{uuid}`
- Validates API key by making authenticated request
- Verifies the key belongs to the claimed UUID
- Caches validations for 5 minutes
- Cleans up cache to prevent memory leaks

**Methods:**
```java
public AuthResult validateApiKey(String apiKey, String claimedUuid)
```

**Response Handling:**
- `200 OK` → Key valid, extract username
- `401 Unauthorized` → Invalid API key
- `403 Forbidden` → Key doesn't belong to UUID
- `404 Not Found` → Player not found on Wynncraft

---

## Error Responses

### Missing Authentication
```json
{
  "status": "error",
  "message": "Authentication required: provide either (Username + Server-ID) or Wynncraft-Api-Key"
}
```

### Invalid Mojang Auth
```json
{
  "status": "error",
  "message": "Authentication failed - invalid session"
}
```

### Invalid Wynncraft API Key
```json
{
  "status": "error",
  "message": "Invalid Wynncraft API key"
}
```

### API Key Doesn't Match UUID
```json
{
  "status": "error",
  "message": "API key does not belong to this account"
}
```

---

## Compatibility Matrix

| Feature | Old Mod (0.11.x) | New Mod (0.12.x) |
|---------|------------------|------------------|
| **Upload Aspects** | ✅ Works | ✅ Works |
| **View Own Aspects** | ✅ Works | ✅ Works |
| **View Others' Aspects** | ✅ Works | ✅ Works |
| **Leaderboard** | ✅ Works | ✅ Works |
| **Player List** | ✅ Works | ✅ Works |
| **Loot Pools** | ⚠️ Not in old mod | ✅ Works |
| **Gambits** | ⚠️ Not in old mod | ✅ Works |

**Notes:**
- Old mod (1.21.4 / v0.11.x) only has personal aspects feature
- New mod (1.21.11 / v0.12.x) has all features
- Both mods can upload aspects and view data
- Backend supports both authentication methods

---

## Migration Path

### For Users on Old Mod (0.11.x)

**Option 1: Keep using old mod**
- Set Wynncraft API key: `/WynnExtras apikey <your-key>`
- Upload aspects as usual
- Everything works with Wynncraft API key auth

**Option 2: Upgrade to new mod**
- Update to WynnExtras 0.12.x (1.21.11)
- No API key needed - authentication is automatic
- Get access to new features (loot pools, gambits, leaderboard)

### For Users on New Mod (0.12.x)

- No action needed
- Authentication is fully automatic
- Mojang sessionserver handles everything

---

## Testing

### Test Old Mod Auth

```bash
# 1. Get a Wynncraft API key
# Visit: https://account.wynncraft.com/
# Generate API key

# 2. Get your UUID
curl https://api.mojang.com/users/profiles/minecraft/Notch

# 3. Test upload (replace with your data)
curl -X POST http://wynnextras.com/user \
  -H "Content-Type: application/json" \
  -H "Wynncraft-Api-Key: YOUR-KEY-HERE" \
  -d '{
    "uuid": "YOUR-UUID-HERE",
    "playerName": "YourName",
    "modVersion": "0.11.3",
    "aspects": [
      {"name": "Radiant Gem", "rarity": "Rare", "amount": 1}
    ],
    "updatedAt": 1738072800000
  }'
```

### Test New Mod Auth

This requires the mod client - can't be tested with curl because it needs Minecraft session.

---

## Performance

### Caching Strategy

**Wynncraft API Key Validations:**
- Cached for 5 minutes per (apiKey + uuid) pair
- Reduces Wynncraft API calls
- Automatic cache cleanup at 1000 entries

**Mojang Sessionserver:**
- No caching needed (one-time verification)
- serverIds expire after 30 seconds

### API Call Volume

**Without Caching:**
- Every aspect upload = 1 Wynncraft API call

**With Caching:**
- First upload = 1 Wynncraft API call
- Subsequent uploads within 5 min = 0 API calls
- Typical user: ~1 call per session

---

## Security Considerations

### Wynncraft API Key Method

**Pros:**
- Validates against official Wynncraft API
- Verifies key ownership
- Cached to reduce attack surface

**Cons:**
- API key could be stolen if client is compromised
- Less secure than Mojang sessionserver
- Requires users to manage API keys

**Mitigation:**
- Only use as fallback for old mod compatibility
- Encourage users to upgrade to new mod
- Cache validations to reduce repeated checks

### Mojang Sessionserver Method

**Pros:**
- Industry-standard authentication
- Cryptographically secure
- No secrets exposed to client
- Replay attack prevention

**Cons:**
- None (this is the gold standard)

---

## Future Considerations

### When to Remove Old Auth

Consider removing Wynncraft API key auth when:
1. Less than 5% of users use old mod version
2. All active users have upgraded
3. At least 6 months after new mod release

### Monitoring

Track authentication method usage:
```sql
SELECT
  COUNT(*) as total,
  SUM(CASE WHEN auth_method = 'mojang' THEN 1 ELSE 0 END) as mojang_auth,
  SUM(CASE WHEN auth_method = 'wynncraft' THEN 1 ELSE 0 END) as wynncraft_auth
FROM aspect_uploads
WHERE uploaded_at > NOW() - INTERVAL '7 days';
```

---

## Documentation Updates

### Updated Files
- `BACKEND.md` - Added dual auth section
- `DUAL_AUTH.md` - This file (comprehensive guide)
- `IMPLEMENTATION.md` - Updated authentication section
- `README.md` - Updated API examples

### Controller Javadocs
All controllers now have updated documentation showing both auth methods.

---

## Summary

✅ **Old mod (0.11.x) works** - Uses Wynncraft API key
✅ **New mod (0.12.x) works** - Uses Mojang sessionserver
✅ **No breaking changes** - Existing users unaffected
✅ **Secure authentication** - Both methods validated
✅ **Smooth migration** - Users can upgrade at their own pace
✅ **Performance optimized** - Caching reduces API calls

The backend now supports both old and new mod versions seamlessly!
