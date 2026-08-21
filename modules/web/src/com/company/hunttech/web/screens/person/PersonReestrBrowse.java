package com.company.hunttech.web.screens.person;

import com.company.hunttech.entity.Person;
import com.hunttech.hrm.gui.components.OvaFallbackImage;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.gui.screen.LookupComponent;

import javax.inject.Inject;
import java.util.Set;

@UiController("hunttech_PersonReestr.browse")
@UiDescriptor("person-reestr-browse.xml")
@LookupComponent("personsTable")
@LoadDataBeforeShow
public class PersonReestrBrowse extends StandardLookup<Person> {

    @Inject
    private CollectionContainer<Person> personsDc;
    @Inject
    private CollectionLoader<Person> personsDl;
    @Inject
    private GroupTable<Person> personsTable;
    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;

    @Inject
    private OvaFallbackImage logoPic;
    @Inject
    private Label<String> detailTitle;
    @Inject
    private Label<String> detailSubtitle;
    @Inject
    private Label<String> detailLocation;
    @Inject
    private Button openEditCardBtn;
    @Inject
    private Label<String> detailPhone;
    @Inject
    private Label<String> detailMobPhone;
    @Inject
    private Label<String> detailEmail;
    @Inject
    private Label<String> detailTelegram;
    @Inject
    private Label<String> detailSkype;
    @Inject
    private Label<String> detailWhatsApp;
    @Inject
    private Label<String> detailCompany;
    @Inject
    private Label<String> detailDepartment;

    @Subscribe
    public void onInit(InitEvent event) {
        setupTableColumns();
        setupTableSelection();
        setupSidebarButtons();
    }

    private void setupTableColumns() {
        personsTable.addGeneratedColumn("personPicColumn", person -> {
            HBoxLayout retBox = uiComponents.create(HBoxLayout.class);
            retBox.setWidthFull();
            retBox.setHeightFull();
            retBox.setAlignment(Component.Alignment.MIDDLE_CENTER);

            Image image = uiComponents.create(Image.class);
            image.setScaleMode(Image.ScaleMode.SCALE_DOWN);
            image.setWidth("24px");
            image.setHeight("24px");
            image.setStyleName("circle-20px");
            image.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (person != null && person.getFileImageFace() != null) {
                image.setSource(FileDescriptorResource.class).setFileDescriptor(person.getFileImageFace());
            } else {
                image.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
            }

            retBox.add(image);
            return retBox;
        });

        personsTable.addGeneratedColumn("fullName", person -> {
            String name = (person != null && person.getInstanceName() != null) ? person.getInstanceName() : "Без имени";
            StringBuilder sub = new StringBuilder();
            if (person != null) {
                String phone = person.getPhone() != null ? person.getPhone() : person.getMobPhone();
                if (phone != null && !phone.trim().isEmpty()) {
                    sub.append("📞 ").append(phone.trim());
                }
                if (person.getEmail() != null && !person.getEmail().trim().isEmpty()) {
                    if (sub.length() > 0) sub.append(" • ");
                    sub.append("✉ ").append(person.getEmail().trim());
                }
                if (person.getTelegramName() != null && !person.getTelegramName().trim().isEmpty()) {
                    if (sub.length() > 0) sub.append(" • ");
                    sub.append("@").append(person.getTelegramName().trim());
                }
            }
            String textHtml = "<div style='text-align: left;'><div style='font-weight: 600; color: #1e293b; font-size: 13px; white-space: normal; word-break: break-word; line-height: 1.35;'>" + name + "</div>" +
                    (sub.length() > 0 ? "<div style='font-size: 11px; color: #64748b; white-space: normal; word-break: break-word; line-height: 1.35;'>" + sub.toString() + "</div>" : "") + "</div>";
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidth("100%");
            lbl.setValue(textHtml);
            return lbl;
        });

        personsTable.addGeneratedColumn("personPosition", person -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidthFull();
            String pos = (person != null && person.getPersonPosition() != null && person.getPersonPosition().getPositionRuName() != null)
                    ? person.getPersonPosition().getPositionRuName() : "Сотрудник";
            lbl.setValue("<span style='background: rgba(43, 130, 201, 0.12); color: #2b82c9; padding: 3px 8px; border-radius: 4px; font-weight: 600; font-size: 11px; display: inline-block; white-space: normal; word-break: break-word; line-height: 1.35; max-width: 100%;'>" + pos + "</span>");
            return lbl;
        });

        personsTable.addGeneratedColumn("companyDepartment", person -> {
            if (person == null || person.getCompanyDepartment() == null) {
                Label<String> plain = uiComponents.create(Label.NAME);
                plain.setHtmlEnabled(true);
                plain.setValue("<span style='color: #94a3b8; font-size: 11px;'>—</span>");
                return plain;
            }
            String depName = person.getCompanyDepartment().getDepartamentRuName() != null ? person.getCompanyDepartment().getDepartamentRuName() : "";
            String compName = (person.getCompanyDepartment().getCompanyName() != null && person.getCompanyDepartment().getCompanyName().getComanyName() != null)
                    ? person.getCompanyDepartment().getCompanyName().getComanyName() : "";
            String textHtml = "<div style='text-align: left;'><div style='font-weight: 600; color: #1e293b; font-size: 12.5px; white-space: normal; word-break: break-word; line-height: 1.35;'>" +
                    (!compName.isEmpty() ? "🏢 " + compName : (!depName.isEmpty() ? "📁 " + depName : "-")) + "</div>" +
                    (!compName.isEmpty() && !depName.isEmpty() ? "<div style='font-size: 11px; color: #64748b; white-space: normal; word-break: break-word; line-height: 1.35;'>📁 " + depName + "</div>" : "") + "</div>";
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            lbl.setWidth("100%");
            lbl.setValue(textHtml);
            return lbl;
        });

