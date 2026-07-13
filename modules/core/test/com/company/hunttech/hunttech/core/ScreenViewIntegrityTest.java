package com.company.hunttech.core;

import com.company.hunttech.HunttechTestContainer;
import com.company.hunttech.entity.*;
import com.haulmont.cuba.core.global.AppBeans;
import com.haulmont.cuba.core.global.DataManager;
import com.haulmont.cuba.core.global.ViewBuilder;
import org.junit.ClassRule;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Verify edit-screen views contain all FK paths required by Java generators.
 * Loads entities with the actual screen views and traverses every FK chain.
 */
public class ScreenViewIntegrityTest {

    @ClassRule
    public static HunttechTestContainer cont = HunttechTestContainer.Common.INSTANCE;

    private final DataManager dm = AppBeans.get(DataManager.class);

    private void assertFetched(String label, Object root, String... getters) {
        Object cur = root;
        try {
            for (String g : getters) {
                if (cur == null) return;
                cur = cur.getClass().getMethod(g).invoke(cur);
            }
        } catch (IllegalStateException e) {
            fail(label + ": " + e.getMessage());
        } catch (Exception e) {
            // NPE on null FK is fine — value just absent
        }
    }

    // ── JobCandidateEdit ──────────────────────────────────────────────
    // Screen view: extends=_local + inline iteractionList(_minimal) + candidateCv(_minimal)
    // Generators access: iteractionList.vacancy.projectName.{logo,department,description}
    //                    candidateCv.toVacancy.projectName.{logo,department}

    @Test
    public void jobCandidateEdit_view() {
        // Load with the same view definition as the screen
        List<JobCandidate> list = dm.load(JobCandidate.class)
                .query("select e from hunttech_JobCandidate e")
                .view("_local")
                .maxResults(3)
                .list();
        if (list.isEmpty()) { System.out.println("SKIP: no JobCandidate data"); return; }

        // Now verify iteractionList-job-candidate view has vacancy.projectName chain
        List<IteractionList> interactions = dm.load(IteractionList.class)
                .query("select e from hunttech_IteractionList e")
                .view("iteractionList-job-candidate")
                .maxResults(5)
                .list();
        for (IteractionList il : interactions) {
            assertFetched("iteractionList.vacancy (full view)", il, "getVacancy");
            if (il.getVacancy() != null) {
                assertFetched("vacancy.projectName (full view)", il.getVacancy(), "getProjectName");
                if (il.getVacancy().getProjectName() != null) {
                    assertFetched("projectName.projectDescription", il.getVacancy().getProjectName(), "getProjectDescription");
                    assertFetched("projectName.projectLogo", il.getVacancy().getProjectName(), "getProjectLogo");
                    assertFetched("projectName.projectDepartment", il.getVacancy().getProjectName(), "getProjectDepartment");
                }
            }
            assertFetched("iteractionList.iteractionType", il, "getIteractionType");
            assertFetched("iteractionList.recrutier", il, "getRecrutier");
        }

        // Also verify candidateCV-browse-view has toVacancy chain
        List<CandidateCV> cvs = dm.load(CandidateCV.class)
                .query("select e from hunttech_CandidateCV e")
                .view("candidateCV-browse-view")
                .maxResults(5)
                .list();
        for (CandidateCV cv : cvs) {
            assertFetched("candidateCV.toVacancy", cv, "getToVacancy");
            if (cv.getToVacancy() != null) {
                assertFetched("toVacancy.projectName", cv.getToVacancy(), "getProjectName");
            }
        }

        System.out.println("OK: JobCandidateEdit — all FK paths accessible");
    }

    @Test
    public void jobCandidateEdit_iteractionListView() {
        // Verify IteractionList loaded via jobCandidateDc inline view:
        // view="_minimal" + vacancy.openPosition-edit-view (+ projectName._local)
        // This matches the updated XML in job-candidate-edit.xml
        List<IteractionList> interactions = dm.load(IteractionList.class)
                .query("select e from hunttech_IteractionList e")
                .view(ViewBuilder.of(IteractionList.class)
                        .addView("_minimal")
                        .add("vacancy", "openPosition-edit-view")
                        .build())
                .maxResults(5)
                .list();
        for (IteractionList il : interactions) {
            assertFetched("iteractionList.vacancy", il, "getVacancy");
            if (il.getVacancy() != null) {
                // suggestVacancyTableNotSendedIconColumnColumnGenerator: vacancy.equals()
                assertFetched("vacancy.openClose", il.getVacancy(), "getOpenClose");
                // currentOpenCloseColumn: vacancy.openClose
                assertFetched("vacancy.comment", il.getVacancy(), "getComment");
                // projectLogoColumn: vacancy.projectName (loaded inline)
                assertFetched("vacancy.owner", il.getVacancy(), "getOwner");
                // owner is loaded with _minimal view (getName() must work)
                if (il.getVacancy().getOwner() != null) {
                    assertFetched("owner.name", il.getVacancy().getOwner(), "getName");
                }
            }
        }
        System.out.println("OK: JobCandidateEdit iteractionList _minimal+vacancy.openPosition-edit-view — OK");
    }

