package com.company.itpearls.web.screens.openposition;

import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;

@UiController("itpearls_TextViewScreen")
@UiDescriptor("text-view-screen.xml")
public class TextViewScreen extends Screen {
    private String textView;

    @Inject
    private RichTextArea textViewRichTextArea;

    public void setTextView(String textView) {
        this.textView = textView;

        if (textView != null) {
            textViewRichTextArea.setValue(textView);
        }
    }

    public String getTextView() {
        return textView;
    }

    public void setTextViewScreen() {
        textViewRichTextArea.setValue(textView);
    }
}