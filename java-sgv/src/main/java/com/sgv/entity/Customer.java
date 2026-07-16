package com.sgv.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String code;

    @Column(nullable = false)
    private String name;

    private String nuit;
    private String type;
    private String address;
    private String contact;
    private Double creditLimit = 0.0;
    private Double balance = 0.0;
    private Double defaultDiscount = 0.0;
    private Integer fidelityPoints = 0;
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNuit() { return nuit; }
    public void setNuit(String nuit) { this.nuit = nuit; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Double getCreditLimit() { return creditLimit; }
    public void setCreditLimit(Double creditLimit) { this.creditLimit = creditLimit; }
    public Double getBalance() { return balance; }
    public void setBalance(Double balance) { this.balance = balance; }
    public Double getDefaultDiscount() { return defaultDiscount; }
    public void setDefaultDiscount(Double defaultDiscount) { this.defaultDiscount = defaultDiscount; }
    public Integer getFidelityPoints() { return fidelityPoints; }
    public void setFidelityPoints(Integer fidelityPoints) { this.fidelityPoints = fidelityPoints; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    @Override
    public String toString() {
        if (name == null) return "Cliente";
        return code != null ? code + " - " + name : name;
    }
}
