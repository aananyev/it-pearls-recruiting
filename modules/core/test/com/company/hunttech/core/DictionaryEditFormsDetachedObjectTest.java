package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.TestEntityTracker;
import com.company.hunttech.entity.Currency;
import com.company.hunttech.entity.EmployeeWorkStatus;
import com.company.hunttech.entity.FileType;
import com.company.hunttech.entity.Grade;
import com.company.hunttech.entity.OutstaffingRates;
import com.company.hunttech.entity.SignIcons;
import com.company.hunttech.entity.SocialNetworkType;
import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Моделирует detached-сценарии справочных Edit-форм HRM HuntTech (FileTypeEdit,
 * SocialNetworkTypeEdit, GradeEdit, CurrencyEdit, OutstaffingRatesEdit,
 * EmployeeWorkStatusEdit, SignIconsEdit): открытие формы с detached-сущностью
 * через штатные view (те же, что привязаны в XML), изменение и commit
 * detached-объекта, FK-связь outstaffingRates -> currency.
 * Покрывает все поля, которые формы привязывают в XML.
 */
public class DictionaryEditFormsDetachedObjectTest {

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
    public void fileTypeEditViewProvidesEveryFormField() {
        FileType fileType = createFileType();
        UUID id = fileType.getId();

        // Форма FileTypeEdit открывается с detached fileType-view.
        FileType detached = dataManager.load(FileType.class)
                .id(id)
                .view("fileType-view")
                .one();

        assertEquals(fileType.getNameFileType(), detached.getNameFileType());
        assertEquals(fileType.getDecriptionFileType(), detached.getDecriptionFileType());

        // Пользователь меняет поля формы и коммитит detached-объект.
        detached.setNameFileType("Резюме");
        detached.setDecriptionFileType("Файлы резюме кандидатов");
        dataManager.commit(detached);

        FileType reloaded = dataManager.load(FileType.class)
                .id(id)
                .view("fileType-view")
                .one();
        assertEquals("Резюме", reloaded.getNameFileType());
        assertEquals("Файлы резюме кандидатов", reloaded.getDecriptionFileType());
    }

    @Test
    public void socialNetworkTypeEditViewProvidesEveryFormField() {
        SocialNetworkType social = createSocialNetworkType();
        UUID id = social.getId();

        // Форма SocialNetworkTypeEdit открывается с detached socialNetworkType-view;
        // view включает логотип (свойство logo, используемое OvaFallbackImage и upload).
        SocialNetworkType detached = dataManager.load(SocialNetworkType.class)
                .id(id)
                .view("socialNetworkType-view")
                .one();

        assertEquals(social.getSocialNetwork(), detached.getSocialNetwork());
        assertEquals(social.getSocialNetworkURL(), detached.getSocialNetworkURL());
        assertEquals(social.getComment(), detached.getComment());
        assertNotNull("socialNetworkType-view обязан содержать logo", detached.getLogo());

        detached.setSocialNetwork("LinkedIn");
        detached.setSocialNetworkURL("https://linkedin.com");
        detached.setComment("Деловая соцсеть");
        dataManager.commit(detached);

        SocialNetworkType reloaded = dataManager.load(SocialNetworkType.class)
                .id(id)
                .view("socialNetworkType-view")
                .one();
        assertEquals("LinkedIn", reloaded.getSocialNetwork());
        assertEquals("https://linkedin.com", reloaded.getSocialNetworkURL());
        assertEquals("Деловая соцсеть", reloaded.getComment());
    }

    @Test
    public void gradeEditViewProvidesEveryFormField() {
        Grade grade = createGrade();
        UUID id = grade.getId();

        // Форма GradeEdit открывается с detached grade-edit-view.
        Grade detached = dataManager.load(Grade.class)
                .id(id)
                .view("grade-edit-view")
                .one();

        assertEquals(grade.getGradeName(), detached.getGradeName());

        detached.setGradeName("Senior+");
        dataManager.commit(detached);

        Grade reloaded = dataManager.load(Grade.class)
                .id(id)
                .view("grade-edit-view")
                .one();
        assertEquals("Senior+", reloaded.getGradeName());
    }

