#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Правки job-candidate-editor.scss (эталон hover) по дизайн-ревью 2.2.
Применяются к одному файлу, затем копируются во все 7 тем."""
import io, sys, shutil

SCSS = 'modules/web/themes/hover/com.company.hunttech/job-candidate-editor.scss'

with io.open(SCSS, encoding='utf-8') as f:
    src = f.read()

EDITS = []
def edit(desc, old, new, count=1):
    EDITS.append((desc, old, new, count))

# 1. Комментарий эталона (P1-1)
edit('комментарий навигации',
'''    /* Эталон label-навигации — IteractionListEdit (iteraction-list-visual-alignment.scss:112–144, вариант A):
       полная геометрия пункта (min-height 38px, height auto, padding 8×10px, скругление справа)
       + hover (белый на белом .08) + active (жёлтый #ffb11b с заливкой и левой границей). */''',
'''    /* Эталон label-навигации — IteractionListEdit (iteraction-list-visual-alignment.scss:112–161):
       полная геометрия пункта (min-height 27px, height auto, padding 3×10px, скругление справа)
       + hover (белый на белом .08) + active (жёлтый #ffb11b с заливкой и левой границей). */''', 1)

# 2. Геометрия пунктов навигации 38→27 / 8→3 / 18→20 (P1-1)
edit('nav min-height 27px', '      min-height: 38px !important;', '      min-height: 27px !important;', 1)
edit('nav padding 3px 10px', '      padding: 8px 10px !important;', '      padding: 3px 10px !important;', 1)
edit('nav line-height 20px',
     '      font-size: 13px !important;\n      font-weight: 600 !important;\n      line-height: 18px !important;',
     '      font-size: 13px !important;\n      font-weight: 600 !important;\n      line-height: 20px !important;', 1)

# 3. Убрать декоративный маркер ▼ (P1-4)
edit('удалить :before ▼',
'''    .job-candidate-accordion-header:before {
      content: "\\25BC";
      display: inline-block;
      margin-right: 10px;
      color: #0b63b6;
      font-family: Arial, "Segoe UI Symbol", sans-serif;
      font-size: 13px;
      line-height: 20px;
    }

''', '', 1)

# 4. Flex только для .job-candidate-card-row (P1-2)
edit('flex только card-row',
'''    .job-candidate-accordion-open .job-candidate-accordion-content,
    .job-candidate-card-row {
      display: flex !important;
      gap: 16px;
      align-items: flex-start;
    }

    .job-candidate-accordion-open .job-candidate-accordion-content > .v-slot,
    .job-candidate-card-row > .v-slot {
      width: calc(50% - 8px) !important;
      min-width: 0 !important;
      max-width: calc(50% - 8px) !important;
      flex: 1 1 0 !important;
    }''',
'''    .job-candidate-card-row {
      display: flex !important;
      gap: 16px;
      align-items: flex-start;
    }

    .job-candidate-card-row > .v-slot {
      width: calc(50% - 8px) !important;
      min-width: 0 !important;
      max-width: calc(50% - 8px) !important;
      flex: 1 1 0 !important;
    }''', 1)

# 5. Подписи вкладок 12→14px (P2-7)
edit('tabs caption 14px', '      font-size: 12px !important;', '      font-size: 14px !important;', 1)

# 6. Активная вкладка — theme-aware цвет (P2-7)
edit('tabs active $v-selection-color',
'''    .job-candidate-tabs .v-tabsheet-tabitem-selected .v-caption {
      color: #0b63b6 !important;
      border-bottom: 3px solid #0b63b6 !important;
    }''',
'''    .job-candidate-tabs .v-tabsheet-tabitem-selected .v-caption {
      color: $v-selection-color !important;
      border-bottom: 3px solid $v-selection-color !important;
    }''', 1)

# 7. Подписи форм 16→15px (P2-8)
edit('form labels 15px',
'''      color: #516174 !important;
      font-size: 16px !important;
      line-height: 38px !important;''',
'''      color: #516174 !important;
      font-size: 15px !important;
      line-height: 38px !important;''', 1)

# 8. Поля форм 16→15px (P2-8)
edit('form inputs 15px',
'''      height: 38px !important;
      font-size: 16px !important;
      line-height: 38px !important;''',
'''      height: 38px !important;
      font-size: 15px !important;
      line-height: 38px !important;''', 1)

# 9. min-height таблицы соцсетей (P2-9)
edit('socialNetworkTable min-height',
'''      border-radius: 7px;
      overflow: hidden;
    }

    .job-candidate-table .v-grid-header,''',
'''      border-radius: 7px;
      overflow: hidden;
    }

    /* Секция «Социальные сети» переведена на height:AUTO — сетке нужна явная
       минимальная высота, чтобы не схлопываться при малом числе строк. */
    #socialNetworkTable {
      min-height: 320px !important;
    }

    .job-candidate-table .v-grid-header,''', 1)

# 10. Удалить мёртвые audit-правила (P2-10)
edit('удалить audit-box/audit-label',
'''    .job-candidate-audit-box,
    .job-candidate-audit-label {
      width: 100% !important;
      min-width: 0;
    }

    .job-candidate-audit-label {
      color: #718096 !important;
      font-size: 13px !important;
      line-height: 20px !important;
      white-space: normal !important;
    }

''', '', 1)

# 11. Sidebar media ≤1366: 296px + slot (P2-5)
edit('media 1366 sidebar 296px',
'''  @media (max-width: 1366px) {
    .job-candidate-editor {
      .job-candidate-sidebar {
        width: 286px !important;
        min-width: 286px !important;
        max-width: 286px !important;
        padding-left: 16px;
        padding-right: 16px;
      }''',
'''  @media (max-width: 1366px) {
    .job-candidate-editor {
      .v-slot-job-candidate-sidebar,
      .job-candidate-sidebar {
        width: 296px !important;
        min-width: 296px !important;
        max-width: 296px !important;
      }

      .job-candidate-sidebar {
        padding-left: 16px;
        padding-right: 16px;
      }''', 1)

# 12. Строки контактов: slot 150→100px (P3-14)
edit('form-row slot 100px',
'''    .job-candidate-form-row > .v-slot:first-child {
      width: 150px !important;
      min-width: 150px !important;
      padding-right: 12px;
    }

    .job-candidate-form-row > .v-slot:last-child {
      width: calc(100% - 150px) !important;
      min-width: 0 !important;
      flex: 1 1 auto !important;
    }''',
'''    .job-candidate-form-row > .v-slot:first-child {
      width: 100px !important;
      min-width: 100px !important;
      padding-right: 12px;
    }

    .job-candidate-form-row > .v-slot:last-child {
      width: calc(100% - 100px) !important;
      min-width: 0 !important;
      flex: 1 1 auto !important;
    }''', 1)

# 13. Media form-row 128→100px + новый тир ≤1100px (P3-14, P2-5)
edit('media form-row 100px + tier 1100',
'''      .job-candidate-form-row > .v-slot:first-child {
        width: 128px !important;
        min-width: 128px !important;
      }
    }
  }
}''',
'''      .job-candidate-form-row > .v-slot:first-child {
        width: 100px !important;
        min-width: 100px !important;
      }
    }
  }

  @media (max-width: 1100px) {
    .job-candidate-editor {
      .v-slot-job-candidate-sidebar,
      .job-candidate-sidebar {
        width: 284px !important;
        min-width: 284px !important;
        max-width: 284px !important;
      }
    }
  }
}''', 1)

ok = True
for desc, old, new, cnt in EDITS:
    n = src.count(old)
    if n != cnt:
        print('FAIL [%s]: ожидалось %d, найдено %d' % (desc, cnt, n))
        ok = False
    else:
        src = src.replace(old, new)

if not ok:
    sys.exit('SCSS-правки не применены — есть несовпадения.')

with io.open(SCSS, 'w', encoding='utf-8') as f:
    f.write(src)

# Копирование эталона во все 7 тем
THEMES = ['halo', 'havana', 'helium', 'hover',
          'hunttech-modern', 'hunttech-modern-light', 'hunttech-modern-dark']
for t in THEMES:
    dst = 'modules/web/themes/%s/com.company.hunttech/job-candidate-editor.scss' % t
    shutil.copyfile(SCSS, dst)
    print('обновлён:', dst)

print('SCSS применено правок: %d, скопировано тем: %d' % (len(EDITS), len(THEMES)))
