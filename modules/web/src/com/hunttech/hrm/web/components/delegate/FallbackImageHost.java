package com.hunttech.hrm.web.components.delegate;

import com.haulmont.cuba.core.entity.FileDescriptor;
import com.haulmont.cuba.core.global.BeanLocator;
import com.haulmont.cuba.gui.components.Resource;
import com.haulmont.cuba.gui.components.data.ValueSource;

/**
 * Host callbacks required by {@link FallbackImageResourceDelegate}.
 */
public interface FallbackImageHost {

    BeanLocator getBeanLocator();

    ValueSource<FileDescriptor> getBoundValueSource();

    void updateValue(Resource resource);

    <R extends Resource> R createResource(Class<R> resourceClass);
}
