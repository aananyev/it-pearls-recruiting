package com.company.itpearls.web.screens.openpositioncomment;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.entity.OpenPositionComment;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;

@UiController("itpearls_OpenPositionCommentsView")
@DialogMode(forceDialog = true, width = "800px", height = "600px")
@UiDescriptor("OpenPositionCommentsView.xml")
public class OpenPositionCommentsView extends Screen {
    private OpenPosition openPosition = null;
    @Inject
    private DataManager dataManager;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private Dialogs dialogs;
    @Inject
    private UserSession userSession;
    @Inject
    private Events events;
    @Inject
    private Metadata metadata;
    @Inject
    private Notifications notifications;
    @Inject
    private ScrollBoxLayout commentsScrollBox;

    public void setOpenPosition(OpenPosition openPosition) {
        this.openPosition = openPosition;
        setCommentOpenPositionScrollIteractionList(this.openPosition, commentsScrollBox);
        setCommentsOpenPositionScroll(this.openPosition, commentsScrollBox);
    }

    public void setCommentOpenPositionScrollIteractionList(OpenPosition editedEntity, ScrollBoxLayout commentsScrollBox) {
        final String QUERY_OPEN_POSITION_INTERACTIONS =
                "select e from itpearls_IteractionList e " +
                        "where e.vacancy = :vacancy and e.iteractionType.signFeedback = true";
        List<IteractionList> iteractionLists = dataManager.load(IteractionList.class)
                .query(QUERY_OPEN_POSITION_INTERACTIONS)
                .view("iteractionList-view")
                .parameter("vacancy", editedEntity)
                .list();

        if (iteractionLists.size() > 0) {
            for (IteractionList iteractionList : iteractionLists) {
                if (iteractionList.getComment() != null) {
                    if (!iteractionList.getComment().equals("")) {
                        VBoxLayout commentBox = getCommentBox(iteractionList);
                        commentsScrollBox.add(commentBox);
                    }
                }
            }
        }
    }

    public void setCommentsOpenPositionScroll(OpenPosition editedEntity, ScrollBoxLayout commentsScrollBox) {
        if (editedEntity.getOpenPositionComments() != null) {
            for (OpenPositionComment openPositionComment : editedEntity.getOpenPositionComments()) {
                if (openPositionComment.getComment() != null) {
                    VBoxLayout commentBox = getCommentBox(openPositionComment);
                    commentsScrollBox.add(commentsScrollBox);
                }
            }
        }
    }

