# CompanyEdit

Экран: `hunttech_Company.edit`. Контроллер: `CompanyEdit`.

Экран работает как стандартный `StandardEditor<Company>` в режимах create и edit. Основное обязательное поле сущности: `comanyName`. На UI также обязательны город, регион и страна компании.

При commit `CompanyEdit` сохраняет компанию в собственном `DataContext`. При открытии из `JobCandidateEdit` успешный commit возвращает созданную компанию вызывающему экрану через стандартный механизм editor screen. Cancel/discard не создаёт компанию и не меняет кандидата.
