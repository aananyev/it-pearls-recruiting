package com.company.itpearls.web.screens.signicons;

import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_SignIconsSelecter")
@UiDescriptor("sign-icons-selecter.xml")
public class SignIconsSelecter extends Screen {
    private static CubaIcon[] icons = {CubaIcon.FILE, CubaIcon.COMMENT, CubaIcon.MEDKIT, CubaIcon.FLAG, CubaIcon.STAR, CubaIcon.PLUS_CIRCLE, CubaIcon.MINUS_CIRCLE, CubaIcon.CHILD, CubaIcon.FILTER, CubaIcon.CLOCK_O, CubaIcon.FOLDER, CubaIcon.FILE_TEXT_O, CubaIcon.PENCIL, CubaIcon.QUESTION_CIRCLE, CubaIcon.YES, CubaIcon.NO, CubaIcon.AMBULANCE, CubaIcon.ADDRESS_BOOK, CubaIcon.ADJUST, CubaIcon.ARCHIVE, CubaIcon.BAN, CubaIcon.ARROWS, CubaIcon.BELL, CubaIcon.BOMB, CubaIcon.BOOK, CubaIcon.BOOKMARK, CubaIcon.ADDRESS_BOOK, CubaIcon.ENVELOPE, CubaIcon.ENVELOPE_OPEN, CubaIcon.BUG, CubaIcon.BUS, CubaIcon.CALENDAR, CubaIcon.CC, CubaIcon.CHAIN, CubaIcon.CHECK_CIRCLE, CubaIcon.CLONE,CubaIcon.CLOUD, CubaIcon.CODE, CubaIcon.COFFEE,CubaIcon.COPY, CubaIcon.MICROPHONE, CubaIcon.WINDOWS, CubaIcon.WIFI, CubaIcon.USERS, CubaIcon.VOLUME_OFF, CubaIcon.VOLUME_UP, CubaIcon.VOLUME_DOWN, CubaIcon.WHATSAPP, CubaIcon.TELEGRAM, CubaIcon.SKYPE, CubaIcon.UNLOCK, CubaIcon.LOCK, CubaIcon.TRASH, CubaIcon.TIMES, CubaIcon.PAUSE_CIRCLE, CubaIcon.TRY};
    private final static String style_icon_no_border_50px = "icon-no-border-50px";
    final static String open_position_pic_center_large_black = "open-position-pic-center-large-black";

    @Inject
    private UiComponents uiComponents;
    @Inject
    private FlowBoxLayout cubaIconsFlowBix;

    private String iconSelected = "";

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {

        for (CubaIcon icon : icons) {
            Button label = uiComponents.create(Button.class);

            label.setIcon(icon.source());
            label.setStyleName(open_position_pic_center_large_black);
            label.addClickListener(e -> {
                this.iconSelected = e.getButton().getIcon();
                closeWithDefaultAction();
            });

            cubaIconsFlowBix.add(label);
        }
    }

    public String getIconSelected() {
        return this.iconSelected;
    }
}