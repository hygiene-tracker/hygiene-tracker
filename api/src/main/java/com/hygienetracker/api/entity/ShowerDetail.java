
package com.hygienetracker.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "shower_details")
public class ShowerDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "log_id", nullable = false, unique = true)
    private ActivityLog activityLog;

    private Boolean shampoo = false;

    private Boolean conditioner = false;

    @Column(name = "body_wash")
    private Boolean bodyWash = false;

    private Boolean exfoliant = false;

    public ShowerDetail() {
    }

    public Long getId() {
        return id;
    }

    public ActivityLog getActivityLog() {
        return activityLog;
    }

    public void setActivityLog(ActivityLog activityLog) {
        this.activityLog = activityLog;
    }

    public Boolean getShampoo() {
        return shampoo;
    }

    public void setShampoo(Boolean shampoo) {
        this.shampoo = shampoo;
    }

    public Boolean getConditioner() {
        return conditioner;
    }

    public void setConditioner(Boolean conditioner) {
        this.conditioner = conditioner;
    }

    public Boolean getBodyWash() {
        return bodyWash;
    }

    public void setBodyWash(Boolean bodyWash) {
        this.bodyWash = bodyWash;
    }

    public Boolean getExfoliant() {
        return exfoliant;
    }

    public void setExfoliant(Boolean exfoliant) {
        this.exfoliant = exfoliant;
    }
}
