package com.company.itpearls.web.extension;

import com.vaadin.annotations.JavaScript;
import com.vaadin.server.AbstractJavaScriptExtension;
import com.vaadin.ui.AbstractOrderedLayout;

@JavaScript("change-favicon.js")
public class ChangeFaviconExtension extends AbstractJavaScriptExtension {

    public void extend(AbstractOrderedLayout layout, String href) {
        super.extend(layout);

        callFunction("changeFavicon", href);
    }
}