    @Test
    public void currencyEditViewProvidesEveryFormField() {
        Currency currency = createCurrency();
        UUID id = currency.getId();

        // Форма CurrencyEdit открывается с detached currency-view.
        Currency detached = dataManager.load(Currency.class)
                .id(id)
                .view("currency-view")
                .one();

        assertEquals(currency.getCurrencyLongName(), detached.getCurrencyLongName());
        assertEquals(currency.getCurrencyShortName(), detached.getCurrencyShortName());

        detached.setCurrencyLongName("Российский рубль");
        dataManager.commit(detached);

        Currency reloaded = dataManager.load(Currency.class)
                .id(id)
                .view("currency-view")
                .one();
        assertEquals("Российский рубль", reloaded.getCurrencyLongName());
    }

    @Test
    public void outstaffingRatesEditViewProvidesCurrencyFk() {
        Currency currency = createCurrency();
        OutstaffingRates rate = createOutstaffingRates(currency);
        UUID id = rate.getId();

        // Форма OutstaffingRatesEdit открывается с detached outstaffingRates-view;
        // view включает FK currency (поле picker + sidebar читает название валюты).
        OutstaffingRates detached = dataManager.load(OutstaffingRates.class)
                .id(id)
                .view("outstaffingRates-view")
                .one();

        assertEquals(0, rate.getRate().compareTo(detached.getRate()));
        assertEquals(0, rate.getMinSalary().compareTo(detached.getMinSalary()));
        assertEquals(0, rate.getMaxSalary().compareTo(detached.getMaxSalary()));
        assertEquals(0, rate.getMaxIESalary().compareTo(detached.getMaxIESalary()));
        assertNotNull("outstaffingRates-view обязан содержать currency", detached.getCurrency());
        assertEquals(currency.getCurrencyLongName(), detached.getCurrency().getCurrencyLongName());
        assertEquals(rate.getComment(), detached.getComment());

        detached.setRate(new BigDecimal("1550.00"));
        detached.setComment("Обновлённая ставка");
        dataManager.commit(detached);

        OutstaffingRates reloaded = dataManager.load(OutstaffingRates.class)
                .id(id)
                .view("outstaffingRates-view")
                .one();
        assertEquals(0, new BigDecimal("1550.00").compareTo(reloaded.getRate()));
        assertEquals("Обновлённая ставка", reloaded.getComment());
    }

    @Test
    public void employeeWorkStatusEditViewProvidesEveryFormField() {
        EmployeeWorkStatus status = createEmployeeWorkStatus();
        UUID id = status.getId();

        // Форма EmployeeWorkStatusEdit открывается с detached employeeWorkStatus-view.
        EmployeeWorkStatus detached = dataManager.load(EmployeeWorkStatus.class)
                .id(id)
                .view("employeeWorkStatus-view")
                .one();

        assertEquals(status.getWorkStatusName(), detached.getWorkStatusName());
        assertEquals(status.getInStaff(), detached.getInStaff());

        detached.setWorkStatusName("Совместитель");
        detached.setInStaff(Boolean.FALSE);
        dataManager.commit(detached);

        EmployeeWorkStatus reloaded = dataManager.load(EmployeeWorkStatus.class)
                .id(id)
                .view("employeeWorkStatus-view")
                .one();
        assertEquals("Совместитель", reloaded.getWorkStatusName());
        assertEquals(Boolean.FALSE, reloaded.getInStaff());
    }

    @Test
    public void signIconsEditViewProvidesEveryFormField() {
        SignIcons icon = createSignIcons();
        UUID id = icon.getId();

        // Форма SignIconsEdit открывается с detached signIcons-view.
        SignIcons detached = dataManager.load(SignIcons.class)
                .id(id)
                .view("signIcons-view")
                .one();

        assertEquals(icon.getTitleEnd(), detached.getTitleEnd());
        assertEquals(icon.getTitleRu(), detached.getTitleRu());
        assertEquals(icon.getTitleDescription(), detached.getTitleDescription());
        assertEquals(icon.getIconName(), detached.getIconName());
        assertEquals(icon.getIconColor(), detached.getIconColor());

        detached.setTitleRu("Опыт");
        detached.setTitleDescription("Признак опыта работы");
        dataManager.commit(detached);

        SignIcons reloaded = dataManager.load(SignIcons.class)
                .id(id)
                .view("signIcons-view")
                .one();
        assertEquals("Опыт", reloaded.getTitleRu());
        assertEquals("Признак опыта работы", reloaded.getTitleDescription());
    }

