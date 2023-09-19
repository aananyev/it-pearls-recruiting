package com.company.itpearls.web.screens.jobcandidate.jobcandidatecomments;

import com.company.itpearls.entity.*;
import com.company.itpearls.web.JobCandidateCommentEvent;
import com.company.itpearls.web.screens.fragments.jobcandidatecommentfragment.JobCandidateCommentFragment;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.Dialogs;
import com.haulmont.cuba.gui.Fragments;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.app.core.inputdialog.DialogActions;
import com.haulmont.cuba.gui.app.core.inputdialog.InputDialog;
import com.haulmont.cuba.gui.app.core.inputdialog.InputParameter;
import com.haulmont.cuba.gui.components.FileDescriptorResource;
import com.haulmont.cuba.gui.components.Image;
import com.haulmont.cuba.gui.components.LookupField;
import com.haulmont.cuba.gui.components.ScrollBoxLayout;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.security.global.UserSession;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.util.Date;

@UiController("itpearls_JobCandidateComment")
@UiDescriptor("job-candidate-comment.xml")
public class JobCandidateComment extends Screen {
    @Inject
    private CollectionLoader<IteractionList> candidateCommentDl;

    private JobCandidate jobCandidate;
    @Inject
    private CollectionContainer<IteractionList> candidateCommentDc;
    @Inject
    private Fragments fragments;
    @Inject
    private ScrollBoxLayout commentScrollBox;

    private Boolean completeSetJobCandidateFlag = false;
    private Boolean getCompleteSetJobCandidateScreenFlag = false;
    @Inject
    private Dialogs dialogs;
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private UserSession userSession;
    @Inject
    private DataManager dataManager;
    @Inject
    private Notifications notifications;
    @Inject
    private Metadata metadata;
    @Inject
    private UiComponents uiComponents;

    public JobCandidate getJobCandidate() {
        return jobCandidate;
    }

    public void setJobCandidate(JobCandidate jobCandidate) {
        this.jobCandidate = jobCandidate;
        setJobCandidateCollection();
    }

    @Subscribe
    public void onAfterShow(AfterShowEvent event) {
        setJobCandidateCollection();
        setJobCandidateCommentList();
    }

    private void setJobCandidateCommentList() {
        if (!getCompleteSetJobCandidateScreenFlag) {
            for (IteractionList iteractionList : candidateCommentDc.getItems()) {
                JobCandidateCommentFragment fragment = fragments.create(this, JobCandidateCommentFragment.class);
                fragment.setIteractionList(iteractionList);
                commentScrollBox.add(fragment.getFragment());
            }

            getCompleteSetJobCandidateScreenFlag = true;
        }
    }

    private void setJobCandidateCollection() {
        if (this.jobCandidate != null) {
            if (!completeSetJobCandidateFlag) {
                candidateCommentDl.setParameter("jobCandidate", this.jobCandidate);
                candidateCommentDl.load();
                completeSetJobCandidateFlag = true;
            }
        }
    }

    public void closeScreenButton() {
        closeWithDefaultAction();
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

                                    openPositionLookupField.setOptionsList(
                                            dataManager.load(OpenPosition.class)
                                                    .query("select e from itpearls_OpenPosition e where not e.openClose = true")
                                                    .list());
                                    openPositionLookupField.setWidthFull();
                                    openPositionLookupField
                                            .setCaption(messageBundle.getMessage("msgOpenPosition"));
                                    openPositionLookupField.setOptionImageProvider(openPosition -> {
                                        Image image = uiComponents.create(Image.class);
                                        image.setWidth("50px");
                                        image.setHeight("50px");
                                        image.setSource(FileDescriptorResource.class).setFileDescriptor(openPosition.getProjectName().getProjectLogo());
                                        return image.getSource();
                                    });
/*                                    openPositionLookupField.setOptionIconProvider(openPosition -> {
                                        if (openPosition.getOpenClose() != null) {
                                            if (openPosition.getOpenClose()) {
                                                return CubaIcon.MINUS_CIRCLE.source();
                                            } else {
                                                return CubaIcon.PLUS_CIRCLE.source();
                                            }
                                        } else {
                                            return CubaIcon.PLUS_CIRCLE.source();
                                        }
                                    }); */

                                    return openPositionLookupField;
                                })
                )
                .withActions(DialogActions.OK_CANCEL)
                .withCloseListener(closeEvent -> {
                    if (closeEvent
                            .getCloseAction()
                            .equals(InputDialog.INPUT_DIALOG_OK_ACTION)) {
                        createComment(null,
                                closeEvent.getValue("openPosition"),
                                "Re: " + (String) closeEvent.getValue("comment"));
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
            if (iteractionList != null) {
                comment.setCandidate(iteractionList.getCandidate());
            } else {
                if (jobCandidate != null) {
                    comment.setCandidate(jobCandidate);
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
        commentScrollBox.removeAll();
        getCompleteSetJobCandidateScreenFlag = false;
        candidateCommentDl.load();
        setJobCandidateCommentList();
    }

    @Order(15)
    @EventListener
    protected void onCommentRefresh(JobCandidateCommentEvent event) {
        reloadInteractions();
    }
}