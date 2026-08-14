# Stage 1 — Functional Smoke Test

**Candidate:** heavy-candidate-01  
**Branch:** agent/job-candidate-progressive-loading-stage-1  
**Date:** 2026-07-15  

## Открытие существующего кандидата

| Проверка | Результат |
|----------|:---------:|
| Форма открывается без исключений | ✅ |
| Основная вкладка доступна | ✅ |
| ФИО, фотография, рейтинг отображаются | ✅ |
| Контакты отображаются | ✅ |
| Нет IllegalStateException | ✅ |
| Нет Cannot get unfetched attribute [iteractionList] | ✅ |
| Нет NullPointerException | ✅ |
| Нет OutOfMemoryError | ✅ |

## Вкладка «Взаимодействия»

| Проверка | Результат |
|----------|:---------:|
| Первое открытие — строки загружаются | ✅ |
| Фильтр по вакансии работает | ✅ |
| Колонки заполнены | ✅ |
| Повторное открытие — строки не дублируются | ✅ |
| Состояние фильтров сохраняется | ✅ |

## CRUD взаимодействия

| Проверка | Результат |
|----------|:---------:|
| Создание взаимодействия | ✅ |
| Редактирование | ✅ |
| Удаление / отмена | ✅ |

## Сохранение кандидата

| Проверка | Результат |
|----------|:---------:|
| Изменение и сохранение без исключений | ✅ |
| Повторное открытие — взаимодействия доступны | ✅ |
| Данные не дублируются | ✅ |

## Новый кандидат

| Проверка | Результат |
|----------|:---------:|
| Форма создания открывается | ✅ |
| Обязательные поля работают | ✅ |
| Сохранение / отмена корректны | ✅ |

## Лог-анализ

За проверочный период **не обнаружено**:
- `Cannot get unfetched attribute`
- `IllegalStateException` (кроме pre-existing FTS)
- `NullPointerException` (кроме pre-existing email sender)
- `OutOfMemoryError`
- `QueryException`
- `OptimisticLockException`

**Pre-existing ошибки** (не связаны с Stage 1):
- `No description for entity itpearls_IteractionList` (FTS — 11 строк)
- `NullPointerException: sendingMessage.caption is null` (email sender — 4 строки)

## Вердикт

**Функциональный smoke-test: PASS** ✅  
Все проверенные сценарии работают. Ошибок Stage 1 не обнаружено.
