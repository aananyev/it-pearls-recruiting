package com.company.hunttech.core;

import com.company.hunttech.entity.Company;
import com.company.hunttech.entity.JobHistory;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Контрактный автотест целостности схемы БД и сущностей JPA.
 * Предотвращает ошибки вида:
 * "PSQLException: ERROR: column 'actual_address' does not exist"
 * и коллизии уникальных ключей UUID в миграциях AI-функций.
 */
public class EntityDatabaseColumnsIntegrityContractTest {

    private static final String UPDATE_SCRIPTS_DIR = "modules/core/db/update/postgres";
    private static final String CHANGELOG_DIR = "modules/core/db/changelog";

    private static final String DB_URL_127 = "jdbc:postgresql://127.0.0.1:5432/hunttech";
    private static final String DB_URL_LOCAL = "jdbc:postgresql://localhost:5432/hunttech";
    private static final String DB_USER = "cuba";
    private static final String DB_PASSWORD = "cuba";

    @Test
    public void testNoDuplicateAiFunctionUuidsInMigrations() throws IOException {
        Path updatePath = resolveProjectPath(UPDATE_SCRIPTS_DIR);
        Pattern uuidPattern = Pattern.compile("(?i)'([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})'::uuid");

        Set<String> seenUuids = new HashSet<>();
        if (Files.exists(updatePath)) {
            try (Stream<Path> stream = Files.walk(updatePath)) {
                List<Path> sqlFiles = stream.filter(p -> p.toString().endsWith(".sql") && !p.toString().endsWith(".old"))
                        .collect(Collectors.toList());

                for (Path sqlFile : sqlFiles) {
                    String content = new String(Files.readAllBytes(sqlFile), StandardCharsets.UTF_8);
                    if (content.contains("INSERT INTO HUNTTECH_AI_FUNCTION_CONFIGURATION")) {
                        Matcher matcher = uuidPattern.matcher(content);
                        while (matcher.find()) {
                            String uuid = matcher.group(1).toLowerCase();
                            assertFalse("Обнаружен дублирующийся UUID " + uuid + " в миграции " + sqlFile.getFileName(),
                                    seenUuids.contains(uuid));
                            seenUuids.add(uuid);
                        }
                    }
                }
            }
        }
    }

    @Test
    public void testCompanyEntityRequiredColumnsExistInDb() {
        Connection conn = tryGetDatabaseConnection();
        if (conn == null) {
            System.out.println("[EntityDatabaseColumnsIntegrityContractTest] Локальная БД PostgreSQL недоступна, live-проверка пропущена.");
            return;
        }

        try {
            List<String> requiredCompanyColumns = Arrays.asList(
                    "inn", "kpp", "ogrn", "okpo", "oktmo", "okved",
                    "legal_address", "actual_address", "postal_address",
                    "bik", "bank_name", "settlement_account", "correspondent_account",
                    "phone", "email", "website", "legal_entity_name",
                    "address_of_company", "city_of_company_id", "region_of_company_id", "country_of_company_id"
            );

            Set<String> existingColumns = getTableColumns(conn, "hunttech_company");
            for (String col : requiredCompanyColumns) {
                assertTrue("Колонка '" + col + "' должна существовать в таблице HUNTTECH_COMPANY",
                        existingColumns.contains(col.toLowerCase()));
            }

            List<String> requiredJobHistoryColumns = Arrays.asList(
                    "start_date", "end_date", "duties", "raw_position_name", "raw_company_name",
                    "current_company_id", "candidate_id"
            );

            Set<String> existingJobHistoryColumns = getTableColumns(conn, "hunttech_job_history");
            for (String col : requiredJobHistoryColumns) {
                assertTrue("Колонка '" + col + "' должна существовать в таблице HUNTTECH_JOB_HISTORY",
                        existingJobHistoryColumns.contains(col.toLowerCase()));
            }

        } catch (SQLException e) {
            throw new AssertionError("Ошибка при проверке колонок в БД: " + e.getMessage(), e);
        } finally {
            try {
                conn.close();
            } catch (SQLException ignored) {
            }
        }
    }

    private Set<String> getTableColumns(Connection conn, String tableName) throws SQLException {
        Set<String> columns = new HashSet<>();
        String sql = "SELECT column_name FROM information_schema.columns WHERE lower(table_name) = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName.toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    columns.add(rs.getString(1).toLowerCase());
                }
            }
        }
        return columns;
    }

    private Connection tryGetDatabaseConnection() {
        try {
            Class.forName("org.postgresql.Driver");
            try {
                return DriverManager.getConnection(DB_URL_127, DB_USER, DB_PASSWORD);
            } catch (SQLException e) {
                return DriverManager.getConnection(DB_URL_LOCAL, DB_USER, DB_PASSWORD);
            }
        } catch (Exception e) {
            return null;
        }
    }

    private Path resolveProjectPath(String relativePath) {
        Path path = Paths.get(relativePath);
        if (Files.exists(path)) {
            return path;
        }
        Path parent = Paths.get("..", relativePath);
        if (Files.exists(parent)) {
            return parent;
        }
        return path;
    }
}
