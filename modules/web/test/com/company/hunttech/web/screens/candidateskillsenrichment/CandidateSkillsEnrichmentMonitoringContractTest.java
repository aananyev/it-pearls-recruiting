package com.company.hunttech.web.screens.candidateskillsenrichment;

import com.company.hunttech.entity.CandidateCvSkillAnalysis;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Контрактные тесты экрана мониторинга фонового определения навыков.
 */
class CandidateSkillsEnrichmentMonitoringContractTest {

    private File resolveFile(String path) {
        File file = new File(path);
        if (file.exists()) {
            return file;
        }
        if (path.startsWith("modules/web/")) {
            File subFile = new File(path.substring("modules/web/".length()));
            if (subFile.exists()) {
                return subFile;
            }
        }
        return file;
    }

    private Document parseXml(File file) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(file);
    }

    @Test
    void testMonitoringScreenXmlStructure() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/candidateskillsenrichment/candidate-skills-enrichment-monitoring.xml");
        assertTrue(xmlFile.exists(), "candidate-skills-enrichment-monitoring.xml должен существовать");

        Document doc = parseXml(xmlFile);

        // 1. Проверка наличия DataGrid таблицы истории
        NodeList tables = doc.getElementsByTagName("dataGrid");
        boolean tableFound = false;
        for (int i = 0; i < tables.getLength(); i++) {
            Element el = (Element) tables.item(i);
            if ("analysesTable".equals(el.getAttribute("id"))) {
                tableFound = true;
                assertEquals("analysesDc", el.getAttribute("dataContainer"));
                break;
            }
        }
        assertTrue(tableFound, "analysesTable должен присутствовать с привязкой к analysesDc");

        // 2. Проверка KPI панели
        NodeList hboxes = doc.getElementsByTagName("hbox");
        boolean kpiFound = false;
        for (int i = 0; i < hboxes.getLength(); i++) {
            Element el = (Element) hboxes.item(i);
            if ("kpiPanel".equals(el.getAttribute("id"))) {
                kpiFound = true;
                break;
            }
        }
        assertTrue(kpiFound, "kpiPanel должен присутствовать в XML");

        // 3. Проверка кнопок управления воркером
        NodeList buttons = doc.getElementsByTagName("button");
        boolean startBtn = false, stopBtn = false, runOnceBtn = false;
        for (int i = 0; i < buttons.getLength(); i++) {
            Element el = (Element) buttons.item(i);
            String id = el.getAttribute("id");
            if ("startWorkerBtn".equals(id)) startBtn = true;
            if ("stopWorkerBtn".equals(id)) stopBtn = true;
            if ("runOnceBtn".equals(id)) runOnceBtn = true;
        }
        assertTrue(startBtn, "startWorkerBtn должен присутствовать");
        assertTrue(stopBtn, "stopWorkerBtn должен присутствовать");
        assertTrue(runOnceBtn, "runOnceBtn должен присутствовать");

        // 4. Проверка наличия колонки priority в analysesTable
        NodeList columns = doc.getElementsByTagName("column");
        boolean priorityColFound = false;
        for (int i = 0; i < columns.getLength(); i++) {
            Element col = (Element) columns.item(i);
            if ("priority".equals(col.getAttribute("id"))) {
                priorityColFound = true;
                break;
            }
        }
        assertTrue(priorityColFound, "Колонка priority должна присутствовать в analysesTable");

        // 5. Проверка сортировки с приоритетом в analysesDl
        NodeList queries = doc.getElementsByTagName("query");
        assertTrue(queries.getLength() > 0, "Запрос analysesDl должен присутствовать");
        String queryText = queries.item(0).getTextContent();
        assertTrue(queryText.contains("priority"), "Запрос analysesDl должен учитывать priority");

        NodeList collections = doc.getElementsByTagName("collection");
        boolean browseViewFound = false;
        for (int i = 0; i < collections.getLength(); i++) {
            Element collection = (Element) collections.item(i);
            if ("analysesDc".equals(collection.getAttribute("id"))) {
                browseViewFound = "candidateCvSkillAnalysis-browse-view".equals(collection.getAttribute("view"));
                break;
            }
        }
        assertTrue(browseViewFound, "analysesDc должен загружать metadata через browse-view");

        String controller = new String(Files.readAllBytes(resolveFile(
                "modules/web/src/com/company/hunttech/web/screens/candidateskillsenrichment/CandidateSkillsEnrichmentMonitoring.java").toPath()),
                StandardCharsets.UTF_8);
        assertTrue(controller.contains("analysesDl.load()"),
                "Refresh должен повторно загружать записи с provider/model metadata");
    }

    @Test
    void testDeltaDialogXmlStructure() throws Exception {
        File xmlFile = resolveFile("modules/web/src/com/company/hunttech/web/screens/candidateskillsenrichment/candidate-skill-delta-dialog.xml");
        assertTrue(xmlFile.exists(), "candidate-skill-delta-dialog.xml должен существовать");

        Document doc = parseXml(xmlFile);
        NodeList flowBoxes = doc.getElementsByTagName("flowBox");
        boolean added = false, updated = false, unchanged = false, skipped = false;
        for (int i = 0; i < flowBoxes.getLength(); i++) {
            Element el = (Element) flowBoxes.item(i);
            String id = el.getAttribute("id");
            if ("addedFlow".equals(id)) added = true;
            if ("updatedFlow".equals(id)) updated = true;
            if ("unchangedFlow".equals(id)) unchanged = true;
            if ("skippedFlow".equals(id)) skipped = true;
        }
        assertTrue(added, "addedFlow должен присутствовать");
        assertTrue(updated, "updatedFlow должен присутствовать");
        assertTrue(unchanged, "unchangedFlow должен присутствовать");
        assertTrue(skipped, "skippedFlow должен присутствовать");
    }

    @Test
    void testProviderModelRendererDistinguishesAiFallbackAndLegacyMetadata() {
        CandidateCvSkillAnalysis ai = new CandidateCvSkillAnalysis();
        ai.setProviderCode("openrouter");
        ai.setModelName("model-x");
        assertEquals("openrouter / model-x", CandidateSkillsEnrichmentMonitoring.formatProviderAndModel(ai));

        CandidateCvSkillAnalysis fallback = new CandidateCvSkillAnalysis();
        fallback.setExecutionSource(CandidateCvSkillAnalysis.EXECUTION_SOURCE_DICTIONARY_FALLBACK);
        assertEquals("Fallback: справочник", CandidateSkillsEnrichmentMonitoring.formatProviderAndModel(fallback));

        CandidateCvSkillAnalysis providerOnly = new CandidateCvSkillAnalysis();
        providerOnly.setExecutionSource(CandidateCvSkillAnalysis.EXECUTION_SOURCE_AI_METADATA_INCOMPLETE);
        providerOnly.setProviderCode("openrouter");
        assertEquals("AI: openrouter / модель недоступна",
                CandidateSkillsEnrichmentMonitoring.formatProviderAndModel(providerOnly));

        CandidateCvSkillAnalysis modelOnly = new CandidateCvSkillAnalysis();
        modelOnly.setExecutionSource(CandidateCvSkillAnalysis.EXECUTION_SOURCE_AI_METADATA_INCOMPLETE);
        modelOnly.setModelName("model-x");
        assertEquals("AI: провайдер недоступен / model-x",
                CandidateSkillsEnrichmentMonitoring.formatProviderAndModel(modelOnly));

        CandidateCvSkillAnalysis legacy = new CandidateCvSkillAnalysis();
        assertEquals("Метаданные недоступны", CandidateSkillsEnrichmentMonitoring.formatProviderAndModel(legacy));
    }
}
