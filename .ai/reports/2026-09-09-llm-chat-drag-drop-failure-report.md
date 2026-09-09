# Аналитический отчёт: расследование причин неработающего Drag-and-Drop элемента вызова LLM-chat на Production

> **Дата исследования:** 2026-09-09  
> **Роли:** Аналитик, Автоматизированный тестировщик (QA), Frontend-разработчик  
> **Окружение проверки:** Production `http://92.63.101.170:8080/hrm/` и локальный инстанс `http://localhost:8080/hrm/`  
> **Учётная запись:** `alan` / `Dodo-2012`  
> **Объект:** Плавающий элемент вызова LLM-chat на главном экране (`ExtMainScreen`)  
> **Статус дефекта:** Подтверждён в живой среде (CDP-сессия)  

---

## 1. Сводка результатов тестирования в живой среде

Субагент **«Автоматизированный тестировщик»** выполнил авторизацию под учётной записью `alan` / `Dodo-2012` и провёл серию инструментальных тестов перетаскивания (mouse_down → move -200px X, -100px Y → mouse_up):

| Параметр | Ожидание | Фактический результат | Статус |
|---|---|---|:---:|
| **Начальные координаты** | Центроид кнопки `(961, 946)` | `(961, 946)` | OK |
| **Смещение при перетаскивании** | Перемещение в точку `(761, 846)` | Остался в `(961, 946)` | ❌ СБОЙ |
| **Изменение CSS inline-стилей** | Изменение `style.left` и `style.top` | Стили не применились / перебиты `bottom/right` | ❌ СБОЙ |
| **Ошибки в консоли JS** | Отсутствие JS Exceptions | Ошибок в консоли нет (скрипт не падает, но блокирован) | ⚠ Тихий сбой |

Видеозапись сессии тестирования: `file:///Users/alekseyananyev/.gemini/antigravity-ide/brain/58b40f30-551b-4b8b-b291-04d9b674d43c/test_chat_drag_drop_1788911814249.webp`.

---

## 2. Архитектурные и технические первопричины дефекта

В ходе анализа исходного кода (`ExtMainScreen.java`, `LlmChatLauncherExtension.java`, `llm-chat-launcher.js` и `chat-style.scss` во всех 7 темах) выявлены 4 ключевые причины:

### Причина 1: Конфликт CSS Box Model (`bottom: !important` / `right: !important` против `left`/`top`)
В файлах `chat-style.scss` для классов `.v-window.llm-chat-launcher-window` задано базовое правило:
```scss
.v-window.llm-chat-launcher-window {
    position: fixed !important;
    right: 24px !important;
    bottom: 24px !important;
}
```
Когда клиентский скрипт `llm-chat-launcher.js` в обработчике `onMove` пытается динамически выставлять координаты перетаскивания:
```javascript
windowElement.style.setProperty('left', clampedLeft + 'px', 'important');
windowElement.style.setProperty('top', clampedTop + 'px', 'important');
```
Если класс `.llm-chat-launcher-custom-position` не успевает примениться или в CSS-каскаде браузера селектор с `right: 24px !important; bottom: 24px !important;` имеет равный или больший вес, браузер **одновременно** получает `left`, `right`, `top`, `bottom`. В спецификации CSS при одновременном указании противоположных координат на элементе с фиксированной шириной (56px) свойство `right` побеждает `left` (в LTR-направлении), из-за чего окно визуально прибито к правому нижнему краю экрана.

---

### Причина 2: Перехват событий мыши виджетом кнопки Vaadin (`.v-button`)
Внутри `llmChatLauncherWindow` помещен стандартный серверный Vaadin-компонент `com.vaadin.ui.Button` (`#llmChatLauncher`).  
- Vaadin Button регистрирует собственные обработчики `mousedown`, `mouseup` и `click` в GWT-виде (`VButton`).
- При нажатии мыши на кнопке Vaadin вызывает `event.preventDefault()` или поглощает фазу `bubble` для реализации анимации нажатия (`.v-pressed`) и отправки RPC-события клика на сервер.
- В результате обработчики `pointerdown` / `mousedown` расширения, навешенные без агрессивного перехвата на этапе `capture: true` на уровне самой кнопки и окна, либо не получают событие движения мыши, либо событие прерывается на первом пикселе сдвига.

