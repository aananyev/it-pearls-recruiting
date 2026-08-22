package com.company.hunttech.core;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Архитектурный контрактный тест: предотвращает ошибки {@code DevelopmentException: Unable to find an instance of type 'interface ...'}.
 * Сканирует все UI-контроллеры экранов в модуле {@code app-web}, находит все инжекции ({@code @Inject}) сервисов
 * из пакетов {@code com.company.hunttech.service.*} и {@code com.company.hunttech.core.*} и проверяет, что каждый
 * middleware-сервис зарегистрирован в {@code web-spring.xml} в секции {@code remoteServices}.
 */
public class WebServiceProxyRegistryContractTest {

    private static final String WEB_SPRING_PATH = "modules/web/src/com/company/hunttech/web-spring.xml";
    private static final String WEB_SCREENS_DIR = "modules/web/src/com/company/hunttech/web/screens";
    private static final String GLOBAL_SERVICE_DIR = "modules/global/src/com/company/hunttech/service";

    private static final Pattern INJECT_FIELD_PATTERN = Pattern.compile(
            "@Inject\\s+(?:private|protected|public)?\\s+([A-Za-z0-9_]+(?:<[^>]+>)?)\\s+([A-Za-z0-9_]+);",
            Pattern.MULTILINE);

    private static final Pattern IMPORT_PATTERN = Pattern.compile(
            "import\\s+(com\\.company\\.hunttech\\.(?:service|core|app)\\.[A-Za-z0-9_]+);");

    private static final Pattern WEB_SPRING_ENTRY_PATTERN = Pattern.compile(
            "<entry\\s+key=\"([^\"]+)\"\\s+value=\"([^\"]+)\"\\s*/>");

    @Test
    public void testAllInjectedServicesAreRegisteredInWebSpring() throws IOException {
        Path root = projectRoot();
        Path webSpringPath = root.resolve(WEB_SPRING_PATH);
        assertTrue("web-spring.xml должен существовать", Files.exists(webSpringPath));

        String webSpringXml = new String(Files.readAllBytes(webSpringPath), StandardCharsets.UTF_8);
        Set<String> registeredServiceClasses = extractRegisteredServiceClasses(webSpringXml);

        // Находим все глобальные интерфейсы сервисов
        Set<String> globalServiceSimpleNames = findGlobalServiceSimpleNames(root);

        // Сканируем все web-контроллеры
        Path webScreensPath = root.resolve(WEB_SCREENS_DIR);
        List<Path> screenJavaFiles = Files.walk(webScreensPath)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

        List<String> missingRegistrations = new ArrayList<>();

        for (Path screenFile : screenJavaFiles) {
            String javaContent = new String(Files.readAllBytes(screenFile), StandardCharsets.UTF_8);
            Map<String, String> importsMap = extractImports(javaContent);

            Matcher matcher = INJECT_FIELD_PATTERN.matcher(javaContent);
            while (matcher.find()) {
                String typeName = matcher.group(1);
                String fieldName = matcher.group(2);

                // Проверяем, является ли тип сервисом
                if (typeName.endsWith("Service") || globalServiceSimpleNames.contains(typeName)) {
                    String fullClassName = importsMap.get(typeName);
                    if (fullClassName == null && typeName.contains(".")) {
                        fullClassName = typeName;
                    }

                    // Если сервис из глобального или core пакета (не CUBA-стандартный UI компонент)
                    if (fullClassName != null && isMiddlewareServicePackage(fullClassName)) {
                        if (!registeredServiceClasses.contains(fullClassName)) {
                            String relPath = root.relativize(screenFile).toString();
                            missingRegistrations.add(String.format(
                                    "В экране '%s' поле '@Inject private %s %s;' требует регистрации сервиса '%s' в %s",
                                    relPath, typeName, fieldName, fullClassName, WEB_SPRING_PATH));
                        }
                    } else if (fullClassName == null && globalServiceSimpleNames.contains(typeName)) {
                        // Сервис из глобального пакета без явного импорта (в том же пакете или import .*)
                        String inferredClass = "com.company.hunttech.service." + typeName;
                        if (!registeredServiceClasses.contains(inferredClass)) {
                            String relPath = root.relativize(screenFile).toString();
                            missingRegistrations.add(String.format(
                                    "В экране '%s' поле '@Inject private %s %s;' требует регистрации '%s' в %s",
                                    relPath, typeName, fieldName, inferredClass, WEB_SPRING_PATH));
                        }
                    }
                }
            }
        }

        if (!missingRegistrations.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append("\nОбнаружены незарегистрированные middleware-сервисы в web-spring.xml:\n");
            for (String err : missingRegistrations) {
                sb.append("  - ").append(err).append("\n");
            }
            sb.append("\nДля исправления добавьте соответствующие <entry key=\"...\" value=\"...\"/> в web-spring.xml в блок hunttech_proxyCreator.\n");
            fail(sb.toString());
        }
    }

    private boolean isMiddlewareServicePackage(String fullClassName) {
        return (fullClassName.startsWith("com.company.hunttech.service.") ||
                fullClassName.startsWith("com.company.hunttech.core.") ||
                fullClassName.startsWith("com.company.hunttech.app."))
                && !fullClassName.startsWith("com.company.hunttech.web.");
    }

    private Set<String> extractRegisteredServiceClasses(String webSpringXml) {
        Set<String> set = new HashSet<>();
        Matcher m = WEB_SPRING_ENTRY_PATTERN.matcher(webSpringXml);
        while (m.find()) {
            set.add(m.group(2).trim());
        }
        return set;
    }

    private Map<String, String> extractImports(String javaContent) {
        Map<String, String> imports = new HashMap<>();
        Matcher m = IMPORT_PATTERN.matcher(javaContent);
        while (m.find()) {
            String full = m.group(1).trim();
            String simple = full.substring(full.lastIndexOf('.') + 1);
            imports.put(simple, full);
        }
        return imports;
    }

    private Set<String> findGlobalServiceSimpleNames(Path root) throws IOException {
        Set<String> set = new HashSet<>();
        Path globalServicePath = root.resolve(GLOBAL_SERVICE_DIR);
        if (Files.exists(globalServicePath)) {
            Files.walk(globalServicePath)
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(p -> {
                        String name = p.getFileName().toString().replace(".java", "");
                        set.add(name);
                    });
        }
        return set;
    }

    private static Path projectRoot() {
        Path root = Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("build.gradle"))) {
            root = root.getParent();
        }
        assertTrue("Не найден корень проекта HRM HuntTech", root != null);
        return root;
    }
}
