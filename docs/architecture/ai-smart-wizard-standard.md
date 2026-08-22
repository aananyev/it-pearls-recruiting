# Архитектурный стандарт проектирования умных мастеров (Smart AI Wizards) в HRM HuntTech

> Связанный скилл: [.agents/skills/hunttech-ai-wizard-design/SKILL.md](../../.agents/skills/hunttech-ai-wizard-design/SKILL.md)

---

## 1. Назначение и концепция

Умные мастера (Smart AI Wizards) в HRM HuntTech автоматизируют рутинный ввод данных для ключевых бизнес-сущностей системы (Кандидаты, Вакансии, Компании/Контрагенты, Проекты). Мастер позволяет пользователю найти или загрузить информацию из любых внешних источников (интернет-поиск по названию/ИНН, файлы резюме/карточек, произвольный текст, URL-ссылки) и в один клик перенести структурированные реквизиты в экран редактирования.

---

## 2. Сквозная архитектура (4 уровня)

```
[ GLOBAL MODULE ]
  ├── DTO: CompanyRequisitesParsedData / VacancyParsedData / CvParsedData
  └── Service Interface: CompanySearchAiService (NAME, FUNCTION_CODE)

[ CORE MODULE ]
  ├── Service Bean: CompanySearchAiServiceBean (AiExecutionService, fallback, JSON parsing)
  ├── DB SQL: modules/core/db/update/postgres/26/26xxxx-x-addAiFunction.sql
  └── Liquibase XML: modules/core/db/changelog/26xxxx-x-addAiFunction.xml (CDATA, dbms, HALT)

[ WEB MODULE ]
  ├── Spring Config: web-spring.xml (cuba_WebRemoteProxyBeanCreator entry)
  ├── Wizard Screen: smart-...-upload-screen.xml + Smart...UploadScreen.java (960×760px, modal)
  └── Parent Editor: CompanyEdit.xml + CompanyEdit.java (кнопка MAGIC, DataContext merge)

[ QUALITY ASSURANCE ]
  ├── Unit/Contract Tests: WebServiceProxyRegistryContractTest + WizardContractTest
  └── Code Review: Alibaba OCR (`ocr review --audience agent`) перед коммитом и деплоем
```

---

## 3. Чеклист разработки и проверки визарда

1. **DTO**: Создать POJO в `modules/global/...` с геттерами/сеттерами и сериализацией.
2. **Global Service**: Объявить интерфейс с константами `NAME = "hunttech_..."` и кодом AI-функции.
3. **Core Bean**: Реализовать вызов `AiExecutionService.executeText(...)`, резервный вызов базовых функций и гибкий парсинг JSON (`candidates`, `items`, `data`, flat) с проверкой `item.isObject()`.
4. **Seed SQL**: Создать скрипт добавления AI-функции в `HUNTTECH_AI_FUNCTION_CONFIGURATION`.
5. **Liquibase XML**: Создать файл с `<preConditions onFail="HALT"><tableExists tableName="HUNTTECH_AI_FUNCTION_CONFIGURATION"/></preConditions>`, `dbms="postgresql"` и `<![CDATA[ ... ]]>`, подключить в `db.changelog-master.xml`.
6. **web-spring.xml**: Зарегистрировать proxy в секции `remoteServices`.
7. **XML Диалога**: Создать форму 960×760px с 4 вкладками, блоком выбора кандидатов (`candidatesCard`), блоком предпросмотра (`previewCard`) и проверкой дубликатов (`duplicateBox`).
8. **Java Диалога**: Асинхронный запуск через `BackgroundTask`, поддержка `setInitialSearchParams(...)`.
9. **Родительская форма**: Вызов диалога по кнопке `font-icon:MAGIC`, применение данных к `getEditedEntity()`, привязка связанных сущностей через `dataContext.merge(...)`.
10. **Контрактный тест и OCR**: Написать автотест, запустить сборку и проверку `ocr review --audience agent`.

---

## 4. История изменений

| Дата | Изменение |
|------|-----------|
| 2026-08-22 | Разработан и стандартизирован сквозной архитектурный пайплайн Smart AI Wizards на примере мастера поиска и автозаполнения карточки компании в `CompanyEdit` |