---

### Причина 3: Разрыв между Vaadin Window и дочерним DOM-контентом
В `ExtMainScreen.java` расширение подключается к окну:
```java
new LlmChatLauncherExtension().extend(llmChatLauncherWindow, storageKey, initialPosition, this::saveUserChatPosition);
```
В Vaadin `Window` DOM-структура многослойна:
```html
<div id="llmChatLauncherWindow" class="v-window ...">
  <div class="v-window-contents">
    <div class="v-scrollable">
      <div id="llmChatLauncher" class="v-button ...">
        <span class="llm-chat-launcher-icon">
          <svg ...>...</svg>
        </span>
      </div>
    </div>
  </div>
</div>
```
Когда пользователь нажимает мышью, целевым элементом (`e.target`) становится `svg`, `path` или внутренний `span`. Если для них не установлено `pointer-events: none;`, браузер начинает нативный drag картинки (`HTML5 dragstart`) или выделение текста, что генерирует событие `pointercancel` и моментально сбрасывает флаг `dragging = false`.

---

### Причина 4: Отсутствие гарантированного захвата указателя (`setPointerCapture`)
При быстром движении мыши курсор мгновенно покидает границы кнопки 56×56 px.  
Без вызова `event.target.setPointerCapture(event.pointerId)` события `pointermove` перестают поступать в элемент, как только курсор сместился за пределы кнопки на 10-20 пикселей.

---

## 3. План решения проблемы (Action Plan для разработчика)

### Шаг 1. В `chat-style.scss` (для всех 7 тем):
1. Разрешить конфликт позиционирования:
```scss
.v-window.llm-chat-launcher-window {
    position: fixed !important;
    touch-action: none !important;
    user-select: none !important;
    -webkit-user-drag: none !important;
}

/* По умолчанию в правом нижнем углу */
.v-window.llm-chat-launcher-window:not(.llm-chat-launcher-custom-position) {
    right: 24px !important;
    bottom: 24px !important;
    left: auto !important;
    top: auto !important;
}

/* При кастомной позиции или драге — абсолютный приоритет top/left */
.v-window.llm-chat-launcher-window.llm-chat-launcher-custom-position,
.v-window.llm-chat-launcher-window.llm-chat-launcher-dragging {
    right: auto !important;
    bottom: auto !important;
}
```

2. Отключить перехват событий на внутренних декоративных элементах:
```scss
.llm-chat-launcher-icon,
.llm-chat-svg-icon,
.llm-chat-svg-icon *,
.llm-chat-launcher-spark,
.llm-chat-launcher-label {
    pointer-events: none !important;
    user-select: none !important;
    -webkit-user-drag: none !important;
}
```

### Шаг 2. В `llm-chat-launcher.js`:
1. На этапе `pointerdown`:
   - Вызывать `e.stopPropagation()`.
   - Вызывать `element.setPointerCapture(e.pointerId)`.
   - Немедленно сбрасывать `right = 'auto'` и `bottom = 'auto'`.
2. Навешивать глобальные слушатели `pointermove` и `pointerup` на `window` в фазе перехвата `{ capture: true, passive: false }`.
3. При смещении более 5px (`threshold`) блокировать последующий `click`, чтобы перетаскивание не открывало диалог чата.

---

## 4. Заключение

Дефект локализован. Он обусловлен взаимным конфликтом CSS-правил `bottom/right !important` с inline-координатами `top/left`, а также перехватом мыши нативным виджетом `VButton` и SVG-графикой кнопки. Для исправления требуется синхронное обновление `chat-style.scss` во всех 7 темах и доработка JS-коннектора `llm-chat-launcher.js`.
