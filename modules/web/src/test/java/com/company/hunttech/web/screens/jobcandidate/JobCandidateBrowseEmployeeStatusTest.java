package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.Employee;
import com.company.hunttech.entity.EmployeeWorkStatus;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobCandidateBrowseEmployeeStatusTest {

    private static final Path SCREEN_CONTROLLER = Paths.get(
            "modules/web/src/com/company/hunttech/web/screens/jobcandidate/JobCandidateBrowse.java");

    @Test
    public void employeeStatusReturnsLoadedValuesAndHandlesNulls() {
        Employee employee = mock(Employee.class);
        EmployeeWorkStatus workStatus = mock(EmployeeWorkStatus.class);
        when(employee.getWorkStatus()).thenReturn(workStatus);

        when(workStatus.getInStaff()).thenReturn(true, false, null);

        assertEquals(Boolean.TRUE, JobCandidateBrowse.getEmployeeInStaff(employee));
        assertEquals(Boolean.FALSE, JobCandidateBrowse.getEmployeeInStaff(employee));
        assertNull(JobCandidateBrowse.getEmployeeInStaff(employee));
        assertNull(JobCandidateBrowse.getEmployeeInStaff(null));

        Employee withoutStatus = mock(Employee.class);
        assertNull(JobCandidateBrowse.getEmployeeInStaff(withoutStatus));
    }

    @Test
    public void employeeStatusDoesNotBreakGridForDetachedObjects() {
        Employee detachedEmployee = mock(Employee.class);
        when(detachedEmployee.getWorkStatus()).thenThrow(
                new IllegalStateException("Cannot get unfetched attribute from detached object"));
        assertNull(JobCandidateBrowse.getEmployeeInStaff(detachedEmployee));

        Employee employee = mock(Employee.class);
        EmployeeWorkStatus detachedStatus = mock(EmployeeWorkStatus.class);
        when(employee.getWorkStatus()).thenReturn(detachedStatus);
        when(detachedStatus.getInStaff()).thenThrow(
                new IllegalStateException("Cannot get unfetched attribute [inStaff] from detached object"));
        assertNull(JobCandidateBrowse.getEmployeeInStaff(employee));
    }

    @Test
    public void employeeCacheViewFetchesInStaffForDetachedRendering() throws Exception {
        String source = new String(Files.readAllBytes(SCREEN_CONTROLLER), StandardCharsets.UTF_8);

        assertTrue("Employee cache must fetch workStatus.inStaff before entities are detached",
                source.contains(".add(\"workStatus\", workStatusView -> workStatusView.add(\"inStaff\"))"));
    }
}
