package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.DataGrid;
import com.haulmont.cuba.gui.components.TextArea;
import com.haulmont.cuba.gui.components.TextField;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.screen.ScreenFragment;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import org.jsoup.Jsoup;

import javax.inject.Inject;

@UiController("itpearls_OpenPositionDetailScreenFragment")
@UiDescriptor("open-position-detail-screen-fragment.xml")
public class OpenPositionDetailScreenFragment extends ScreenFragment {
    @Inject
    private TextArea<String> commentTextArea;
    @Inject
    private TextField<String> middleNameTextField;

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        commentTextArea.setValue(Jsoup.parse(commentTextArea.getRawValue()).text());

        if(middleNameTextField.getValue().equals(null)) {
            middleNameTextField.setVisible(false);
        }
    }
}