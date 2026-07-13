# JobCandidateEdit

Экран: `hunttech_JobCandidate.edit`. Контроллер: `JobCandidateEdit`.

Поле «Компания» находится в главном `TabSheet` `tabSheetSocialNetworks`, вкладка `tabCandidate`. ID поля: `currentCompanyField`. Тип компонента: `LookupPickerField<Company>`, привязка `jobCandidateDc.currentCompany`, options container `currentCompaniesDc`.

Пользователь может выбрать существующую компанию через стандартный lookup, открыть выбранную компанию через стандартный open и создать новую компанию через действие «Создать компанию».

Создание открывает `hunttech_Company.edit` в dialog create mode. После commit созданная `Company` сливается в `DataContext` кандидата, добавляется в `currentCompaniesDc` и устанавливается в `currentCompanyField`. Экран кандидата остаётся открытым, несохранённые изменения кандидата сохраняются в состоянии формы.

При cancel/discard/close дочернего редактора значение поля не меняется, кандидат не сохраняется автоматически.
