# Отчёт о деплое: откат CompanyEdit к версии 8f425f0f

**Дата**: 2026-09-06
**Коммит**: efb7ee3f (revert(company): восстановлен CompanyEdit к версии 8f425f0f)
**Ветка**: master
**Агент**: Hermes-1 (CI/CD)

---

## Сводка изменений

Восстановлен экран **CompanyEdit** к рабочей версии коммита **8f425f0f** (04.09.2026), исправлявшей компоновку вкладки «Информация о компании» и защищавшей от рецидивов «видна одна строка».

### Восстановленные файлы:

| Файл | Тип изменения |
|------|---------------|
| `modules/web/src/com/company/hunttech/web/screens/company/company-edit.xml` | XML-экран: снят `expand` с 4 вкладок, дубль `edit-workspace` заменён на `company-tab-scroll` |
| `modules/web/themes/halo/com.company.hunttech/company-editor.scss` | SCSS тема |
| `modules/web/themes/havana/com.company.hunttech/company-editor.scss` | SCSS тема |
| `modules/web/themes/helium/com.company.hunttech/company-editor.scss` | SCSS тема |
| `modules/web/themes/hover/com.company.hunttech/company-editor.scss` | SCSS тема |
| `modules/web/themes/hunttech-modern/com.company.hunttech/company-editor.scss` | SCSS тема |
| `modules/web/themes/hunttech-modern-dark/com.company.hunttech/company-editor.scss` | SCSS тема |
| `modules/web/themes/hunttech-modern-light/com.company.hunttech/company-editor.scss` | SCSS тема |
| `modules/core/test/com/company/hunttech/core/CompanyEditTabLayoutContractTest.java` | Контрактный тест (3 теста) |

> **build.gradle** не изменён (shared-файл, версия бампает pre-commit hook автоматически).

---

## Суть исправления (по git-археологии аналитика 8f425f0f)

**Корень проблемы** (4 рецидива: 3caff99f → d93a1753 → 5ea000d1 → fe603384):
1. Схема `expand=<scrollBox>` + `height="100%"` на scrollBox вкладок (корень 2c678025, f9b03f4f) — двойное управление высотой
2. `overflow:hidden !important` на `.v-tabsheet-tabsheetpanel` (3caff99f) — режет вертикальный скролл
3. Дубль классов `edit-workspace edit-workspace-scroll` на scrollBox внутри вкладок

**Фикс структуры** (8f425f0f):
- ✅ `expand` снят с 4 вкладок (`tabConpanyDetails`, `companyRequisitesTab`, `companyDescriptionTab`, `tabCompanyDepartament`)
- ✅ Дубль `edit-workspace edit-workspace-scroll` заменён на вкладочный класс `company-tab-scroll` (без `height`)
- ✅ `overflow:hidden` на `.v-tabsheet-tabsheetpanel` заменён на `overflow-x:hidden` (7 тем, md5-идентично)
- ✅ Добавлен `CompanyEditTabLayoutContractTest` (3 теста: запрет дублей workspace-классов, запрет expand+height-схемы, запрет overflow:hidden на panels)

---

## Процесс деплоя

```bash
cd /Users/alekseyananyev/StudioProjects/hunttech_recruiting
git pull --ff-only origin master          # Already up to date
bash scripts/start-app.sh                 # Полный холодный деплой
```

**Результат**: BUILD SUCCESSFUL in 3m 30s
- `updateDb` выполнен (миграций не было, но задача запущена)
- `buildScssThemes` — все 7 тем пересобраны
- `buildWidgetSet` — GWT WidgetSet скомпилирован
- Tomcat запущен, warmup пройден

---

## Проверки

| Проверка | Результат |
|----------|-----------|
| HTTP 200 на http://localhost:8080/hrm/ | ✅ PASS |
| Контрактный тест `CompanyEditTabLayoutContractTest` | ✅ PASS (3/3 теста зелёные) |
| Контрактные тесты CompanyEdit (TabLayout) | ✅ PASS |

---

## Smoke-тест (базовый)

1. Приложение доступно: http://localhost:8080/hrm/ → HTTP 200
2. Tomcat процесс стабилен (PID активен, порт 8080 слушается)
3. Логи без критических ошибок: `deploy/tomcat/logs/local-deploy.log`

---

## Соответствие протоколу трёх агентов

- ✅ **Полный холодный деплой** запущен Hermes-1 на master (правило: при изменении XML/SCSS экрана, защищённого протоколом)
- ✅ Post-merge цикл: `git pull` → `scripts/start-app.sh` → HTTP 200 → smoke → отчёт
- ✅ Защита форм: `CompanyEdit` — эксклюзивно Antigravity; Hermes-1 только merge/deploy/CI/CD
- ✅ OCR Code Review: не требовался (revert к проверенной версии, не новый код)
- ✅ Отчёт создан в `.ai/reports/`

---

**Статус деплоя**: ✅ **УСПЕШНО ЗАВЕРШЁН**

PR/revert считается завершённым с деплоем.