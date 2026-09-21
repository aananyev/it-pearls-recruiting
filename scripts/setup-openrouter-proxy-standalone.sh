#!/usr/bin/env bash
# Автономный скрипт для настройки локального прокси OpenRouter на сервере 192.168.1.135
# Скопируйте этот файл на сервер и запустите: sudo bash setup-openrouter-proxy-standalone.sh

set -euo pipefail

LOGGER() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*"
}

REMOTE_PROXY_HOST="209.46.2.183"
REMOTE_PROXY_PORT="8000"
PROXY_USERNAME="tWQrfq"
PROXY_PASSWORD="YtJRww"
LOCAL_LISTEN_PORT="8119"
TARGET_HOST="openrouter.ai"
TARGET_PORT="443"
SERVICE_NAME="openrouter-proxy"

LOGGER "=== Настройка OpenRouter прокси для HuntTech ==="
LOGGER "Локальный порт: 127.0.0.1:$LOCAL_LISTEN_PORT"
LOGGER "Удалённый прокси: $REMOTE_PROXY_HOST:$REMOTE_PROXY_PORT"
LOGGER "Аутентификация: $PROXY_USERNAME:*****"

# Проверка прав root
if [ "$EUID" -ne 0 ]; then
    LOGGER "ОШИБКА: Скрипт требует права root. Запустите: sudo bash $0"
    exit 1
fi

# Проверка и установка socat
if ! command -v socat &> /dev/null; then
    LOGGER "socat не найден. Установка..."
    if command -v apt-get &> /dev/null; then
        apt-get update && apt-get install -y socat
    elif command -v yum &> /dev/null; then
        yum install -y socat
    elif command -v dnf &> /dev/null; then
        dnf install -y socat
    else
        LOGGER "ОШИБКА: Неизвестный пакетный менеджер. Установите socat вручную."
        exit 1
    fi
fi

LOGGER "socat версия: $(socat -V 2>&1 | head -1)"

# Создание systemd сервиса
SERVICE_PATH="/etc/systemd/system/${SERVICE_NAME}.service"

LOGGER "Создание systemd сервиса: $SERVICE_PATH"

cat > "$SERVICE_PATH" <<EOF
[Unit]
Description=OpenRouter Proxy for HuntTech (local:$LOCAL_LISTEN_PORT -> $TARGET_HOST:$TARGET_PORT via $REMOTE_PROXY_HOST:$REMOTE_PROXY_PORT)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
ExecStart=/usr/bin/socat TCP4-LISTEN:${LOCAL_LISTEN_PORT},bind=127.0.0.1,fork,reuseaddr PROXY:${REMOTE_PROXY_HOST}:${TARGET_HOST}:${TARGET_PORT},proxyport=${REMOTE_PROXY_PORT},proxyauth=${PROXY_USERNAME}:${PROXY_PASSWORD}
Restart=on-failure
RestartSec=5
LimitNOFILE=4096
CPUQuota=50%
MemoryMax=100M

[Install]
WantedBy=multi-user.target
EOF

LOGGER "Перезагрузка systemd..."
systemctl daemon-reload

LOGGER "Включение и запуск сервиса..."
systemctl enable "${SERVICE_NAME}"
systemctl start "${SERVICE_NAME}"

LOGGER "Статус сервиса:"
systemctl status "${SERVICE_NAME}" --no-pager -l

LOGGER "Ожидание запуска (2 сек)..."
sleep 2

LOGGER "Тестирование прокси..."
if curl -sf -x "http://127.0.0.1:${LOCAL_LISTEN_PORT}" "https://openrouter.ai/api/v1/auth/key" > /dev/null; then
    LOGGER "✅ УСПЕХ: Прокси работает. OpenRouter доступен через 127.0.0.1:${LOCAL_LISTEN_PORT}"
else
    LOGGER "⚠️ ПРЕДУПРЕЖДЕНИЕ: Не удалось проверить через curl."
    LOGGER "Проверьте вручную: curl -v -x http://127.0.0.1:${LOCAL_LISTEN_PORT} https://openrouter.ai/api/v1/auth/key"
    LOGGER "Логи сервиса: journalctl -u ${SERVICE_NAME} -f"
fi

LOGGER ""
LOGGER "=== ГОТОВО ==="
LOGGER "Управление сервисом:"
LOGGER "  systemctl status ${SERVICE_NAME}"
LOGGER "  systemctl stop ${SERVICE_NAME}"
LOGGER "  systemctl start ${SERVICE_NAME}"
LOGGER "  journalctl -u ${SERVICE_NAME} -f"
LOGGER ""
LOGGER "Откат (если нужно):"
LOGGER "  systemctl stop ${SERVICE_NAME}"
LOGGER "  systemctl disable ${SERVICE_NAME}"
LOGGER "  rm ${SERVICE_PATH}"
LOGGER "  systemctl daemon-reload"