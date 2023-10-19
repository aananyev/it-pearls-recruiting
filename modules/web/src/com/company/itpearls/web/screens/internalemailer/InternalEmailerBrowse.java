package com.company.itpearls.web.screens.internalemailer;

import com.company.itpearls.entity.InternalEmailerTemplate;
import com.company.itpearls.entity.StdSelections;
import com.company.itpearls.entity.StdSelectionsColor;
import com.company.itpearls.web.screens.internalemailertemplate.InternalEmailerTemplateEdit;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.actions.BaseAction;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.InternalEmailer;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;

@UiController("itpearls_InternalEmailer.browse")
@UiDescriptor("internal-emailer-browse.xml")
@LookupComponent("emailersTable")
@LoadDataBeforeShow
public class InternalEmailerBrowse extends StandardLookup<InternalEmailer> {
    @Inject
    private UiComponents uiComponents;
    @Inject
    private DataManager dataManager;
    @Inject
    private DataGrid<InternalEmailer> emailersTable;
    @Inject
    private CollectionLoader<InternalEmailer> emailersDl;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private ScreenBuilders screenBuilders;

    @Install(to = "emailersTable.replyInternalEmailerColumn", subject = "columnGenerator")
    private Component emailersTableReplyInternalEmailerColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        final String QUERY_GET_REPLY = "select e from itpearls_InternalEmailer e " +
                "where e.replyInternalEmailer = :replyInternalEmailer";

        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);
        retHbox.setHeightFull();
        retHbox.setWidthFull();

        Label replyLabel = uiComponents.create(Label.class);
        replyLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        replyLabel.setStyleName("h1-green");

        Label selectionLabel = setSelectionLabel(event.getItem());

        if (dataManager
                .load(InternalEmailer.class)
                .query(QUERY_GET_REPLY)
                .parameter("replyInternalEmailer", event.getItem())
                .view("internalEmailer-view")
                .list()
                .size() > 0) {
            replyLabel.setVisible(true);
            replyLabel.setIconFromSet(CubaIcon.MAIL_REPLY);
            retHbox.add(replyLabel);
        } else {
            replyLabel.setVisible(false);
        }

        retHbox.add(selectionLabel);


        return retHbox;
    }

    private Label setSelectionLabel(InternalEmailer item) {
        Label star = uiComponents.create(Label.class);
        star.setIconFromSet(CubaIcon.STAR);
        star.setAlignment(Component.Alignment.MIDDLE_LEFT);
        star.setStyleName("pic-center-large-orange");


        if (item.getSelectedForAction() != null) {
            if (item.getSelectionSymbolForActions() == null) {
                if (item.getSelectedForAction()) {
                    star.setVisible(true);
                } else {
                    star.setVisible(false);
                }
            } else {
                StdSelections s = StdSelections.fromId(item.getSelectionSymbolForActions());

                switch (s) {
                    case STAR_RED:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_RED.getId());
                        break;
                    case STAR_YELLOW:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_YELLOW.getId());
                        break;
                    case STAR_GREEN:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_GREEN.getId());
                        break;
                    case FLAG_RED:
                        star.setIconFromSet(CubaIcon.FLAG);
                        star.setStyleName(StdSelectionsColor.FLAG_RED.getId());
                        break;
                    case FLAG_YELLOW:
                        star.setIconFromSet(CubaIcon.FLAG);
                        star.setStyleName(StdSelectionsColor.FLAG_YELLOW.getId());
                        break;
                    case FLAG_GREEN:
                        star.setIconFromSet(CubaIcon.FLAG);
                        star.setStyleName(StdSelectionsColor.FLAG_GREEN.getId());
                        break;
                    default:
                        star.setIconFromSet(CubaIcon.STAR);
                        star.setStyleName(StdSelectionsColor.STAR_YELLOW.getId());
                        break;
                }
            }
        } else {
            star.setVisible(false);
        }

        return star;
    }

    @Install(to = "emailersTable.toEmail", subject = "columnGenerator")
    private Component emailersTableToEmailColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        if (event.getItem().getToEmail().getFileImageFace() != null) {
            return generateImageWithLabel(event.getItem().getToEmail().getFileImageFace(),
                    event.getItem().getToEmail().getFullName());
        } else {
            return generateImageWithLabel(null, event.getItem().getToEmail().getFullName());
        }
    }

    @Install(to = "emailersTable.fromEmail", subject = "columnGenerator")
    private Component emailersTableFromEmailColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        if (event.getItem().getFromEmail().getFileImageFace() != null) {
            return generateImageWithLabel(event.getItem().getFromEmail().getFileImageFace(),
                    event.getItem().getFromEmail().getName());
        } else {
            return generateImageWithLabel(null, event.getItem().getFromEmail().getName());
        }
    }

    private HBoxLayout generateImageWithLabel(FileDescriptor fileDescriptor, String name) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();
        retHBox.setSpacing(true);

        Image image = uiComponents.create(Image.class);
        image.setWidth("20px");
        image.setHeight("20px");
        image.setAlignment(Component.Alignment.MIDDLE_CENTER);
        image.setScaleMode(Image.ScaleMode.SCALE_DOWN);

        if (fileDescriptor != null) {
            image.setSource(FileDescriptorResource.class)
                    .setFileDescriptor(fileDescriptor);
        } else {
            image.setSource(ThemeResource.class)
                    .setPath("icons/no-programmer.jpeg");
        }

        image.setStyleName("circle-20px");

        Label label = uiComponents.create(Label.class);
        label.setValue(name);
        label.setStyleName("table-wordwrap");
        label.setAlignment(Component.Alignment.MIDDLE_LEFT);

        retHBox.add(image);
        retHBox.add(label);

        retHBox.expand(label);

        return retHBox;
    }


    private void selectForAction(StdSelections star) {
        InternalEmailer internalEmailer = emailersTable.getSingleSelected();
        internalEmailer.setSelectedForAction(true);
        internalEmailer.setSelectionSymbolForActions(star.getId());
        dataManager.commit(internalEmailer);

        emailersDl.load();
        emailersTable.repaint();
        try {
            emailersTable.setSelected(internalEmailer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void clearSelection() {
        InternalEmailer internalEmailer = emailersTable.getSingleSelected();
        internalEmailer.setSelectedForAction(null);
        internalEmailer.setSelectionSymbolForActions(null);
        dataManager.commit(internalEmailer);

        emailersDl.load();
        emailersTable.repaint();
        try {
            emailersTable.setSelected(internalEmailer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setActionToActionPopupButton(PopupButton actionButton, InternalEmailer internalEmailer) {
        final String separatorChar = "⎯";
        String separator = separatorChar.repeat(15);

        actionButton.addAction(new BaseAction("separator2Action")
                .withCaption(separator));
        actionButton.getAction("separator2Action").setEnabled(false);

        actionButton.addAction(new BaseAction("selectedForActionActionStarRed")
                .withIcon(CubaIcon.STAR.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionStarRed"))
                .withHandler(actionPerformedAction -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    selectForAction(StdSelections.STAR_RED);
                    try {
                        emailersTable.scrollTo((InternalEmailerTemplate) internalEmailer);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("selectedForActionActionStarYellow")
                .withIcon(CubaIcon.STAR.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionStarYellow"))
                .withHandler(actionPerformedAction -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    selectForAction(StdSelections.STAR_YELLOW);
                    try {
                        emailersTable.scrollTo((InternalEmailerTemplate) internalEmailer);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("selectedForActionActionStarGreen")
                .withIcon(CubaIcon.STAR.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionStarGreen"))
                .withHandler(actionPerformedAction -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    selectForAction(StdSelections.STAR_GREEN);
                    try {
                        emailersTable.scrollTo((InternalEmailerTemplate) internalEmailer);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("selectedForActionActionFlagRed")
                .withIcon(CubaIcon.FLAG.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionFlagRed"))
                .withHandler(actionPerformedAction -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    selectForAction(StdSelections.FLAG_RED);
                    try {
                        emailersTable.scrollTo((InternalEmailerTemplate) internalEmailer);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("selectedForActionActionFlagYellow")
                .withIcon(CubaIcon.FLAG.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionFlagYellow"))
                .withHandler(actionPerformedAction -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    selectForAction(StdSelections.FLAG_YELLOW);
                    try {
                        emailersTable.scrollTo((InternalEmailerTemplate) internalEmailer);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("selectedForActionActionFlagGreen")
                .withIcon(CubaIcon.FLAG.source())
                .withCaption(messageBundle.getMessage("msgSelectForActionFlagGreen"))
                .withHandler(actionPerformedAction -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    selectForAction(StdSelections.FLAG_GREEN);
                    try {
                        emailersTable.scrollTo((InternalEmailerTemplate) internalEmailer);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                }));

        actionButton.addAction(new BaseAction("clearSelection")
                .withIcon(CubaIcon.PICKERFIELD_CLEAR.source())
                .withCaption(messageBundle.getMessage("msgClearSelection"))
                .withHandler(actionPerformedAction -> {
                    emailersTable.setSelected((InternalEmailerTemplate) internalEmailer);
                    clearSelection();
                    try {
                        emailersTable.scrollTo((InternalEmailerTemplate) internalEmailer);
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    }

                }));
    }

    @Install(to = "emailersTable.actionButtonColumn", subject = "columnGenerator")
    private Component emailersTableActionButtonColumnColumnGenerator(DataGrid.ColumnGeneratorEvent<InternalEmailer> event) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);
        retHbox.setWidthFull();
        retHbox.setHeightFull();

        PopupButton actionButton = uiComponents.create(PopupButton.class);
        actionButton.setIconFromSet(CubaIcon.BARS);
        actionButton.setShowActionIcons(true);
        actionButton.setWidthAuto();
        actionButton.setHeightAuto();
        actionButton.setAlignment(Component.Alignment.MIDDLE_CENTER);

        actionButton.addAction(new BaseAction("replyEmail")
                .withCaption(messageBundle.getMessage("msgReplyEmail"))
                .withHandler(actionPerformedEvent -> resendEmailAction(event.getItem())));

        setActionToActionPopupButton(actionButton, event.getItem());

        retHbox.add(actionButton);

        return retHbox;
    }

    protected void resendEmailAction(InternalEmailer internalEmailer) {
        screenBuilders.editor(InternalEmailer.class, this)
                .newEntity()
                .withInitializer(emailer -> {
                    emailersTable.setSelected(internalEmailer);
                    emailer.setReplyInternalEmailer(internalEmailer);
                    emailer.setToEmail(internalEmailer.getToEmail());
                })
                .withOpenMode(OpenMode.DIALOG)
                .build()
                .show();
    }
}