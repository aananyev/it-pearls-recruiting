# Реорганизация пунктов меню раздела «Проекты» (application-project)

## 1. Основание и цели задачи
**Роль:** Аналитик  
**Дата:** 2026-09-10  
**Запрос пользователя:**
> в главном меню:
> - скрой пункт "Проекты", переименуй пункт "Реестр проектов" в "Проекты заказчиков"
> - скрой пункт "Люди", переименой пункт "Реестр людей" в "Контактные лица"
> - скрой пункт "Компании", переименуй пункт "Реестр компаний" в "Компании"
> - скрой пункт "Группы компаний", переименуй пункт "Реестр групп компаний" в "Группы компаний"
> - скрой пункт "Департаменты/Отделы", переименуй пункт "Реестр департментов" в "Департаменты"

## 2. Анализ текущего состояния
В файле конфигурации главного меню `modules/web/src/com/company/hunttech/web-menu.xml` раздел меню с id `application-project` содержит спаренные устаревшие и современные реестровые формы:
1. `hunttech_Project.browse` («Проекты») и `hunttech_ProjectReestr.browse` («Реестр проектов»)
2. `hunttech_Person.browse` («Люди») и `hunttech_PersonReestr.browse` («Реестр людей»)
3. `hunttech_CompanyDepartament.browse` («Департамент / Отдел») и `hunttech_CompanyDepartamentReestr.browse` («Реестр департаментов»)
4. `hunttech_Company.browse` («Компании») и `hunttech_CompanyReestr.browse` («Реестр компаний»)
5. `hunttech_CompanyGroup.browse` («Группа компаний») и `hunttech_CompanyGroupReestr.browse` («Реестр групп компаний»)

## 3. Целевое состояние
1. Устаревшие пункты (`hunttech_Project.browse`, `hunttech_Person.browse`, `hunttech_CompanyDepartament.browse`, `hunttech_Company.browse`, `hunttech_CompanyGroup.browse`) скрываются из меню `application-project`.
2. Реестровые пункты получают лаконичные и целевые бизнес-названия:
   - `hunttech_ProjectReestr.browse`: «Проекты заказчиков»
   - `hunttech_PersonReestr.browse`: «Контактные лица»
   - `hunttech_CompanyDepartamentReestr.browse`: «Департаменты»
   - `hunttech_CompanyReestr.browse`: «Компании»
   - `hunttech_CompanyGroupReestr.browse`: «Группы компаний»
3. Обновляются локализационные пакеты:
   - `modules/web/src/com/company/hunttech/web/messages_ru.properties`
   - `modules/web/src/com/company/hunttech/web/messages.properties`
4. Реализуется контрактный автотест в `modules/core/test/com/company/hunttech/core/MainMenuReorganizationContractTest.java`.
5. Валидация через Gradle (`agent-gradle.sh`) и Alibaba OCR CLI.
