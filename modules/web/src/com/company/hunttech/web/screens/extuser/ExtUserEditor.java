package com.company.hunttech.web.screens.extuser;

import com.haulmont.bali.util.ParamsMap;
import com.haulmont.cuba.core.global.PersistenceHelper;
import com.haulmont.cuba.gui.WindowManager;
import com.haulmont.cuba.gui.app.security.user.edit.UserEditor;
import com.haulmont.cuba.gui.components.Button;
import com.haulmont.cuba.gui.components.Window;
import com.haulmont.cuba.security.entity.User;

import javax.inject.Inject;

/**
 * Расширяет штатный редактор пользователя CUBA только для запуска стандартного
 * диалога смены пароля из нижней панели формы HRM HuntTech.
 */
public class ExtUserEditor extends UserEditor {

    @Inject
    private Button changePasswordBtn;

    @Override
    protected void postInit() {
        super.postInit();

        // Для нового пользователя пароль задаётся штатными полями UserEditor при сохранении.
        // Отдельный диалог CUBA работает только с уже сохранённой учётной записью.
        changePasswordBtn.setVisible(!PersistenceHelper.isNew(getItem()));
    }

    /**
     * Открывает штатный экран CUBA sec$User.changePassword для редактируемого пользователя.
     * Диалог использует UserManagementService платформы и не затрагивает сохранение остальных
     * полей, ролей и замещений текущего редактора.
     */
    public void changePassword() {
        User user = getItem();
        if (user == null || PersistenceHelper.isNew(user)) {
            return;
        }

        Window changePasswordDialog = openWindow(
                "sec$User.changePassword",
                WindowManager.OpenType.DIALOG,
                ParamsMap.of("user", user)
        );
        changePasswordDialog.addCloseListener(actionId -> changePasswordBtn.focus());
    }
}
