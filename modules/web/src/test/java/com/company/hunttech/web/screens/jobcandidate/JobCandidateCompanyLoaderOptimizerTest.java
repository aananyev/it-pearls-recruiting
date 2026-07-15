package com.company.hunttech.web.screens.jobcandidate;

import com.company.hunttech.entity.Company;
import com.haulmont.cuba.gui.model.CollectionLoader;
import org.junit.Test;

import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JobCandidateCompanyLoaderOptimizerTest {

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void preventAutomaticLoadRegistersPreLoadCancellation() {
        CollectionLoader<Company> loader = mock(CollectionLoader.class);

        JobCandidateCompanyLoaderOptimizer.preventAutomaticLoad(loader);

        // Регрессия Stage 2: loader обязан получить listener до @LoadDataBeforeShow,
        // иначе при открытии формы снова загрузятся все компании.
        verify(loader).addPreLoadListener(any(Consumer.class));
    }
}