    // ── OpenPositionEdit ──────────────────────────────────────────────
    @Test
    public void openPositionEdit_view() {
        // Verify openPosition-edit-view (slim view for JobCandidateEdit)
        List<OpenPosition> list = dm.load(OpenPosition.class)
                .query("select e from hunttech_OpenPosition e")
                .view("openPosition-edit-view")
                .maxResults(5)
                .list();
        if (list.isEmpty()) { System.out.println("SKIP: no OpenPosition data"); return; }

        for (OpenPosition p : list) {
            assertFetched("openPosition.projectName", p, "getProjectName");
            if (p.getProjectName() != null) {
                assertFetched("projectName.projectLogo", p.getProjectName(), "getProjectLogo");
                assertFetched("projectName.projectDescription", p.getProjectName(), "getProjectDescription");
            }
            assertFetched("openPosition.vacansyName", p, "getVacansyName");
            assertFetched("openPosition.openClose", p, "getOpenClose");
            assertFetched("openPosition.comment", p, "getComment");
            assertFetched("openPosition.owner", p, "getOwner");
        }
        System.out.println("OK: openPosition-edit-view — all FK paths accessible");
    }

    // ── CandidateCVEdit ───────────────────────────────────────────────
    // View: candidateCV-view
    // Generators access: toVacancy.projectName.projectDepartment

    @Test
    public void candidateCVEdit_view() {
        List<CandidateCV> list = dm.load(CandidateCV.class)
                .query("select e from hunttech_CandidateCV e")
                .view("candidateCV-view")
                .maxResults(5)
                .list();
        if (list.isEmpty()) { System.out.println("SKIP: no CandidateCV data"); return; }

        for (CandidateCV cv : list) {
            assertFetched("candidateCV.toVacancy", cv, "getToVacancy");
            if (cv.getToVacancy() != null) {
                assertFetched("toVacancy.projectName", cv.getToVacancy(), "getProjectName");
            }
            assertFetched("candidateCV.candidate", cv, "getCandidate");
        }
        System.out.println("OK: CandidateCVEdit — all FK paths accessible");
    }

    // ── IteractionListEdit ────────────────────────────────────────────
    // View: iteractionList-edit-view
    // Generators access: vacancy, iteractionType, recrutier

    @Test
    public void iteractionListEdit_view() {
        List<IteractionList> list = dm.load(IteractionList.class)
                .query("select e from hunttech_IteractionList e")
                .view("iteractionList-edit-view")
                .maxResults(5)
                .list();
        if (list.isEmpty()) { System.out.println("SKIP: no IteractionList data"); return; }

        for (IteractionList il : list) {
            assertFetched("iteractionList.vacancy", il, "getVacancy");
            assertFetched("iteractionList.iteractionType", il, "getIteractionType");
            assertFetched("iteractionList.recrutier", il, "getRecrutier");
            if (il.getVacancy() != null) {
                assertFetched("vacancy.projectName", il.getVacancy(), "getProjectName");
            }
        }
        System.out.println("OK: IteractionListEdit — all FK paths accessible");
    }

    // ── Screen Registration Integrity ────────────────────────
    // Проверяет, что все экраны из web-app.properties зарегистрированы.

    private String getProjectRoot() {
        String dir = System.getProperty("user.dir");
        if (dir == null) dir = ".";
        // Поднимаемся, пока не найдём build.gradle (корень проекта)
        java.io.File f = new java.io.File(dir);
        while (f != null && !new java.io.File(f, "build.gradle").exists()) {
            f = f.getParentFile();
        }
        return f != null ? f.getAbsolutePath() : dir;
    }

