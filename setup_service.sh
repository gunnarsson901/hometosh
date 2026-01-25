#!/bin/bash

# Ensure script is run as root
if [ "$EUID" -ne 0 ]; then 
  echo "Please run as root (sudo ./setup_service.sh)"
  exit
fi

echo "Installing Flask for root user..."
# Use apt instead of pip to avoid externally-managed-environment error
apt-get install -y python3-flask

echo "Configuring service file with correct paths..."
CURRENT_USER=$(logname || echo $SUDO_USER)
CURRENT_DIR=$(pwd)

# Replace placeholders in service file
sed -i "s|User=root|User=root|g" dpi-server.service
sed -i "s|WorkingDirectory=.*|WorkingDirectory=$CURRENT_DIR|g" dpi-server.service
sed -i "s|ExecStart=.*|ExecStart=/usr/bin/python3 $CURRENT_DIR/dpi_server.py|g" dpi-server.service

echo "Installing systemd service..."
cp dpi-server.service /etc/systemd/system/dpi-server.service

echo "Reloading systemd daemon..."
systemctl daemon-reload

echo "Enabling and starting dpi-server..."
systemctl enable dpi-server
systemctl restart dpi-server

echo "Status:"
systemctl status dpi-server --no-pager
