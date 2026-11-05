package com.trinity.banking.domain.model;

public enum AccountType {
    CHECKING("33"),
    SAVINGS("53");

    private final String numberPrefix;

    AccountType(String numberPrefix) {
        this.numberPrefix = numberPrefix;
    }

    public String getNumberPrefix() {
        return numberPrefix;
    }
}
