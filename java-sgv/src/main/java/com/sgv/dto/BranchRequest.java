package com.sgv.dto;

import jakarta.validation.constraints.NotBlank;

public class BranchRequest {
    @NotBlank
    private String name;
    private String nuit;
    private String address;
    private String contact;
    private Boolean isHead;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getNuit() { return nuit; }
    public void setNuit(String nuit) { this.nuit = nuit; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }
    public Boolean getIsHead() { return isHead; }
    public void setIsHead(Boolean isHead) { this.isHead = isHead; }
}
