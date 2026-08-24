package com.sgv.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "metric_units")
public class MetricUnit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 10)
    private String abbreviation;

    @Column(nullable = false)
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    @Override
    public String toString() {
        return abbreviation + " - " + description;
    }
}
