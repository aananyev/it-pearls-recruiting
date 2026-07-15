# Main Tab Refactoring — Hermes Report

**SHA:** 1f244527
**Date:** 2026-07-15
**Branch:** agent/job-candidate-main-tab-field-layout-merged

## Changes
- Город перенесён из левой карточки в правую
- Левая карточка: Имя, Отчество, Фамилия, Дата рождения (4 строки)
- Правая карточка: Город, Должность, Компания, Доп. позиции (4 строки)
- Ширина подписей: 80px (как во вкладке Контакты)

## Verification
- JobCandidateEdit.java: NOT modified
- Component IDs: preserved
- Data bindings: preserved
- JPQL: preserved
- HTTP 200
