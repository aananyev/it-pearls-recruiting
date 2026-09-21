#!/usr/bin/env bash
# Script to set up a local TCP proxy for OpenRouter on port 8119
# forwards through remote proxy 209.46.2.183:8000 with auth tWQrfq:YtJRww
# Intended for execution on the HuntTech application server (prod or staging).

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

LOGGER "Setting up OpenRouter proxy:"
LOGGER "  Local listen: 127.0.0.1:$LOCAL_LISTEN_PORT -> $TARGET_HOST:$TARGET_PORT"
LOGGER "  Via remote proxy: $REMOTE_PROXY_HOST:$REMOTE_PROXY_PORT"
LOGGER "  Proxy auth: $PROXY_USERNAME:*****"

# Check if socat is installed
if ! command -v socat &> /dev/null; then
    LOGGER "socat not found. Attempting to install..."
    # Detect package manager
    if command -v apt-get &> /dev/null; then
        sudo apt-get update && sudo apt-get install -y socat
    elif command -v yum &> /dev/null; then
        sudo yum install -y socat
    elif command -v dnf &> /dev/null; then
        sudo dnf install -y socat
    elif command -v brew &> /dev/null; then
        brew install socat
    else
        LOGGER "ERROR: Unable to install socat. Please install it manually and rerun."
        exit 1
    fi
fi

LOGGER "socat version: $(socat -V 2>&1 | head -1)"

# Create systemd service file
SERVICE_NAME="openrouter-proxy"
SERVICE_PATH="/etc/systemd/system/${SERVICE_NAME}.service"

LOGGER "Creating systemd service at $SERVICE_PATH"

sudo tee "$SERVICE_PATH" > /dev/null <<EOF
[Unit]
Description=OpenRouter Proxy for HuntTech (local:$LOCAL_LISTEN_PORT -> $TARGET_HOST:$TARGET_PORT via $REMOTE_PROXY_HOST:$REMOTE_PROXY_PORT)
After=network-online.target
Wants=network-online.target

[Service]
Type=simple
User=root
# Alternatively, create a dedicated user:
# User=openproxy
ExecStart=/usr/bin/socat TCP4-LISTEN:${LOCAL_LISTEN_PORT},bind=127.0.0.1,fork,reuseaddr PROXY:${REMOTE_PROXY_HOST}:${TARGET_HOST}:${TARGET_PORT},proxyport=${REMOTE_PROXY_PORT},proxyauth=${PROXY_USERNAME}:${PROXY_PASSWORD}
Restart=on-failure
RestartSec=5
# Limit resources to prevent abuse
LimitNOFILE=4096
CPUQuota=50%
MemoryMax=100M

[Install]
WantedBy=multi-user.target
EOF

LOGGER "Reloading systemd daemon..."
sudo systemctl daemon-reload

LOGGER "Enabling and starting service..."
sudo systemctl enable "${SERVICE_NAME}"
sudo systemctl start "${SERVICE_NAME}"

LOGGER "Checking service status..."
sudo systemctl status "${SERVICE_NAME}" --no-pager -l

LOGGER "Testing proxy connectivity..."
# Wait a moment for service to start
sleep 2
if curl -sf -x http://127.0.0.1:${LOCAL_LISTEN_PORT} https://openrouter.ai/api/v1/auth/key > /dev/null; then
    LOGGER "SUCCESS: Proxy is working. Received HTTP 200 from OpenRouter auth endpoint."
else
    LOGGER "WARNING: Unable to verify proxy via curl. Check logs and service status."
    LOGGER "You can test manually with:"
    LOGGER "  curl -v -x http://127.0.0.1:${LOCAL_LISTEN_PORT} https://openrouter.ai/api/v1/auth/key"
fi

LOGGER "Setup complete. To manage the service:"
LOGGER "  sudo systemctl status ${SERVICE_NAME}"
LOGGER "  sudo systemctl stop ${SERVICE_NAME}"
LOGGER "  sudo systemctl start ${SERVICE_NAME}"
LOGGER "  sudo journalctl -u ${SERVICE_NAME} -f"