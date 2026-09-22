# Создание Pull Request из ChatGPT Work

## Назначение

Краткая инструкция для публикации изменений HRM HuntTech из ChatGPT Work, в том числе когда локальный `git push` недоступен из-за отсутствия GitHub credentials.

## Порядок работы

1. Получить актуальный `origin/master`, проверить открытые PR, `.ai/tasks/`, документацию и зафиксировать полный `BASE_SHA`.
2. Создать отдельную ветку `agent/<task-name>` от точного `BASE_SHA`. Не использовать устаревший локальный HEAD.
3. Внести минимальные изменения, обновить тесты, документацию и task-чекпоинт. Выполнить доступные проверки и `git diff --check`.
4. Создать локальный commit формата `<type>(<scope>): описание`. Не включать пароли, токены, `.env` и другие секреты.
5. Сначала попробовать обычный `git push -u origin <branch>`.
6. Если push недоступен, использовать подключённый GitHub API:
   - `github.create_branch` — создать удалённую ветку от точного `BASE_SHA`;
   - `github.create_blob` — загрузить полное содержимое каждого изменённого файла;
   - `github.create_tree` — собрать дерево поверх tree SHA базового commit;
   - `github.create_commit` — создать один удалённый commit с родителем `BASE_SHA`;
   - `github.update_ref` — передвинуть удалённую ветку на созданный commit без `force`;
   - `github.create_pull_request` — открыть Draft PR из `agent/<task-name>` в `master`.
7. В описании PR указать цель, scope, изменённые компоненты, тесты, результаты проверок, ограничения и точный проверяемый SHA. До независимой проверки использовать статус `WAITING_FOR_HERMES`.
8. Повторно получить PR через `github.get_pr_info` и проверить: правильные `base/head`, `head_sha`, Draft-статус, число файлов и отсутствие merge-конфликтов.
9. Не выполнять merge и deploy без отдельной команды Алексея.

## Подсказка для нового чата

> Создай стандартный Draft PR HRM HuntTech через подключённый GitHub API. Сначала проверь актуальный `master`, открытые PR и точный `BASE_SHA`. Создай ветку `agent/<task>`, опубликуй изменения через `create_blob → create_tree → create_commit → update_ref`, затем вызови `create_pull_request` в `master`. В PR укажи `WAITING_FOR_HERMES`, точный `head_sha`, проверки и ограничения. После создания проверь PR через `get_pr_info`. Merge и deploy не выполняй.

## История изменений

| Дата | Изменение |
|---|---|
| 2026-09-22 | Создана инструкция публикации PR через GitHub API из ChatGPT Work. |
