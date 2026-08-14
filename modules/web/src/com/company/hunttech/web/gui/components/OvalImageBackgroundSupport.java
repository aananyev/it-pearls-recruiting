package com.company.hunttech.web.gui.components;

import com.vaadin.ui.AbstractComponent;
import org.apache.commons.lang3.StringUtils;

/**
 * Поддержка CSS-фона для овальных image-компонентов (HRM HuntTech).
 *
 * <p>Vaadin 8 не имеет server-side API для inline-стилей, поэтому фон задаётся
 * динамическим CSS-классом, который инжектится в страницу через
 * {@code Page.getStyles().add(css)} — тот же приём, что в
 * {@code SignIconsEdit.injectColorCss} и других экранах проекта.</p>
 *
 * <p>Класс строится из хэша значения фона: одинаковые значения переиспользуют
 * один класс и не плодят дубликаты CSS в DOM, разные значения не перекрашивают
 * соседние овалы на странице.</p>
 */
public final class OvalImageBackgroundSupport {

    static final String STYLE_PREFIX = "ht-oval-image-bg-";

    private OvalImageBackgroundSupport() {
    }

    /**
     * Применяет CSS-фон к Vaadin-компоненту. Пустое значение — no-op
     * (фон, заданный в XML, остаётся до уничтожения компонента).
     */
    public static void applyBackground(AbstractComponent component, String background) {
        if (component == null || StringUtils.isBlank(background)) {
            return;
        }
        String styleClass = STYLE_PREFIX + Integer.toHexString(background.hashCode());
        component.addStyleName(styleClass);

        String css = String.format(".%s { background: %s !important; }", styleClass, background);
        if (component.getUI() != null) {
            component.getUI().getPage().getStyles().add(css);
        } else {
            // XML-loader вызывает setter до attach (страница ещё недоступна) —
            // инжектим стиль при первом attach компонента.
            component.addAttachListener(e -> {
                if (component.getUI() != null) {
                    component.getUI().getPage().getStyles().add(css);
                }
            });
        }
    }
}
