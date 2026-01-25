#!/bin/bash

# Ensure script is run as root
if [ "$EUID" -ne 0 ]; then 
  echo "Please run as root (sudo ./setup_service.sh)"
  exit
fi

echo "Installing Flask for root user..."
pip3 install -r requirements.txt

echo "Installing systemd service..."
cp dpi-server.service /etc/systemd/system/dpi-server.service

echo "Reloading systemd daemon..."
systemctl daemon-reload

echo "Enabling and starting dpi-server..."
systemctl enable dpi-server
systemctl restart dpi-server

echo "Status:"
systemctl status dpi-server --no-pager
