# Test Report — SettingWindow AI Profile

| Команда | Exit code | Результат |
|---------|-----------|-----------|
| `app-web:compileJava` (attempt 1) | 1 | ❌ setCollapsed → не существует |
| `app-web:compileJava` (attempt 2) | 0 | ✅ PASS (setExpanded) |
| `:app-global:compileJava :app-core:compileJava :app-core:compileTestJava :app-web:compileJava :app-web:compileTestJava` | 0 | ✅ BUILD SUCCESSFUL |
| `UserAiContextServiceBeanTest` | 0 | ✅ 7/7 PASS |
| `ScreenViewIntegrityTest` | N/A | ❌ **NOT AVAILABLE IN DEPLOY_SHA** — тест не входит в историю ветки |
| `:app-web:buildScssThemes` (initial) | 1 | ❌ hunttech-modern-light без styles.scss |
| `:app-web:buildScssThemes` (after fix) | 0 | ✅ PASS |
| `clean assemble` | 0 | ✅ BUILD SUCCESSFUL |
| `clean buildWar` | 0 | ✅ BUILD SUCCESSFUL |

## Предупреждения и ошибки

1. `ScreenViewIntegrityTest` — отсутствует в baseline данной ветки. Тест добавлен позже на `feat/ai-entity-analysis`. Не является регрессией.
2. SCSS — локальный каталог `hunttech-modern-light` (ignored/generated) не содержал `styles.scss`. Удалён локально, код темы не изменялся.
