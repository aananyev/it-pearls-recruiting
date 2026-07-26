package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.haulmont.chile.core.model.MetaClass;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.Metadata;
import org.junit.ClassRule;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Защищает единую metadata-модель HRM HuntTech от повторного появления
 * legacy entities и таблиц после завершённой миграции production-базы.
 */
public class EntityNamespaceIntegrityTest {

    private static final String LEGACY_ENTITY_PREFIX = "it" + "pearls_";
    private static final String LEGACY_TABLE_PREFIX = "it" + "pearls_";

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    @Test
    public void registeredProjectEntitiesUseHunttechNamespace() {
        Metadata metadata = AppBeans.get(Metadata.class);
        List<String> legacyMetadataClasses = new ArrayList<>();
        List<String> invalidProjectNamespaces = new ArrayList<>();
        int projectEntityCount = 0;

        for (MetaClass metaClass : metadata.getSession().getClasses()) {
            String metaName = metaClass.getName();
            if (metaName.startsWith(LEGACY_ENTITY_PREFIX)) {
                legacyMetadataClasses.add(metaName);
            }

            Class<?> javaClass = metaClass.getJavaClass();
            if (javaClass != null && javaClass.getName().startsWith("com.company.hunttech.entity.")) {
                projectEntityCount++;
                if (!metaName.startsWith("hunttech_")) {
                    invalidProjectNamespaces.add(javaClass.getName() + " -> " + metaName);
                }
            }
        }

        assertTrue("В runtime metadata не обнаружены project entities HRM HuntTech", projectEntityCount > 0);
        assertTrue("Runtime metadata содержит legacy entities: " + legacyMetadataClasses,
                legacyMetadataClasses.isEmpty());
        assertTrue("Project entities зарегистрированы вне namespace hunttech: " + invalidProjectNamespaces,
                invalidProjectNamespaces.isEmpty());
    }

    @Test
    public void metadataDescriptorRegistersOnlyHunttechModel() throws IOException {
        String metadataXml = readProjectFile("modules/global/src/com/company/hunttech/metadata.xml");

        assertTrue(metadataXml.contains("root-package=\"com.company.hunttech\""));
        assertTrue(metadataXml.contains("namespace=\"hunttech\""));
        assertFalse(metadataXml.contains("root-package=\"com.company.itpearls\""));
        assertFalse(metadataXml.contains("namespace=\"itpearls\""));
    }

    @Test
    public void runtimeSourcesDoNotReferenceLegacyEntitiesOrTables() throws IOException {
        List<String> violations = findLegacyRuntimeReferences();

        assertTrue("Рабочие исходники содержат legacy entity/table references: " + violations,
                violations.isEmpty());
    }

    /**
     * Проверяет только runtime source roots. Исторические DB update, deployment и rollback
     * артефакты намеренно не входят в scan, поскольку должны распознавать старые имена.
     */
    private List<String> findLegacyRuntimeReferences() throws IOException {
        Path root = findProjectRoot();
        List<Path> sourceRoots = Arrays.asList(
                root.resolve("modules/global/src"),
                root.resolve("modules/core/src"),
                root.resolve("modules/web/src")
        );
        List<String> violations = new ArrayList<>();

        for (Path sourceRoot : sourceRoots) {
            if (!Files.exists(sourceRoot)) {
                continue;
            }

            try (Stream<Path> files = Files.walk(sourceRoot)) {
                files.filter(Files::isRegularFile)
                        .filter(this::isRuntimeTextFile)
                        .forEach(path -> inspectRuntimeSource(root, path, violations));
            }
        }

        return violations;
    }

    private boolean isRuntimeTextFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".java")
                || name.endsWith(".xml")
                || name.endsWith(".properties");
    }

    /**
     * Ищет контракты сущностей, JPQL и таблиц, не запрещая legacy screen IDs,
     * message keys и другие идентификаторы, которые не являются metadata-name сущности.
     */
    private void inspectRuntimeSource(Path root, Path path, List<String> violations) {
        try {
            String relativePath = root.relativize(path).toString().replace('\\', '/');
            String lowerPath = relativePath.toLowerCase(Locale.ROOT);
            String source = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
            String normalized = source.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);

            if (lowerPath.contains("/com/company/itpearls/")) {
                violations.add(relativePath + ": legacy package path");
            }

            List<String> forbiddenContracts = Arrays.asList(
                    "@entity(name = \"" + LEGACY_ENTITY_PREFIX,
                    "@entity(name=\"" + LEGACY_ENTITY_PREFIX,
                    "@table(name = \"" + LEGACY_TABLE_PREFIX,
                    "@table(name=\"" + LEGACY_TABLE_PREFIX,
                    " from " + LEGACY_ENTITY_PREFIX,
                    " join " + LEGACY_ENTITY_PREFIX,
                    " update " + LEGACY_ENTITY_PREFIX,
                    " delete from " + LEGACY_ENTITY_PREFIX,
                    "entity=\"" + LEGACY_ENTITY_PREFIX,
                    "entity = \"" + LEGACY_ENTITY_PREFIX,
                    "class=\"" + LEGACY_ENTITY_PREFIX,
                    "class = \"" + LEGACY_ENTITY_PREFIX,
                    "getclassnn(\"" + LEGACY_ENTITY_PREFIX,
                    "getclassnn( \"" + LEGACY_ENTITY_PREFIX,
                    "getclass(\"" + LEGACY_ENTITY_PREFIX,
                    "getclass( \"" + LEGACY_ENTITY_PREFIX
            );

            for (String forbiddenContract : forbiddenContracts) {
                if (normalized.contains(forbiddenContract)) {
                    violations.add(relativePath + ": " + forbiddenContract.trim());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось проверить runtime source " + path, e);
        }
    }

    private String readProjectFile(String relativePath) throws IOException {
        return new String(Files.readAllBytes(findProjectRoot().resolve(relativePath)), StandardCharsets.UTF_8);
    }

    private Path findProjectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertNotNull("Не найден корень проекта для проверки namespace HRM HuntTech", root);
        return root;
    }
}
