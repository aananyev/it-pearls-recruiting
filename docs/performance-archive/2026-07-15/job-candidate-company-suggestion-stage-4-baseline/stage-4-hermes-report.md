# Stage 4 — Baseline suggestion-поиска компаний

**SHA:** d7a2a828  
**Дата:** 2026-07-15  
**Статус:** DEPLOYED  

Изменений кода нет — только документация и план сравнения.
Текущие параметры поиска:
- `minSearchStringLength=2`
- `suggestionsLimit=50`
- `asyncSearchDelayMs=300`
- Формат: `%$searchString%` (contains)
- Запрос: `lower(e.comanyName) like lower(:searchString)`

Приложение работает, готово к тестированию suggestion-поиска.