        personsTable.addGeneratedColumn("cityOfResidence", person -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String city = (person != null && person.getCityOfResidence() != null && person.getCityOfResidence().getCityRuName() != null)
                    ? person.getCityOfResidence().getCityRuName() : "—";
            lbl.setValue("<span style='font-size: 12px; color: #334155;'>" + ("—".equals(city) ? "—" : "📍 " + city) + "</span>");
            return lbl;
        });

        personsTable.addGeneratedColumn("positionCountry", person -> {
            Label<String> lbl = uiComponents.create(Label.NAME);
            lbl.setHtmlEnabled(true);
            String country = (person != null && person.getPositionCountry() != null && person.getPositionCountry().getCountryRuName() != null)
                    ? person.getPositionCountry().getCountryRuName() : "—";
            lbl.setValue("<span style='font-size: 12px; color: #334155;'>" + ("—".equals(country) ? "—" : "🌍 " + country) + "</span>");
            return lbl;
        });
    }

    private void setupTableSelection() {
        personsTable.addSelectionListener(e -> {
            Set<Person> selected = e.getSelected();
            if (selected != null && !selected.isEmpty()) {
                Person single = selected.iterator().next();
                updateSidebarDetails(single);
            } else {
                clearSidebarDetails();
            }
        });
    }

    private void setupSidebarButtons() {
        openEditCardBtn.addClickListener(e -> {
            Person selected = personsTable.getSingleSelected();
            if (selected != null) {
                screenBuilders.editor(personsTable)
                        .editEntity(selected)
                        .withOpenMode(OpenMode.DIALOG)
                        .show();
            }
        });
    }

    @Subscribe("actionsPopupButton.refreshAction")
    public void onRefreshAction(Action.ActionPerformedEvent event) {
        personsDl.load();
    }

    @Subscribe("actionsPopupButton.excelExportAction")
    public void onExcelExportAction(Action.ActionPerformedEvent event) {
        Action excel = personsTable.getAction("excel");
        if (excel != null) {
            excel.actionPerform(personsTable);
        }
    }

    @Subscribe(id = "personsDl", target = Target.DATA_LOADER)
    private void onPersonsDlPostLoad(CollectionLoader.PostLoadEvent<Person> event) {
        Person current = personsTable.getSingleSelected();
        if (current != null) {
            updateSidebarDetails(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            personsTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebarDetails();
        }
    }

    private void updateSidebarDetails(Person person) {
        openEditCardBtn.setEnabled(true);

        // Фото персоны
        if (person.getFileImageFace() != null) {
            logoPic.setSource(FileDescriptorResource.class).setFileDescriptor(person.getFileImageFace());
        } else {
            logoPic.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        }

        // Заголовки
        detailTitle.setValue(person.getInstanceName());

        if (person.getPersonPosition() != null) {
            detailSubtitle.setValue(person.getPersonPosition().getPositionRuName());
        } else {
            detailSubtitle.setValue("-");
        }

        StringBuilder loc = new StringBuilder();
        if (person.getCityOfResidence() != null) loc.append(person.getCityOfResidence().getCityRuName());
        if (person.getPositionCountry() != null) {
            if (loc.length() > 0) loc.append(", ");
            loc.append(person.getPositionCountry().getCountryRuName());
        }
        detailLocation.setValue(loc.length() > 0 ? loc.toString() : "-");

        // Контакты
        detailPhone.setValue(person.getPhone() != null ? person.getPhone() : "-");
        detailMobPhone.setValue(person.getMobPhone() != null ? person.getMobPhone() : "-");
        detailEmail.setValue(person.getEmail() != null ? person.getEmail() : "-");
        detailTelegram.setValue(person.getTelegramName() != null ? "@" + person.getTelegramName() : "-");
        detailSkype.setValue(person.getSkypeName() != null ? person.getSkypeName() : "-");
        detailWhatsApp.setValue(person.getWatsupName() != null ? person.getWatsupName() : "-");

        // Компания и подразделение
        if (person.getCompanyDepartment() != null) {
            if (person.getCompanyDepartment().getCompanyName() != null) {
                detailCompany.setValue(person.getCompanyDepartment().getCompanyName().getComanyName());
            } else {
                detailCompany.setValue("-");
            }
            detailDepartment.setValue(person.getCompanyDepartment().getDepartamentRuName());
        } else {
            detailCompany.setValue("Компания не указана");
            detailDepartment.setValue("-");
        }
    }

    private void clearSidebarDetails() {
        openEditCardBtn.setEnabled(false);
        logoPic.setSource(ThemeResource.class).setPath("icons/no-programmer.jpeg");
        detailTitle.setValue("Выберите человека");
        detailSubtitle.setValue("-");
        detailLocation.setValue("-");
        detailPhone.setValue("-");
        detailMobPhone.setValue("-");
        detailEmail.setValue("-");
        detailTelegram.setValue("-");
        detailSkype.setValue("-");
        detailWhatsApp.setValue("-");
        detailCompany.setValue("-");
        detailDepartment.setValue("-");
    }
}
