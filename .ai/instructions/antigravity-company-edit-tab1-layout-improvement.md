# Задание для Antigravity: улучшение компоновки первой вкладки CompanyEdit

## Контекст
В master (commit e83730e8) восстановлен `scrollBox` в sidebar CompanyEdit из коммита 7ec7a843.
Приложение работает: Widgetset 200, App 200, рестарт чистый.

## Проблема
На первой вкладке **"Информация о компании"** (`tabConpanyDetails`) элементы наезжают друг на друга:
- чекбоксы `ourLegalEnityCheckBox` и `checkBoxOurClient`
- поля `comanyNameField`, `companyShortNameField`
- `companyGroupLookupPickerField` и `companyDirectorLookupPickerField`

Требуется адаптивная резиновая верстка по контракту Edit-форм HRM.

## Текущее состояние (company-edit.xml, строки 256-340)
```xml
<tab id="tabConpanyDetails"
     caption="mainMsg://msgCompanyDetail"
     spacing="true"
     expand="companyDetailsScroll"
     stylename="company-main-tab">
    <scrollBox id="companyDetailsScroll"
               width="100%"
               height="100%"
               orientation="vertical"
               scrollBars="vertical"
               stylename="edit-workspace edit-workspace-scroll">
        <vbox id="companyDetailsContent"
              width="100%"
              spacing="true"
              stylename="edit-workspace-content">
            <groupBox id="companyMainCard"
                      caption="msg://msgCompanyMainSection"
                      stylename="edit-card"
                      showAsPanel="true"
                      collapsable="true"
                      collapsed="false"
                      width="100%">
                <vbox id="companyMainFieldsBox"
                      width="100%"
                      spacing="true">
                    <!-- Чекбоксы в одной строке -->
                    <hbox spacing="true"
                          width="100%"
                          stylename="company-main-tab-top-checks">
                        <checkBox id="ourLegalEnityCheckBox"
                                  dataContainer="companyDc"
                                  caption="msg://msgOurLegalEntity"
                                  property="ourLegalEntity"
                                  stylename="edit-form-control"/>
                        <checkBox id="checkBoxOurClient"
                                  caption="mainMsg://msgOurClient"
                                  dataContainer="companyDc"
                                  property="ourClient"
                                  stylename="edit-form-control"/>
                    </hbox>
                    <!-- Наименования в одной строке -->
                    <hbox spacing="true" width="100%">
                        <textField id="comanyNameField"
                                   dataContainer="companyDc"
                                   property="comanyName"
                                   caption="msg://msgCompanyName"
                                   stylename="edit-form-control"
                                   box.expandRatio="1"
                                   width="100%"/>
                        <textField id="companyShortNameField"
                                   dataContainer="companyDc"
                                   property="companyShortName"
                                   caption="mainMsg://msgCountryShortName"
                                   stylename="edit-form-control"
                                   box.expandRatio="1"
                                   width="100%"/>
                    </hbox>
                    <!-- Группа и директор в одной строке -->
                    <hbox spacing="true" width="100%">
                        <lookupPickerField id="companyGroupLookupPickerField"
                                           dataContainer="companyDc"
                                           property="companyGroup"
                                           optionsContainer="companyGroupDc"
                                           caption="msg://msgCompanyGroup"
                                           stylename="edit-form-control"
                                           box.expandRatio="1"
                                           width="100%"/>
                        <lookupPickerField id="companyDirectorLookupPickerField"
                                           dataContainer="companyDc"
                                           property="companyDirector"
                                           optionsContainer="companyDirectorsDc"
                                           caption="msg://msgCompanyDirector"
                                           stylename="edit-form-control"
                                           box.expandRatio="1"
                                           width="100%"/>
                    </hbox>
```

## Требования к улучшению

### 1. Чекбоксы (строка 1) — прижать влево, без растяжения
```xml
<hbox spacing="true" width="100%" stylename="company-main-tab-top-checks">
    <checkBox ... stylename="edit-form-control" box.expandRatio="0"/>
    <checkBox ... stylename="edit-form-control" box.expandRatio="0"/>
    <!-- spacer для заполнения оставшегося пространства -->
    <vbox width="100%"/>
</hbox>
```

### 2. Наименования (строка 2) — 50/50 с min-width
```xml
<hbox spacing="true" width="100%">
    <textField ... box.expandRatio="1" width="100%"/>
    <textField ... box.expandRatio="1" width="100%"/>
</hbox>
```
Добавить в SCSS (company-editor.scss):
```scss
.company-main-tab-top-checks {
  .edit-form-control { box-expand-ratio: 0; }
}
.edit-workspace-content .edit-form-control {
  min-width: 280px; // не сжимать меньше
}
```

### 3. Группа и директор (строка 3) — 50/50
Аналогично наименованиям.

### 4. Последующие поля — по одной в строке (100% ширины)
- `companyOwnershipLookupPickerField`
- `cityOfCompanyLookupPickerField`
- `regionOfCompanyLookupPickerField`
- `countryOfCompanyLookupPickerField`
- `companyDescriptionRichTextArea` — на всю ширину, height="200px"

### 5. Убрать `grid` — использовать только `hbox` + `box.expandRatio`

## Эталон
- `IteractionListEdit` — резиновая верстка вкладок
- `CompanyReestrBrowse` — сайдбар 312px, workspace с scrollBox

## Принятие изменений
```bash
# В worktree Antigravity (../hrm-antigravity)
git fetch origin master
git merge origin/master
# Или через rebase если есть свои коммиты
```

## Проверка
1. `fast-deploy.sh --conf` — деплой XML
2. Открыть CompanyEdit → вкладка "Информация о компании"
3. Изменить размер окна браузера — элементы не должны наезжать
4. Контрактные тесты: `:app-core:test --tests "*CompanyEdit*ContractTest"`

## Дедлайн
Присылать PR в master с меткой WAITING_FOR_HERMES после верификации.