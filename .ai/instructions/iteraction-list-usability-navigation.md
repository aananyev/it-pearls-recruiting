# Hermes — проверка IteractionListEdit usability navigation

PROJECT: HRM HuntTech
REPO: aananyev/it-pearls-recruiting
BRANCH: agent/iteraction-list-usability-navigation
BASE: master
VERIFIED HEAD: полный HEAD SHA из PR; несовпадение означает HEAD_MISMATCH
MODE: проверка без изменения кода

Команды: `git diff --check`; compile web/core; профильные тесты IteractionListEditAccordionLayoutTest, IteractionListAccordionNavigationTest, IteractionListMostPopularInteractionTest, LeftSidebarAvatarComponentTest; ScreenViewIntegrityTest 8/8; buildScssThemes; clean assemble.

Local smoke: HTTP `/hrm/` 200; кандидат и вакансия в одной строке без horizontal scroll и с одинаковым CSS; пять пунктов слева раскрывают блоки и фокусируют поля; пять равных кнопок соответствуют топ-5 текущего пользователя за год и устанавливают точный тип; save/cancel/subscription без регрессии; Tomcat critical errors NONE; P1=0; P2=0.

Hermes не меняет код/docs, не делает commit/push/rebase/merge и не изменяет production. Отчёт обязан содержать `проверен HEAD: <SHA>`.
