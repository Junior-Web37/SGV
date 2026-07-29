package com.sgv.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
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

    @Column(name = "credit_limit", precision = 19, scale = 4)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "default_discount", precision = 19, scale = 4)
    private BigDecimal defaultDiscount = BigDecimal.ZERO;

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

    public BigDecimal getCreditLimitAmount() { return creditLimit != null ? creditLimit : BigDecimal.ZERO; }
    public void setCreditLimitAmount(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public Double getCreditLimit() { return creditLimit != null ? creditLimit.doubleValue() : 0.0; }
    public void setCreditLimit(Double creditLimit) { this.creditLimit = creditLimit != null ? BigDecimal.valueOf(creditLimit) : BigDecimal.ZERO; }

    public BigDecimal getBalanceAmount() { return balance != null ? balance : BigDecimal.ZERO; }
    public void setBalanceAmount(BigDecimal balance) { this.balance = balance; }
    public Double getBalance() { return balance != null ? balance.doubleValue() : 0.0; }
    public void setBalance(Double balance) { this.balance = balance != null ? BigDecimal.valueOf(balance) : BigDecimal.ZERO; }

    public BigDecimal getDefaultDiscountAmount() { return defaultDiscount != null ? defaultDiscount : BigDecimal.ZERO; }
    public void setDefaultDiscountAmount(BigDecimal defaultDiscount) { this.defaultDiscount = defaultDiscount; }
    public Double getDefaultDiscount() { return defaultDiscount != null ? defaultDiscount.doubleValue() : 0.0; }
    public void setDefaultDiscount(Double defaultDiscount) { this.defaultDiscount = defaultDiscount != null ? BigDecimal.valueOf(defaultDiscount) : BigDecimal.ZERO; }

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
