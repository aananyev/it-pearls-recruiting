package com.company.hunttech.core;

import com.company.hunttech.entity.UserAiProfile;
import com.company.itpearls.ItpearlsTestContainer;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.ViewRepository;
import org.junit.ClassRule;
import org.junit.Test;

import static org.junit.Assert.*;

public class ScreenViewIntegrityTest {

    @ClassRule
    public static ItpearlsTestContainer cont = new ItpearlsTestContainer();

    @Test
    public void test1_userAiProfile_class_exists() throws Exception {
        Class.forName("com.company.hunttech.entity.UserAiProfile");
    }

    @Test
    public void test2_userAiProfile_entity_registered() {
        Metadata md = AppBeans.get(Metadata.class);
        assertNotNull(md.getClassNN("hunttech_UserAiProfile"));
    }

    @Test
    public void test3_hunttech_service_bean() {
        assertNotNull(AppBeans.get("hunttech_UserAiContextService"));
    }

    @Test
    public void test4_hunttech_service_interface() {
        assertNotNull(AppBeans.get(com.company.hunttech.service.UserAiContextService.class));
    }

    @Test
    public void test5_userAiProfile_view_registered() {
        /*
         * SettingsWindow создаёт legacy datasource до вызова контроллера. Поэтому тест
         * проверяет именно наличие именованного view, а не только доступность репозитория.
         */
        ViewRepository viewRepository = AppBeans.get(ViewRepository.class);
        assertNotNull(viewRepository.getView(UserAiProfile.class, "userAiProfile-view"));
    }

    @Test
    public void test6_extUser_entity_registered() {
        Metadata md = AppBeans.get(Metadata.class);
        assertNotNull(md.getClassNN("itpearls_ExtUser"));
    }

    @Test
    public void test7_jobCandidate_entity_registered() {
        Metadata md = AppBeans.get(Metadata.class);
        assertNotNull(md.getClassNN("itpearls_JobCandidate"));
    }

    @Test
    public void test8_hunttech_model_root_registered() {
        Metadata md = AppBeans.get(Metadata.class);
        assertNotNull(md.getClassNN("itpearls_ExtUser"));
        assertNotNull(md.getClassNN("hunttech_UserAiProfile"));
    }
}
