import paho.mqtt.publish as publish
import sys
import time

# --- Inställningar ---
HAND_PI_IP = "192.168.10.42"  # Din Hand-Pi:s IP
TOPIC_BASE = "hometosh/hand/"

# Definitioner av gester (finger: vinkel)
GESTURES = {
    "open":  {"pinkie": 0, "ring": 0, "middle": 0, "index": 0, "thumb": 0},
    "fist":  {"pinkie": 90, "ring": 90, "middle": 90, "index": 90, "thumb": 90},
    "peace": {"pinkie": 90, "ring": 90, "middle": 0, "index": 0, "thumb": 90},
    "point": {"pinkie": 90, "ring": 90, "middle": 90, "index": 0, "thumb": 90},
    "wave":  "special" # Hanteras i koden
}

def send_move(finger, angle):
    publish.single(TOPIC_BASE + finger, str(angle), hostname=HAND_PI_IP)

def run_gesture(name):
    if name == "wave":
        print("Vinkar...")
        for _ in range(3):
            for f in ["pinkie", "ring", "middle", "index"]: send_move(f, 0)
            time.sleep(0.3)
            for f in ["pinkie", "ring", "middle", "index"]: send_move(f, 45)
            time.sleep(0.3)
    elif name in GESTURES:
        print(f"Kör gest: {name}")
        for finger, angle in GESTURES[name].items():
            send_move(finger, angle)
    else:
        print("Okänd gest!")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Användning:")
        print("  python3 hometosh_hand.py <gest>          (t.ex. open, fist, wave, peace)")
        print("  python3 hometosh_hand.py <finger> <vinkel> (t.ex. index 45)")
        sys.exit(1)

    if len(sys.argv) == 2:
        run_gesture(sys.argv[1])
    else:
        send_move(sys.argv[1], sys.argv[2])