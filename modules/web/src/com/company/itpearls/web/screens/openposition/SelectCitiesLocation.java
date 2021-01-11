package com.company.itpearls.web.screens.openposition;

import com.company.itpearls.entity.City;
import com.company.itpearls.entity.OpenPosition;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.Sort;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.HasValue;
import com.haulmont.cuba.gui.components.RadioButtonGroup;
import com.haulmont.cuba.gui.components.TwinColumn;
import com.haulmont.cuba.gui.components.data.Options;
import com.haulmont.cuba.gui.model.InstanceLoader;
import com.haulmont.cuba.gui.screen.*;
import groovy.lang.MetaClass;

import javax.inject.Inject;
import javax.swing.text.html.parser.Entity;
import java.util.*;

@UiController("itpearls_SelectCitiesLocation")
@UiDescriptor("select-cities-location.xml")
public class SelectCitiesLocation extends Screen {
    @Inject
    private DataManager dataManager;
    @Inject
    private TwinColumn<City> citiesLocationTwinColumn;

    private List<City> cities = new ArrayList<>();
    private List<City> cityList = new ArrayList<>();
    @Inject
    private RadioButtonGroup countries;
    @Inject
    private InstanceLoader<OpenPosition> citiesDl;

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        cities = dataManager.load(City.class)
                .query("select e from itpearls_City e order by e.cityRuName")
                .list();

        citiesLocationTwinColumn.setOptionsList(cities);
        radioButtonInit();
    }

    private void radioButtonInit() {
        Map<String, Integer> priorityMap = new LinkedHashMap<>();

        priorityMap.put("Все", 0);
        priorityMap.put("Россия", 1);
//        priorityMap.put("Россия, московское время", 2);

        countries.setOptionsMap(priorityMap);
    }

    @Subscribe("countries")
    public void onCountriesValueChange(HasValue.ValueChangeEvent event) {
        String query = "select e from itpearls_City e " +
                "where e.cityRegion.regionCountry.countryRuName = :countries " +
                "order by e.cityRuName";

        switch ((int)countries.getValue()) {
            case 0:
                cities = dataManager.load(City.class)
                        .query("select e from itpearls_City e order by e.cityRuName")
                        .list();
                break;
            case 1:
                cities = dataManager.load(City.class)
                        .query(query)
                        .parameter("countries", "Россия")
                        .view("city-view")
                        .list();
                break;
        }

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
        return cityList;
    }
}