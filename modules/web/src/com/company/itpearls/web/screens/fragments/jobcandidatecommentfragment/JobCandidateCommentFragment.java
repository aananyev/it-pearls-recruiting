package com.company.itpearls.web.screens.fragments.jobcandidatecommentfragment;

import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;

@UiController("itpearls_JobCandidateCommentFragment")
@UiDescriptor("job-candidate-comment-fragment.xml")
public class JobCandidateCommentFragment extends ScreenFragment {
    private IteractionList iteractionList;
    @Inject
    private Label<String> nameLabel;
    @Inject
    private Label<String> vacancyLabel;
    @Inject
    private Label<String> textLabel;
    @Inject
    private Label<String> dateLabel;
    @Inject
    private Image imageLeft;
    @Inject
    private Image imageRight;
    @Inject
    private Button replyButton;
    @Inject
    private Dialogs dialogs;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private DataManager dataManager;
    @Inject
    private Metadata metadata;
    @Inject
    private UserSession userSession;
    @Inject
    private Notifications notifications;
    @Inject
    private HBoxLayout innerBox;

    public IteractionList getIteractionList() {
        return iteractionList;
    }

    public void setIteractionList(IteractionList iteractionList) {
        this.iteractionList = iteractionList;

        setIteractionListLabels();
    }

    private void setIteractionListLabels() {
        if (iteractionList != null) {
            if (iteractionList.getRecrutier() != null) {
                nameLabel.setValue(iteractionList.getRecrutier().getName());
            }

            if (iteractionList.getVacancy() != null) {
                vacancyLabel.setValue(!iteractionList.getVacancy().getVacansyName().equals("Default")
                        ? iteractionList.getVacancy().getVacansyName() : "");
            }

            if (iteractionList.getComment() != null) {
                textLabel.setValue(iteractionList.getComment());
            }

            if (iteractionList.getDateIteraction() != null) {
                dateLabel.setValue(iteractionList.getDateIteraction().toString());
            }

            if (iteractionList.getRecrutier().getFileImageFace() != null) {
                imageLeft.setSource(FileDescriptorResource.class).setFileDescriptor(iteractionList.getRecrutier().getFileImageFace());
                imageRight.setSource(FileDescriptorResource.class).setFileDescriptor(iteractionList.getRecrutier().getFileImageFace());
            } else {
                imageLeft.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
                imageRight.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            }

            if (userSession.getUser().equals(iteractionList.getRecrutier())) {
                imageLeft.setVisible(true);
                imageRight.setVisible(false);
                innerBox.setAlignment(Component.Alignment.TOP_RIGHT);
            } else {
                imageLeft.setVisible(false);
                imageRight.setVisible(true);
                innerBox.setAlignment(Component.Alignment.TOP_LEFT);
            }

            replyButton.addClickListener(e -> {
                dialogs.createInputDialog(this)
                        .withCaption(messageBundle.getMessage("msgComment"))
                        .withParameters(
                                InputParameter.stringParameter("comment")
                                        .withCaption(messageBundle.getMessage("msgInputComment"))
                                        .withRequired(true),
                                InputParameter.entityParameter("openPosition", OpenPosition.class)
                                        .withCaption(messageBundle.getMessage("msgOpenPosition"))
                                        .withDefaultValue(iteractionList.getVacancy())
                        )
                        .withActions(DialogActions.OK_CANCEL)
                        .withCloseListener(closeEvent -> {
                            if (closeEvent
                                    .getCloseAction()
                                    .equals(InputDialog.INPUT_DIALOG_OK_ACTION)) {
//                                replyButtonInvoke(e, "(" + ") Re:" + (String) closeEvent.getValue("comment"));
                                createComment(iteractionList,
                                        iteractionList.getVacancy(),
                                        "(" + ") Re:" + (String) closeEvent.getValue("comment"));
                           }
                        }).show();
            });
        }
    }

    private void replyButtonInvoke(Button.ClickEvent e, String comment) {
        dialogs.createInputDialog(this)
                .withCaption(messageBundle.getMessage("msgInputComment"))
                .withParameters(InputParameter.stringParameter("comment")
                                .withCaption(messageBundle.getMessage("msgComment"))
                                .withRequired(true),
                        InputParameter.entityParameter("openPosition", OpenPosition.class)
                                .withDefaultValue(iteractionList.getVacancy()))
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(inputDialogCloseEvent -> {
                    if (inputDialogCloseEvent.closedWith(DialogOutcome.OK)) {
                        String inputComment = inputDialogCloseEvent.getValue("comment");
                        OpenPosition inputOpenPosition = inputDialogCloseEvent.getValue("openPosition");

                        createComment(iteractionList, inputOpenPosition, inputComment);
                    }

                })
                .show();
    }

    private void createComment(IteractionList iteractionList, OpenPosition openPosition, String inputComment) {
        Iteraction iteractionComment = null;

        try {
            iteractionComment = dataManager
                    .loadValue("select e from itpearls_Iteraction e where e.signComment = true",
                            Iteraction.class)
                    .one();
        } catch (IllegalStateException e) {
        }

        if (iteractionComment != null) {
            BigDecimal numberInteraction = dataManager
                    .loadValue("select max(e.numberIteraction) from itpearls_IteractionList e",
                            BigDecimal.class)
                    .one();
            numberInteraction = numberInteraction.add(BigDecimal.ONE);

            IteractionList comment = metadata.create(IteractionList.class);
            comment.setCandidate(iteractionList.getCandidate());
            comment.setDateIteraction(new Date());
            comment.setCurrentOpenClose(iteractionList.getVacancy().getOpenClose() != null ?
                    iteractionList.getVacancy().getOpenClose() : false);
            comment.setRecrutier((ExtUser) userSession.getUser());

            if (inputComment == null) {
                comment.setComment(inputComment);
            } else {
                comment.setComment(inputComment);
            }

            comment.setRecrutierName(userSession.getUser().getName());
            comment.setCurrentPriority(0);
            comment.setIteractionType(iteractionComment);
            comment.setRating(0);
            comment.setNumberIteraction(numberInteraction);

            if (openPosition != null) {
                comment.setVacancy(openPosition);
            } else {
                comment.setVacancy(dataManager
                        .loadValue("select e from itpearls_OpenPosition e where e.vacansyName like \'Default\'",
                                OpenPosition.class)
                        .one());
            }
            
            dataManager.commit(comment);

            reloadInteractions();
        } else {
            notifications.create(Notifications.NotificationType.ERROR)
                    .withCaption(messageBundle.getMessage("msgError"))
                    .withDescription(messageBundle.getMessage("msgDoNotCommentInteraction"))
                    .withType(Notifications.NotificationType.ERROR)
                    .show();
        }
    }

    private void reloadInteractions() {
    }
}