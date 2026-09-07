# ПРОДОЛЖЕНИЕ РАБОТЫ ChatGPT: PR #230 feat/llm-chat (LLM-чат) после паузы по лимитам

Дата: 2026-09-05 · От: Hermes-1 (CI/CD) · Для: ChatGPT

## ШАГ 0 — Восстанови контекст (не полагайся на память сессии)
1. git fetch; checkout feat/llm-chat; git rebase origin/master
   (ветка отстала от master на 7 коммитов; конфликт в build.gradle по
   версии — резолв в пользу старшей версии из master, 0.441+).
2. Прочитай свои канонические статусы: docs/ai/LLM_CHAT_CURRENT_STATUS.md
   и docs/ai/LLM_CHAT_RELEASE_READINESS_REPORT.md — там точка остановки.
3. ⚠️ ИЗМЕНЕНИЯ ПРОТОКОЛА, пока ты паузил (.ai/instructions/
   three-agent-git-protocol-2026-08-15.md, коммиты cb07a229/6792209e):
   формы CompanyEdit, ExtSettingsWindow, ExtUserEdit — ЭКСКЛЮЗИВ Antigravity
   (их XML/SCSS/контрактные тесты не трогать). Плюс у Hermes-2 активные
   задачи по open-position-edit.xml и job-candidate-edit.xml — их тоже не
   трогать. После rebase проверь:
   git diff --name-only origin/master...HEAD | grep -iE "company-edit|ext-settings|ext-user|open-position-edit|job-candidate-edit"
   — вывод обязан быть пуст; если что-то есть — убери до продолжения.

## ШАГ 1 — Продолжи с точки остановки (этап 4, безопасность и данные)
Закрой оставшийся security/data checklist этапа 4 (цитата из твоего статуса):
доказать lifecycle ключей, отсутствие секретов в UI/log/error/audit,
retention и consent/privacy version; remediation legacy plaintext keys.

## ШАГ 2 — Далее по roadmap: этап 5
Authenticated staging через proxy, push/recovery, provider sandbox,
quota/fallback, concurrency, accessibility, regression.
(Миграции 260904-1..6 из твоего PR применяются только после мержа, на dev-БД —
это работа Hermes-1, не запускай их сам.)

## ДИСЦИПЛИНА (обязательно)
- Каждый завершённый срез: коммит (русское сообщение) +
  git push origin HEAD:feat/llm-chat.
- В ТОМ ЖЕ коммите обновляй docs/ai/LLM_CHAT_CURRENT_STATUS.md: что сделано,
  что осталось, следующий шаг. Это твоя страховка от исчерпания лимитов —
  любая новая сессия возобновляется с этого документа.
- Если лимиты кончаются посреди среза: зафиксируй WIP-коммитом точное
  состояние в статус-документ и запушь — лучше «остановлен на X» в git,
  чем потерянный контекст.
- НЕ мержи PR сам, не трогай master, прод hr.hunttech.ru и общую копию
  hunttech_recruiting — мерж и деплой за Hermes-1.
- Локальные gradle-прогоны — только если у тебя есть доступ к машине и
  обёртке scripts/agent-gradle.sh; иначе помечай в PR «не собиралось локально» —
  проверит Hermes-1.
- Перед финальной готовностью PR: ocr review --audience agent (Alibaba OCR)
  на diff ветки vs origin/master, результат в описание PR.

## ОТЧЁТ
По завершении этапа 4 (или при блокере) — сообщение пользователю для
передачи Hermes-1: что закрыто из checklist, evidence (файлы/тесты),
что осталось на этап 5.
