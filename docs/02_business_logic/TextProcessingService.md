# Сервис умной текстовой обработки и AI-форматирования (`TextProcessingService`)

## 1. Назначение и архитектура

Сервис `hunttech_TextProcessingService` (`com.company.hunttech.service.TextProcessingService`) предназначен для структурирования и качественного оформления текстов резюме кандидатов, описаний вакансий и сопроводительных писем в HRM HuntTech.

### Основные принципы:
1. **Сохранение фактов и данных (Non-destructive):**
   * Сервис строго сохраняет все исходные данные (даты, компании, контакты, стек технологий, формулировки). Текст не сокращается, не перефразируется и не искажается.
2. **Типографическое AI-форматирование:**
   * Форматирование в **HTML** (`formatHtml`): оформление разделов (Опыт работы, Образование, Навыки, Контакты и т.д.), маркированных списков (`<ul><li>`), аккуратных абзацев (`<p>`).
   * Форматирование в **Plain Text** (`formatPlainText`): читаемые текстовые разделители разделов (`═══ НАВЫКИ ═══`), выравнивание списков с маркером `•`.
3. **Отказоустойчивость (AI Fallback):**
   * При недоступности AI Control Plane (отсутствие ключей, сбой сети) сервис автоматически переключается на встроенный регулярный типографический движок `formatHtmlLocally` / `formatPlainTextLocally`.

---

## 2. API сервиса

```java
public interface TextProcessingService {
    String NAME = "hunttech_TextProcessingService";
    String FUNCTION_TEXT_SMART_FORMAT_HTML = "TEXT_SMART_FORMAT_HTML";
    String FUNCTION_TEXT_SMART_FORMAT_PLAIN = "TEXT_SMART_FORMAT_PLAIN";
    String PARAM_SOURCE_TEXT = "sourceText";

    String formatHtml(String rawText);
    String formatPlainText(String rawText);
}
```

---

## 3. Интеграция в пользовательский интерфейс (`CandidateCVEdit`)

1. **Выпадающее меню «Действия» (`cvActionsPopupButton`):**
   * Расположено над `candidateCVRichTextArea` с выравниванием по правому краю (`align="MIDDLE_RIGHT"`).
   * Содержит действия:
     * **«Сканировать навыки»** (AI анализ и сохранение в `CandidateSkill`)
     * **«Преобразование»** (умное AI-форматирование текста в RichTextArea через `textProcessingService.formatHtml(...)`)
     * **«Сканировать резюме»** (классический regex-парсер)
     * **«Распознавание»** (извлечение контактов)
     * **«Исходное»** (переключение между форматированным/исходным текстом).
2. **Sidebar «Резюме для вакансии»:**
   * Убрана тяжелая рамка карточки (`border: 0; background: transparent; padding: 0;`).
   * Уменьшена ширина лейблов до 52px, что высвободило максимум пространства под значения должности, вакансии и проекта.
3. **Label-навигация «Разделы вкладки»:**
   * Убран лишний интервал перед текстом навигационных пунктов, высота приведена в точное соответствие с надписью (`min-height: 26px; padding: 3px 8px; line-height: 16px;`).
