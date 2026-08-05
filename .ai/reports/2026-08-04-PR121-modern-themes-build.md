# Build Report — 2026-08-04
## PR #121 — fix(themes): восстановить modern-темы

```text
PROJECT: HRM HuntTech
STATUS: MERGED_AND_DEPLOYED
Repo: aananyev/it-pearls-recruiting
Branch: agent/modern-themes-foundation-fix
PR: 121
Base: master
Verified HEAD: 48f817478bc9d5a3a599a5d07fe1cad2c289d873
HEAD match: PASS
Conflicts: NONE (mergeable=MERGEABLE, mergeable_state=CLEAN)
Tests: ModernThemesFoundationContractTest 5/5 PASS
SCSS: buildScssThemes PASS (7 тем; modern-light 867 294 b, modern-dark 861 371 b, базовый слой на месте, v-menubar 145)
Build: clean assemble BUILD SUCCESSFUL (3m 29s)
Deploy: restart OK
HTTP: /hrm/ 200, widgetset 200
Smoke light: PASS (hunttech-modern-light: app-bg rgb(243,245,248), sidemenu 250px, меню «Кандидаты» есть)
Smoke dark: PASS (hunttech-modern-dark: app-bg rgb(15,18,23) — настоящая тёмная палитра, sidemenu 250px, меню «Кандидаты» есть)
Tomcat errors: NONE по сценарию тем (только фоновый шум Emailer: sendingMessage.caption is null — не связан с темами)
Docs: docs/architecture/HRM_HuntTech_Modern_Themes_Contract.md + README обновлены (из PR)
P1: 0
P2: 0
Merge: PERFORMED 2026-08-04 (команда Алексея «получи PR смержи») — git merge origin/agent/modern-themes-foundation-fix, FF 329aff9e..48f81747, push origin master
Production: NOT CHANGED (локальный деплой)
```

## Скриншоты
- `docs/performance-archive/2026-08-04/halo-precision/screenshots/pr121_modern_light_main_1920.png`
- `docs/performance-archive/2026-08-04/halo-precision/screenshots/pr121_modern_dark_main_1920.png`

## Примечание
Режим верификации — read-only, код/SCSS/XML/docs не менялись (кроме локального переключения
темы alan в sec_user_setting для smoke; тема возвращена на halo). Локальная проверочная ветка
`verify-pr121-modern-themes` создана от origin-ветки PR и может быть удалена после merge.
