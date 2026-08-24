package com.sgv.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Armazém central (warehouse) — entidade separada das filiais/lojas.
 * Compras a fornecedores entram no StockWarehouse. Depois são distribuídas
 * para as filiais via WarehouseTransfer.
 */
@Entity
@Table(name = "warehouses")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String code;

    @Column(nullable = false)
    private String name;

    private String nuit;
    private String address;
    private String contact;
    private boolean isActive = true;
    private LocalDateTime createdAt = LocalDateTime.now();
    private String notes;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNuit() { return nuit; }
    public void setNuit(String nuit) { this.nuit = nuit; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    @Override
    public String toString() {
        return code + " — " + name;
    }
}
