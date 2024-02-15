package com.company.itpearls.web.screens.candidatecv;

import com.company.itpearls.core.StdImage;
import com.company.itpearls.core.TextManipulationService;
import com.haulmont.cuba.core.app.FileStorageService;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.FileStorageException;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.icons.Icons;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.CandidateCV;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.security.global.UserSession;

import javax.inject.Inject;
import java.util.Base64;
import java.util.Date;

@UiController("itpearls_CandidateCV.browse")
@UiDescriptor("candidate-cv-browse.xml")
@LookupComponent("candidateCVsTable")
@LoadDataBeforeShow
public class CandidateCVBrowse extends StandardLookup<CandidateCV> {
    @Inject
    private CollectionLoader<CandidateCV> candidateCVsDl;
    @Inject
    private CheckBox checkBoxSetOnlyMy;
    @Inject
    private UserSession userSession;

    static String GROUP_INTERN = "Стажер";
    @Inject
    private UiComponents uiComponents;
    @Inject
    private FileStorageService fileStorageService;
    @Inject
    private TextManipulationService textManipulationService;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        if (userSession.getUser().getGroup().getName().equals(GROUP_INTERN)) {
            candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
            candidateCVsDl.load();

            checkBoxSetOnlyMy.setValue(true);
            checkBoxSetOnlyMy.setEditable(false);
        }
    }

    @Subscribe("checkBoxSetOnlyMy")
    public void onCheckBoxSetOnlyMyValueChange(HasValue.ValueChangeEvent<Boolean> event) {
        if (checkBoxSetOnlyMy.getValue()) {
            candidateCVsDl.setParameter("userName", "%" + userSession.getUser().getLogin() + "%");
        } else {
            candidateCVsDl.removeParameter("userName");
        }

        candidateCVsDl.load();
    }

    @Install(to = "candidateCVsTable.originalFileCVcolumn", subject = "columnGenerator")
    private Icons.Icon candidateCVsTableOriginalFileCVcolumnColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        if (!(event.getItem().getOriginalFileCV() == null &&
                event.getItem().getLinkOriginalCv() == null )) {
            return CubaIcon.FILE_TEXT;
        } else {
            return CubaIcon.FILE_TEXT_O;
        }
    }

    @Install(to = "candidateCVsTable.itPearlsCVcolumn", subject = "columnGenerator")
    private Icons.Icon candidateCVsTableItPearlsCVcolumnColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        if (!(event.getItem().getLinkItPearlsCV() == null)) {
            return CubaIcon.FILE_TEXT;
        } else {
            return CubaIcon.FILE_TEXT_O;
        }
    }

    @Install(to = "candidateCVsTable.itPearlsCVcolumn", subject = "styleProvider")
    private String candidateCVsTableItPearlsCVcolumnStyleProvider(CandidateCV candidateCV) {
        if (!(candidateCV.getLinkItPearlsCV() == null)) {
            return "open-position-pic-center-large-green";
        } else {
            return "open-position-pic-center-large-red";
        }
    }

    @Install(to = "candidateCVsTable.originalFileCVcolumn", subject = "styleProvider")
    private String candidateCVsTableOriginalFileCVcolumnStyleProvider(CandidateCV candidateCV) {
        if (!(candidateCV.getOriginalFileCV() == null &&
                candidateCV.getLinkOriginalCv() == null)) {
            return "open-position-pic-center-large-green";
        } else {
            return "open-position-pic-center-large-red";
        }
    }

    @Install(to = "candidateCVsTable.cvReady", subject = "columnGenerator")
    private Icons.Icon candidateCVsTableCvReadyColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        return CubaIcon.PENCIL_SQUARE_O;
    }

    @Install(to = "candidateCVsTable.cvReady", subject = "styleProvider")
    private String candidateCVsTableCvReadyStyleProvider(CandidateCV candidateCV) {
        if (candidateCV.getLetter() != null &&
                candidateCV.getTextCV() != null &&
                candidateCV.getLinkItPearlsCV() != null) {
            return "open-position-pic-center-large-green";
        } else {
            if ((candidateCV.getLetter() == null ||
                    candidateCV.getLinkItPearlsCV() == null ) &&
                    candidateCV.getOriginalFileCV() != null ) {
                return "open-position-pic-center-large-yellow";
            } else {
                return "open-position-pic-center-large-red";
            }
        }
    }

    @Install(to = "candidateCVsTable.resumePosition", subject = "columnGenerator")
    private Component candidateCVsTableResumePositionColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);
        retHbox.setWidthFull();
        retHbox.setHeightFull();
        retHbox.setSpacing(true);

        Image logoImage = uiComponents.create(Image.class);
        logoImage.setWidth("20px");
        logoImage.setStyleName("circle-20px-noborder");
        logoImage.setScaleMode(Image.ScaleMode.FILL);
        logoImage.setAlignment(Component.Alignment.MIDDLE_LEFT);

        if (event.getItem().getResumePosition() != null) {
            if (event.getItem().getResumePosition().getLogo() != null) {
                try {
                    logoImage
                            .setSource(FileDescriptorResource.class)
                            .setFileDescriptor(event.getItem().getResumePosition().getLogo());
                } catch (Exception e) {
                    e.printStackTrace();
                    logoImage.setVisible(false);
                }
            } else {
                logoImage.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
                logoImage.setVisible(false);
            }
        } else {
            logoImage.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            logoImage.setVisible(false);
        }

        Label positionLabel = uiComponents.create(Label.class);
        positionLabel.setWidthAuto();
        positionLabel.setHeightAuto();
        positionLabel.setAlignment(Component.Alignment.MIDDLE_LEFT);
        positionLabel.setValue(event.getItem().getResumePosition());

        retHbox.add(logoImage);
        retHbox.add(positionLabel);
        retHbox.expand(positionLabel);

        return retHbox;
    }

    private HBoxLayout columnGeneratorImageText(FileDescriptor fileDescriptor,
                                                String text,
                                                String defaultImage,
                                                String imageWidth) {
        HBoxLayout retHbox = uiComponents.create(HBoxLayout.class);
        retHbox.setWidth("100%");
        retHbox.setHeight("100%");
        retHbox.setSpacing(true);
        retHbox.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Image logoImage = uiComponents.create(Image.class);
        logoImage.setWidth(imageWidth);
        logoImage.setHeight(imageWidth);
        logoImage.setStyleName(new StringBuilder("circle-").append(imageWidth).append("-white-border").toString());
        logoImage.setScaleMode(Image.ScaleMode.FILL);
        logoImage.setAlignment(Component.Alignment.MIDDLE_CENTER);
        logoImage.setDescriptionAsHtml(true);
        logoImage.setDescription(textManipulationService.getImage(fileDescriptor));

        if (fileDescriptor != null) {
            try {
                logoImage
                        .setSource(FileDescriptorResource.class)
                        .setFileDescriptor(fileDescriptor);
            } catch (Exception e) {
                e.printStackTrace();

                logoImage.setSource(ThemeResource.class).setPath(defaultImage);
                logoImage.setVisible(true);
            }
        } else {
            logoImage.setSource(ThemeResource.class).setPath(defaultImage);
            logoImage.setVisible(true);
        }

        Label companyLabel = uiComponents.create(Label.class);
        companyLabel.setValue(text);
        companyLabel.setWidthAuto();
        companyLabel.setHeightAuto();
        companyLabel.setStyleName("table-wordwrap");
        companyLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        companyLabel.setDescriptionAsHtml(true);
        companyLabel.setDescription(textManipulationService.getImage(fileDescriptor));

        retHbox.add(logoImage);
        retHbox.add(companyLabel);
        retHbox.expand(companyLabel);

        return retHbox;

    }
    @Install(to = "candidateCVsTable.fullName", subject = "columnGenerator")
    private Component candidateCVsTableFullNameColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        return columnGeneratorImageText(event.getItem().getFileImageFace(),
                event.getItem().getCandidate().getFullName(),
                StdImage.NO_PROGRAMMER,
                "30px");
    }
    @Install(to = "candidateCVsTable.vacansyName", subject = "columnGenerator")
    private Component candidateCVsTableVacansyNameColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        if (event.getItem().getToVacancy() != null) {
            FileDescriptor fileDescriptor = null;
            if (event.getItem().getToVacancy() != null) {
                if (event.getItem().getToVacancy().getProjectName() != null) {
                    fileDescriptor = event.getItem().getToVacancy().getProjectName().getProjectLogo();
                }
            }

            StringBuilder stringBuilder = new StringBuilder();

            if (event.getItem().getToVacancy() != null) {
                stringBuilder.append(event.getItem().getToVacancy().getVacansyName());
            }

            return columnGeneratorImageText(fileDescriptor,
                    stringBuilder.toString(),
                    StdImage.NO_PROGRAMMER,
                    "30px");
        } else {
            return uiComponents.create(Label.class);
        }
    }

    @Install(to = "candidateCVsTable.owner", subject = "columnGenerator")
    private Component candidateCVsTableOwnerColumnGenerator(DataGrid.ColumnGeneratorEvent<CandidateCV> event) {
        FileDescriptor fileDescriptor = null;
        StringBuilder stringBuilder = new StringBuilder();

        if (event.getItem().getOwner() != null) {
            fileDescriptor = event.getItem().getOwner().getFileImageFace();
        }

        if (event.getItem().getOwner() != null) {
            stringBuilder.append(event.getItem().getOwner().getName());
        }

        return columnGeneratorImageText(fileDescriptor,
                stringBuilder.toString(),
                StdImage.NO_PROGRAMMER,
                "30px");
    }
}