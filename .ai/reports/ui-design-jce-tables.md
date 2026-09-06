# СПЕЦИФИКАЦИЯ UI/UX ДЛЯ 5 ТАБЛИЦ JobCandidateEdit
Класс → правило контракта → тема

## Исходные данные
- Анализ: .ai/reports/analysis-jce-tables-before.md
- Контракт: docs/ui/ReestrBrowse_Design_Contract.md (разделы 3, 4, 7, 8, 9)
- Эталон: JobCandidateReestrBrowse, OpenPositionReestrBrowse

## Маппинг таблиц

| Таблица (id) | Текущий класс (stylename) | Правило контракта (целевой stylename) | Темы (все 7 копий) | Особенности |
|---|---|---|---|---|
| socialNetworkTable | job-candidate-table | borderless grid candidate-browse-grid | halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark | editorEnabled=true, word-break для URL |
| lastProjectTable | no-horizontal-lines job-candidate-table | borderless grid candidate-browse-grid no-horizontal-lines | halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark | генераторы колонок, колонка-кнопка addInteractionsViewButton |
| suggestVacancyTable | no-horizontal-lines job-candidate-table | borderless grid candidate-browse-grid no-horizontal-lines | halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark | captionAsHtml=true, иконка статуса 20px |
| jobCandidateIteractionListTable | job-candidate-table | borderless grid candidate-browse-grid | halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark | bodyRowHeight: 36px → 38px, reorderingAllowed=true (не трогать) |
| jobCandidateCandidateCvTable | job-candidate-table | borderless grid candidate-browse-grid | halo, havana, helium, hover, hunttech-modern, hunttech-modern-light, hunttech-modern-dark | bodyRowHeight: 55px (HTML-контент — оставить, обосновать в PR) |

## Правила SCSS (аддитивные изменения в job-candidate-editor.scss)

### Общее правило для всех 5 таблиц
Создать скопнутые селекторы внутри `.job-candidate-editor`, которые наследуют от `.candidate-browse-grid`:

```scss
.job-candidate-editor {
  // Наследуем строку 38px + word-break для всех таблиц с candidate-browse-grid
  .candidate-browse-grid {
    // Уже определён в edit-screen-shared-styles.scss:1017-1051
    // и job-candidate-editor.scss:1518-1536
  }
  
  // Дополнительные правила для таблиц с no-horizontal-lines
  .no-horizontal-lines.candidate-browse-grid {
    .v-grid-row,
    .v-table-row {
      border-bottom: none !important;
    }
  }
  
  // Переопределение bodyRowHeight для iteraction-таблицы
  #jobCandidateIteractionListTable.candidate-browse-grid {
    .v-grid-row,
    .v-table-row {
      min-height: 38px !important; // контракт
    }
  }
  
  // CV-таблица: оставить 55px, но добавить word-break
  #jobCandidateCandidateCvTable.candidate-browse-grid {
    .v-grid-row,
    .v-table-row {
      min-height: 55px !important; // HTML-контент, обосновано в PR
    }
  }
}
```

### Word-break для текстовых колонок
```scss
.job-candidate-editor {
  .candidate-browse-grid {
    // URL в socialNetworkTable
    #socialNetworkTable .v-grid-cell[col-id="networkURLS"],
    #socialNetworkTable .v-table-cell-content[col-id="networkURLS"] {
      word-break: break-all !important;
      overflow-wrap: break-word !important;
    }
    
    // Названия вакансий в suggestVacancyTable и iteractionTable
    #suggestVacancyTable .v-grid-cell[col-id="vacansyName"],
    #jobCandidateIteractionListTable .v-grid-cell[col-id*="vacancy"],
    #jobCandidateCandidateCvTable .v-grid-cell[col-id="toVacancy"] {
      word-break: break-word !important;
      white-space: normal !important;
    }
  }
}
```

### Hover/Selected состояния (п.4.3 контракта)
```scss
.job-candidate-editor {
  .candidate-browse-grid {
    .v-grid-row:hover,
    .v-table-row:hover {
      background: rgba($v-selection-color, 0.07) !important;
    }
    .v-grid-row-selected,
    .v-table-row-selected {
      background: rgba($v-selection-color, 0.16) !important;
    }
  }
}
```

### Кнопки в buttonsPanel (п.4 контракта)
```scss
.job-candidate-editor {
  .candidate-browse-grid {
    .v-buttons-panel .v-button {
      min-height: 34px !important; // candidate-btn height
      min-width: 34px !important;
      border-radius: 4px !important;
      font-size: 12.5px !important;
      font-weight: 600 !important;
    }
  }
}
```

### Колонка-кнопка lastProjectTable (п.5 задачи)
```scss
.job-candidate-editor {
  #lastProjectTable .candidate-browse-grid {
    .v-grid-cell[col-id="idViewIteractionsButton"] .v-button,
    #lastProjectTable .v-table-cell[col-id="idViewIteractionsButton"] .v-button {
      @extend .candidate-btn; // если генератор читает stylename
      // или добавить stylename="candidate-btn" в генераторе (требует проверки)
    }
  }
}
```

## Темы (синхронизация 7 копий)
Все изменения в `job-candidate-editor.scss` должны быть идентично продублированы в:
1. `modules/web/themes/halo/com.company.hunttech/job-candidate-editor.scss` (база)
2. `modules/web/themes/havana/com.company.hunttech/job-candidate-editor.scss`
3. `modules/web/themes/helium/com.company.hunttech/job-candidate-editor.scss`
4. `modules/web/themes/hover/com.company.hunttech/job-candidate-editor.scss`
5. `modules/web/themes/hunttech-modern/com.company.hunttech/job-candidate-editor.scss`
6. `modules/web/themes/hunttech-modern-light/com.company.hunttech/job-candidate-editor.scss`
7. `modules/web/themes/hunttech-modern-dark/com.company.hunttech/job-candidate-editor.scss`

Проверка: md5 всех 7 файлов после правок должен быть идентичен (правило проекта §7.1-7.2 контракта).

## Отступления от контракта (для PR)

1. **jobCandidateCandidateCvTable: bodyRowHeight=55px** — HTML-контент (CV с форматированием) требует большей высоты строки. Обоснование: контракт §3.2 позволяет динамическую высоту многострочных ячеек; 55px — минимально необходимое для читаемости HTML-CV.

2. **lastProjectTable: no-horizontal-lines** — сохранено как часть эталонного вида (задача п.6).

3. **jobCandidateIteractionListTable: reorderingAllowed=true, textSelectionEnabled=false** — поведение, не дизайн (задача п.55), не трогать.

4. **Генераторы колонок (lastProjectTable, suggestVacancyTable)** — бизнес-логика, не трогать (п.55 задачи).
