#!/usr/bin/env python3
import tkinter as tk
from tkinter import ttk, messagebox

class DPICalculator(tk.Tk):
    def __init__(self):
        super().__init__()

        self.title("VC4-KMS-DPI-Generic Calculator (Interactive)")
        self.geometry("1000x700")

        # Layout: Left Panel (Controls), Right Panel (Visuals)
        self.paned = ttk.PanedWindow(self, orient=tk.HORIZONTAL)
        self.paned.pack(fill=tk.BOTH, expand=True)

        self.left_frame = ttk.Frame(self.paned, padding="10", width=400)
        self.right_frame = ttk.Frame(self.paned, padding="10")
        
        self.paned.add(self.left_frame, weight=1)
        self.paned.add(self.right_frame, weight=3)

        # Variables
        self.inputs = {}
        self.flags = {}
        self.color_format = tk.StringVar(value="rgb888")
        
        # --- Left Panel: Controls ---
        self.setup_controls()

        # --- Right Panel: Visualization ---
        self.setup_canvas()
        
        # Initial Draw
        self.redraw_canvas()

    def setup_controls(self):
        # Header
        ttk.Label(self.left_frame, text="Parameters", font=("Helvetica", 14, "bold")).pack(pady=(0, 10), anchor="w")

        # Scrollable Frame for inputs if needed, but simple pack for now
        control_container = ttk.Frame(self.left_frame)
        control_container.pack(fill=tk.BOTH, expand=True)

        # Standard Fields
        self.create_labeled_entry(control_container, "H Active:", "hactive", 512)
        self.create_labeled_entry(control_container, "H Front Porch:", "hfp", 16)
        self.create_labeled_entry(control_container, "H Sync:", "hsync", 32)
        self.create_labeled_entry(control_container, "H Back Porch:", "hbp", 48)
        
        ttk.Separator(control_container, orient='horizontal').pack(fill='x', pady=10)

        self.create_labeled_entry(control_container, "V Active:", "vactive", 342)
        self.create_labeled_entry(control_container, "V Front Porch:", "vfp", 10)
        self.create_labeled_entry(control_container, "V Sync:", "vsync", 2)
        self.create_labeled_entry(control_container, "V Back Porch:", "vbp", 30)

        ttk.Separator(control_container, orient='horizontal').pack(fill='x', pady=10)

        self.create_labeled_entry(control_container, "Clock (Hz):", "clock-frequency", 15667200)
        
        # Format
        fmt_frame = ttk.Frame(control_container)
        fmt_frame.pack(fill='x', pady=2)
        ttk.Label(fmt_frame, text="Format:").pack(side=tk.LEFT)
        formats = ["rgb888", "rgb666", "rgb565", "rgb666-padhi", "bgr888"]
        ttk.OptionMenu(fmt_frame, self.color_format, formats[0], *formats).pack(side=tk.RIGHT)

        # Buttons
        btn_frame = ttk.Frame(self.left_frame)
        btn_frame.pack(fill='x', pady=20)
        
        ttk.Button(btn_frame, text="Auto-Fill / Reset", command=self.auto_fill_defaults).pack(fill='x', pady=2)
        ttk.Button(btn_frame, text="Generate Config", command=self.generate_config).pack(fill='x', pady=2)

        # Stats Label
        self.stats_label = ttk.Label(self.left_frame, text="Stats: N/A", justify=tk.LEFT, font=("Courier", 9))
        self.stats_label.pack(fill='x', pady=10)

        # Logic Variables
        self.lock_refresh = tk.BooleanVar(value=False)
        self.target_refresh = 0.0
        
        # Lock Refresh Checkbox
        ttk.Checkbutton(self.left_frame, text="Lock Refresh Rate (Adjust H-Porches)", 
                        variable=self.lock_refresh, command=self.on_lock_toggle).pack(pady=5)

    def on_lock_toggle(self):
        if self.lock_refresh.get():
            # Snap current refresh rate as target
            h_total = self.get_val("hactive") + self.get_val("hfp") + self.get_val("hsync") + self.get_val("hbp")
            v_total = self.get_val("vactive") + self.get_val("vfp") + self.get_val("vsync") + self.get_val("vbp")
            clk = self.get_val("clock-frequency")
            if h_total > 0 and v_total > 0:
                self.target_refresh = clk / (h_total * v_total)
            else:
                self.target_refresh = 60.0 # Fallback

    def create_labeled_entry(self, parent, label, key, default):
        frame = ttk.Frame(parent)
        frame.pack(fill='x', pady=2)
        ttk.Label(frame, text=label, width=15).pack(side=tk.LEFT)
        
        var = tk.IntVar(value=default)
        entry = ttk.Entry(frame, textvariable=var)
        entry.pack(side=tk.RIGHT, expand=True, fill='x')
        
        # Bind changes
        # Use trace for all, but handle Clock specifically if Locked
        var.trace_add("write", lambda *args, k=key: self.on_input_change(k))
        self.inputs[key] = var

    def on_input_change(self, key_changed=None):
        # Prevent recursion loop if we are updating inputs programmatically
        if getattr(self, '_updating', False):
            return

        if key_changed == "clock-frequency" and self.lock_refresh.get():
            self.adjust_timings_for_clock()

        self.redraw_canvas()
        self.update_stats()

    def adjust_timings_for_clock(self):
        # Keep V-Total constant, adjust H-Total to match target_refresh with new Clock
        try:
            self._updating = True
            
            clk = self.get_val("clock-frequency")
            if clk <= 0 or self.target_refresh <= 0: return

            # Refresh = Clock / (H_Total * V_Total)
            # => H_Total = Clock / (Refresh * V_Total)
            
            v_total = self.get_val("vactive") + self.get_val("vfp") + self.get_val("vsync") + self.get_val("vbp")
            if v_total <= 0: return

            target_h_total = clk / (self.target_refresh * v_total)
            target_h_total = int(round(target_h_total))

            # Current H parts
            ha = self.get_val("hactive")
            hs = self.get_val("hsync")
            # We need to find new HFP + HBP = target_h_total - ha - hs
            
            new_blanking = target_h_total - ha - hs
            if new_blanking < 0: new_blanking = 0 # Safety

            # Distribute proportionally based on current HFP/HBP ratio?
            curr_hfp = self.get_val("hfp")
            curr_hbp = self.get_val("hbp")
            curr_total_porch = curr_hfp + curr_hbp
            
            if curr_total_porch > 0:
                ratio = curr_hfp / curr_total_porch
                new_hfp = int(new_blanking * ratio)
                new_hbp = new_blanking - new_hfp
            else:
                # Split evenly if they were 0
                new_hfp = new_blanking // 2
                new_hbp = new_blanking - new_hfp

            self.inputs["hfp"].set(new_hfp)
            self.inputs["hbp"].set(new_hbp)

        except Exception as e:
            print(f"Error adjusting timings: {e}")
        finally:
            self._updating = False

    def setup_canvas(self):
        ttk.Label(self.right_frame, text="Drag the Blue Box to adjust Position (Porches)", font=("Helvetica", 10, "italic")).pack(anchor="n")
        
        self.canvas = tk.Canvas(self.right_frame, bg="#333333")
        self.canvas.pack(fill=tk.BOTH, expand=True, padx=5, pady=5)

        # Bind mouse events
        self.canvas.bind("<ButtonPress-1>", self.on_drag_start)
        self.canvas.bind("<B1-Motion>", self.on_drag_motion)
        self.canvas.bind("<Configure>", lambda e: self.redraw_canvas())

        self.drag_data = {"x": 0, "y": 0}

    def get_val(self, key):
        try:
            return self.inputs[key].get()
        except:
            return 0

    def set_val(self, key, value):
        self.inputs[key].set(int(value))

    def auto_fill_defaults(self):
        # Reset to Mac SE like defaults if things look weird, or standard VGA
        # For this tool, let's assume Mac SE defaults since requested
        defaults = {
            "hactive": 512, "hfp": 16, "hsync": 32, "hbp": 48,
            "vactive": 342, "vfp": 10, "vsync": 2, "vbp": 30,
            "clock-frequency": 15667200
        }
        for k, v in defaults.items():
            self.set_val(k, v)

    def update_stats(self):
        h_total = self.get_val("hactive") + self.get_val("hfp") + self.get_val("hsync") + self.get_val("hbp")
        v_total = self.get_val("vactive") + self.get_val("vfp") + self.get_val("vsync") + self.get_val("vbp")
        clk = self.get_val("clock-frequency")
        
        if h_total > 0 and v_total > 0:
            refresh = clk / (h_total * v_total)
            txt = (f"H-Total: {h_total} px\n"
                   f"V-Total: {v_total} lines\n"
                   f"Refresh: {refresh:.2f} Hz\n"
                   f"H-Freq:  {clk/h_total/1000:.2f} kHz")
        else:
            txt = "Invalid Timings"
        
        self.stats_label.config(text=txt)

    def redraw_canvas(self):
        self.canvas.delete("all")
        
        w = self.canvas.winfo_width()
        h = self.canvas.winfo_height()
        if w < 10 or h < 10: return

        # Get Timings
        ha = self.get_val("hactive")
        hfp = self.get_val("hfp")
        hsync = self.get_val("hsync")
        hbp = self.get_val("hbp")
        
        va = self.get_val("vactive")
        vfp = self.get_val("vfp")
        vsync = self.get_val("vsync")
        vbp = self.get_val("vbp")

        h_total = ha + hfp + hsync + hbp
        v_total = va + vfp + vsync + vbp

        if h_total == 0 or v_total == 0: return

        # Calculate Scale to fit canvas with margin
        margin = 20
        avail_w = w - 2 * margin
        avail_h = h - 2 * margin
        
        scale_x = avail_w / h_total
        scale_y = avail_h / v_total
        
        # Use the smaller scale to maintain aspect ratio, or stretch? 
        # Usually easier to see if we stretch to fill available space, 
        # but let's try to keep relative aspect ratio if it fits, otherwise fill.
        # Actually, for timing diagrams, filling the space is better visibility.
        
        # Draw Total Frame (The "Wire" signal boundary)
        # We model the cycle as: Sync -> Back Porch -> Active -> Front Porch
        # But visually on screen usually: Active is center, surrounded by porches.
        # Let's map coordinates:
        # X: 0 -> Sync -> BP -> Active -> FP -> End
        
        xs_sync = margin
        xe_sync = xs_sync + hsync * scale_x
        
        xs_bp = xe_sync
        xe_bp = xs_bp + hbp * scale_x
        
        xs_act = xe_bp
        xe_act = xs_act + ha * scale_x
        
        xs_fp = xe_act
        xe_fp = xs_fp + hfp * scale_x
        
        # Y Coordinates
        ys_sync = margin
        ye_sync = ys_sync + vsync * scale_y
        
        ys_bp = ye_sync
        ye_bp = ys_bp + vbp * scale_y
        
        ys_act = ye_bp
        ye_act = ys_act + va * scale_y
        
        ys_fp = ye_act
        ye_fp = ys_fp + vfp * scale_y

        # Draw Regions
        # 1. Total Area (Outline)
        self.canvas.create_rectangle(margin, margin, xe_fp, ye_fp, outline="#555", width=2, tags="total")
        
        # 2. Sync Area (Red-ish hint)
        # H-Sync stripe
        self.canvas.create_rectangle(xs_sync, margin, xe_sync, ye_fp, fill="#442222", outline="")
        # V-Sync stripe
        self.canvas.create_rectangle(margin, ys_sync, xe_fp, ye_sync, fill="#442222", outline="")
        # Intersection
        self.canvas.create_rectangle(xs_sync, ys_sync, xe_sync, ye_sync, fill="#662222", outline="")

        # 3. Active Area (Blue, Draggable)
        self.active_rect = self.canvas.create_rectangle(xs_act, ys_act, xe_act, ye_act, 
                                                        fill="#225588", outline="#4488aa", width=2, tags="active")
        
        # Text Labels
        self.canvas.create_text((xs_act + xe_act)/2, (ys_act + ye_act)/2, text="ACTIVE DISPLAY", fill="white", font=("Helvetica", 12, "bold"))
        
        # Draw dimension lines or labels?
        # HBP
        if hbp > 0:
            self.canvas.create_text((xs_bp + xe_bp)/2, ys_act + 10, text=f"HBP\n{hbp}", fill="#888", anchor="n", font=("Arial", 8))
        # HFP
        if hfp > 0:
            self.canvas.create_text((xs_fp + xe_fp)/2, ys_act + 10, text=f"HFP\n{hfp}", fill="#888", anchor="n", font=("Arial", 8))
        # VBP
        if vbp > 0:
            self.canvas.create_text(xs_act + 10, (ys_bp + ye_bp)/2, text=f"VBP {vbp}", fill="#888", anchor="w", font=("Arial", 8))

    def on_drag_start(self, event):
        # Check if clicked inside active rect
        x = self.canvas.canvasx(event.x)
        y = self.canvas.canvasy(event.y)
        coords = self.canvas.coords("active")
        if not coords: return
        x1, y1, x2, y2 = coords
        
        if x1 <= x <= x2 and y1 <= y <= y2:
            self.drag_data["x"] = x
            self.drag_data["y"] = y
            self.drag_data["dragging"] = True
        else:
            self.drag_data["dragging"] = False

    def on_drag_motion(self, event):
        if not self.drag_data.get("dragging"): return

        dx_px = event.x - self.drag_data["x"]
        dy_px = event.y - self.drag_data["y"]

        # Convert pixels to timing units
        # Re-calculate scale (inefficient to do every frame, but safe)
        h_total = self.get_val("hactive") + self.get_val("hfp") + self.get_val("hsync") + self.get_val("hbp")
        v_total = self.get_val("vactive") + self.get_val("vfp") + self.get_val("vsync") + self.get_val("vbp")
        
        w = self.canvas.winfo_width()
        h = self.canvas.winfo_height()
        margin = 20
        scale_x = (w - 2 * margin) / h_total if h_total else 1
        scale_y = (h - 2 * margin) / v_total if v_total else 1

        # Calculate delta in timing units
        dx_units = int(dx_px / scale_x)
        dy_units = int(dy_px / scale_y)

        # Apply changes if significant
        if dx_units != 0:
            # Moving Right: Increase HBP, Decrease HFP
            # Moving Left: Decrease HBP, Increase HFP
            new_hbp = self.get_val("hbp") + dx_units
            new_hfp = self.get_val("hfp") - dx_units
            
            # Constraints
            if new_hbp >= 0 and new_hfp >= 0:
                self.inputs["hbp"].set(new_hbp)
                self.inputs["hfp"].set(new_hfp)
                self.drag_data["x"] = event.x # Reset reference only on success

        if dy_units != 0:
            # Moving Down: Increase VBP, Decrease VFP
            new_vbp = self.get_val("vbp") + dy_units
            new_vfp = self.get_val("vfp") - dy_units
            
            if new_vbp >= 0 and new_vfp >= 0:
                self.inputs["vbp"].set(new_vbp)
                self.inputs["vfp"].set(new_vfp)
                self.drag_data["y"] = event.y

    def generate_config(self):
        config = []
        config.append("dtoverlay=vc4-kms-v3d")
        config.append("dtoverlay=vc4-kms-dpi-generic")
        
        # Timings
        params = []
        for key in ['hactive', 'hfp', 'hsync', 'hbp', 'vactive', 'vfp', 'vsync', 'vbp']:
            params.append(f"{key}={self.get_val(key)}")
            
        params.append(f"clock-frequency={self.get_val('clock-frequency')}")
        params.append(self.color_format.get())

        for p in params:
            config.append(f"dtparam={p}")
            
        # Show in a popup window
        top = tk.Toplevel(self)
        top.title("Config.txt Output")
        text = tk.Text(top, height=15, width=50)
        text.pack(fill=tk.BOTH, expand=True)
        text.insert("1.0", "\n".join(config))

if __name__ == "__main__":
    app = DPICalculator()
    app.mainloop()