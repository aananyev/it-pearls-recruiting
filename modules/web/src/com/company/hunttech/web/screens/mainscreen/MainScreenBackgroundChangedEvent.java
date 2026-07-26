package com.company.hunttech.web.screens.mainscreen;

import com.haulmont.cuba.gui.events.UiEvent;
import org.springframework.context.ApplicationEvent;

/**
 * Сигнализирует текущей браузерной вкладке, что сохранённый фон изменился.
 * UiEvent не распространяется на другие пользовательские UI-сеансы.
 */
public class MainScreenBackgroundChangedEvent extends ApplicationEvent implements UiEvent {

    public MainScreenBackgroundChangedEvent(Object source) {
        super(source);
    }
}
