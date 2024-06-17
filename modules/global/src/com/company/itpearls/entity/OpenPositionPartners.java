package com.company.itpearls.entity;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import java.util.List;

@Entity(name = "itpearls_OpenPositionPartners")
public class OpenPositionPartners extends OpenPosition {
    private static final long serialVersionUID = 6676042004787806348L;

    @JoinTable(name = "ITPEARLS_OPEN_POSITION_PARTNERS_PARTNERS_LINK",
            joinColumns = @JoinColumn(name = "OPEN_POSITION_PARTNERS_ID"),
            inverseJoinColumns = @JoinColumn(name = "PARTNERS_ID"))
    @ManyToMany
    private List<Partners> partners;

    public List<Partners> getPartners() {
        return partners;
    }

    public void setPartners(List<Partners> partners) {
        this.partners = partners;
    }
}