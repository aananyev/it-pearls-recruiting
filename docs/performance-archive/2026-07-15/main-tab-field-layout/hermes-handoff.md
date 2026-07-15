# Передача Hermes: вкладка «Основное» JobCandidateEdit

**Проект:** HRM HuntTech  
**Ветка:** `agent/job-candidate-main-tab-field-layout-merged`  
**Исходный HEAD перед передачей:** `d0ece5d3f6cae20989595b87ba9ce9c8259833a3`  
**Дата:** 2026-07-15

## Область проверки

В `modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml` вкладка `tabMain` («Основное») переведена на строковую компоновку по шаблону вкладки «Контакты».

Проверить, что:

- `personalDataBlock` и `professionalDataBlock` занимают равные доли ширины;
- каждое поле размещено в отдельной строке `hbox` со стилем `job-candidate-form-row`;
- подписи имеют фиксированную ширину 118 px;
- поля ввода занимают оставшуюся ширину строки;
- видимы и доступны поля «Имя», «Отчество», «Фамилия», «Дата рождения», «Город», «Должность», «Компания», «Доп. позиции»;
- сохранены все существующие component ID, `dataContainer`, `property`, `required`, actions, invoke и JPQL;
- `JobCandidateEdit.java` не изменён;
- бизнес-логика и поведение компонентов не изменены.

## Обязательные команды

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-web:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-web:test \
          --tests "com.company.hunttech.web.screens.jobcandidate.JobCandidateCvInitialViewOptimizerTest" \
          --no-daemon --stacktrace

./gradlew :app-web:test \
          --tests "com.company.hunttech.web.screens.jobcandidate.JobCandidateSocialNetworkInitialViewOptimizerTest" \
          --no-daemon --stacktrace

./gradlew test \
          --tests '*ScreenViewIntegrityTest*' \
          --no-daemon --stacktrace

./gradlew :app-web:buildScssThemes \
          --no-daemon --stacktrace

./gradlew clean assemble \
          --no-daemon --stacktrace
```

Ожидаемый результат `ScreenViewIntegrityTest`: **8/8 PASS**.

## Развёртывание и smoke-тест

Развернуть точный итоговый HEAD ветки в локальном Tomcat и проверить:

```text
http://localhost:8080/hrm/
```

Ожидаемый HTTP-статус: **200**.

Ручные сценарии:

1. Открыть существующего кандидата.
2. Открыть нового кандидата.
3. Проверить вкладку «Основное» в доступных темах приложения.
4. Проверить ввод и изменение ФИО и даты рождения.
5. Проверить lookup города и должности.
6. Проверить поиск, lookup и открытие карточки компании.
7. Проверить отображение дополнительных позиций и действие кнопки добавления.
8. Проверить «Сохранить и закрыть» и «Отмена».
9. Проверить, что вкладка «Контакты» и остальные вкладки не получили визуальных или функциональных регрессий.

## Анализ логов

В логах не должно быть новых ошибок по изменённому сценарию:

- CUBA layout/expand errors;
- `Cannot get unfetched attribute`;
- detached entity errors;
- `IllegalStateException`;
- `NullPointerException`;
- `OutOfMemoryError`.

## Отчёт

Сохранить итоговый отчёт в:

```text
docs/performance-archive/2026-07-15/main-tab-field-layout/hermes-report.md
```

PASS допускается только при проверке точного итогового SHA текущей ветки. При обнаружении дефекта исходный код не изменять без отдельного разрешения Алексея; зафиксировать проблему и приложить логи/скриншоты.
