package com.company.itpearls.web.screens.fragments.candidatecv;

import com.company.itpearls.core.StdImage;
import com.company.itpearls.entity.CandidateCV;
import com.company.itpearls.entity.CandidateCVWorkPlaces;
import com.company.itpearls.entity.Company;
import com.company.itpearls.entity.Position;
import com.company.itpearls.web.screens.company.CompanyEdit;
import com.haulmont.cuba.core.global.Metadata;
import com.haulmont.cuba.gui.*;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.components.data.value.ContainerValueSource;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.text.SimpleDateFormat;
import java.util.Date;

@UiController("itpearls_WorkPlacesFragment")
@UiDescriptor("work-places-fragment.xml")
@LoadDataBeforeShow
public class WorkPlacesFragment extends ScreenFragment {
    @Inject
    private DateField<Date> endDateField;
    @Inject
    private GroupBoxLayout workPlaceGroupBox;
    @Inject
    private DateField<Date> startDateField;
    @Inject
    private CheckBox workToThisDayCheckBox;
    @Inject
    private MessageBundle messageBundle;
    private Boolean deletedWorkPlace = false;
    @Inject
    private TextField<String> workPlaceCommentTextField;
    @Inject
    private RichTextArea functionalityAtWorkRichTextArea;
    @Inject
    private RichTextArea achievementsRichTextArea;
    @Inject
    private RichTextArea personalRoleRichTextArea;
    private CandidateCVWorkPlaces candidateCVWorkPlaces;
    @Inject
    private SuggestionPickerField<Company> companySuggestPickerField;
    @Inject
    private Metadata metadata;
    private CandidateCV candidateCV;
    @Inject
    private Button deleteWorkPlaceButton;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private Notifications notifications;
    @Inject
    private Dialogs dialogs;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private LookupPickerField<Position> positionLookupPickerField;
    @Inject
    private Image projectLogoImage;

    public Boolean getDeletedWorkPlace() {
        return deletedWorkPlace;
    }

    public Company getCompany() {
        return companySuggestPickerField.getValue();
    }

    public void setDeletedWorkPlace(Boolean deletedWorkPlace) {
        this.deletedWorkPlace = deletedWorkPlace;
    }

    @Subscribe("endDateField")
    public void onEndDateFieldValueChange(HasValue.ValueChangeEvent<Date> event) {
        workPlaceGroupBox.setCaption(groupBoxSetHeader());

        if (this.candidateCVWorkPlaces != null) {
            if (event.getValue() != null) {
                this.candidateCVWorkPlaces.setEndDate(event.getValue());
            }
        }
    }

    @Subscribe("workToThisDayCheckBox")
    public void onWorkToThisDayCheckBoxValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        workPlaceGroupBox.setCaption(groupBoxSetHeader());

        if (event.getValue() != null) {
            if (event.getValue()) {
                endDateField.setEditable(false);
            } else {
                endDateField.setEditable(true);
            }
        } else {
            endDateField.setEditable(true);
        }

