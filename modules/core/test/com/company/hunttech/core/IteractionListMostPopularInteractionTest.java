package com.company.hunttech.core;
import org.junit.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;
/** Защищает годовой контракт пяти быстрых кнопок взаимодействий. */
public class IteractionListMostPopularInteractionTest {
    @Test public void queryUsesCurrentUserAndRollingYear() throws IOException {String c=controller();assertTrue(c.contains("POPULAR_INTERACTION_BUTTONS = 5"));assertTrue(c.contains("calendar.add(Calendar.YEAR, -1)"));assertTrue(c.contains("e.recrutier = :user"));assertTrue(c.contains("e.dateIteraction between :periodStart and :periodEnd"));assertTrue(c.contains(".parameter("user", userSession.getUser())"));}
    @Test public void exactlyFiveEqualButtonsAreCreated() throws IOException {String c=controller();assertTrue(c.contains("index < POPULAR_INTERACTION_BUTTONS"));assertTrue(c.contains("mostPopularHbox.removeAll()"));assertTrue(c.contains("popularButton.setWidth("100%")"));assertTrue(c.contains("mostPopularHbox.expand(popularButtons.toArray(new Component[0]))"));assertTrue(c.contains("configureEmptyPopularButton"));}
    @Test public void clickAssignsExactInteractionWithoutCaptionParsing() throws IOException {String c=controller();assertTrue(c.contains("iteractionTypeField.setValue(interaction)"));assertTrue(c.contains("iteractionTypeField.focus()"));assertFalse(c.contains("getCaption().substring"));assertFalse(c.contains("setCaptionAsHtml(true)"));}
    @Test public void descriptorAndScssProtectFiveButtonGeometry() throws IOException {String d=read("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");String s=read("modules/web/themes/halo/com.company.hunttech/iteraction-list-accordion-navigation.scss");assertTrue(d.contains("stylename="iteraction-list-popular-buttons""));assertTrue(s.contains("max-width: 20% !important"));assertTrue(s.contains(".iteraction-list-popular-button"));}
    private String controller()throws IOException{return read("modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");}
    private String read(String p)throws IOException{return new String(Files.readAllBytes(root().resolve(p)),StandardCharsets.UTF_8);}
    private Path root(){Path r=Paths.get(System.getProperty("user.dir",".")).toAbsolutePath();while(r!=null&&!Files.exists(r.resolve("build.gradle"))){r=r.getParent();}assertNotNull(r);return r;}
}
