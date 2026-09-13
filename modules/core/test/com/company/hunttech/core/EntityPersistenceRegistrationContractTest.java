package com.company.hunttech.core;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;

/**
 * Контрактный тест целостности метаданных CUBA Platform:
 * Проверяет, что каждая JPA-сущность (@Entity / @Table) в модуле global
 * зарегистрирована в modules/global/src/com/company/hunttech/persistence.xml.
 *
 * Предотвращает ошибки вида:
 * "IllegalArgumentException: MetaClass not found for hunttech_..."
 * при старте блока hrm-core.
 */
public class EntityPersistenceRegistrationContractTest {

    private static final Pattern ENTITY_ANNOTATION = Pattern.compile("@Entity\\b");
    private static final Pattern TABLE_ANNOTATION = Pattern.compile("@Table\\s*\\(");
    private static final Pattern PACKAGE_PATTERN = Pattern.compile("package\\s+([a-zA-Z0-9_.]+);");
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("public\\s+(?:class|enum)\\s+([a-zA-Z0-9_]+)");

    private File resolveFile(String relativePath) {
        File file = new File(relativePath);
        if (file.exists()) {
            return file;
        }
        File subFile = new File("../../" + relativePath);
        if (subFile.exists()) {
            return subFile;
        }
        File coreFile = new File("../" + relativePath);
        if (coreFile.exists()) {
            return coreFile;
        }
        return file;
    }

    @Test
    public void testAllJpaEntitiesRegisteredInPersistenceXml() throws Exception {
        File persistenceFile = resolveFile("modules/global/src/com/company/hunttech/persistence.xml");
        assertTrue("Файл persistence.xml должен существовать", persistenceFile.exists());

        String persistenceXml = new String(Files.readAllBytes(persistenceFile.toPath()), StandardCharsets.UTF_8);

        File entityDir = resolveFile("modules/global/src/com/company/hunttech/entity");
        assertTrue("Каталог entity должен существовать", entityDir.exists());

        List<File> javaFiles = new ArrayList<>();
        collectJavaFiles(entityDir, javaFiles);

        List<String> missingEntities = new ArrayList<>();

        for (File f : javaFiles) {
            String content = new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);

            // Проверяем, является ли класс JPA сущностью (@Table или @Entity)
            boolean isJpaEntity = TABLE_ANNOTATION.matcher(content).find() ||
                    (ENTITY_ANNOTATION.matcher(content).find() && !content.contains("@MappedSuperclass"));

            if (isJpaEntity) {
                String pkg = extractGroup(content, PACKAGE_PATTERN);
                String className = extractGroup(content, CLASS_NAME_PATTERN);

                if (pkg != null && className != null) {
                    String fullFqcn = pkg + "." + className;
                    String requiredEntry = "<class>" + fullFqcn + "</class>";
                    if (!persistenceXml.contains(requiredEntry)) {
                        missingEntities.add(fullFqcn);
                    }
                }
            }
        }

        assertTrue("Следующие JPA-сущности отсутствуют в persistence.xml (добавьте их во избежание MetaClass not found):\n" +
                String.join("\n", missingEntities), missingEntities.isEmpty());
    }

    private void collectJavaFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                collectJavaFiles(file, result);
            } else if (file.getName().endsWith(".java")) {
                result.add(file);
            }
        }
    }

    private String extractGroup(String text, Pattern pattern) {
        Matcher m = pattern.matcher(text);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
}
