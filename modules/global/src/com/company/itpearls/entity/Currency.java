package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_CURRENCY")
@Entity(name = "itpearls_Currency")
public class Currency extends StandardEntity {
    private static final long serialVersionUID = 9098961481898121009L;

    @NotNull
    @Column(name = "CURRENCY_LONG_NAME", nullable = false, unique = true, length = 128)
    private String currencyLongName;

    @NotNull
    @Column(name = "CURRENCY_SHORT_NAME", nullable = false, unique = true, length = 3)
    private String currencyShortName;

    public String getCurrencyShortName() {
        return currencyShortName;
    }

    public void setCurrencyShortName(String currencyShortName) {
        this.currencyShortName = currencyShortName;
    }

    public String getCurrencyLongName() {
        return currencyLongName;
    }

    public void setCurrencyLongName(String currencyLongName) {
        this.currencyLongName = currencyLongName;
    }
}