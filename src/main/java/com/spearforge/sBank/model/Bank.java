package com.spearforge.sBank.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Bank {

    private String username;
    private String uuid;
    private String bankname;
    private double balance;

    public String getUsername() {
        return username;
    }

    public String getUniqueId() {
        return uuid;
    }
    
    public double getBalance() {
        return balance;
    }

    /** Keeps the stored currency exact to cents without formatting or locale-dependent parsing. */
    public void setBalance(double balance) {
        if (!Double.isFinite(balance) || balance < 0) {
            throw new IllegalArgumentException("Bank balance must be finite and non-negative");
        }
        this.balance = Math.round(balance * 100.0D) / 100.0D;
    }

}
