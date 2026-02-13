#!/bin/bash

# Update package list
echo "Updating package list..."
sudo apt-get update

# Install system dependencies
# python3-tk is required for tkinter (used in dpi_calculator.py)
echo "Installing system dependencies..."
sudo apt-get install -y python3-pip python3-tk

# Install Python dependencies
echo "Installing Python dependencies..."
pip3 install -r requirements.txt

echo "Dependencies installed successfully."
