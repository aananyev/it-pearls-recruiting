package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.TestEntityTracker;
import com.company.hunttech.entity.City;
import com.company.hunttech.entity.Country;
import com.company.hunttech.entity.Region;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Моделирует detached-сценарии гео-форм HRM HuntTech (CountryEdit, RegionEdit,
 * CityEdit): открытие формы с detached-сущностью через edit-view, изменение и
 * commit detached-объекта, вложенные FK/композиции (country -> region -> city).
 * Покрывает все поля, которые формы привязывают в XML.
 */
public class GeolocationEditFormsDetachedObjectTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private DataManager dataManager;
    private TestEntityTracker tracker;

    @Before
    public void setUp() {
        dataManager = AppBeans.get(DataManager.class);
        tracker = new TestEntityTracker(dataManager);
    }

    @After
    public void tearDown() {
        tracker.cleanup();
    }

    @Test
    public void countryEditViewProvidesEveryFormField() {
        Country country = createCountry();
        Region region = createRegion(country);
        UUID countryId = country.getId();

        // Форма CountryEdit открывается с detached country-edit-view.
        Country detached = dataManager.load(Country.class)
                .id(countryId)
                .view("country-edit-view")
                .one();

        assertEquals(country.getCountryRuName(), detached.getCountryRuName());
        assertEquals(country.getCountryShortName(), detached.getCountryShortName());
        assertEquals(country.getPhoneCode(), detached.getPhoneCode());

        // Composition-коллекция регионов загружена в том же view, как в таблице формы.
        List<Region> regions = detached.getCountryOfRegion();
        assertNotNull("country-edit-view обязан содержать countryOfRegion", regions);
        assertTrue("В country-edit-view не подтянулся привязанный регион", regions.size() >= 1);
        assertEquals(region.getRegionRuName(), regions.get(0).getRegionRuName());
    }

    @Test
    public void modifyDetachedCountryAndCommit() {
        Country country = createCountry();
        UUID id = country.getId();

        // Форма открыта: detached-объект, пользователь меняет поля.
        Country detached = dataManager.load(Country.class)
                .id(id)
                .view("country-edit-view")
                .one();
        detached.setCountryShortName("R2");
        detached.setPhoneCode(888);
        dataManager.commit(detached);

        Country reloaded = dataManager.load(Country.class)
                .id(id)
                .view("country-edit-view")
                .one();
        assertEquals("R2", reloaded.getCountryShortName());
        assertEquals(Integer.valueOf(888), reloaded.getPhoneCode());
    }

    @Test
    public void regionEditViewProvidesCountryAndCities() {
        Country country = createCountry();
        Region region = createRegion(country);
        City city = createCity(region);
        UUID regionId = region.getId();

        // Форма RegionEdit открывается с detached region-edit-view.
        Region detached = dataManager.load(Region.class)
                .id(regionId)
                .view("region-edit-view")
                .one();

        assertEquals(region.getRegionRuName(), detached.getRegionRuName());
        assertEquals(region.getRegionCode(), detached.getRegionCode());

        // FK regionCountry подгружен с country-picker-view (поле формы + sidebar).
        assertNotNull("region-edit-view обязан содержать regionCountry", detached.getRegionCountry());
        assertEquals(country.getCountryRuName(), detached.getRegionCountry().getCountryRuName());

        // Composition regionOfCity подгружена (таблица «Города региона»).
        List<City> cities = detached.getRegionOfCity();
        assertNotNull("region-edit-view обязан содержать regionOfCity", cities);
        assertTrue("В region-edit-view не подтянулся привязанный город", cities.size() >= 1);
        assertEquals(city.getCityRuName(), cities.get(0).getCityRuName());
    }

    @Test
    public void cityEditViewProvidesRegionForSidebarAndPicker() {
        Country country = createCountry();
        Region region = createRegion(country);
        City city = createCity(region);
        UUID cityId = city.getId();

        // Форма CityEdit открывается с detached city-edit-view.
        City detached = dataManager.load(City.class)
                .id(cityId)
                .view("city-edit-view")
                .one();

        assertEquals(city.getCityRuName(), detached.getCityRuName());
        assertEquals(city.getCityPhoneCode(), detached.getCityPhoneCode());

        // FK cityRegion подгружен с region-picker-view: поле формы + sidebar
        // (sidebar читает cityRegion.regionRuName — свойство обязано быть в view).
        assertNotNull("city-edit-view обязан содержать cityRegion", detached.getCityRegion());
        assertEquals(region.getRegionRuName(), detached.getCityRegion().getRegionRuName());
    }

    @Test
    public void regionAddedToDetachedCountryComposition() {
        Country country = createCountry();

        // Detached-страна из формы: добавляем регион в composition-коллекцию и коммитим.
        Country detached = dataManager.load(Country.class)
                .id(country.getId())
                .view("country-edit-view")
                .one();

        Region newRegion = dataManager.create(Region.class);
        newRegion.setRegionRuName("TestRegion-" + UUID.randomUUID());
        newRegion.setRegionCode(uniqueCode());
        newRegion.setRegionCountry(detached);
        tracker.track(dataManager.commit(newRegion, "region-edit-view"));

        // После commit страна обязана видеть новый регион в своей коллекции.
        Country reloaded = dataManager.load(Country.class)
                .id(country.getId())
                .view("country-edit-view")
                .one();
        boolean found = false;
        for (Region r : reloaded.getCountryOfRegion()) {
            if (r.getId().equals(newRegion.getId())) {
                found = true;
            }
        }
        assertTrue("Добавленный регион не появился в countryOfRegion", found);
    }

    private Country createCountry() {
        Country country = dataManager.create(Country.class);
        country.setCountryRuName("TestCountry-" + UUID.randomUUID());
        country.setCountryShortName("T" + Math.abs(UUID.randomUUID().hashCode() % 9 + 1));
        country.setPhoneCode(999);
        return tracker.track(dataManager.commit(country, "country-edit-view"));
    }

    private Region createRegion(Country country) {
        Region region = dataManager.create(Region.class);
        region.setRegionRuName("TestRegion-" + UUID.randomUUID());
        region.setRegionCode(uniqueCode());
        region.setRegionCountry(country);
        return tracker.track(dataManager.commit(region, "region-edit-view"));
    }

    private City createCity(Region region) {
        City city = dataManager.create(City.class);
        city.setCityRuName("TestCity-" + UUID.randomUUID());
        city.setCityPhoneCode(uniquePhoneCode());
        city.setCityRegion(region);
        return tracker.track(dataManager.commit(city, "city-edit-view"));
    }

    private Integer uniqueCode() {
        return Math.abs(UUID.randomUUID().hashCode() % 900000) + 100000;
    }

    private String uniquePhoneCode() {
        return String.valueOf(Math.abs(UUID.randomUUID().hashCode() % 90000) + 10000);
    }
}
