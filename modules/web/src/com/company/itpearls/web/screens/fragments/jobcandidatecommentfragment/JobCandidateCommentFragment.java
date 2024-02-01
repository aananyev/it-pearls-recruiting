package com.company.itpearls.web.screens.fragments.jobcandidatecommentfragment;

import com.company.itpearls.UiNotificationEvent;
import com.company.itpearls.core.StandartMapsService;
import com.company.itpearls.core.StarsAndOtherService;
import com.company.itpearls.entity.ExtUser;
import com.company.itpearls.entity.Iteraction;
import com.company.itpearls.entity.IteractionList;
import com.company.itpearls.entity.OpenPosition;
import com.company.itpearls.web.JobCandidateCommentEvent;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Events;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.core.global.UserSessionSource;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogOutcome;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

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
    @Inject
    private UserSessionSource userSessionSource;
    @Inject
    private Events events;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Label<String> starLabel;
    @Inject
    private StarsAndOtherService starsAndOtherService;
    @Inject
    private StandartMapsService standartMapsService;

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

            if (iteractionList.getRating() != null) {
                starLabel.setValue(starsAndOtherService.setStars(iteractionList.getRating() + 1));
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
                                        .withDefaultValue(iteractionList.getVacancy()),
                                InputParameter.parameter("rating")
                                        .withField(() -> {
                                            LookupField<Integer> ratingField = uiComponents.create(LookupField.class);
                                            ratingField.setWidthFull();
                                            ratingField.setCaption(messageBundle.getMessage("msgRating"));

                                            /* Map<String, Integer> map = new LinkedHashMap<>();
                                            map.put(new StringBuilder()
                                                    .append(starsAndOtherService.setStars(1))
                                                    .append(" Полный негатив")
                                                    .toString(), 0);
                                            map.put(new StringBuilder()
                                                    .append(starsAndOtherService.setStars(2))
                                                    .append(" Сомнительно")
                                                    .toString(), 1);
                                            map.put(new StringBuilder()
                                                    .append(starsAndOtherService.setStars(3))
                                                    .append(" Нейтрально")
                                                    .toString(), 2);
                                            map.put(new StringBuilder()
                                                    .append(starsAndOtherService.setStars(4))
                                                    .append(" Положительно")
                                                    .toString(), 3);
                                            map.put(new StringBuilder()
                                                    .append(starsAndOtherService.setStars(5))
                                                    .append(" Отлично!")
                                                    .toString(), 4);*/

                                            ratingField.setOptionsMap(standartMapsService.setRatingMap());

                                            return ratingField;
                                        })
                                        .withRequired(true)
                        )
                        .withActions(DialogActions.OK_CANCEL)
                        .withCloseListener(closeEvent -> {
                            if (closeEvent
                                    .getCloseAction()
                                    .equals(InputDialog.INPUT_DIALOG_OK_ACTION)) {
                                Integer rating = closeEvent.getValue("rating");
                                createComment(iteractionList,
                                        closeEvent.getValue("openPosition"),
                                        rating, new StringBuilder()
                                                .append(iteractionList.getRecrutier().getName())
                                                .append(" Re: ")
                                                .append((String) closeEvent.getValue("comment"))
                                                .toString());
                                /* TODO тут надо сделать отправку сообщения что кому-то прилетел коммент */
                                events.publish(new UiNotificationEvent(this, new StringBuilder()
                                        .append(iteractionList.getRecrutier().getName())
                                        .append(": ")
                                        .append(closeEvent.getValue("comment").toString())
                                        .append(". \n\n")
                                        .append(messageBundle.getMessage("msgJobCandidate"))
                                        .append(": ")
                                        .append(iteractionList.getCandidate().getFullName())
                                        .append("\n")
                                        .append(messageBundle.getMessage("msgFrom"))
                                        .append(" ")
                                        .append(userSession.getUser().getName())
                                        .toString()));
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
                                .withDefaultValue(iteractionList.getVacancy()),
                        InputParameter.parameter("rating")
                                .withField(() -> {
                                    LookupField<Integer> ratingField = uiComponents.create(LookupField.class);
                                    ratingField.setWidthFull();
                                    ratingField.setCaption(messageBundle.getMessage("msgRating"));

/*                                    Map<String, Integer> map = new LinkedHashMap<>();
                                    map.put(starsAndOtherService.setStars(1) + " Полный негатив", 0);
                                    map.put(starsAndOtherService.setStars(2) + " Сомнительно", 1);
                                    map.put(starsAndOtherService.setStars(3) + " Нейтрально", 2);
                                    map.put(starsAndOtherService.setStars(4) + " Положительно", 3);
                                    map.put(starsAndOtherService.setStars(5) + " Отлично!", 4); */

                                    ratingField.setOptionsMap(standartMapsService.setRatingMap());

                                    return ratingField;
                                })
                                .withRequired(true)
                )
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(inputDialogCloseEvent -> {
                    if (inputDialogCloseEvent.closedWith(DialogOutcome.OK)) {
                        String inputComment = inputDialogCloseEvent.getValue("comment");
                        OpenPosition inputOpenPosition = inputDialogCloseEvent.getValue("openPosition");

                        Integer rating = inputDialogCloseEvent.getValue("rating");
                        createComment(iteractionList, inputOpenPosition, rating, inputComment);
                    }

                })
                .show();
    }

    private void createComment(IteractionList iteractionList, OpenPosition openPosition, Integer rating, String inputComment) {
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
            comment.setRating(rating);
            if (iteractionList != null) {
                comment.setCandidate(iteractionList.getCandidate());
            } else {
                if (this.iteractionList != null) {
                    comment.setCandidate(this.iteractionList.getCandidate());
                }
            }

            comment.setDateIteraction(new Date());
            comment.setCurrentOpenClose(openPosition.getOpenClose() != null ?
                    openPosition.getOpenClose() : false);
            comment.setRecrutier((ExtUser) userSession.getUser());

            if (inputComment == null) {
                comment.setComment(inputComment);
            } else {
                comment.setComment(inputComment);
            }

            comment.setRecrutierName(userSession.getUser().getName());
            comment.setCurrentPriority(0);
            comment.setIteractionType(iteractionComment);
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
        UserSession userSession = userSessionSource.getUserSession();
        events.publish(new JobCandidateCommentEvent(this, userSession.getUser()));
    }

    public void addCommentButtonInvoke() {
        dialogs.createInputDialog(this)
                .withCaption(messageBundle.getMessage("msgComment"))
                .withParameters(
                        InputParameter.stringParameter("comment")
                                .withCaption(messageBundle.getMessage("msgInputComment"))
                                .withRequired(true),
                        InputParameter.parameter("openPosition")
                                .withField(() -> {
                                    LookupField<OpenPosition> openPositionLookupField
                                            = uiComponents.create(LookupField.of(OpenPosition.class));
                                    openPositionLookupField.setPopupWidth("800px");

                                    openPositionLookupField.setOptionsList(
                                            dataManager.load(OpenPosition.class)
                                                    .query("select e from itpearls_OpenPosition e where not e.openClose = true")
                                                    .view("openPosition-view")
                                                    .list());
                                    openPositionLookupField.setWidthFull();
                                    openPositionLookupField
                                            .setCaption(messageBundle.getMessage("msgOpenPosition"));
                                    openPositionLookupField.setOptionIconProvider(openPosition -> {
                                        if (openPosition.getOpenClose() != null) {
                                            if (openPosition.getOpenClose()) {
                                                return CubaIcon.MINUS_CIRCLE.source();
                                            } else {
                                                return CubaIcon.PLUS_CIRCLE.source();
                                            }
                                        } else {
                                            return CubaIcon.PLUS_CIRCLE.source();
                                        }
                                    });

                                    return openPositionLookupField;
                                }),
                        InputParameter.parameter("rating")
                                .withField(() -> {
                                    LookupField<Integer> ratingField = uiComponents.create(LookupField.class);
                                    ratingField.setWidthFull();
                                    ratingField.setCaption(messageBundle.getMessage("msgRating"));

/*                                    Map<String, Integer> map = new LinkedHashMap<>();
                                    map.put(starsAndOtherService.setStars(1) + " Полный негатив", 0);
                                    map.put(starsAndOtherService.setStars(2) + " Сомнительно", 1);
                                    map.put(starsAndOtherService.setStars(3) + " Нейтрально", 2);
                                    map.put(starsAndOtherService.setStars(4) + " Положительно", 3);
                                    map.put(starsAndOtherService.setStars(5) + " Отлично!", 4); */

                                    ratingField.setOptionsMap(standartMapsService.setRatingMap());

                                    return ratingField;
                                })
                                .withRequired(true)
                )
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent
                            .getCloseAction()
                            .equals(InputDialog.INPUT_DIALOG_OK_ACTION)) {
                        Integer rating = closeEvent.getValue("rating");
                        createComment(null,
                                closeEvent.getValue("openPosition"),
                                rating,
                                "Re: " + (String) closeEvent.getValue("comment"));
                    }
                })
                .show();

    }
}