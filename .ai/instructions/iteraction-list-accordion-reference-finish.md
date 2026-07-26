# Повторная проверка IteractionListEdit после Vaadin RPC ошибки

PROJECT: HRM HuntTech  
REPO: `aananyev/it-pearls-recruiting`  
BRANCH: `agent/iteraction-list-rpc-session-fix`  
BASE: `master`  
MODE: проверка точного HEAD PR без изменения функционального кода, документации, ветки и production.

Полный HEAD SHA указан в PR. До начала Hermes обязан подтвердить: ветка существует; branch HEAD = PR HEAD = переданный SHA; `base=master`; conflicts=NONE. Несовпадение — `HEAD_MISMATCH`, проверку остановить.

## Причина повторной проверки

Предыдущая сборка и профильные тесты прошли, но smoke выполнялся в существующем браузерном сеансе после redeploy. Исключение `ServerRpcManager.applyInvocation: object is not an instance of declaring class` соответствует рассинхронизации Vaadin connector state между старым UI-сеансом браузера и новым серверным деревом компонентов.

Контракт приложения дополнительно защищён тестом `IteractionListRpcCompatibilityContractTest`: `OvaFallbackImage` использует стандартный `WebImage/CubaImage`, не регистрирует собственный `ServerRpc`, XML `invoke` существуют в контроллере, а ID пяти аккордеонов совпадают с Java-инъекциями.

## Обязательные статические проверки

```bash
git diff --check

./gradlew :app-web:compileJava \
          :app-core:compileTestJava \
          --no-daemon --stacktrace

./gradlew :app-core:test \
          --tests 'com.company.hunttech.core.IteractionListEditAccordionLayoutTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionNavigationTest' \
          --tests 'com.company.hunttech.core.IteractionListSidebarContextPanelTest' \
          --tests 'com.company.hunttech.core.IteractionListAccordionCssContractTest' \
          --tests 'com.company.hunttech.core.IteractionListMostPopularInteractionTest' \
          --tests 'com.company.hunttech.core.LeftSidebarAvatarComponentTest' \
          --tests 'com.company.hunttech.core.IteractionListRpcCompatibilityContractTest' \
          --no-daemon --stacktrace

./gradlew test --tests '*ScreenViewIntegrityTest*' --no-daemon --stacktrace
./gradlew :app-web:buildScssThemes --no-daemon --stacktrace
./gradlew clean assemble --no-daemon --stacktrace
```

Ожидается: профильные тесты PASS; RPC contract `4/4 PASS`; SCSS семи тем PASS; `BUILD SUCCESSFUL`. По `ScreenViewIntegrityTest` отдельно перечислить результат каждой из 8 проверок; ранее существовавшие `itpearls_*` failures не скрывать и не приписывать этому PR.

## Обязательный чистый widgetset deploy

Не использовать обычный `./gradlew deploy` для повторного smoke. Выполнить:

```bash
APP_CONTEXT=hrm ./scripts/rebuild-widgetset-and-start.sh
```

После запуска сравнить три SHA-256:

```bash
BUILD_JS="modules/web-toolkit/build/web/VAADIN/widgetsets/com.company.hunttech.web.toolkit.ui.AppWidgetSet/com.company.hunttech.web.toolkit.ui.AppWidgetSet.nocache.js"
DEPLOY_JS="deploy/tomcat/webapps/hrm/VAADIN/widgetsets/com.company.hunttech.web.toolkit.ui.AppWidgetSet/com.company.hunttech.web.toolkit.ui.AppWidgetSet.nocache.js"
HTTP_JS="$(mktemp)"

curl -fsS \
  "http://localhost:8080/hrm/VAADIN/widgetsets/com.company.hunttech.web.toolkit.ui.AppWidgetSet/com.company.hunttech.web.toolkit.ui.AppWidgetSet.nocache.js" \
  -o "$HTTP_JS"

sha256sum "$BUILD_JS" "$DEPLOY_JS" "$HTTP_JS"
```

Все три хэша должны совпасть. Несовпадение — `FAILED_VERIFICATION`, runtime smoke не продолжать.

## Обязательный новый браузерный сеанс

1. Закрыть все вкладки HRM HuntTech.
2. Удалить site data для `localhost:8080` либо использовать новый профиль браузера.
3. Открыть новое окно Incognito/Private.
4. Перейти по адресу `http://localhost:8080/hrm/?restartApplication`.
5. Не использовать вкладку, открытую до redeploy.

Hard refresh старой вкладки не считается новым Vaadin UI-сеансом.

## Runtime и visual smoke

1. Открыть создание и редактирование `IteractionListEdit`.
2. Нажать последовательно все пять пунктов sidebar и заголовки всех пяти GroupBox.
3. Подтвердить отсутствие `IllegalArgumentException`, `ServerRpcManager.applyInvocation` и UIDL/RPC ошибок в Tomcat logs.
4. Проверить caption «Кандидат и вакансия», раскрытие `participantsAccordion`, сворачивание остальных секций и фокус `candidateField`.
5. Проверить пять одинаковых зелёных pill-кнопок, каждая занимает 20% полноширинного блока.
6. Проверить порядок sidebar: изображения → номер/дата → индекс → карточка вакансии.
7. Проверить овальный `projectLogoImage` через `OvaFallbackImage` и fallback.
8. Проверить candidate/vacancy picker, тип и действие, результат, комментарий, подписку, save/cancel.
9. Повторить visual smoke во всех семи темах; horizontal scroll, обрезания и перекрытия отсутствуют.
10. Проверить `http://localhost:8080/hrm/` = HTTP 200, Tomcat critical errors NONE, P1=0, P2=0.

Отчёт должен содержать формулировку `проверен HEAD: <полный SHA>`, результаты трёх widgetset-хэшей и указание, что smoke выполнен в новом браузерном сеансе. Отчёт можно подготовить локально в `.ai/reports/`, но Hermes не делает commit, push, rebase, merge и не изменяет код, docs или production. Результат передать комментарием к PR.
