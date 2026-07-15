# Координация ChatGPT и Hermes в проекте HRM HuntTech

## Источник истины

Общий рабочий контекст хранится в отдельной служебной ветке:

```text
origin/coordination/active-work
```

Файл текущего состояния:

```text
.ai/active-work.yml
```

Перед началом любой задачи каждый агент обязан выполнить:

```bash
git fetch origin --prune
git show origin/coordination/active-work:.ai/active-work.yml
```

Активная рабочая ветка определяется только полем `work.branch`. Последний допустимый SHA определяется только полем `work.head_sha`.

## Правила

1. Одновременно код изменяет только агент, указанный в `work.owner`.
2. Перед изменением кода агент устанавливает себя владельцем и статус `active`.
3. При `owner: chatgpt` Hermes не изменяет код; при `owner: hermes` ChatGPT не изменяет код.
4. После завершения этапа агент выполняет push в `work.branch`, обновляет `work.head_sha`, заполняет `handoff`, устанавливает `owner: none` и статус `review` либо `ready`.
5. Смена ветки, формы, сущности, сервиса или scope без обновления `.ai/active-work.yml` запрещена.
6. Личный branch Hermes, параллельная реализация, cherry-pick между рабочими ветками, force push и переписывание опубликованной истории запрещены без прямого указания пользователя.
7. Если локальный HEAD, `origin/<work.branch>` и `work.head_sha` не совпадают, работа блокируется до выяснения расхождения.
8. При незакоммиченных изменениях запрещены автоматические `stash`, `reset --hard` и переключение ветки.

## Получение текущей ветки

```bash
ACTIVE_BRANCH=$(git show origin/coordination/active-work:.ai/active-work.yml \
  | sed -n 's/^  branch: //p')

git switch "$ACTIVE_BRANCH"
git pull --ff-only origin "$ACTIVE_BRANCH"
```

## Проверка состояния

```bash
git fetch origin --prune
git status --short --branch
git rev-parse HEAD
git rev-parse "origin/$ACTIVE_BRANCH"
git show origin/coordination/active-work:.ai/active-work.yml
```

## Передача работы

После push рабочего коммита обновить `.ai/active-work.yml` в ветке `coordination/active-work`:

```yaml
work:
  branch: <активная ветка>
  base_sha: <родитель итогового коммита>
  head_sha: <итоговый SHA>
  status: review
  owner: none
  updated_at: <ISO-8601>

handoff:
  previous_agent: Hermes
  summary: >
    Краткое описание выполненного этапа.
  required_actions:
    - следующие проверки или действия
```

Документация проекта ведётся на русском. Во всей документации используется название HRM HuntTech. Изменение экрана, сущности, сервиса, view, loader или JPQL требует синхронного обновления связанной документации.
