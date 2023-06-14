package com.company.itpearls.entity;

import com.haulmont.chile.core.annotations.NamePattern;
import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Table(name = "ITPEARLS_OUTSTAFFING_RATES")
@Entity(name = "itpearls_OutstaffingRates")
@NamePattern("%s %s|rate,currency")
public class OutstaffingRates extends StandardEntity {
    private static final long serialVersionUID = -6456725000595675601L;

    @Column(name = "RATE", nullable = false)
    @NotNull
    private BigDecimal rate;

    @Column(name = "MIN_SALARY")
    private BigDecimal minSalary;

    @Column(name = "MAX_SALARY")
    private BigDecimal maxSalary;

    @Column(name = "MAX_IE_SALARY")
    private BigDecimal maxIESalary;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "CURRENCY_ID")
    private Currency currency;

    @Lob
    @Column(name = "COMMENT_")
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(Currency currency) {
        this.currency = currency;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setMinSalary(BigDecimal minSalary) {
        this.minSalary = minSalary;
    }

    public BigDecimal getMinSalary() {
        return minSalary;
    }

    public BigDecimal getMaxIESalary() {
        return maxIESalary;
    }

    public void setMaxIESalary(BigDecimal maxIESalary) {
        this.maxIESalary = maxIESalary;
    }

    public BigDecimal getMaxSalary() {
        return maxSalary;
    }

    public void setMaxSalary(BigDecimal maxSalary) {
        this.maxSalary = maxSalary;
    }

}