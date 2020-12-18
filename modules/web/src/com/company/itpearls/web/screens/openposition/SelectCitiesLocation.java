package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.City;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.TwinColumn;
import com.haulmont.cuba.gui.components.data.Options;
import com.haulmont.cuba.gui.screen.*;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@UiController("itpearls_SelectCitiesLocation")
@UiDescriptor("select-cities-location.xml")
public class SelectCitiesLocation extends Screen {
    @Inject
    private DataManager dataManager;
    @Inject
    private TwinColumn<City> citiesLocationTwinColumn;

    private List<City> cities = new ArrayList<>();
    private List<City> cityList = new ArrayList<>();

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        cities = dataManager.load(City.class)
                .query("select e from itpearls_City e order by e.cityRuName")
                .list();

        citiesLocationTwinColumn.setOptionsList(cities);
    }

    @Subscribe("closeBtn")
    public void onCloseBtnClick(Button.ClickEvent event) {
        closeWithDefaultAction();
    }

    public void setCitiesList(List<City> citiesList) {
        this.cityList = citiesList;
        citiesLocationTwinColumn.setValue(cityList);
    }

    public List<City> getCitiesList() {
        cityList = (List<City>) citiesLocationTwinColumn.getValue();
        return this.cityList;
    }
}