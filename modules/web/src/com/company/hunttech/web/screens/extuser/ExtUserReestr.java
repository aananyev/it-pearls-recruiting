package com.company.hunttech.web.screens.extuser;

import com.company.hunttech.entity.ExtUser;
import com.company.hunttech.entity.StdPictures;
import com.company.hunttech.web.util.FileDescriptorImageHelper;
import com.haulmont.cuba.core.global.FileLoader;
import com.haulmont.cuba.core.global.Messages;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.core.global.Security;
import com.haulmont.cuba.gui.Notifications;
import com.haulmont.cuba.gui.ScreenBuilders;
import com.haulmont.cuba.gui.UiComponents;
import com.haulmont.cuba.gui.components.*;
import com.haulmont.cuba.gui.model.CollectionContainer;
import com.haulmont.cuba.gui.model.CollectionLoader;
import com.haulmont.cuba.gui.model.InstanceContainer;
import com.haulmont.cuba.gui.screen.Install;
import com.haulmont.cuba.gui.screen.LoadDataBeforeShow;
import com.haulmont.cuba.gui.screen.LookupComponent;
import com.haulmont.cuba.gui.screen.MapScreenOptions;
import com.haulmont.cuba.gui.screen.OpenMode;
import com.haulmont.cuba.gui.screen.StandardLookup;
import com.haulmont.cuba.gui.screen.Subscribe;
import com.haulmont.cuba.gui.screen.Target;
import com.haulmont.cuba.gui.screen.UiController;
import com.haulmont.cuba.gui.screen.UiDescriptor;
import com.haulmont.cuba.security.entity.EntityOp;
import com.hunttech.hrm.web.components.WebOvaFallbackImage;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Контроллер экрана «Реестр пользователей» (ExtUserReestr).
 * Реализован в стандарте HuntTech Reestr Split-View Master-Detail:
 * левый сайдбар 312px с шапкой профиля, быстрым обзором ролей, контактов и параметров доступа,
 * командный тулбар, generic-фильтр и таблица с аватарами 24px.
 */
@UiController("hunttech_ExtUser.reestr")
@UiDescriptor("ext-user-reestr.xml")
@LookupComponent("usersTable")
@LoadDataBeforeShow
public class ExtUserReestr extends StandardLookup<ExtUser> {

    @Inject
    private GroupTable<ExtUser> usersTable;
    @Inject
    private CollectionContainer<ExtUser> usersDc;
    @Inject
    private CollectionLoader<ExtUser> usersDl;

    @Inject
    private UiComponents uiComponents;
    @Inject
    private ScreenBuilders screenBuilders;
    @Inject
    private Notifications notifications;
    @Inject
    private Messages messages;
    @Inject
    private Security security;
    @Inject
    private FileLoader fileLoader;

    // Компоненты левого сайдбара (312px)
    @Inject
    private WebOvaFallbackImage detailPic;
    @Inject
    private Label<String> detailFullName;
    @Inject
    private Label<String> detailLoginPosition;
    @Inject
    private Label<String> detailGroup;
    @Inject
    private Label<String> detailStatus;

    @Inject
    private Button detailEditBtn;
    @Inject
    private Button detailChangePasswordBtn;

    // Секция 1: Контакты и реквизиты
    @Inject
    private Label<String> detailEmail;
    @Inject
    private Label<String> detailTelegram;
    @Inject
    private Label<String> detailTimeZone;
    @Inject
    private Label<String> detailLanguage;

    // Секция 2: Роли и доступ
    @Inject
    private Label<String> detailRoles;
    @Inject
    private Label<String> detailDashboards;
    @Inject
    private Label<String> detailStatistics;

    // Кнопки тулбара
    @Inject
    private Button changePasswordBtn;

    @Subscribe
    public void onInit(InitEvent event) {
        initQuickActionHandlers();
    }

    private void initQuickActionHandlers() {
        detailEditBtn.addClickListener(e -> editSelected());
        detailChangePasswordBtn.addClickListener(e -> changePasswordForSelected());
        changePasswordBtn.addClickListener(e -> changePasswordForSelected());
    }

    @Subscribe(id = "usersDc", target = Target.DATA_CONTAINER)
    public void onUsersDcItemChange(InstanceContainer.ItemChangeEvent<ExtUser> event) {
        ExtUser user = event.getItem();
        updateSidebar(user);
    }

    @Subscribe(id = "usersDl", target = Target.DATA_LOADER)
    private void onUsersDlPostLoad(CollectionLoader.PostLoadEvent<ExtUser> event) {
        ExtUser current = usersTable.getSingleSelected();
        if (current != null) {
            updateSidebar(current);
        } else if (!event.getLoadedEntities().isEmpty()) {
            usersTable.setSelected(event.getLoadedEntities().get(0));
        } else {
            clearSidebar();
        }
    }

