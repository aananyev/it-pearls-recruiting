package com.company.hunttech.core;
import org.junit.Test;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static org.junit.Assert.*;
/** Защищает кликабельный индекс default-экрана IteractionListEdit. */
public class IteractionListAccordionNavigationTest {
    @Test public void defaultControllerCreatesFiveNavigationButtons() throws IOException {String c=controller();assertTrue(c.contains("@UiController("hunttech_IteractionList.edit")"));assertTrue(c.contains("iteractionListNavigation.removeAll()"));assertTrue(c.contains("participantsAccordionNav"));assertTrue(c.contains("popularAccordionNav"));assertEquals(5,count(c,"addExpandedStateChangeListener"));}
    @Test public void navigationFocusesFirstFieldOfEachInputBlock() throws IOException {String c=controller();assertTrue(c.contains("candidateField::focus"));assertTrue(c.contains("iteractionTypeField::focus"));assertTrue(c.contains("ratingField::focus"));assertTrue(c.contains("commentField::focus"));assertTrue(c.contains("popularAccordion.setExpanded(popularAccordion == selectedAccordion)"));}
    @Test public void navigationMethodsDoNotWriteEntityOrRunQueries() throws IOException {String c=controller();String n=c.substring(c.indexOf("private void initAccordionNavigation()"),c.indexOf("private static final String QUERY_CHAIN_LAST"));assertFalse(n.contains("getEditedEntity()"));assertFalse(n.contains("dataManager"));assertFalse(n.contains("commit("));assertFalse(n.contains("setValue("));}
    @Test public void compatibilityControllerIsThinAliasWithUniqueId() throws IOException {String a=read("modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEditAccordionNavigation.java");assertTrue(a.contains("@UiController("hunttech_IteractionList.edit.accordion")"));assertTrue(a.contains("@UiDescriptor("iteraction-list-edit.xml")"));assertTrue(a.contains("extends IteractionListEdit"));assertFalse(a.contains("@Subscribe"));}
    private String controller()throws IOException{return read("modules/web/src/com/company/hunttech/web/screens/iteractionlist/IteractionListEdit.java");}
    private int count(String t,String token){int r=0,i=0;while((i=t.indexOf(token,i))>=0){r++;i+=token.length();}return r;}
    private String read(String p)throws IOException{return new String(Files.readAllBytes(root().resolve(p)),StandardCharsets.UTF_8);}
    private Path root(){Path r=Paths.get(System.getProperty("user.dir",".")).toAbsolutePath();while(r!=null&&!Files.exists(r.resolve("build.gradle"))){r=r.getParent();}assertNotNull(r);return r;}
}