    private VBoxLayout getCommentBox(IteractionList iteractionList) {
        VBoxLayout retBox = uiComponents.create(VBoxLayout.class);
        retBox.setWidthFull();
        retBox.setSpacing(false);
        retBox.setMargin(true);
        retBox.setMargin(false);
//        retBox.setHeight("100px");

        HBoxLayout innerBox = uiComponents.create(HBoxLayout.class);
        innerBox.setMargin(true);
        innerBox.setWidthAuto();
        innerBox.setSpacing(true);

        VBoxLayout outerBox = uiComponents.create(VBoxLayout.class);
        outerBox.setMargin(false);
        outerBox.setWidthAuto();
        outerBox.setSpacing(false);

        if (iteractionList.getComment() != null
                && !iteractionList.getComment().equals("")) {
            Label name = uiComponents.create(Label.class);

            if (iteractionList.getRecrutier() != null) {
                name.setValue(iteractionList.getRecrutier().getName() != null
                        ? iteractionList.getRecrutier().getName() :
                        (iteractionList.getRecrutier().getName() != null
                                ? iteractionList.getRecrutier().getName() : ""));
            }
            name.setStyleName("tailName");

            HBoxLayout starsAndCommentHBox = uiComponents.create(HBoxLayout.class);
            starsAndCommentHBox.setWidthAuto();
            starsAndCommentHBox.setSpacing(true);
            Label candidateName = uiComponents.create(Label.class);
            candidateName.addStyleName("table-wordwrap");
            candidateName.setValue(iteractionList.getCandidate().getFullName()
                    + " / "
                    + iteractionList.getCandidate().getPersonPosition().getPositionRuName());

            Label stars = uiComponents.create(Label.class);
            stars.addStyleName("table-wordwrap");

            if (iteractionList.getRating() != null) {
                stars.setValue(starsAndOtherService.setStars(iteractionList.getRating() + 1));
            } else {
                stars.setValue(starsAndOtherService.noneStars());
            }

            Label text = uiComponents.create(Label.class);
            text.setValue(iteractionList.getComment() != null ?
                    iteractionList.getComment().replaceAll("\n\n", "\n") : "");
            text.addStyleName("table-wordwrap");

            starsAndCommentHBox.add(stars);
            starsAndCommentHBox.add(text);

            Label date = uiComponents.create(Label.class);
            date.setValue(iteractionList.getDateIteraction() != null ?
                    iteractionList.getDateIteraction() : "");
            date.setAlignment(Component.Alignment.BOTTOM_RIGHT);
            date.setStyleName("tailDate");

            Image image = uiComponents.create(Image.class);

            if (iteractionList.getRecrutier() != null) {
                if (((ExtUser) iteractionList.getRecrutier()).getFileImageFace() != null) {
                    image.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(((ExtUser) iteractionList.getRecrutier()).getFileImageFace());
                } else {
                    image.setSource(ThemeResource.class)
                            .setPath("icons/no-programmer.jpeg");
                }
            } else {
                image.setSource(ThemeResource.class)
                        .setPath("icons/no-programmer.jpeg");
            }

            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("50px");
            image.setHeight("50px");
            image.setStyleName("circle-50px");

            innerBox.setStyleName("toolTip");

            Button replyButton = uiComponents.create(Button.class);
            replyButton.setWidthAuto();
            replyButton.setAlignment(Component.Alignment.BOTTOM_RIGHT);
            replyButton.setCaption(messageBundle.getMessage("msgReplyButton"));
            replyButton.setDescription(messageBundle.getMessage("msgReplyButtonDesc"));
            replyButton.addClickListener(e -> {
                dialogs.createInputDialog(this)
                        .withCaption(messageBundle.getMessage("msgComment"))
                        .withParameters(
                                InputParameter.stringParameter("comment")
                                        .withCaption(messageBundle.getMessage("msgInputComment"))
                                        .withRequired(true)
                        )
                        .withActions(DialogActions.OK_CANCEL)
                        .withCloseListener(closeEvent -> {
                            if (closeEvent
                                    .getCloseAction()
                                    .equals(InputDialog.INPUT_DIALOG_OK_ACTION)) {
                                replyButtonInvoke(e, "("
                                        + name.getValue()
                                        + ") Re:"
                                        + (String) closeEvent.getValue("comment"));
                            }
                        })
                        .show();
            });

            if (userSession.getUser().getLogin().equals(iteractionList.getCreatedBy())) {
                outerBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                date.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                // vacancy.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                text.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                name.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                innerBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                innerBox.addStyleName("tailMyMessage");
            } else {
                outerBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
                date.setAlignment(Component.Alignment.MIDDLE_LEFT);
                // vacancy.setAlignment(Component.Alignment.MIDDLE_LEFT);
                text.setAlignment(Component.Alignment.MIDDLE_LEFT);
                name.setAlignment(Component.Alignment.MIDDLE_LEFT);
                innerBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
                innerBox.addStyleName("tailOtherMessage");
            }

            outerBox.add(name);
            /* if (!vacancy.getValue().equals("")) {
                outerBox.add(vacancy);
            } */

            outerBox.add(candidateName);
            outerBox.add(starsAndCommentHBox);
            outerBox.add(date);
            outerBox.add(replyButton);

            if (!userSession.getUser().getLogin().equals(iteractionList.getCreatedBy())) {
                innerBox.add(image);
            }

            innerBox.add(outerBox);
            if (userSession.getUser().getLogin().equals(iteractionList.getCreatedBy())) {
                innerBox.add(image);
            }

            retBox.add(innerBox);
        }

        return retBox;
    }

