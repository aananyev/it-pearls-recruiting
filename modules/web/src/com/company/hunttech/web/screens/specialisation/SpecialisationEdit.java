package com.company.hunttech.web.screens.specialisation;

import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TabSheet;
import com.haulmont.cuba.gui.screen.*;
import com.company.hunttech.entity.Specialisation;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

@UiController("hunttech_Specialisation.edit")
@UiDescriptor("specialisation-edit.xml")
@EditedEntityContainer("specialisationDc")
@LoadDataBeforeShow
public class SpecialisationEdit extends StandardEditor<Specialisation> {

    // Presentation-only sidebar-навигация по вкладкам (контракт §3.1/§3.4):
    // пункты label-навигации переключают TabSheet, данные и lifecycle не затрагиваются.
    @Inject
    private TabSheet tabSheet;
    @Inject
    private Button specialisationNav;
    @Inject
    private Button candidatesNav;

    private static final String ACTIVE_NAV_STYLE = "label-nav-item-active";

    @Subscribe
    public void onInit(InitEvent event) {
        initTabNavigation();
    }

    /**
     * Строит sidebar-навигацию по вкладкам: клик переключает TabSheet и помечает
     * активный пункт (presentation-only; loaders, commit и данные не затрагиваются).
     */
    private void initTabNavigation() {
        specialisationNav.addClickListener(e -> switchToTab("tabSpecialisation", specialisationNav));
        candidatesNav.addClickListener(e -> switchToTab("tabCandidate", candidatesNav));

        tabSheet.addSelectedTabChangeListener(event -> {
            TabSheet.Tab selected = event.getSelectedTab();
            if (selected != null && selected.getName() != null) {
                updateActiveNavigation(selected.getName());
            }
        });

        TabSheet.Tab initialTab = tabSheet.getSelectedTab();
        String initialName = initialTab != null && initialTab.getName() != null
                ? initialTab.getName() : "tabSpecialisation";
        updateActiveNavigation(initialName);
    }

    private void switchToTab(String tabName, Button activeButton) {
        tabSheet.setSelectedTab(tabName);
        updateActiveNavigation(tabName);
    }

    private void updateActiveNavigation(String tabName) {
        for (Button navigationButton : navigationButtons()) {
            navigationButton.removeStyleName(ACTIVE_NAV_STYLE);
        }
        navigationButtonFor(tabName).addStyleName(ACTIVE_NAV_STYLE);
    }

    private List<Button> navigationButtons() {
        return Arrays.asList(specialisationNav, candidatesNav);
    }

    private Button navigationButtonFor(String tabName) {
        switch (tabName) {
            case "tabCandidate":
                return candidatesNav;
            case "tabSpecialisation":
            default:
                return specialisationNav;
        }
    }
}
