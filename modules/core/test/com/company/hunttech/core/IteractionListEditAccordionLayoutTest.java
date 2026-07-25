package com.company.hunttech.core;

import org.junit.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/** Защищает responsive-контракт пяти аккордеонов и двух основных picker-полей. */
public class IteractionListEditAccordionLayoutTest {
    @Test public void defaultDescriptorContainsNavigationAndFiveAccordions() throws Exception {
        Path descriptorPath = projectRoot().resolve("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(descriptorPath.toFile());
        String descriptor = readProjectFile("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        assertTrue(descriptor.contains("id="iteractionListNavigation""));
        assertTrue(descriptor.contains("id="participantsAccordion""));
        assertTrue(descriptor.contains("id="interactionAccordion""));
        assertTrue(descriptor.contains("id="resultAccordion""));
        assertTrue(descriptor.contains("id="commentAccordion""));
        assertTrue(descriptor.contains("id="popularAccordion""));
        assertEquals(5, count(descriptor, "stylename="iteraction-list-nav-item"));
    }
    @Test public void candidateAndVacancyShareStyleAndFitOneRow() throws IOException {
        String descriptor = readProjectFile("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        String participants = descriptor.substring(descriptor.indexOf("id="participantsAccordion""), descriptor.indexOf("id="interactionAccordion""));
        assertTrue(participants.contains("<columns count="2"/>"));
        assertEquals(2, count(participants, "stylename="iteraction-list-primary-picker""));
        assertTrue(descriptor.contains("width="1100"/>"));
        assertTrue(descriptor.contains("width="228px""));
    }
    @Test public void onlyFirstAccordionIsExpandedInitially() throws IOException {
        String d = readProjectFile("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        assertTrue(section(d,"id="participantsAccordion"","id="interactionAccordion"").contains("collapsed="false""));
        assertTrue(section(d,"id="interactionAccordion"","id="resultAccordion"").contains("collapsed="true""));
        assertTrue(section(d,"id="resultAccordion"","id="commentAccordion"").contains("collapsed="true""));
        assertTrue(section(d,"id="commentAccordion"","id="popularAccordion"").contains("collapsed="true""));
        assertTrue(d.substring(d.indexOf("id="popularAccordion"")).contains("collapsed="true""));
    }
    @Test public void businessBindingsAndActionsRemainAvailable() throws IOException {
        String d = readProjectFile("modules/web/src/com/company/hunttech/web/screens/iteractionlist/iteraction-list-edit.xml");
        assertOrdered(d,"id="candidateField"","id="vacancyFiels"","id="iteractionTypeField"","id="buttonsPanelCallAction"","id="ratingField"","id="recrutierField"","id="communicationMethodField"","id="commentField"","id="mostPopularHbox"");
        assertTrue(d.contains("invoke="callActionEntity""));
        assertTrue(d.contains("invoke="onButtonSubscribeClick""));
        assertTrue(d.contains("action="windowCommitAndClose""));
    }
    @Test public void allThemesConstrainWidthInsideLocalRoot() throws IOException {
        String[] themes={"halo","havana","helium","hover","hunttech-modern","hunttech-modern-light","hunttech-modern-dark"};
        for(String theme:themes){String s=readProjectFile("modules/web/themes/"+theme+"/com.company.hunttech/iteraction-list-accordion-navigation.scss");assertTrue(s.contains(".iteraction-list-editor"));assertTrue(s.contains(".iteraction-list-primary-picker"));assertTrue(s.contains("max-width: 20% !important"));assertTrue(s.contains("overflow-x: hidden !important"));}
    }
    private String section(String t,String s,String e){return t.substring(t.indexOf(s),t.indexOf(e,t.indexOf(s)));}
    private int count(String t,String token){int r=0,i=0;while((i=t.indexOf(token,i))>=0){r++;i+=token.length();}return r;}
    private void assertOrdered(String d,String...m){int p=-1;for(String x:m){int c=d.indexOf(x);assertTrue(c>=0);assertTrue(c>p);p=c;}}
    private String readProjectFile(String p)throws IOException{return new String(Files.readAllBytes(projectRoot().resolve(p)),StandardCharsets.UTF_8);}
    private Path projectRoot(){Path r=Paths.get(System.getProperty("user.dir",".")).toAbsolutePath();while(r!=null&&!Files.exists(r.resolve("build.gradle"))){r=r.getParent();}assertNotNull("Не найден корень проекта HRM HuntTech",r);return r;}
}
