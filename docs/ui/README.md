# UI-спецификации HRM HuntTech

Каталог содержит канонические технические спецификации экранов и фрагментов CUBA Platform. Каждый документ начинается с Business & Context Intro, описывает data containers, loaders, lifecycle, actions, визуальную компоновку и историю изменений.

## Экраны

| Экран | Документ | Назначение |
|---|---|---|
| `JobCandidateEdit` | [JobCandidateEdit_Spec.md](JobCandidateEdit_Spec.md) | Карточка кандидата, вкладки, история вакансий, анализ навыков и защита от OOM |
| `CandidateCVEdit` | [CandidateCVEdit_Spec.md](CandidateCVEdit_Spec.md) | Редактор резюме, ленивое чтение текста и безопасная загрузка фотографии |
| `AiPromptTemplateBrowse` | [AiPromptTemplateBrowse_Spec.md](AiPromptTemplateBrowse_Spec.md) | Локализованный список системных промптов AI |
| `AiPromptTemplateEdit` | [AiPromptTemplateEdit_Spec.md](AiPromptTemplateEdit_Spec.md) | Читаемый редактор системного промпта с фиксированной панелью действий |
| `UserAiConfigurationBrowse` | [UserAiConfigurationBrowse_Spec.md](UserAiConfigurationBrowse_Spec.md) | Настройка API и выбор текущей нейросети для AI-анализа |
| `UserAiConfigurationEdit` | [UserAiConfigurationEdit_Spec.md](UserAiConfigurationEdit_Spec.md) | Читаемый редактор подключения к AI API с резервным состоянием по умолчанию |

## Правила актуализации

При изменении XML-дескриптора, Java-контроллера, loader, view, JPQL, actions или поведения экрана соответствующая спецификация обновляется в той же сессии. Новая запись истории изменений добавляется первой строкой таблицы с датой `YYYY-MM-DD`.