        if (this.candidateCVWorkPlaces != null) {
            if (event.getValue() != null) {
                this.candidateCVWorkPlaces.setWorkToThisDay(event.getValue());
            }
        }
    }

    @Subscribe("companySuggestPickerField")
    public void onCompanySuggestPickerFieldValueChange(HasValue.ValueChangeEvent<Company> event) {
        workPlaceGroupBox.setCaption(groupBoxSetHeader());
        setProjectImage(event.getValue());

        if (this.candidateCVWorkPlaces != null) {
            if (event.getValue() != null) {
                this.candidateCVWorkPlaces.setWorkPlace(event.getValue());
            }
        }
    }

    private void setProjectImage(Company company) {
        if (company != null) {
            if (company.getFileCompanyLogo() != null) {
                FileDescriptorResource resource = projectLogoImage.createResource(FileDescriptorResource.class)
                        .setFileDescriptor(company.getFileCompanyLogo());
                projectLogoImage.setSource(resource);
            } else {
                projectLogoImage.setSource(ThemeResource.class).setPath(StdImage.NO_COMPANY);
            }
        } else {
            projectLogoImage.setSource(ThemeResource.class).setPath(StdImage.NO_COMPANY);
        }
    }

    @Subscribe("startDateField")
    public void onStartDateFieldValueChange(HasValue.ValueChangeEvent<Date> event) {
        workPlaceGroupBox.setCaption(groupBoxSetHeader());

        if (this.candidateCVWorkPlaces != null) {
            if (event.getValue() != null) {
                this.candidateCVWorkPlaces.setStartDate(event.getValue());
            }
        }
    }

    private String groupBoxSetHeader() {
        StringBuilder sb = new StringBuilder();
        if (companySuggestPickerField.getValue() != null && startDateField.getValue() != null &&
                (endDateField.getValue() != null || workToThisDayCheckBox.getValue() == true)) {
            if (((Company) companySuggestPickerField.getValue()).getComanyName() != null) {
                sb.append(((Company) companySuggestPickerField.getValue()).getComanyName());
            }

            if (((Company) companySuggestPickerField.getValue()).getCompanyShortName() != null) {
                sb.append(" / ");
                sb.append(((Company) companySuggestPickerField.getValue()).getCompanyShortName());
            }

            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.YYYY");

            if ((startDateField.getValue() != null)
                    && (endDateField.getValue() != null)
                    && (workToThisDayCheckBox.getValue() != null)) {

                sb.append(" - c ")
                        .append(sdf.format(startDateField.getValue()))
                        .append(" по ")
                        .append(workToThisDayCheckBox.getValue() == true
                                ? messageBundle.getMessage("msgCurrentTime")
                                : sdf.format(endDateField.getValue()))
                        .toString();
            }
        }

        return sb.toString();
    }

    public Position getPosition() {
        return positionLookupPickerField.getValue();
    }

    public RichTextArea getAchievementsRichTextArea() {
        return achievementsRichTextArea;
    }

    public String getWorkPlaceComment() {
        return workPlaceCommentTextField.getValue();
    }

    public String getFunctionalityAtWork() {
        return functionalityAtWorkRichTextArea.getValue();
    }

    public String getAchievements() {
        return achievementsRichTextArea.getValue();
    }

    public Boolean getWorkToThisDay() {
        return workToThisDayCheckBox.getValue();
    }

    public Date getEndDate() {
        return endDateField.getValue();
    }

    public Date getStartDate() {
        return startDateField.getValue();
    }

    public String getPersonalRole() {
        return personalRoleRichTextArea.getValue();
    }

    public void setNewWorkPlace(CandidateCVWorkPlaces candidateCVWorkPlaces) {
        if (candidateCVWorkPlaces != null) {
            positionLookupPickerField.setValue(candidateCVWorkPlaces.getPosition());
            companySuggestPickerField.setValue(candidateCVWorkPlaces.getWorkPlace());
            startDateField.setValue(candidateCVWorkPlaces.getStartDate());
            endDateField.setValue(candidateCVWorkPlaces.getEndDate());
            workToThisDayCheckBox.setValue(candidateCVWorkPlaces.getWorkToThisDay());
            workPlaceCommentTextField.setValue(candidateCVWorkPlaces.getWorkPlaceComment());
            functionalityAtWorkRichTextArea.setValue(candidateCVWorkPlaces.getFunctionalityAtWork());
            personalRoleRichTextArea.setValue(candidateCVWorkPlaces.getPersonalRole());
            achievementsRichTextArea.setValue(candidateCVWorkPlaces.getAchievements());

            this.candidateCVWorkPlaces = candidateCVWorkPlaces;
            groupBoxSetHeader();

            workPlaceGroupBox.setCollapsable(true);
            workPlaceGroupBox.setStyleName("label_button_grey");

        } else {
            createCandidateCVWorkPlaces();
        }
    }

    public void setCandidateCV(CandidateCV candidateCV) {
        this.candidateCV = candidateCV;
        candidateCVWorkPlaces.setCandidateCV(this.candidateCV);
    }

    @Subscribe("workPlaceCommentTextField")
    public void onWorkPlaceCommentTextFieldValueChange(HasValue.ValueChangeEvent<String> event) {
        if (this.candidateCVWorkPlaces != null) {
            if (event.getValue() != null) {
                this.candidateCVWorkPlaces.setWorkPlaceComment(event.getValue());
            }
        }
    }

    @Subscribe("functionalityAtWorkRichTextArea")
    public void onFunctionalityAtWorkRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        if (this.candidateCVWorkPlaces != null) {
            if (event.getValue() != null) {
                this.candidateCVWorkPlaces.setFunctionalityAtWork(event.getValue());
            }
        }
    }

    @Subscribe("achievementsRichTextArea")
    public void onAchievementsRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        if (this.candidateCVWorkPlaces != null) {
            if (event.getValue() != null) {
                this.candidateCVWorkPlaces.setAchievements(event.getValue());
            }
        }
    }

    @Subscribe("personalRoleRichTextArea")
    public void onPersonalRoleRichTextAreaValueChange(HasValue.ValueChangeEvent<String> event) {
        if (this.candidateCVWorkPlaces != null) {
            if (event.getValue() != null) {
                this.candidateCVWorkPlaces.setPersonalRole(event.getValue());
            }
        }
    }

    public CandidateCVWorkPlaces getCandidateCVWorkPlaces() {
        this.candidateCVWorkPlaces.setPosition(positionLookupPickerField.getValue());
        this.candidateCVWorkPlaces.setWorkPlace(companySuggestPickerField.getValue());
        this.candidateCVWorkPlaces.setStartDate(startDateField.getValue());
        this.candidateCVWorkPlaces.setEndDate(endDateField.getValue());
        this.candidateCVWorkPlaces.setWorkToThisDay(workToThisDayCheckBox.getValue());
        this.candidateCVWorkPlaces.setWorkPlaceComment(workPlaceCommentTextField.getValue());
        this.candidateCVWorkPlaces.setFunctionalityAtWork(functionalityAtWorkRichTextArea.getValue());
        this.candidateCVWorkPlaces.setPersonalRole(personalRoleRichTextArea.getValue());
        this.candidateCVWorkPlaces.setAchievements(achievementsRichTextArea.getValue());

        return this.candidateCVWorkPlaces;
    }

    public void createCandidateCVWorkPlaces() {
        this.candidateCVWorkPlaces = metadata.create(CandidateCVWorkPlaces.class);
    }

    @Subscribe
    public void onInit(InitEvent event) {
        getScreenData().loadAll();
        deleteWorkPlaceButton.addClickListener(e -> deleteWorkPlaceButton());
        initCompanyField(); // TO-DO но не работают логотипы компаний на SuggestionPickerField
        initPositionTypeField();
        projectLogoImage.setSource(ThemeResource.class).setPath(StdImage.NO_COMPANY);
    }

    @Subscribe
    public void onAfterInit(AfterInitEvent event) {
        setProjectImage(this.candidateCVWorkPlaces.getWorkPlace());
    }

    @Subscribe("companySuggestPickerField")
    public void onCompanySuggestPickerFieldValueChange1(HasValue.ValueChangeEvent<Company> event) {

    }

    private void deleteWorkPlaceButton() {
        workPlaceGroupBox.setVisible(false);
        setDeletedWorkPlace(true);
    }


    private void initCompanyField() {
//        companySuggestPickerField.setOptionIconProvider(this::companyTypeFieldImageProvider);
    }

    private Resource companyTypeFieldImageProvider(Company company) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (company.getFileCompanyLogo() != null) {
            return retImage.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(company.getFileCompanyLogo());
        } else {
            retImage.setVisible(false);
            return retImage.createResource(ThemeResource.class).setPath(StdImage.NO_COMPANY);
        }
    }

    private void initPositionTypeField() {
        positionLookupPickerField.setOptionImageProvider(this::positionLookupPickerFieldImageProvider);
    }

    private Resource positionLookupPickerFieldImageProvider(Position position) {
        Image retImage = uiComponents.create(Image.class);
        retImage.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        retImage.setWidth("30px");

        if (position.getLogo() != null) {
            return retImage.createResource(FileDescriptorResource.class)
                    .setFileDescriptor(position
                            .getLogo());
        } else {
            retImage.setVisible(false);
            return retImage.createResource(ThemeResource.class).setPath(StdImage.NO_PROGRAMMER);
        }
    }

    @Install(to = "companySuggestPickerField", subject = "enterActionHandler")
    private void companySuggestPickerFieldEnterActionHandler(String s) {
        dialogs.createOptionDialog()
                .withCaption(messageBundle.getMessage("msgWarning"))
                .withMessage(String.format(messageBundle.getMessage("msgCreateNewCompany"), s))
                .withActions(new DialogAction(DialogAction.Type.YES, Action.Status.PRIMARY)
                        .withHandler(e -> {
                            screenBuilders.editor(Company.class, this)
                                    .newEntity().withScreenClass(CompanyEdit.class)
                                    .withInitializer(ev -> ev.setComanyName(s))
                                    .withAfterCloseListener(eacl -> companySuggestPickerField.setValue(eacl.getScreen().getEditedEntity()))
                                    .show();
                        }), new DialogAction(DialogAction.Type.NO))
                .show();
    }

}