    private VBoxLayout getCommentBox(OpenPositionComment openPositionComment) {
        VBoxLayout retBox = uiComponents.create(VBoxLayout.class);
        retBox.setWidthFull();
        retBox.setSpacing(false);
        retBox.setMargin(true);
        retBox.setMargin(false);
//        retBox.setHeight("100px");

        HBoxLayout innerBox = uiComponents.create(HBoxLayout.class);
        innerBox.setMargin(true);
        innerBox.setWidthAuto();
        innerBox.setSpacing(true);

        VBoxLayout outerBox = uiComponents.create(VBoxLayout.class);
        outerBox.setMargin(false);
        outerBox.setWidthAuto();
        outerBox.setSpacing(false);

        if (openPositionComment.getComment() != null
                && !openPositionComment.getComment().equals("")) {
            Label name = uiComponents.create(Label.class);

            if (openPositionComment.getUser() != null) {
                name.setValue(openPositionComment.getUser().getName() != null
                        ? openPositionComment.getUser().getName() :
                        (openPositionComment.getUser().getName() != null ? openPositionComment.getUser().getName() : ""));
            }
            name.setStyleName("tailName");

            HBoxLayout starsAndCommentHBox = uiComponents.create(HBoxLayout.class);
            starsAndCommentHBox.setWidthAuto();
            starsAndCommentHBox.setSpacing(true);

            Label stars = uiComponents.create(Label.class);
            stars.addStyleName("table-wordwrap");

            if (openPositionComment.getRating() != null) {
                stars.setValue(starsAndOtherService.setStars(openPositionComment.getRating() + 1));
            } else {
                stars.setValue(starsAndOtherService.noneStars());
            }

            Label text = uiComponents.create(Label.class);
            text.setValue(openPositionComment.getComment() != null ?
                    openPositionComment.getComment().replaceAll("\n\n", "\n") : "");
            text.addStyleName("table-wordwrap");

            starsAndCommentHBox.add(stars);
            starsAndCommentHBox.add(text);

            Label date = uiComponents.create(Label.class);
            date.setValue(openPositionComment.getDateComment() != null ?
                    openPositionComment.getDateComment() : "");
            date.setAlignment(Component.Alignment.BOTTOM_RIGHT);
            date.setStyleName("tailDate");

            Image image = uiComponents.create(Image.class);

            if (openPositionComment.getUser() != null) {
                if (((ExtUser) openPositionComment.getUser()).getFileImageFace() != null) {
                    image.setSource(FileDescriptorResource.class)
                            .setFileDescriptor(((ExtUser) openPositionComment.getUser()).getFileImageFace());
                } else {
                    image.setSource(ThemeResource.class)
                            .setPath("icons/no-programmer.jpeg");
                }
            } else {
                image.setSource(ThemeResource.class)
                        .setPath("icons/no-programmer.jpeg");
            }

            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("50px");
            image.setHeight("50px");
            image.setStyleName("circle-50px");

            innerBox.setStyleName("toolTip");

            Button replyButton = uiComponents.create(Button.class);
            replyButton.setWidthAuto();
            replyButton.setAlignment(Component.Alignment.BOTTOM_RIGHT);
            replyButton.setCaption(messageBundle.getMessage("msgReplyButton"));
            replyButton.setDescription(messageBundle.getMessage("msgReplyButtonDesc"));
            replyButton.addClickListener(e -> {
                dialogs.createInputDialog(this)
                        .withCaption(messageBundle.getMessage("msgComment"))
                        .withParameters(
                                InputParameter.stringParameter("comment")
                                        .withCaption(messageBundle.getMessage("msgInputComment"))
                                        .withRequired(true)
                        )
                        .withActions(DialogActions.OK_CANCEL)
                        .withCloseListener(closeEvent -> {
                            if (closeEvent
                                    .getCloseAction()
                                    .equals(InputDialog.INPUT_DIALOG_OK_ACTION)) {
                                replyButtonInvoke(e, "("
                                        + name.getValue()
                                        + ") Re:"
                                        + (String) closeEvent.getValue("comment"));
                            }
                        })
                        .show();
            });

            if (userSession.getUser().getLogin().equals(openPositionComment.getCreatedBy())) {
                outerBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                date.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                // vacancy.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                text.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                name.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                innerBox.setAlignment(Component.Alignment.MIDDLE_RIGHT);
                innerBox.addStyleName("tailMyMessage");
            } else {
                outerBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
                date.setAlignment(Component.Alignment.MIDDLE_LEFT);
                // vacancy.setAlignment(Component.Alignment.MIDDLE_LEFT);
                text.setAlignment(Component.Alignment.MIDDLE_LEFT);
                name.setAlignment(Component.Alignment.MIDDLE_LEFT);
                innerBox.setAlignment(Component.Alignment.MIDDLE_LEFT);
                innerBox.addStyleName("tailOtherMessage");
            }

            outerBox.add(name);
            /* if (!vacancy.getValue().equals("")) {
                outerBox.add(vacancy);
            } */

            outerBox.add(starsAndCommentHBox);
            outerBox.add(date);
            outerBox.add(replyButton);

            if (!userSession.getUser().getLogin().equals(openPositionComment.getCreatedBy())) {
                innerBox.add(image);
            }

            innerBox.add(outerBox);
            if (userSession.getUser().getLogin().equals(openPositionComment.getCreatedBy())) {
                innerBox.add(image);
            }

            retBox.add(innerBox);
        }

        return retBox;

    }

    private void replyButtonInvoke(Button.ClickEvent e, String replyStr) {
        createComment(replyStr);

        events.publish(new UiNotificationEvent(this,
                messageBundle.getMessage("msgPublishOpenPositionComment")
                        + ":"
                        + openPosition.getVacansyName()));
    }

    private void createComment(String commentStr) {

        OpenPositionComment comment = metadata.create(OpenPositionComment.class);
        comment.setOpenPosition(openPosition);
        comment.setDateComment(new Date());
        comment.setUser((ExtUser) userSession.getUser());

        if (commentStr != null) {
            comment.setComment(commentStr);

            List<OpenPositionComment> openPositionComments = openPosition.getOpenPositionComments();
            openPositionComments.add(comment);

//            dataContext.merge(comment);

        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgDoNotCommentMessage"))
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }
    }

    public void closeButton() {
        closeWithDefaultAction();
    }
}