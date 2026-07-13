# JobCandidateEdit Component Data Dependencies

Дата: 2026-07-13.

| Область | Данные | Решение |
| ------- | ------ | ------- |
| Первая карточка `jobCandidateCard` | локальные поля кандидата, город, текущая компания, должность, фото, последние проекты, рекомендуемые вакансии | Остаются в initial view или отдельных легких запросах. |
| Верхняя метка CV | факт наличия CV | Заменено на `count(CandidateCV)` без загрузки всей коллекции. |
| Skill box | текст последнего CV | Заменено на ограниченный запрос последнего CV, без загрузки всей коллекции. |
| Вкладка `tabCandidate` | ФИО, компания, должность, город, дата рождения | Компания переведена на `SuggestionPickerField`; city/position оставлены как небольшие справочники. |
| Вкладка контактов | social networks и контактные поля | `socialNetwork` загружается при первом открытии `tabContactInfo`. |
| Вкладка interactions | `iteractionList`, фильтр вакансий, frequent interaction actions | `iteractionList` загружается при первом открытии `tabIteraction`. |
| Вкладка resume | `candidateCv` и CV actions | `candidateCv` загружается при первом открытии `tabResume` narrow view `candidateCV-browse-view`. |
| Comments | comments query и open positions для comment picker | Уже загружается лениво при `commentsTab`. |

