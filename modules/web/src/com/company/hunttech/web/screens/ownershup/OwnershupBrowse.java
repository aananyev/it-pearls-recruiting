package com.company.hunttech.web.screens.ownershup;

import com.company.hunttech.entity.Ownershup;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.GroupTable;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;

@UiController("hunttech_Ownershup.browse")
@UiDescriptor("ownershup-browse.xml")
@LookupComponent("ownershupsTable")
@LoadDataBeforeShow
public class OwnershupBrowse extends StandardLookup<Ownershup> {

    @Inject
    private GroupTable<Ownershup> ownershupsTable;
    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Label<String> detailShortType;
    @Inject
    private Label<String> detailLongType;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Messages messages;
    @Inject
    private ScreenBuilders screenBuilders;

    @Subscribe
    public void onInit(InitEvent event) {
        ownershupsTable.addSelectionListener(selectionEvent -> updateSidebar(ownershupsTable.getSingleSelected()));
        openEditCardBtn.addClickListener(clickEvent -> openSelectedEditor());
    }

    private void updateSidebar(Ownershup selected) {
        if (selected != null) {
            String shortType = selected.getShortType() != null ? selected.getShortType() : "";
            String longType = selected.getLongType() != null ? selected.getLongType() : "";
            detailTitle.setValue(!shortType.isEmpty() ? shortType : messages.getMessage(getClass(), "sidebarSubtitle"));
            detailSubtitle.setValue(!longType.isEmpty() ? longType : "-");
            detailShortType.setValue(!shortType.isEmpty() ? shortType : "-");
            detailLongType.setValue(!longType.isEmpty() ? longType : "-");
            openEditCardBtn.setEnabled(true);
        } else {
            detailTitle.setValue(messages.getMessage(getClass(), "sidebarDefaultTitle"));
            detailSubtitle.setValue(messages.getMessage(getClass(), "sidebarSubtitle"));
            detailShortType.setValue("-");
            detailLongType.setValue("-");
            openEditCardBtn.setEnabled(false);
        }
    }

    private void openSelectedEditor() {
        Ownershup selected = ownershupsTable.getSingleSelected();
        if (selected != null) {
            screenBuilders.editor(ownershupsTable)
                    .editEntity(selected)
                    .withScreenClass(OwnershupEdit.class)
                    .show();
        }
    }
}