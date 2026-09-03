---
name: "hunttech-vacancy-opening"
description: "Use when opening or filling HuntTech HRM vacancies from customer vacancy text, especially SSP vacancies, including standardized vacancy description, checklist, search map, interview plan, and HRM field population."
---

# HuntTech Vacancy Opening

Use this skill when creating or filling an `OpenPosition` in HuntTech HRM from a customer vacancy description.

## Core Rules

- Source of truth for candidate-facing content is only the customer vacancy text.
- If data is missing, unclear, disputed, or not stated, use exactly:
  `НЕТ ДАННЫХ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.`
- If data is contradictory, use exactly:
  `УКАЗАНЫ ПРОТИВОРЕЧИВЫЕ ДАННЫЕ, УТОЧНЯЙТЕ У РЕКРУТЕРА НА СОБЕСЕДОВАНИИ.`
- Do not invent salary, bonuses, compensation, contacts, company facts, or external context.
- Do not change prod unless the user explicitly approves the specific write operation.

## SSP 62630 Defaults

When opening SSP vacancy `62630`, use the following confirmed values unless the user overrides them:

- Project: `SSP "Сбытовая и сервисная компания немецкого концерна. Проект  Лейсан Шестаковой /Штат HuntTech ТК/ГПХ или ИП. Актирование 3 месяца/`
- Customer-side responsible person: Ляйсан Шестакова.
- HuntTech owner: Ольга Кожевникова.
- Number of positions: `1`.
- Priority: normal.
- Work format: remote.
- City/location: `Регионы РФ Москва +/- 2 часа`.
- Grade: `Senior`.

## Prompt References

Load only the prompt needed for the field being generated:

- Standardized vacancy description: `references/standardized-description-prompt.txt`.
- Must-have checklist: `references/checklist-prompt.txt`.
- Search map / recruiter instruction: `references/search-map-prompt.txt`.
- Selling interview plan: `references/interview-plan-prompt.txt`.

More prompt references may be added as the user provides them.
