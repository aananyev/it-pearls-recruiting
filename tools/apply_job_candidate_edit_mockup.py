#!/usr/bin/env python3
"""
Одноразовый генератор файлов редизайна JobCandidateEdit.

Сценарий используется только для безопасной доставки крупных XML/SCSS-файлов
через отдельную ветку GitHub. После применения служебный файл удаляется.
"""
from __future__ import annotations

import base64
import gzip
from pathlib import Path

XML_GZIP_BASE64 = "H4sIAFbOVWoC/+097XLcRnL