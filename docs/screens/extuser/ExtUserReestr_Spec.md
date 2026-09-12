# Спецификация UI/UX: Реестр пользователей (ExtUserReestr)

## 1. Назначение экрана
Экран «Реестр пользователей» (`hunttech_ExtUser.reestr`) предназначен для административного управления пользователями системы HRM HuntTech в современном стандарте Split-View Master-Detail. Форма обеспечивает быстрый обзор профиля пользователя, контактных данных, назначенных ролей и прав доступа без обязательного открытия модального окна редактирования.

---

## 2. Архитектурная композиция Split-View

```
┌──────────────────────────────────────────────────────────────────────────────────────────────────┐
│ Административная панель / Реестр пользователей (42px)                                            │
├───────────────────────┬──────────────────────────────────────────────────────────────────────────┤
│ ЛЕВЫЙ САЙДБАР (312px) │ ПРАВАЯ РАБОЧАЯ ОБЛАСТЬ (margin="true,true,true,false")                   │
│ (job-candidate-sidebar│ ┌──────────────────────────────────────────────────────────────────────┐ │
│  + edit-sidebar)      │ │ Командный тулбар (candidate-filter-bar):                             │ │
│                       │ │ [Создать] [Редактировать] [Удалить] [Сменить пароль] [Excel]         │ │
│ • Аватар 120×120px    │ ├──────────────────────────────────────────────────────────────────────┤ │
│ • ФИО (h2 bold)       │ │ Generic Filter (collapsable="true" collapsed="true")                 │ │
│ • Логин / Должность   │ ├──────────────────────────────────────────────────────────────────────┤ │
│ • Группа доступа      │ Карточка таблицы (candidate-table-card edit-card):                     │ │
│ • Статус (🟢 / 🔴)    │ │ ┌─────┬──────────┬──────────────┬──────────────┬──────────┬────────┐ │ │
│ • Быстрые действия    │ │ │Фото │ Логин    │ ФИО          │ Должность    │ Группа   │ Статус │ │ │
│   - Редактировать     │ │ ├─────┼──────────┼──────────────┼──────────────┼──────────┼────────┤ │ │
│   - Сменить пароль    │ │ │ 👤  │ alan     │ Ананьев А.   │ Руководитель │ Руковод. │   🟢   │ │ │
│   - Сбросить сессии   │ │ └─────┴──────────┴──────────────┴──────────────┴──────────┴────────┘ │ │
│ • Контакты и реквизиты│ │ RowsCount (компактный счетчик 24px)                                  │ │
│   - Email             │ └──────────────────────────────────────────────────────────────────────┘ │
│   - Telegram          │                                                                          │
│   - Часовой пояс      │                                                                          │
│ • Роли и доступ       │                                                                          │
│   - Список ролей      │                                                                          │
│   - Дашборды / Стат.  │                                                                          │
└───────────────────────┴──────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Требования к визуальному оформлению (UI/UX Дизайнер)

1. **Цветовая палитра сайдбара (как у всех форм)**:
   - Корневой `<layout>` получает stylename `job-candidate-editor edit-screen-layout`.
   - Контейнер сайдбара получает stylename `job-candidate-sidebar edit-sidebar`.
   - Это гарантирует применение темно-синего брендового фона (`#172638`), отступов, скруглений и корректной типографики во всех 7 SCSS-темах HuntTech (`hunttech-modern`, `hunttech-modern-dark`, `hunttech-modern-light`, `halo`, `havana`, `helium`, `hover`).

2. **Шапка профиля пользователя**:
   - Аватар: `ovaFallbackImage id="detailAvatar"` (120×120px, oval, fallback: `icons/no-candidate.png`).
   - Приоритет отображения фото: персональный аватар (`userAvatar`) → официальное фото (`officialPhoto`) → legacy `fileImageFace` через метод `extUser.resolveProfilePhoto()`.
   - Типографика (4 уровня):
     - Уровень 1: ФИО пользователя (`edit-sidebar-title h2 candidate-sidebar-fullname bold`).
     - Уровень 2: Логин / Должность (`edit-sidebar-subtitle h4 candidate-sidebar-position bold`).
     - Уровень 3: Группа доступа (`edit-help candidate-sidebar-city bold`).
     - Уровень 4: Статус активности (`edit-help candidate-sidebar-city`).

3. **Секции сайдбара**:
   - Стилизация: `job-candidate-navigation label-navigation`.
   - Заголовки: `label-nav-title job-candidate-section-title`.
   - Таблицы свойств: `edit-sidebar-summary` с 2 колонками (метрологическая метка / значение).

---

## 4. Требования к целостности данных (Data View Integrity)

1. Используется view `extUser-view`:
   - Включает `_local` (login, name, firstName, lastName, middleName, position, email, active, timeZone, telegram, dashboards, statistics).
   - Включает связи `officialPhoto`, `userAvatar`, `fileImageFace`, `group`, `userRoles.role`.
2. Исключены ошибки `UnfetchedAttributeException` и `LazyInitializationException`.

---

## 5. Интеграция в административную панель

1. **Идентификатор контроллера**: `hunttech_ExtUser.reestr`.
2. **Экранный XML**: `com/company/hunttech/web/screens/extuser/ext-user-reestr.xml`.
3. **Класс контроллера**: `com.company.hunttech.web.screens.extuser.ExtUserReestr` extends `StandardLookup<ExtUser>`.
4. **Регистрация в меню**:
   В `web-menu.xml`:
   ```xml
   <item screen="hunttech_ExtUser.reestr" caption="mainMsg://menu_config.hunttech_ExtUser.reestr"
         icon="USERS" insertBefore="sec$User.browse"/>
   ```
5. **Локализация**:
   - `menu_config.hunttech_ExtUser.reestr=Реестр пользователей`
   - `menu_config.hunttech_ExtUser.reestr=Users Registry`
