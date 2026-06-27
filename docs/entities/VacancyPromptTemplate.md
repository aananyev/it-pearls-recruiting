# VacancyPromptTemplate — шаблон промпта вакансии

> Cross-link: [itpearls_VacancyPromptTemplate.browse_Spec.md](../ui/itpearls_VacancyPromptTemplate.browse_Spec.md) · [itpearls_VacancyPromptTemplate.edit_Spec.md](../ui/itpearls_VacancyPromptTemplate.edit_Spec.md)

---

## Business & Context Intro

### Назначение и Бизнес-смысл (What & Why)

Хранит тексты промптов и параметры (температура, системный контекст) для AI-стандартизации описаний вакансий через `HrmAiService`.

### Связи в интерфейсе и Навигация (UI Context & Navigation)

Меню **Управление AI** → browse/edit `itpearls_VacancyPromptTemplate`. Используется сервисом по уникальному `code`.

### Краткий обзор бизнес-логики поведения (Behavior Summary)

Администратор CRUD шаблонов; при вызове AI сервис загружает шаблон по коду и передаёт `promptText` / `systemContext` / `temperature` провайдеру.

---

## 1. Архитектура Сущности (Data Model Layer)

| Поле | Тип | Описание |
|------|-----|----------|
| code | String(64), unique | Код шаблона |
| name | String(255) | Наименование |
| promptText | LOB | Текст промпта |
| systemContext | String(1000) | Системный контекст |
| temperature | Double | Температура (default 0.7) |

Таблица: `ITPEARLS_VACANCY_PROMPT_TEMPLATE`. Миграция: `260627-3-createVacancyPromptTemplate`.

---

## 2. Интерфейсный Слой (UI & Layout)

| View | Назначение |
|------|------------|
| `vacancyPromptTemplate-browse-view` | Browse: code, name, temperature |
| `vacancyPromptTemplate-edit-view` | Edit: все поля |
| `vacancyPromptTemplate-view` | Полный _local |

| Экран | Controller |
|-------|------------|
| Browse | `itpearls_VacancyPromptTemplate.browse` |
| Edit | `itpearls_VacancyPromptTemplate.edit` |

---

## 3. Бизнес-логика (Controller Layer)

`HrmAiServiceBean` — загрузка по `code`, вызов AI-провайдера.

---

## 4. Взаимодействие компонентов

`HrmAiService` ← `VacancyPromptTemplate` + `UserAiConfiguration`.

---

## 5. Инструкция по развертыванию (Deployment Guide)

Миграция Liquibase `260627-1-addAiEntities.xml`; пересборка `app-global`, `app-web`.

---

## История изменений

| Дата | Изменение |
|------|-----------|
| 2026-06-27 | Browse/edit экраны, views `vacancyPromptTemplate-browse-view`, `vacancyPromptTemplate-edit-view`, меню aiAdministration |
| 2026-06-27 | Создание сущности и миграции AI |
