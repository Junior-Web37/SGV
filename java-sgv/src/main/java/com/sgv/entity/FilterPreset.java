package com.sgv.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "filter_presets")
public class FilterPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    private String type; // PRODUCT, CUSTOMER, SALE

    private String name;

    @Lob
    private String data;

    private LocalDateTime createdAt = LocalDateTime.now();

    public FilterPreset() {}

    public FilterPreset(Long userId, String type, String name, String data) {
        this.userId = userId;
        this.type = type;
        this.name = name;
        this.data = data;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getData() { return data; }
    public void setData(String data) { this.data = data; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
