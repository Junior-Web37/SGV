package com.sgv.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "branches")
public class Branch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String nuit;
    private String address;
    private String contact;
    private boolean isHead = false;
    // ─── AT — Facturação Electrónica ─────────────────────────────────────────
    /** Número do certificado de software atribuído pela AT */
    private String softwareCertNumber;
    /** Número da licença de software */
    private String licenseNumber;
    // ─────────────────────────────────────────────────────────────────────────

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNuit() { return nuit; }
    public void setNuit(String nuit) { this.nuit = nuit; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public boolean isHead() { return isHead; }
    public void setHead(boolean head) { isHead = head; }
    // ─── AT getters/setters ───────────────────────────────────────────────────
    public String getSoftwareCertNumber() { return softwareCertNumber; }
    public void setSoftwareCertNumber(String softwareCertNumber) { this.softwareCertNumber = softwareCertNumber; }
    public String getLicenseNumber() { return licenseNumber; }
    public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return name != null ? name : "Filial";
    }
}
