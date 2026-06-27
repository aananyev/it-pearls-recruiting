package com.company.itpearls.web.login;

import com.company.itpearls.web.extension.ChangeFaviconExtension;
import com.haulmont.cuba.gui.Route;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.screen.*;
import com.haulmont.cuba.web.app.login.LoginScreen;
import com.haulmont.cuba.web.gui.screen.ScreenDependencyUtils;
import com.vaadin.ui.AbstractOrderedLayout;
import com.vaadin.ui.Dependency;

import javax.inject.Inject;

@Route(path = "login", root = true)
@UiController("loginBranded")
@UiDescriptor("app-login-screen.xml")
public class AppLoginScreen extends LoginScreen {

    private static final String DEFAULT_FAVICON = "./VAADIN/themes/hover/favicon.ico";

    @Inject
    protected VBoxLayout loginWrapper;

    @Inject
    protected HBoxLayout bottomPanel;

    @Inject
    protected Label<String> poweredByLink;
    @Inject
    private Image backgroundImage;

    @Subscribe
    public void onAppLoginScreenInit(InitEvent event) {
        loadStyles();
        setFavicon();
        initBottomPanel();
    }

    private void setFavicon() {
        ChangeFaviconExtension extension = new ChangeFaviconExtension();
        extension.extend(loginWrapper.unwrap(AbstractOrderedLayout.class), DEFAULT_FAVICON);
    }

    private void initLoginImage() {
        int count = (int) (Math.random() * 20 + 1);
        backgroundImage
                .setSource(RelativePathResource.class)
                .setPath("VAADIN/brand-login-screen/recruit" + count + ".jpg");

/*        FileDescriptor fileDescriptor = applicationSetupService.getActiveCompanyIcon();

        ChangeFaviconExtension extension = new ChangeFaviconExtension();
        extension.extend(loginWrapper.unwrap(AbstractOrderedLayout.class),
                "./VAADIN/themes/hover/icons/no-company.png"); */
    }
    @Subscribe("submit")
    public void onSubmit(Action.ActionPerformedEvent event) {
        login();
    }

    protected void loadStyles() {
        ScreenDependencyUtils.addScreenDependency(this,
                "vaadin://brand-login-screen/login.css", Dependency.Type.STYLESHEET);
    }

    /**
     * Нижняя панель левой колонки: подпись HuntTech слева, выбор языка справа.
     * При скрытом селекторе локали — центрирование подписи.
     */
    protected void initBottomPanel() {
        bottomPanel.setAlignment(Component.Alignment.MIDDLE_CENTER);
        poweredByLink.setAlignment(Component.Alignment.MIDDLE_LEFT);

        if (!globalConfig.getLocaleSelectVisible()) {
            poweredByLink.setAlignment(Component.Alignment.MIDDLE_CENTER);

            if (!webConfig.getLoginDialogPoweredByLinkVisible()) {
                bottomPanel.setVisible(false);
            }
        }
    }

/*    @Override
    protected void initLogoImage() {
        logoImage.setSource(RelativePathResource.class)
                .setPath("VAADIN/brand-login-screen/cuba-icon-login.jpg");

        logoImage = getLogoImage();
    }

    private Image getLogoImage() {
        Image image = uiComponents.create(Image.class);

        image.setSource(FileDescriptorResource.class).setFileDescriptor(applicationSetupService.getActiveCompanyLogo());

        return image;
    } */

    @Subscribe
    public void onBeforeShow(BeforeShowEvent event) {
        initLoginImage();
    }
}