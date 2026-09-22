# Задание для ChatGPT (LLM-чат разработка): безопасная синхронизация PR из GitLab

## Цель
Получить все актуальные изменения из `origin/master` (GitHub/GitLab) в песочницу разработки LLM-чата, **не ломая текущую работу**.

## Ограничения (КРИТИЧНО)
- ❌ **НЕ деплоить** ничего
- ❌ **НЕ запускать** приложение/тесты
- ❌ **НЕ делать** `gradle clean` / `build` / `deploy`
- ❌ **НЕ трогать** `build.gradle` (версия бампается pre-commit хуком)
- ✅ ТОЛЬКО `git fetch` + `git merge`/`rebase` с конфликтами
- ✅ Конфликты решать **вручную**, беря **обе стороны** (shared файлы)

---

## Протокол безопасной синхронизации

### 1. Подготовка (зафиксировать текущее состояние)
```bash
cd /path/to/llm-chat-sandbox

# 1.1 Сохранить незакоммиченные правки (если есть)
git stash push -m "llm-chat-wip-$(date +%Y%m%d-%H%M%S)"

# 1.2 Запомнить текущую ветку и коммит
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
CURRENT_COMMIT=$(git rev-parse HEAD)
echo "Было: $CURRENT_BRANCH @ $CURRENT_COMMIT"
```

### 2. Синхронизация с upstream
```bash
# 2.1 Получить обновления
git fetch origin

# 2.2 Проверить, что изменилось в master
git log --oneline HEAD..origin/master | head -20
git diff --name-only HEAD..origin/master

# 2.3 Если своих коммитов нет — fast-forward
if [ "$(git rev-parse HEAD)" = "$(git merge-base HEAD origin/master)" ]; then
    git merge --ff-only origin/master
    echo "✅ Fast-forward merge выполнен"
else
    # 2.4 Если есть свои коммиты — rebase с ручным разрешением конфликтов
    git rebase origin/master
    # При конфликтах:
    #   git status          # увидеть UU файлы
    #   git diff            # посмотреть конфликты
    #   Ручками оставить ОБЕ СТОРОНЫ в shared файлах:
    #     - build.gradle (версия не трогать)
    #     - messages*.properties
    #     - styles.scss (7 тем)
    #     - docs/README.md
    #   git add <resolved>
    #   git rebase --continue
fi
```

### 3. Проверка результата
```bash
# 3.1 Статус
git status
git log --oneline -5

# 3.2 Вернуть стэш (если был)
if git stash list | grep -q "llm-chat-wip"; then
    git stash pop
    echo "✅ WIP восстановлен"
fi

# 3.3 Проверить, что своя работа на месте
git diff HEAD~1 --name-only  # свои изменения
```

### 4. Shared-файлы: правила разрешения конфликтов

| Файл | Стратегия |
|------|-----------|
| `build.gradle` | **Только pre-commit хук бампает версию**. В конфликте — оставить свою версию + их изменения (кроме version) |
| `messages.properties` / `messages_ru.properties` | **Объединить ключи** — оставить все уникальные ключи из обеих сторон |
| `themes/hunttech-*/styles.scss` | **Объединить** — 7 тем должны быть идентичны master + свои кастомные стили |
| `web-menu.xml` | **Объединить пункты меню** |
| `docs/README.md` | **Объединить секции** |

---

## Чек-лист перед продолжением работы

- [ ] `git status` чистый (кроме своих WIP изменений)
- [ ] `git log --oneline -1` показывает актуальный master + свои коммиты поверх
- [ ] Нет маркеров конфликта `<<<<<<< HEAD` в коде
- [ ] `build.gradle` версия не откатилась назад
- [ ] Свои незакоммиченные правки восстановлены из stash

---

## Если что-то пошло не так

```bash
# Откатить rebase/merge
git rebase --abort
# или
git merge --abort

# Вернуться к сохранённому состоянию
git reset --hard $CURRENT_COMMIT
git stash pop  # если был stash
```

---

## Контекст для понимания

**Что нового в master (последние PR):**
- PR #244: Удалена `CompanyReestrEdit`, все вызовы → `CompanyEdit` (@PrimaryEditorScreen)
- PR #245: Исправлен widgetset (копирование в VAADIN/widgetsets)
- PR #246: Восстановлен sidebar scrollBox (7ec7a843) в CompanyEdit
- Оптимизация запуска: JPDA отключён, Telegram Bot остановка в @PreDestroy
- Удалены .old миграции

**Твоя задача (LLM-чат):** продолжать разработку чата, не затрагивая CompanyEdit/CompanyReestrBrowse.

---

## Команды для копирования в терминал

```bash
# === БЕЗОПАСНАЯ СИНХРОНИЗАЦИЯ ===
cd /path/to/llm-chat-sandbox
git stash push -m "llm-chat-wip-$(date +%Y%m%d-%H%M%S)"
git fetch origin
git log --oneline HEAD..origin/master | head -20
# Если своих коммитов нет:
git merge --ff-only origin/master
# Если есть свои коммиты:
git rebase origin/master
# ... разрешить конфликты вручную (обе стороны) ...
git status
git stash pop
echo "✅ Синхронизация завершена. Продолжай разработку."
```