# Coordination Protocol: Build Instructions

## Channels

### 1. `.ai/instructions/` (локально, без гита)
ChatGPT пишет инструкции перед созданием PR:
- `.ai/instructions/{date}-{topic}.md`
- Hermes читает перед обработкой PR

### 2. Ветка `coordination/build-instructions` (git)
Для формальных заданий:
- ChatGPT пушит коммиты в эту ветку
- Hermes читает перед обработкой PR
- Формат файлов: `.ai/build-instructions/{date}-{topic}.md`

## Формат инструкции ChatGPT → Hermes

```markdown
# Build Instruction — YYYY-MM-DD
## PR: #XXX — краткое название

### Что делать
- ...
### Особенности сборки
- ...
### Что проверить после деплоя
- ...
```

## Формат ответа Hermes → ChatGPT
Запись в `.ai/reports/` после сборки.
