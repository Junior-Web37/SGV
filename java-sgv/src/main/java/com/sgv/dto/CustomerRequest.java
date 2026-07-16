package com.sgv.dto;

public class CustomerRequest {
    private String code;
    @jakarta.validation.constraints.NotBlank
    private String name;
    private String nuit;
    private String type;
    private Double creditLimit;
    private Double balance;
    private Double defaultDiscount;
    private Integer fidelityPoints;

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
}
