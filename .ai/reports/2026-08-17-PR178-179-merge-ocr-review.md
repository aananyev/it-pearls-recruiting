# OCR Code Review Report — 2026-08-17

## PR #178 (agent/antigravity-dev) + PR #179 (agent/dsh-dev) — merge + ревью

PROJECT: HRM HuntTech
Reviewed by: ocr CLI (Alibaba open-code-review, deepseek-v4-flash)
Range: ac1e3c3b..8e4898b8 (оба PR смержены в master: 9e50f4bf, 8e4898b8)
Files reviewed: 13 · Comments: 18 (2 high / 11 medium / 5 low)
Tokens: ~789 617 (input ~742 787, output ~46 830), 2m11s, retries 8/71 (network, all recovered)

## Замечания

### High

1. `themes/hover/.../hover-ext.scss:1957-1968` [security] — скрытие водяного знака amCharts. **СНЯТО**: пользователь подтвердил наличие платной/Enterprise-лицензии amCharts — юридического риска нет.
2. `themes/hunttech-modern/.../hunttech-modern-ext.scss:254-263` [bug] — селекторы-контейнеры (`.amcharts-chart-div a`, `.c-chart-container a`, `.c-chart-canvas a`, `.v-amcharts-chart a`, `.c-amcharts-chart a` и т.п.) скрывают ВСЕ ссылки внутри контейнеров графиков: кликабельные url-точки данных, ссылки легенды, drill-down станут невидимы/некликабельны. Достаточно оставить `a[href*="amcharts.com"]`.

### Medium

3. Все 6 тем — глобальные `a[href*="amcharts"]` / `a[href*="amcharts.com"]` без скоупа на контейнер графиков скроют любую легитимную ссылку с «amcharts» в href по всему приложению: halo-ext.scss:1802-1803, helium-ext.scss:1594-1595, havana-ext.scss:1587-1588, hunttech-modern-light-ext.scss:254-255 (плюс избыточность: второй селектор уже покрыт первым).
4. havana-ext.scss:1589-1592 — контейнерные селекторы матчат и tooltip/legend/категорийные ссылки графиков (не только водяной знак).
5. admin-ai-dashboard.xml:61, :148 — KPI-карточки и ряды графиков теперь зависят от `ai-dashboard-styles.scss`, который импортирован ТОЛЬКО в темы `halo` и `hunttech-modern`. В havana/helium/hover/hunttech-modern-dark/hunttech-modern-light вёрстка дашбордов сломается (карточки схлопнутся/переполнятся, графики встанут колонкой вместо 60/40 и 50/50).
6. admin-ai-dashboard.xml:102 — `expand="costDynamicsBox"` (Vaadin expand ratio) конфликтует с flex 6/4 из `.ai-charts-row-60-40`; chartsRow2 (50/50) полагается только на CSS — несогласованно.
7. hunttech-modern ai-dashboard-styles.scss:240-246 — `spacing="true"` в XML + `gap:16px` в CSS → двойной зазор (~32px) между карточками графиков; нужен один механизм.
8. hunttech-modern ai-dashboard-styles.scss:217-226 — `.kpi-sub` nowrap+ellipsis без tooltip обрезает динамические значения («Топ: …», «Corp: $X | User: $Y») — контекст недоступен; предложение: line-clamp 2 строки.
9. halo ai-dashboard-styles.scss:248-253 — flex 6:4 через `:first-child`/`:last-child` — хрупко при добавлении средней карточки.

### Low (не блокируют, зафиксировано)

10. Избыточные CSS-свойства при `display:none` (visibility/opacity/clip/width/height/margin/padding/font-size/line-height/position) — halo-ext.scss:1812, hunttech-modern-light-ext.scss:264, hover-ext.scss:1957.
11. Мёртвые ширины: `width:25%` в ai-dashboard-styles.scss (halo:125, hunttech-modern:123) при `flex:1 1 0%`; `width="45%"` functionChartBox в user-ai-dashboard.xml:107 при flex 4/1.

## Решение

Замечания задокументированы, правки НЕ вносятся (по решению пользователя). Лицензия amCharts подтверждена (платная/Enterprise) — High #1 закрыт. Остальной список — технический долг на усмотрение будущих задач по дашбордам AI (приоритет: High #2 — сузить селекторы; Medium #5 — кросстемовость SCSS).
