package com.company.itpearls.core;

import com.haulmont.cuba.security.app.Authenticated;
import org.springframework.stereotype.Component;

@Component(RecrutiesTasksBean.NAME)
public class RecrutiesTasksBean {
    public static final String NAME = "itpearls_RecrutiesTasksBean";

    @Authenticated
    public void checkSubscribe() {

    }

}