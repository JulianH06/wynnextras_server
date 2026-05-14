package com.julianh06.wynnextras_server.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_user_activity", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"activity_date", "user_uuid"})
})
public class DailyUserActivity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "activity_date", nullable = false)
    private LocalDate activityDate;

    @Column(name = "user_uuid", nullable = false, length = 36)
    private String userUuid;

    @Column(nullable = false, length = 32)
    private String username;

    @Column(nullable = false, length = 32)
    private String modVersion;

    @Column(nullable = false)
    private long heartbeatCount;

    @Column(nullable = false)
    private Instant firstHeartbeatAt;

    @Column(nullable = false)
    private Instant lastHeartbeatAt;

    public DailyUserActivity() {}

    public DailyUserActivity(LocalDate activityDate, String userUuid, String username, String modVersion, Instant heartbeatAt) {
        this.activityDate = activityDate;
        this.userUuid = userUuid;
        this.username = username;
        this.modVersion = modVersion;
        this.heartbeatCount = 1;
        this.firstHeartbeatAt = heartbeatAt;
        this.lastHeartbeatAt = heartbeatAt;
    }

    public void recordHeartbeat(String username, String modVersion, Instant heartbeatAt) {
        this.username = username;
        this.modVersion = modVersion;
        this.heartbeatCount++;
        this.lastHeartbeatAt = heartbeatAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public LocalDate getActivityDate() { return activityDate; }
    public void setActivityDate(LocalDate activityDate) { this.activityDate = activityDate; }

    public String getUserUuid() { return userUuid; }
    public void setUserUuid(String userUuid) { this.userUuid = userUuid; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getModVersion() { return modVersion; }
    public void setModVersion(String modVersion) { this.modVersion = modVersion; }

    public long getHeartbeatCount() { return heartbeatCount; }
    public void setHeartbeatCount(long heartbeatCount) { this.heartbeatCount = heartbeatCount; }

    public Instant getFirstHeartbeatAt() { return firstHeartbeatAt; }
    public void setFirstHeartbeatAt(Instant firstHeartbeatAt) { this.firstHeartbeatAt = firstHeartbeatAt; }

    public Instant getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(Instant lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }
}
