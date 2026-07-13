# BR-JC-COMPANY-001: выбор и создание компании кандидата

Кандидат может быть связан с существующей компанией через `JobCandidate.currentCompany`.

Если нужной компании нет в справочнике, пользователь может создать её из поля «Компания» в `JobCandidateEdit`. Для создания открывается стандартный `CompanyEdit`.

После успешного commit `CompanyEdit` созданная `Company` автоматически устанавливается в поле кандидата. Сохранение компании не сохраняет кандидата автоматически; связь фиксируется в базе только при последующем сохранении `JobCandidate`.

При cancel/discard/close `CompanyEdit` новая компания не сохраняется, значение `JobCandidate.currentCompany` не меняется, несохранённые изменения кандидата не теряются.

```mermaid
sequenceDiagram
    participant U as Пользователь
    participant JC as JobCandidateEdit
    participant DC as JobCandidate DataContext
    participant CE as CompanyEdit
    participant DB as Database

    U->>JC: Создать компанию
    JC->>CE: Open hunttech_Company.edit create dialog
    U->>CE: Save and close
    CE->>DB: Commit Company
    CE-->>JC: COMMIT + saved Company
    JC->>DC: merge(Company)
    JC->>JC: set currentCompanyField
    U->>JC: Save candidate
    JC->>DB: Commit JobCandidate.currentCompany
```
