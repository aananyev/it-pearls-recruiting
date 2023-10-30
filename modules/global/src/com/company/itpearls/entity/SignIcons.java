package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;
import com.haulmont.cuba.core.entity.annotation.Lookup;
import com.haulmont.cuba.core.entity.annotation.LookupType;
import com.haulmont.cuba.core.entity.annotation.OnDeleteInverse;
import com.haulmont.cuba.core.global.DeletePolicy;

import javax.persistence.*;

@Table(name = "ITPEARLS_SIGN_ICONS")
@Entity(name = "itpearls_SignIcons")
public class SignIcons extends StandardEntity {
    private static final long serialVersionUID = -8240546117640869291L;

    @Column(name = "TITLE_END", length = 25)
    private String titleEnd;

    @Column(name = "TITLE_RU", length = 25)
    private String titleRu;

    @Column(name = "TITLE_DESCRIPTION")
    private String titleDescription;

    @Column(name = "ICON_NAME", length = 40)
    private String iconName;

    @Column(name = "ICON_COLOR", length = 10)
    private String iconColor;

    @Lookup(type = LookupType.DROPDOWN, actions = {})
    @OnDeleteInverse(DeletePolicy.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID")
    private ExtUser user;

    public String getTitleDescription() {
        return titleDescription;
    }

    public void setTitleDescription(String titleDescription) {
        this.titleDescription = titleDescription;
    }

    public ExtUser getUser() {
        return user;
    }

    public void setUser(ExtUser user) {
        this.user = user;
    }

    public String getIconColor() {
        return iconColor;
    }

    public void setIconColor(String iconColor) {
        this.iconColor = iconColor;
    }

    public String getIconName() {
        return iconName;
    }

    public void setIconName(String iconName) {
        this.iconName = iconName;
    }

    public String getTitleRu() {
        return titleRu;
    }

    public void setTitleRu(String titleRu) {
        this.titleRu = titleRu;
    }

    public String getTitleEnd() {
        return titleEnd;
    }

    public void setTitleEnd(String titleEnd) {
        this.titleEnd = titleEnd;
    }
}