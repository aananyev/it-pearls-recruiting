package com.company.itpearls.web.screens.fragments;

import com.haulmont.cuba.gui.components.RichTextArea;
import com.haulmont.cuba.gui.screen.Screen;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;

import javax.inject.Inject;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

@UiController("itpearls_Onlytext")
@UiDescriptor("onlyText.xml")
public class Onlytext extends Screen {
    @Inject
    private RichTextArea textRichTextArea;
    private String resultText;
    private Boolean cancel;

    public void okButtonInvoke() {
        cancel = false;
        closeWithDefaultAction();
    }

    public void cancelButtonInvoke() {
        cancel = true;
        closeWithDefaultAction();
    }

    public void copyFromClipboardButtonInvoke() {
        String result = "";
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable contents = clipboard.getContents(null);

        boolean hasTransferableText =
                (contents != null) &&
                        contents.isDataFlavorSupported(DataFlavor.stringFlavor);
        if (hasTransferableText) {
            try {
                result = (String)contents.getTransferData(DataFlavor.stringFlavor);
            }
            catch (UnsupportedFlavorException | IOException ex){
                System.out.println(ex);
                ex.printStackTrace();
            }
        }

        textRichTextArea.setValue(result);
    }

    @Subscribe
    public void onAfterClose(AfterCloseEvent event) {
       resultText = textRichTextArea.getValue();
    }

    public String getResultText() {
        return this.resultText;
    }

    public Boolean getCancel() {
        return this.cancel;
    }
}