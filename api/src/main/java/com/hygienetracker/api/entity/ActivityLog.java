
package com.hygienetracker.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;

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

    /** When the log was saved (a point in time, stored with its time zone). */
    @Column(name = "logged_at", nullable = false)
    private Instant loggedAt;

    /** The user's calendar day for this activity; streaks and scores count days by this. */
    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

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

    public Instant getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(Instant loggedAt) {
        this.loggedAt = loggedAt;
    }

    public LocalDate getLocalDate() {
        return localDate;
    }

    public void setLocalDate(LocalDate localDate) {
        this.localDate = localDate;
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
