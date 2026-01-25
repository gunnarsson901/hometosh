#!/usr/bin/env python3
import os
import re
from flask import Flask, request, jsonify

app = Flask(__name__)

CONFIG_PATH = "/boot/firmware/config.txt"
# For testing safely without messing up boot, uncomment below:
# CONFIG_PATH = "./config_test.txt" 

def read_config():
    if not os.path.exists(CONFIG_PATH):
        return {}
    
    config = {}
    with open(CONFIG_PATH, 'r') as f:
        content = f.read()
    
    # Extract params using regex or simple parsing
    # Looking for dtparam=...
    # simple parser for specific keys we care about
    keys = ['hactive', 'hfp', 'hsync', 'hbp', 'vactive', 'vfp', 'vsync', 'vbp', 'clock-frequency', 'temperature', 'color-mode']
    
    # Known color formats to look for
    color_formats = ['rgb888', 'rgb666', 'rgb565', 'rgb666-padhi', 'bgr888']

    for line in content.splitlines():
        if line.strip().startswith('dtparam='):
            # remove dtparam=
            param = line.strip()[8:]
            
            # Check for key=value
            if '=' in param:
                k, v = param.split('=', 1)
                if k in keys:
                    try:
                        # Try to parse int if possible, else string
                        config[k] = int(v)
                    except:
                        config[k] = v
            # Check for boolean/single flags (like color format)
            elif param in color_formats:
                config['color-format'] = param
    
    # Defaults if missing
    defaults = {
        "hactive": 512, "hfp": 16, "hsync": 32, "hbp": 48,
        "vactive": 342, "vfp": 10, "vsync": 2, "vbp": 30,
        "clock-frequency": 15667200,
        "color-format": "rgb565",
        "temperature": 6500,
        "color-mode": "default"
    }
    
    for k, v in defaults.items():
        if k not in config:
            config[k] = v
            
    return config

@app.route('/api/config', methods=['GET'])
def get_config():
    return jsonify(read_config())

@app.route('/api/config', methods=['POST'])
def save_config():
    data = request.json
    if not data:
        return jsonify({"error": "No data"}), 400

    # Read current lines to preserve other settings
    if os.path.exists(CONFIG_PATH):
        with open(CONFIG_PATH, 'r') as f:
            lines = f.readlines()
    else:
        lines = []

    # Remove old DPI settings to avoid duplicates
    # We remove lines starting with dtparam=key=... for our specific keys
    keys_to_update = ['hactive', 'hfp', 'hsync', 'hbp', 'vactive', 'vfp', 'vsync', 'vbp', 'clock-frequency', 'temperature', 'color-mode']
    color_formats = ['rgb888', 'rgb666', 'rgb565', 'rgb666-padhi', 'bgr888']
    
    new_lines = []
    for line in lines:
        skip = False
        s_line = line.strip()
        if s_line.startswith('dtparam='):
            param = s_line[8:]
            if '=' in param:
                k, _ = param.split('=', 1)
                if k in keys_to_update:
                    skip = True
            elif param in color_formats:
                skip = True
        
        # Also remove overlays to ensure we put them back in correct order if we want
        if s_line.startswith('dtoverlay=vc4-kms-v3d') or s_line.startswith('dtoverlay=vc4-kms-dpi-generic'):
             skip = True

        if not skip:
            new_lines.append(line)

    # Append new config
    new_lines.append("\n# --- DPI Config (Added by Remote App) ---\n")
    new_lines.append("dtoverlay=vc4-kms-v3d\n")
    new_lines.append("dtoverlay=vc4-kms-dpi-generic\n")
    
    for k in keys_to_update:
        if k in data:
            new_lines.append(f"dtparam={k}={data[k]}\n")
            
    # Handle color format as a standalone flag
    if 'color-format' in data and data['color-format'] in color_formats:
         new_lines.append(f"dtparam={data['color-format']}\n")

    # Write back
    try:
        with open(CONFIG_PATH, 'w') as f:
            f.writelines(new_lines)
        return jsonify({"status": "saved", "message": "Config saved. Reboot to apply."})
    except Exception as e:
        return jsonify({"error": str(e)}), 500

@app.route('/api/reboot', methods=['POST'])
def reboot():
    os.system("reboot")
    return jsonify({"status": "rebooting"})

if __name__ == '__main__':
    # Listen on all interfaces
    app.run(host='0.0.0.0', port=5000)