    private void updateSidebar(ExtUser user) {
        if (user == null) {
            clearSidebar();
            return;
        }

        // 1. Аватар профиля (с проверкой физической доступности файла)
        FileDescriptorImageHelper.setUserProfilePhoto(detailPic, fileLoader, user);

        // 2. Типографика шапки
        String fullName = buildFio(user);
        detailFullName.setValue(fullName);

        String loginPos = "@" + (user.getLogin() != null ? user.getLogin() : "");
        if (StringUtils.isNotBlank(user.getPosition())) {
            loginPos += " • " + user.getPosition();
        }
        detailLoginPosition.setValue(loginPos);

        String groupName = user.getGroup() != null ? user.getGroup().getName() : "-";
        detailGroup.setValue(groupName);

        boolean isActive = Boolean.TRUE.equals(user.getActive());
        detailStatus.setValue(isActive
                ? "🟢 " + messages.getMessage(getClass(), "statusActive")
                : "🔴 " + messages.getMessage(getClass(), "statusInactive"));

        // 3. Контакты
        detailEmail.setValue(StringUtils.defaultIfBlank(user.getEmail(), "-"));
        detailTelegram.setValue(StringUtils.defaultIfBlank(user.getTelegram(), "-"));
        detailTimeZone.setValue(StringUtils.defaultIfBlank(user.getTimeZone(), "-"));
        detailLanguage.setValue(StringUtils.defaultIfBlank(user.getLanguage(), "-"));

        // 4. Роли и доступ
        if (user.getUserRoles() != null && !user.getUserRoles().isEmpty()) {
            List<String> roleNames = user.getUserRoles().stream()
                    .filter(ur -> ur.getRole() != null)
                    .map(ur -> StringUtils.isNotBlank(ur.getRole().getLocName())
                            ? ur.getRole().getLocName()
                            : ur.getRole().getName())
                    .distinct()
                    .collect(Collectors.toList());
            detailRoles.setValue(roleNames.isEmpty() ? "-" : String.join(", ", roleNames));
        } else {
            detailRoles.setValue("-");
        }

        detailDashboards.setValue(Boolean.TRUE.equals(user.getDashboards())
                ? messages.getMessage(getClass(), "yes")
                : messages.getMessage(getClass(), "no"));
        detailStatistics.setValue(Boolean.TRUE.equals(user.getStatistics())
                ? messages.getMessage(getClass(), "yes")
                : messages.getMessage(getClass(), "no"));

        // 5. Разрешение действий
        boolean canUpdate = isUserUpdatePermitted();
        detailEditBtn.setEnabled(canUpdate);
        detailChangePasswordBtn.setEnabled(canUpdate);
        changePasswordBtn.setEnabled(canUpdate);
    }

    private boolean isUserUpdatePermitted() {
        return security.isEntityOpPermitted(ExtUser.class, EntityOp.UPDATE);
    }

    private void clearSidebar() {
        detailPic.setSource(ThemeResource.class).setPath(StdPictures.NO_CANDIDATE.getId());

        detailFullName.setValue(messages.getMessage(getClass(), "selectUserPrompt"));
        detailLoginPosition.setValue("");
        detailGroup.setValue("");
        detailStatus.setValue("");

        detailEmail.setValue("-");
        detailTelegram.setValue("-");
        detailTimeZone.setValue("-");
        detailLanguage.setValue("-");

        detailRoles.setValue("-");
        detailDashboards.setValue("-");
        detailStatistics.setValue("-");

        detailEditBtn.setEnabled(false);
        detailChangePasswordBtn.setEnabled(false);
        changePasswordBtn.setEnabled(false);
    }

    @Install(to = "usersTable.avatar", subject = "columnGenerator")
    private Component usersTableAvatarColumnGenerator(ExtUser user) {
        HBoxLayout box = uiComponents.create(HBoxLayout.class);
        box.setWidthFull();
        box.setHeightFull();
        box.setAlignment(Component.Alignment.MIDDLE_CENTER);

        Image img = uiComponents.create(Image.class);
        img.setAlignment(Component.Alignment.MIDDLE_CENTER);
        img.setScaleMode(Image.ScaleMode.SCALE_DOWN);
        img.setWidth("24px");
        img.setHeight("24px");
        img.setStyleName("circle-20px");

        FileDescriptorImageHelper.setUserProfilePhoto(img, fileLoader, user);
        box.add(img);
        return box;
    }

    private void editSelected() {
        Action editAction = usersTable.getAction("edit");
        if (editAction != null && editAction.isEnabled()) {
            editAction.actionPerform(usersTable);
        }
    }

    private void changePasswordForSelected() {
        if (!isUserUpdatePermitted()) {
            notifications.create()
                    .withCaption(messages.getMessage(getClass(), "accessDeniedMessage"))
                    .withType(Notifications.NotificationType.WARNING)
                    .show();
            return;
        }
        ExtUser selected = usersTable.getSingleSelected();
        if (selected == null || PersistenceHelper.isNew(selected)) {
            return;
        }
        screenBuilders.screen(this)
                .withScreenId("sec$User.changePassword")
                .withOptions(new MapScreenOptions(Collections.singletonMap("user", selected)))
                .withOpenMode(OpenMode.DIALOG)
                .show();
    }

    private String buildFio(ExtUser user) {
        if (user == null) {
            return "";
        }
        if (StringUtils.isNotBlank(user.getName())) {
            return user.getName();
        }
        StringBuilder sb = new StringBuilder();
        if (StringUtils.isNotBlank(user.getLastName())) {
            sb.append(user.getLastName());
        }
        if (StringUtils.isNotBlank(user.getFirstName())) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(user.getFirstName());
        }
        if (StringUtils.isNotBlank(user.getMiddleName())) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(user.getMiddleName());
        }
        return sb.length() > 0 ? sb.toString() : StringUtils.defaultIfBlank(user.getLogin(), "-");
    }
}
