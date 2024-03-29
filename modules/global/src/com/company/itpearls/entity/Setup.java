package com.company.itpearls.entity;

import com.haulmont.cuba.core.entity.StandardEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Table(name = "ITPEARLS_SETUP")
@Entity(name = "itpearls_Setup")
public class Setup extends StandardEntity {
    private static final long serialVersionUID = 9098238188468208769L;

    @NotNull
    @Column(name = "PERAM_NAME", nullable = false, unique = true, length = 30)
    protected String paramName;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "PARAM_USER_ID")
    @NotNull
    protected ExtUser paramUser;

    @Column(name = "PARAM_SET", length = 80)
    protected String paramSetStr;

    @Column(name = "PARAM_SET_BOOL")
    protected Boolean paramSetBool;

    public void setParamUser(ExtUser paramUser) {
        this.paramUser = paramUser;
    }

    public ExtUser getParamUser() {
        return paramUser;
    }

    public Boolean getParamSetBool() {
        return paramSetBool;
    }

    public void setParamSetBool(Boolean paramSetBool) {
        this.paramSetBool = paramSetBool;
    }

    public String getParamSetStr() {
        return paramSetStr;
    }

    public void setParamSetStr(String paramSetStr) {
        this.paramSetStr = paramSetStr;
    }

    public String getParamName() {
        return paramName;
    }

    public void setParamName(String paramName) {
        this.paramName = paramName;
    }
}