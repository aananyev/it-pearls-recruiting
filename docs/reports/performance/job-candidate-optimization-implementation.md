# JobCandidateEdit Optimization Implementation

Дата: 2026-07-13.

## Старый fetch graph

Initial inline view `jobCandidateDc` загружал `candidateCv`, `iteractionList`, `socialNetwork`, `positionList`, вложенные vacancy/project/company/file связи и справочник компаний через `currentCompaniesDc`.

## Новый fetch graph

Initial view оставляет локальные поля `JobCandidate`, `cityOfResidence`, `currentCompany`, `fileImageFace`, `personPosition` и `positionList`, потому что позиции используются на первой карточке для рекомендуемых вакансий.

## Lazy loaders

| Данные | Когда загружаются |
| ------ | ----------------- |
| `candidateCv` | первое открытие `tabResume` |
| `iteractionList` | первое открытие `tabIteraction` |
| `socialNetwork` | первое открытие `tabContactInfo` |
| comments | первое открытие `commentsTab` |

## Company field

`currentCompanyField` заменён с полного `LookupPickerField` options container на `SuggestionPickerField` с server-side search по `comanyName`, лимитом подсказок 50 и сохранением lookup/open/create actions.

## DataContext

Лениво загруженные сущности merge-ятся в экранный `DataContext` перед присвоением коллекции кандидату. Это сохраняет редактирование в текущем editor context.

## Что не менялось

Бизнес-логика, validation, сохранение кандидата, создание `Company`, схема БД и production не менялись.