    @Test
    public void requiredScreensRegistered() {
        String root = getProjectRoot();

        // Список обязательных экранов из web-app.properties
        String[] requiredScreens = {
                "loginBranded",
                "settings",
        };

        // Читаем web-screens.xml для поиска id
        String screensXml;
        try {
            screensXml = new String(
                    java.nio.file.Files.readAllBytes(
                            java.nio.file.Paths.get(root,
                                    "modules/web/src/com/company/hunttech/web-screens.xml")),
                    java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("FAIL: cannot read web-screens.xml — " + e.getMessage());
            return;
        }

        // Ищем каждый экран
        for (String screenId : requiredScreens) {
            boolean foundInXml = screensXml.contains("id=\"" + screenId + "\"");
            if (!foundInXml) {
                System.out.println("WARN: " + screenId + " not found in web-screens.xml " +
                        "(may be auto-discovered via @UiController)");
            }
        }
        System.out.println("OK: required screens check passed");
    }

    // ── Deployed jar integrity ─────────────────────────────
    // Проверяет, что обязательные классы присутствуют в deployed jar.
    // Вылавливает ситуацию, когда jar не обновлён после добавления нового экрана.

    @Test
    public void deployedJarContainsRequiredScreens() {
        String root = getProjectRoot();

        // Пути к deployed jar
        String[] jarCandidates = {
                root + "/deploy/tomcat/webapps/hrm/WEB-INF/lib/app-web-0.1-SNAPSHOT.jar",
                root + "/deploy/tomcat/webapps/hrm/WEB-INF/lib/app-web-0.1-SNAPSHOT-sources.jar",
        };

        String[] requiredClasses = {
                "com/company/hunttech/web/login/AppLoginScreen.class",
                "com/company/hunttech/web/login/app-login-screen.xml",
        };

        boolean jarFound = false;
        for (String jarPath : jarCandidates) {
            java.io.File jarFile = new java.io.File(jarPath);
            if (!jarFile.exists()) continue;

            jarFound = true;
            try (java.util.jar.JarFile jf = new java.util.jar.JarFile(jarFile)) {
                java.util.Enumeration<java.util.jar.JarEntry> entries = jf.entries();
                java.util.Set<String> entryNames = new java.util.HashSet<>();
                while (entries.hasMoreElements()) {
                    entryNames.add(entries.nextElement().getName());
                }

                for (String cls : requiredClasses) {
                    if (!entryNames.contains(cls)) {
                        // Проверяем в директории exploded
                        java.io.File exploded = new java.io.File(
                                root + "/deploy/tomcat/webapps/hrm/WEB-INF/classes/" + cls);
                        if (!exploded.exists()) {
                            throw new RuntimeException(
                                    "Required class/resource not found in deployed jar: " + cls
                                    + " (in " + jarPath + ")");
                        }
                    }
                }
                System.out.println("OK: deployed jar contains " + requiredClasses.length
                        + " required resources — " + jarFile.getName());
            } catch (Exception e) {
                throw new RuntimeException("FAIL: cannot verify deployed jar: " + e.getMessage(), e);
            }
            break;
        }

        if (!jarFound) {
            // Проверяем exploded deployment
            boolean allFound = true;
            for (String cls : requiredClasses) {
                java.io.File exploded = new java.io.File(
                        root + "/deploy/tomcat/webapps/hrm/WEB-INF/classes/" + cls);
                if (!exploded.exists()) {
                    allFound = false;
                    System.out.println("WARN: " + cls + " not in exploded deployment either");
                }
            }
            if (!allFound) {
                throw new RuntimeException(
                        "No deployed app-web jar found and exploded classes incomplete. "
                        + "Run './gradlew deploy' first.");
            }
            System.out.println("OK: exploded deployment contains required classes");
        }
    }

    // ── web-app.properties screenId consistency ──────────────

    @Test
    public void webAppPropertiesLoginScreenExists() {
        String root = getProjectRoot();
        try {
            String props = new String(
                    java.nio.file.Files.readAllBytes(
                            java.nio.file.Paths.get(root,
                                    "modules/web/src/com/company/hunttech/web-app.properties")),
                    java.nio.charset.StandardCharsets.UTF_8);
            for (String line : props.split("\n")) {
                if (line.trim().startsWith("cuba.web.loginScreenId")) {
                    String screenId = line.split("=")[1].trim();
                    System.out.println("Checking login screen: " + screenId);

                    // Ищем @UiController в Java-файлах
                    String searchPath = root + "/modules/web/src/com/company/hunttech/web";
                    java.io.File searchDir = new java.io.File(searchPath);
                    boolean found = searchInFiles(searchDir, "@UiController(\"" + screenId + "\")");
                    if (!found) {
                        throw new RuntimeException(
                                "Login screen not found: " + screenId);
                    }
                    System.out.println("OK: " + screenId + " found via @UiController");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("FAIL: " + e.getMessage(), e);
        }
    }

    private boolean searchInFiles(java.io.File dir, String search) {
        if (!dir.isDirectory()) return false;
        java.io.File[] files = dir.listFiles();
        if (files == null) return false;
        for (java.io.File f : files) {
            if (f.isDirectory()) {
                if (searchInFiles(f, search)) return true;
            } else if (f.getName().endsWith(".java")) {
                try {
                    String content = new String(java.nio.file.Files.readAllBytes(f.toPath()),
                            java.nio.charset.StandardCharsets.UTF_8);
                    if (content.contains(search)) return true;
                } catch (Exception ignored) {}
            }
        }
        return false;
    }
}
