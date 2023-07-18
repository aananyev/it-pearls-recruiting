package com.company.itpearls.web.screens.laboragreement;

import com.haulmont.cuba.core.entity.Entity;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.Component;
import com.haulmont.cuba.gui.components.HBoxLayout;
import com.haulmont.cuba.gui.components.Label;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.icons.CubaIcon;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.screen.*;
import com.company.itpearls.entity.LaborAgreement;
import org.apache.xmlbeans.impl.xb.xmlschema.impl.LangAttributeImpl;

import javax.inject.Inject;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

@UiController("itpearls_LaborAgreement.browse")
@UiDescriptor("labor-agreement-browse.xml")
@LookupComponent("laborAgreementsTable")
@LoadDataBeforeShow
public class LaborAgreementBrowse extends StandardLookup<LaborAgreement> {
    @Inject
    private MessageBundle messageBundle;
    @Inject
    private RadioButtonGroup employeeOrCompanyRadioButton;
    @Inject
    private CollectionLoader<LaborAgreement> laborAgreementsDl;
    @Inject
    private UiComponents uiComponents;

    @Subscribe
    public void onInit(InitEvent event) {
        setEnployeeOrCompanyRadioButton();
    }

    private void setEnployeeOrCompanyRadioButton() {
        Map<String, Integer> employeeOrCustomerMap = new LinkedHashMap<>();
        employeeOrCustomerMap.put(messageBundle.getMessage("msgAllAgreements"), 0);
        employeeOrCustomerMap.put(messageBundle.getMessage(AgreementType.MSG_EMPLOYEE),
                AgreementType.EPLOYEE);
        employeeOrCustomerMap.put(messageBundle.getMessage(AgreementType.MSG_COMPANY),
                AgreementType.COMPANY);

        employeeOrCompanyRadioButton.setOptionsMap(employeeOrCustomerMap);
        employeeOrCompanyRadioButton.addValueChangeListener(e -> {
            if (employeeOrCompanyRadioButton.getValue() == null) {
                laborAgreementsDl.removeParameter("laborAgreementType");
            } else {
                laborAgreementsDl.setParameter("laborAgreementType",
                        employeeOrCompanyRadioButton.getValue());
            }

            laborAgreementsDl.load();
        });
    }

    public Component perhapsColumnGenerator(LaborAgreement entity) {
        HBoxLayout retHBox = uiComponents.create(HBoxLayout.class);
        retHBox.setWidthFull();
        retHBox.setHeightFull();

        Label retLabel = uiComponents.create(Label.class);

        retLabel.setWidthAuto();
        retLabel.setHeightAuto();
        retLabel.setAlignment(Component.Alignment.MIDDLE_CENTER);

        if (checkAgreementEndDate(entity)) {
            retLabel.setIconFromSet(CubaIcon.PLUS_CIRCLE);
            retLabel.setStyleName("open-position-pic-center-large-green");
        } else {
            retLabel.setIconFromSet(CubaIcon.MINUS_CIRCLE);
            retLabel.setStyleName("open-position-pic-center-large-red");
        }

        retHBox.add(retLabel);

        return retHBox;
    }

    private boolean checkAgreementEndDate(LaborAgreement entity) {
        Date currentDate = new Date();

        if (entity.getClosed() != null) {
            if (entity.getClosed()) {
                return false;
            }
        }

        if (currentDate.after(entity.getAgreementDate()) && entity.getPerpetualAgreement()) {
            return true;
        }

        if (currentDate.after(entity.getAgreementDate()) && currentDate.before(entity.getAgreementEndDate())) {
            return true;
        }

        return false;
    }
}