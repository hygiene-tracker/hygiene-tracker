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

    @Column(nullable = false)
    private boolean shampoo = false;

    @Column(nullable = false)
    private boolean conditioner = false;

    @Column(name = "body_wash", nullable = false)
    private boolean bodyWash = false;

    @Column(nullable = false)
    private boolean exfoliant = false;

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

    public boolean getShampoo() {
        return shampoo;
    }

    public void setShampoo(boolean shampoo) {
        this.shampoo = shampoo;
    }

    public boolean getConditioner() {
        return conditioner;
    }

    public void setConditioner(boolean conditioner) {
        this.conditioner = conditioner;
    }

    public boolean getBodyWash() {
        return bodyWash;
    }

    public void setBodyWash(boolean bodyWash) {
        this.bodyWash = bodyWash;
    }

    public boolean getExfoliant() {
        return exfoliant;
    }

    public void setExfoliant(boolean exfoliant) {
        this.exfoliant = exfoliant;
    }
}