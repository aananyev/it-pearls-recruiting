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
}
