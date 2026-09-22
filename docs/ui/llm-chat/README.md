# LLM-chat

Living-документация экрана LLM-chat HRM HuntTech:

- [LlmChatScreen_Spec.md](../LlmChatScreen_Spec.md) — доступ к Local/Hermes-контурам, CUBA specific permissions и требования к manager/write Hermes.

Write-enabled Hermes не считается интегрированным, пока не подтверждены production container/API/auth и CUBA-mediated mutation gateway с безусловным запретом DELETE.

## Форматирование ответов

Сообщения Local LLM, Hermes-viewer и Hermes-operator проходят через
`MarkdownRenderer` перед выводом в HTML-label. Поддерживаются заголовки,
списки, код, ссылки и Markdown-таблицы. Таблицы распознаются как с внешними
разделителями (`| Колонка | Значение |`), так и без них (`Колонка | Значение`),
с обязательной строкой-разделителем (`--- | ---`). Внутри ячейки можно экранировать
разделитель как `\\|`. HTML остаётся санитизированным перед отображением.