    private FileType createFileType() {
        FileType fileType = dataManager.create(FileType.class);
        fileType.setNameFileType("Doc-" + shortUuid());
        fileType.setDecriptionFileType("Тестовый тип файла");
        return tracker.track(dataManager.commit(fileType, "fileType-view"));
    }

    private SocialNetworkType createSocialNetworkType() {
        // Логотип соцсети: запись FileDescriptor без файла в storage — достаточно
        // для detached-проверки view socialNetworkType-view (свойство logo).
        FileDescriptor logo = dataManager.create(FileDescriptor.class);
        logo.setName("sn-" + shortUuid() + ".png");
        logo.setExtension("png");
        logo.setSize(1024L);
        tracker.track(dataManager.commit(logo));

        SocialNetworkType social = dataManager.create(SocialNetworkType.class);
        social.setSocialNetwork("Net-" + shortUuid());
        social.setSocialNetworkURL("https://example-" + shortUuid() + ".com");
        social.setComment("Тестовая соцсеть");
        social.setLogo(logo);
        return tracker.track(dataManager.commit(social, "socialNetworkType-view"));
    }

    private Grade createGrade() {
        Grade grade = dataManager.create(Grade.class);
        grade.setGradeName("Grade-" + shortUuid());
        return tracker.track(dataManager.commit(grade, "grade-edit-view"));
    }

    private Currency createCurrency() {
        Currency currency = dataManager.create(Currency.class);
        currency.setCurrencyLongName("Валюта-" + shortUuid());
        currency.setCurrencyShortName(shortCode3());
        return tracker.track(dataManager.commit(currency, "currency-view"));
    }

    private OutstaffingRates createOutstaffingRates(Currency currency) {
        OutstaffingRates rate = dataManager.create(OutstaffingRates.class);
        rate.setRate(new BigDecimal("1200.00"));
        rate.setMinSalary(new BigDecimal("80000.00"));
        rate.setMaxSalary(new BigDecimal("120000.00"));
        rate.setMaxIESalary(new BigDecimal("100000.00"));
        rate.setCurrency(currency);
        rate.setComment("Тестовый рейт");
        return tracker.track(dataManager.commit(rate, "outstaffingRates-view"));
    }

    private EmployeeWorkStatus createEmployeeWorkStatus() {
        EmployeeWorkStatus status = dataManager.create(EmployeeWorkStatus.class);
        status.setWorkStatusName("Статус-" + shortUuid());
        status.setInStaff(Boolean.TRUE);
        return tracker.track(dataManager.commit(status, "employeeWorkStatus-view"));
    }

    private SignIcons createSignIcons() {
        SignIcons icon = dataManager.create(SignIcons.class);
        icon.setTitleEnd("end-" + shortUuid());
        icon.setTitleRu("Признак-" + shortUuid());
        icon.setTitleDescription("Тестовая иконка признака");
        icon.setIconName("STAR");
        icon.setIconColor("#ffb11b");
        return tracker.track(dataManager.commit(icon, "signIcons-view"));
    }

    private String shortUuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String shortCode3() {
        // Детерминированный 3-символьный код: T + 2 цифры из энтропии UUID
        // (старая реализация давала StringIndexOutOfBounds при hashCode%90+10 < 10
        // и коллизии из-за слабой энтропии Math.abs(hashCode()%90)).
        String hex = UUID.randomUUID().toString().replace("-", "");
        int v = (Character.digit(hex.charAt(0), 16) * 16 + Character.digit(hex.charAt(1), 16)) % 100;
        return String.format("T%02d", v);
    }
}
