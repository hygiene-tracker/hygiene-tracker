
package com.hygienetracker.api.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "type_id", nullable = false)
    private ActivityType type;

    @Column(name = "logged_at", nullable = false)
    private LocalDateTime loggedAt;

    @Column(nullable = false)
    private Integer minutes;

    @OneToOne(
        mappedBy = "activityLog",
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private ShowerDetail showerDetail;

    public ActivityLog() {
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public ActivityType getType() {
        return type;
    }

    public void setType(ActivityType type) {
        this.type = type;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }

    public Integer getMinutes() {
        return minutes;
    }

    public void setMinutes(Integer minutes) {
        this.minutes = minutes;
    }

    public ShowerDetail getShowerDetail() {
        return showerDetail;
    }

    public void setShowerDetail(ShowerDetail showerDetail) {
        this.showerDetail = showerDetail;
    }
}
