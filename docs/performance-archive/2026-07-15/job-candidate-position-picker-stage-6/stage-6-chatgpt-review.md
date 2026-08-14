# Review Stage 5 и Stage 6 — JobCandidateEdit

**Проект:** HRM HuntTech  
**Дата:** 2026-07-15  
**Проверяющий:** ChatGPT  
**Ветка:** `agent/job-candidate-progressive-loading-stage-3-social-networks`

## 1. Stage 5

Ожидаемый отчёт:

```text
docs/performance-archive/2026-07-15/
job-candidate-reference-loaders-stage-5-baseline/
stage-5-reference-loaders-hermes-report.md
```

в репозитории отсутствует.

Между базовым HEAD Stage 5 и началом Stage 6 не найдено коммитов с результатами runtime-замеров `citiesDl` и `personPositionsLc`. Поэтому следующие выводы Stage 5 не считаются доказанными:

- SQL P50/P95 загрузки городов и должностей;
- размер сериализованных коллекций;
- доля loaders во времени открытия экрана;
- необходимость изменения типа поля города;
- необходимость отложенной загрузки справочников.

Stage 5 закрыт пользователем как организационный этап, но не используется как источник измерительных данных.

## 2. Stage 6

### 2.1 Проверенный коммит

```text
7f25633a3c96971dbf923d0f51f77424068ddc33
```

### 2.2 Фактический diff

Изменён только файл:

```text
modules/web/src/com/company/hunttech/web/screens/jobcandidate/job-candidate-edit.xml
```

Единственное функциональное изменение:

```diff
- <view extends="position-view"/>
+ <view extends="position-picker-view"/>
```

Не изменены:

- `personPositionsDc`;
- `personPositionsLc`;
- JPQL и фильтр `%(не использовать)%`;
- сортировка `positionRuName`;
- `cacheable="true"`;
- `personPositionField`;
- `optionsContainer="personPositionsDc"`;
- property `personPosition`;
- действия `lookup` и `open`;
- Java-контроллер;
- `views.xml`;
- сущности и БД.

### 2.3 Корректность view

`position-view` наследует `_local` и загружает весь локальный набор полей `Position`.

`position-picker-view` наследует `_minimal` и явно содержит:

- `positionRuName`;
- `positionEnName`.

`JobCandidateEdit` использует у выбранной должности `positionRuName`; это поле присутствует в новом view.

### 2.4 Верификация

Hermes сообщил:

- `VERIFICATION: PASS (5/5)`;
- `position-picker-view` установлен;
- `position-view` удалён из `personPositionsDc`;
- `optionsContainer` сохранён;
- коммит запушен;
- `/hrm` возвращает HTTP 200.

Пользователь подтвердил успешность проверки и разрешил следующий этап.

## 3. Замечания к Definition of Done

В коммите `7f25633a` отсутствовали:

- обновление `docs/ui/JobCandidateEdit_Spec.md`;
- отчёт `stage-6-position-picker-hermes-report.md`;
- репозиторные логи `ScreenViewIntegrityTest` и `clean assemble`.

Документация синхронизируется отдельным коммитом ChatGPT. Результаты runtime smoke-test принимаются по прямому подтверждению пользователя и Hermes.

## 4. Вердикт

```text
STAGE_6_ACCEPTED
```

Причины:

- diff минимален и полностью соответствует контракту;
- нужные picker-поля присутствуют в view;
- бизнес-логика и UI actions не изменены;
- приложение развёрнуто и отвечает HTTP 200;
- пользователь подтвердил успешное тестирование.

Переход к Stage 7 разрешён. Из-за отсутствия измерительного отчёта Stage 5 Stage 7 не должен менять поле города или его loader без отдельного runtime baseline.
