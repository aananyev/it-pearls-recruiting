# Task: Привести docs-bot к стандартам hunttech-bot-common (низкая сложность)

**Репо:** https://github.com/aananyev/hrm-hunttech-docs-bot.git
**Библиотека:** https://github.com/aananyev/hunttech-bot-common.git

## Строгие правила

- **НЕ менять бизнес-логику** приёма файлов, распознавания, раскладки, отправки
- **НЕ менять** telegram_bot.py, recognizer.py, storage.py, email_sender.py, contracts_layout.py — только подключать библиотечные функции через импорт
- Каждое изменение должно быть обратимым (никакой переработки архитектуры)

## Задачи

### 1. Packaging — pyproject.toml

Создать `pyproject.toml` в корне репо:

```toml
[build-system]
requires = ["setuptools>=64"]
build-backend = "setuptools.backends._legacy:_Backend"

[project]
name = "hrm-hunttech-docs-bot"
version = "0.1.0"
dependencies = [
    "hunttech-bot-common @ git+https://github.com/aananyev/hunttech-bot-common.git",
    "python-telegram-bot>=20.0",
    "httpx",
]
```

Не менять `plugin.yaml`, не менять способ запуска.

### 2. Logging — SecretsMaskingFilter

В `__init__.py` добавить вызов `setup_logging` при регистрации плагина:

```python
from hunttech_bot_common.logging import setup_logging

def register(ctx) -> None:
    setup_logging("hunttech_docs")
    set_plugin_llm(ctx.llm)
    ctx.register_cli_command(...)
```

Не менять существующие вызовы `logging.getLogger()`.

### 3. Files — утилиты

В `contracts_layout.py` и других файлах, где есть работа с путями и именами файлов, добавить импорты:

```python
from hunttech_bot_common.files import safe_join, sanitize_filename, validate_extension
```

Заменить самописные проверки путей/имён на библиотечные ТОЛЬКО там где логика не изменится (проверки, не создание/перемещение).

### 4. Security — валидация

В `telegram_bot.py` и `cli.py`, где обрабатывается пользовательский ввод (caption, текстовые поля), добавить:

```python
from hunttech_bot_common.security import sanitize_text_input
```

Оборачивать входящий текст через `sanitize_text_input()` перед использованием.

### 5. AI Client — AIClient (опционально, если Hermes LLM не будет сломан)

В `recognizer.py` заменить прямой вызов AI на `AIClient` из библиотеки, но **только если интерфейс полностью совместим**. Если есть риск изменения формата ответа — оставить как есть, задача не стоит того.

## Критерии приёмки

- Бот запускается командой `hermes hunttech-docs run`
- Все существующие команды работают
- Ни одна бизнес-функция не изменила поведение
