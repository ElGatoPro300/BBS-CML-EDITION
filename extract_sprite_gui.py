#!/usr/bin/env python3
"""
Sprite Sheet Extractor - GUI Tool
Requires: pip install Pillow
"""

import os
import sys
import tkinter as tk
from tkinter import filedialog, messagebox
from pathlib import Path

try:
    from PIL import Image, ImageTk
except ImportError:
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "Pillow"])
    from PIL import Image, ImageTk


# ── color helpers ─────────────────────────────────────────────────────────────

def color_distance(c1, c2):
    return sum((a - b) ** 2 for a, b in zip(c1[:3], c2[:3])) ** 0.5


def remove_color(img, color, tolerance):
    img = img.convert("RGBA")
    pixels = img.load()
    w, h = img.size
    for y in range(h):
        for x in range(w):
            px = pixels[x, y]
            if color_distance(px, color) <= tolerance:
                pixels[x, y] = (px[0], px[1], px[2], 0)
    return img


def hex_to_rgb(hex_str):
    hex_str = hex_str.lstrip("#")
    if len(hex_str) == 6:
        return tuple(int(hex_str[i:i + 2], 16) for i in (0, 2, 4))
    raise ValueError(f"Invalid hex color: #{hex_str}")


def rgb_to_hex(r, g, b):
    return f"#{r:02X}{g:02X}{b:02X}"


# ── zoomable canvas ───────────────────────────────────────────────────────────

class ZoomCanvas(tk.Canvas):
    """Canvas with mouse-wheel zoom and middle/right-click pan."""

    MIN_ZOOM = 0.1
    MAX_ZOOM = 32.0

    def __init__(self, master, **kw):
        super().__init__(master, **kw)
        self._zoom  = 1.0
        self._pan_x = 0.0
        self._pan_y = 0.0
        self._drag  = None
        self._img_pil = None
        self._img_tk  = None

        self.bind("<MouseWheel>",    self._on_wheel)
        self.bind("<Button-4>",      self._on_wheel)
        self.bind("<Button-5>",      self._on_wheel)
        self.bind("<ButtonPress-2>", self._pan_start)
        self.bind("<B2-Motion>",     self._pan_move)
        self.bind("<ButtonPress-3>", self._pan_start)
        self.bind("<B3-Motion>",     self._pan_move)
        self.bind("<Configure>",     lambda e: self._redraw())

    def set_image(self, pil_img):
        self._img_pil = pil_img.convert("RGBA")
        self.update_idletasks()
        cw = self.winfo_width()  or 600
        ch = self.winfo_height() or 400
        iw, ih = self._img_pil.size
        self._zoom  = min(cw / iw, ch / ih, 1.0)
        self._pan_x = (cw - iw * self._zoom) / 2
        self._pan_y = (ch - ih * self._zoom) / 2
        self._redraw()

    def _redraw(self):
        if self._img_pil is None:
            return
        iw, ih = self._img_pil.size
        dw = max(1, int(iw * self._zoom))
        dh = max(1, int(ih * self._zoom))
        resample = Image.NEAREST if self._zoom >= 2 else Image.LANCZOS
        self._img_tk = ImageTk.PhotoImage(self._img_pil.resize((dw, dh), resample))
        self.delete("img")
        self.create_image(self._pan_x, self._pan_y, anchor="nw",
                          image=self._img_tk, tags="img")
        self.tag_lower("img")
        self.after_idle(self.event_generate, "<<ViewChanged>>")

    def sheet_to_canvas(self, sx, sy):
        return sx * self._zoom + self._pan_x, sy * self._zoom + self._pan_y

    def canvas_to_sheet(self, cx, cy):
        return (cx - self._pan_x) / self._zoom, (cy - self._pan_y) / self._zoom

    def get_zoom(self):
        return self._zoom

    def _on_wheel(self, event):
        factor = 1.15 if (event.num == 4 or event.delta > 0) else 1 / 1.15
        new_zoom = max(self.MIN_ZOOM, min(self.MAX_ZOOM, self._zoom * factor))
        ratio = new_zoom / self._zoom
        self._pan_x = event.x - ratio * (event.x - self._pan_x)
        self._pan_y = event.y - ratio * (event.y - self._pan_y)
        self._zoom  = new_zoom
        self._redraw()

    def _pan_start(self, event):
        self._drag = (event.x, event.y)

    def _pan_move(self, event):
        if self._drag is None:
            return
        self._pan_x += event.x - self._drag[0]
        self._pan_y += event.y - self._drag[1]
        self._drag = (event.x, event.y)
        self._redraw()


# ── main app ──────────────────────────────────────────────────────────────────

class App(tk.Tk):
    C  = "#1e1e2e"
    C1 = "#313244"
    C2 = "#45475a"
    FG = "#cdd6f4"
    AC = "#89b4fa"   # blue
    GR = "#a6e3a1"   # green  (selected)
    YL = "#f9e2af"   # yellow (rubber-band)
    RD = "#f38ba8"   # red

    DRAG_THRESHOLD = 4   # pixels before a press becomes a drag

    def __init__(self):
        super().__init__()
        self.title("Sprite Sheet Extractor")
        self.configure(bg=self.C)
        self.minsize(900, 560)
        self.resizable(True, True)

        self.sheet_path = tk.StringVar()
        self.sprite_w   = tk.IntVar(value=32)
        self.sprite_h   = tk.IntVar(value=32)
        self.gap_x      = tk.IntVar(value=0)
        self.gap_y      = tk.IntVar(value=0)
        self.offset_x   = tk.IntVar(value=0)
        self.offset_y   = tk.IntVar(value=0)
        self.cols       = tk.IntVar(value=0)
        self.rows       = tk.IntVar(value=0)
        self.tolerance  = tk.DoubleVar(value=30.0)
        self.skip_empty = tk.BooleanVar(value=True)
        self.prefix     = tk.StringVar(value="sprite")
        self.output_dir = tk.StringVar()

        self.bg_color      = "#622082"
        self.remove_colors = []
        self._sheet_img    = None
        self._sprite_tk    = None

        # selection state
        self._selection    = set()    # set of (row, col)
        self._press_pos    = None     # (canvas_x, canvas_y) on ButtonPress
        self._dragging     = False    # True once drag threshold exceeded
        self._rubber_id    = None     # canvas item id for rubber-band rect

        self._build_ui()
        self._bind_grid_refresh()

    # ── styles ────────────────────────────────────────────────────────────────

    def _S(self, **kw):
        return {"bg": self.C, "fg": self.FG, "font": ("Segoe UI", 9), **kw}

    def _E(self, **kw):
        return {"bg": self.C1, "fg": self.FG, "insertbackground": self.FG,
                "relief": "flat", "font": ("Segoe UI", 9), **kw}

    def _B(self, **kw):
        return {"bg": self.AC, "fg": self.C, "activebackground": "#74c7ec",
                "activeforeground": self.C, "relief": "flat",
                "font": ("Segoe UI", 9, "bold"), "cursor": "hand2",
                "padx": 8, "pady": 3, **kw}

    def _BD(self, **kw):
        return {"bg": self.C2, "fg": self.FG, "activebackground": "#585b70",
                "activeforeground": self.FG, "relief": "flat",
                "font": ("Segoe UI", 9), "cursor": "hand2",
                "padx": 8, "pady": 3, **kw}

    # ── layout ────────────────────────────────────────────────────────────────

    def _build_ui(self):
        self.columnconfigure(0, weight=0)
        self.columnconfigure(1, weight=1)
        self.rowconfigure(0, weight=1)

        left_outer = tk.Frame(self, bg=self.C, width=300)
        left_outer.grid(row=0, column=0, sticky="nsew")
        left_outer.grid_propagate(False)

        sc = tk.Canvas(left_outer, bg=self.C, highlightthickness=0)
        sb = tk.Scrollbar(left_outer, orient="vertical", command=sc.yview)
        sc.configure(yscrollcommand=sb.set)
        sb.pack(side="right", fill="y")
        sc.pack(side="left", fill="both", expand=True)

        self._left = tk.Frame(sc, bg=self.C, padx=12, pady=10)
        win_id = sc.create_window((0, 0), window=self._left, anchor="nw")

        self._left.bind("<Configure>", lambda e: (
            sc.configure(scrollregion=sc.bbox("all")),
            sc.itemconfig(win_id, width=sc.winfo_width())
        ))
        sc.bind("<Configure>",  lambda e: sc.itemconfig(win_id, width=e.width))
        sc.bind("<MouseWheel>", lambda e: sc.yview_scroll(-1 if e.delta > 0 else 1, "units"))

        self._build_left()

        right = tk.Frame(self, bg=self.C, padx=8, pady=8)
        right.grid(row=0, column=1, sticky="nsew")
        right.rowconfigure(0, weight=3)
        right.rowconfigure(2, weight=1)
        right.columnconfigure(0, weight=1)

        self._build_sheet_preview(right)
        self._build_sprite_preview(right)

    def _build_left(self):
        p = self._left

        self._section(p, "Sprite Sheet")
        row = tk.Frame(p, bg=self.C)
        row.pack(fill="x", pady=(0, 6))
        tk.Entry(row, textvariable=self.sheet_path, **self._E()).pack(side="left", fill="x", expand=True, ipady=4)
        tk.Button(row, text="…", command=self._browse_sheet, **self._BD()).pack(side="left", padx=(4, 0))

        self._section(p, "Sprite Size")
        self._row2(p, "Width (px)", self.sprite_w, "Height (px)", self.sprite_h)

        self._section(p, "Gap Between Sprites")
        self._row2(p, "Gap X (px)", self.gap_x, "Gap Y (px)", self.gap_y)

        self._section(p, "Sheet Offset")
        self._row2(p, "Offset X (px)", self.offset_x, "Offset Y (px)", self.offset_y)

        self._section(p, "Grid Size  (0 = auto)")
        self._row2(p, "Columns", self.cols, "Rows", self.rows)

        self._section(p, "Background Color Removal")

        crow = tk.Frame(p, bg=self.C)
        crow.pack(fill="x", pady=(0, 4))
        self._color_preview = tk.Label(crow, bg=self.bg_color, width=3, relief="flat")
        self._color_preview.pack(side="left", ipady=10, padx=(0, 6))
        self._color_hex = tk.Entry(crow, width=9, **self._E())
        self._color_hex.insert(0, self.bg_color)
        self._color_hex.pack(side="left", ipady=4)
        self._color_hex.bind("<FocusOut>", self._on_hex_typed)
        self._color_hex.bind("<Return>",   self._on_hex_typed)
        tk.Button(crow, text="Pick", command=self._start_pick, **self._BD()).pack(side="left", padx=(4, 0))
        tk.Button(crow, text="Add",  command=self._add_color,  **self._B()).pack(side="left", padx=(4, 0))

        trow = tk.Frame(p, bg=self.C)
        trow.pack(fill="x", pady=(0, 4))
        tk.Label(trow, text="Tolerance:", **self._S()).pack(side="left")
        self._tol_label = tk.Label(trow, text="30", width=4, **self._S())
        self._tol_label.pack(side="right")
        tk.Scale(trow, from_=0, to=441, orient="horizontal", variable=self.tolerance,
                 bg=self.C, fg=self.FG, troughcolor=self.C1, highlightthickness=0,
                 showvalue=False, command=lambda v: self._tol_label.config(text=f"{float(v):.0f}")
                 ).pack(side="left", fill="x", expand=True, padx=4)

        self._color_list = tk.Frame(p, bg=self.C)
        self._color_list.pack(fill="x", pady=(0, 6))

        self._section(p, "Output")
        orow = tk.Frame(p, bg=self.C)
        orow.pack(fill="x", pady=(0, 4))
        tk.Entry(orow, textvariable=self.output_dir, **self._E()).pack(side="left", fill="x", expand=True, ipady=4)
        tk.Button(orow, text="…", command=self._browse_output, **self._BD()).pack(side="left", padx=(4, 0))

        prow = tk.Frame(p, bg=self.C)
        prow.pack(fill="x", pady=(0, 4))
        tk.Label(prow, text="File prefix:", **self._S()).pack(side="left")
        tk.Entry(prow, textvariable=self.prefix, width=14, **self._E()).pack(side="left", padx=(6, 0), ipady=4)

        tk.Checkbutton(p, text="Skip fully transparent sprites", variable=self.skip_empty,
                       bg=self.C, fg=self.FG, selectcolor=self.C1,
                       activebackground=self.C, activeforeground=self.FG,
                       font=("Segoe UI", 9)).pack(anchor="w", pady=(0, 8))

        tk.Button(p, text="Extract Sprites", command=self._extract, **self._B()).pack(fill="x", ipady=6)

        self._status = tk.Label(p, text="", bg=self.C, fg=self.GR,
                                font=("Segoe UI", 9), wraplength=260, justify="left")
        self._status.pack(anchor="w", pady=(6, 0))

    def _build_sheet_preview(self, parent):
        frame = tk.Frame(parent, bg=self.C)
        frame.grid(row=0, column=0, sticky="nsew")
        frame.rowconfigure(1, weight=1)
        frame.columnconfigure(0, weight=1)

        # header row
        hdr = tk.Frame(frame, bg=self.C)
        hdr.grid(row=0, column=0, sticky="ew", pady=(0, 4))

        tk.Label(hdr, text="Sheet Preview",
                 **self._S(fg=self.AC, font=("Segoe UI", 9, "bold"))).pack(side="left")
        self._zoom_label = tk.Label(hdr, text="zoom: 100%", **self._S(fg=self.C2))
        self._zoom_label.pack(side="left", padx=8)
        tk.Label(hdr, text="click=select  ·  shift+click=toggle  ·  drag=select area  ·  right-drag=pan",
                 **self._S(fg=self.C2, font=("Segoe UI", 8))).pack(side="left")

        # selection controls (right-aligned)
        self._sel_label = tk.Label(hdr, text="", **self._S(fg=self.GR, font=("Segoe UI", 8)))
        self._sel_label.pack(side="right", padx=(0, 4))
        tk.Button(hdr, text="Select All", command=self._select_all,   **self._BD()).pack(side="right", padx=(0, 4))
        tk.Button(hdr, text="Clear",      command=self._clear_selection, **self._BD()).pack(side="right", padx=(0, 4))

        # canvas
        self._zoom_canvas = ZoomCanvas(frame, bg="#181825",
                                       highlightthickness=1, highlightbackground=self.C2,
                                       cursor="crosshair")
        self._zoom_canvas.grid(row=1, column=0, sticky="nsew")

        self._zoom_canvas.bind("<Motion>",          self._on_sheet_motion)
        self._zoom_canvas.bind("<ButtonPress-1>",   self._on_press)
        self._zoom_canvas.bind("<B1-Motion>",       self._on_drag)
        self._zoom_canvas.bind("<ButtonRelease-1>", self._on_release)
        self._zoom_canvas.bind("<<ViewChanged>>",   lambda e: self._on_view_changed())

        self._hover_label = tk.Label(frame, text="", **self._S(fg=self.C2, font=("Segoe UI", 8)))
        self._hover_label.grid(row=2, column=0, sticky="w")

    def _build_sprite_preview(self, parent):
        tk.Frame(parent, bg=self.C2, height=1).grid(row=1, column=0, sticky="ew", pady=6)

        bot = tk.Frame(parent, bg=self.C)
        bot.grid(row=2, column=0, sticky="nsew")
        bot.rowconfigure(1, weight=1)
        bot.columnconfigure(1, weight=1)

        tk.Label(bot, text="Selected Sprite Preview",
                 **self._S(fg=self.AC, font=("Segoe UI", 9, "bold"))).grid(row=0, column=0, columnspan=2, sticky="w")

        self._sprite_canvas = tk.Canvas(bot, width=128, height=128, bg="#181825",
                                        highlightthickness=1, highlightbackground=self.C2)
        self._sprite_canvas.grid(row=1, column=0, sticky="ns", padx=(0, 12))

        info = tk.Frame(bot, bg=self.C)
        info.grid(row=1, column=1, sticky="nsew")
        self._sprite_info = tk.Label(info, text="Click a cell on the sheet to preview it.",
                                     **self._S(fg=self.C2, font=("Segoe UI", 8)), justify="left")
        self._sprite_info.pack(anchor="nw")

    # ── helpers ───────────────────────────────────────────────────────────────

    def _section(self, parent, text):
        tk.Label(parent, text=text, **self._S(fg=self.AC, font=("Segoe UI", 9, "bold"))
                 ).pack(anchor="w", pady=(10, 2))

    def _row2(self, parent, l1, v1, l2, v2):
        row = tk.Frame(parent, bg=self.C)
        row.pack(fill="x", pady=(0, 4))
        tk.Label(row, text=l1, width=12, anchor="w", **self._S()).pack(side="left")
        tk.Entry(row, textvariable=v1, width=6, **self._E()).pack(side="left", ipady=4, padx=(0, 10))
        tk.Label(row, text=l2, width=12, anchor="w", **self._S()).pack(side="left")
        tk.Entry(row, textvariable=v2, width=6, **self._E()).pack(side="left", ipady=4)

    def _bind_grid_refresh(self):
        for v in (self.sprite_w, self.sprite_h, self.gap_x, self.gap_y,
                  self.offset_x, self.offset_y, self.cols, self.rows):
            v.trace_add("write", lambda *_: self.after_idle(self._redraw_grid))

    def _on_view_changed(self):
        self._zoom_label.config(text=f"zoom: {int(self._zoom_canvas.get_zoom() * 100)}%")
        self._redraw_grid()

    def _grid_params(self):
        """Return (ox, oy, sw, sh, gx, gy, cols, rows) or None if invalid."""
        if self._sheet_img is None:
            return None
        try:
            sw = self.sprite_w.get()
            sh = self.sprite_h.get()
            if sw <= 0 or sh <= 0:
                return None
            ox = self.offset_x.get()
            oy = self.offset_y.get()
            gx = self.gap_x.get()
            gy = self.gap_y.get()
            iw, ih = self._sheet_img.size
            cols = self.cols.get() or max(1, (iw - ox + gx) // (sw + gx))
            rows = self.rows.get() or max(1, (ih - oy + gy) // (sh + gy))
            return ox, oy, sw, sh, gx, gy, cols, rows
        except (tk.TclError, ValueError, ZeroDivisionError):
            return None

    def _cell_at(self, sx, sy):
        """Return (row, col) for sheet pixel (sx, sy), or None if out of bounds."""
        p = self._grid_params()
        if p is None:
            return None
        ox, oy, sw, sh, gx, gy, cols, rows = p
        iw, ih = self._sheet_img.size
        col = (sx - ox) // (sw + gx)
        row = (sy - oy) // (sh + gy)
        if col < 0 or row < 0 or col >= cols or row >= rows:
            return None
        cx = ox + col * (sw + gx)
        cy = oy + row * (sh + gy)
        # only register if the click landed inside the sprite, not in the gap
        if sx >= cx + sw or sy >= cy + sh:
            return None
        if cx + sw > iw or cy + sh > ih:
            return None
        return int(row), int(col)

    def _cells_in_rect(self, sx1, sy1, sx2, sy2):
        """Return set of (row, col) whose cells overlap the given sheet rect."""
        p = self._grid_params()
        if p is None:
            return set()
        ox, oy, sw, sh, gx, gy, cols, rows = p
        iw, ih = self._sheet_img.size
        rx1, ry1 = min(sx1, sx2), min(sy1, sy2)
        rx2, ry2 = max(sx1, sx2), max(sy1, sy2)
        result = set()
        for r in range(rows):
            for c in range(cols):
                cx = ox + c * (sw + gx)
                cy = oy + r * (sh + gy)
                if cx + sw > iw or cy + sh > ih:
                    continue
                # intersect
                if cx < rx2 and cx + sw > rx1 and cy < ry2 and cy + sh > ry1:
                    result.add((r, c))
        return result

    def _update_sel_label(self):
        n = len(self._selection)
        if n == 0:
            self._sel_label.config(text="all sprites")
        else:
            self._sel_label.config(text=f"{n} selected")

    # ── files ─────────────────────────────────────────────────────────────────

    def _browse_sheet(self):
        path = filedialog.askopenfilename(
            title="Open Sprite Sheet",
            filetypes=[("Images", "*.png *.jpg *.jpeg *.bmp *.gif *.webp"), ("All files", "*.*")]
        )
        if path:
            self.sheet_path.set(path)
            self._load_sheet(path)
            if not self.output_dir.get():
                self.output_dir.set(str(Path(path).parent / (Path(path).stem + "_sprites")))

    def _browse_output(self):
        path = filedialog.askdirectory(title="Select Output Folder")
        if path:
            self.output_dir.set(path)

    def _load_sheet(self, path):
        try:
            self._sheet_img = Image.open(path).convert("RGBA")
            self._selection.clear()
            self._zoom_canvas.set_image(self._sheet_img)
            self._redraw_grid()
            self._update_sel_label()
        except Exception as e:
            messagebox.showerror("Error", f"Could not open image:\n{e}")

    # ── grid overlay ──────────────────────────────────────────────────────────

    def _redraw_grid(self):
        self._zoom_canvas.delete("grid")
        self._zoom_canvas.delete("sel")
        p = self._grid_params()
        if p is None:
            return

        ox, oy, sw, sh, gx, gy, cols, rows = p
        iw, ih = self._sheet_img.size
        zoom = self._zoom_canvas.get_zoom()
        lw = max(1, int(zoom * 0.5))

        for r in range(rows):
            for c in range(cols):
                sx = ox + c * (sw + gx)
                sy = oy + r * (sh + gy)
                if sx + sw > iw or sy + sh > ih:
                    continue
                cx1, cy1 = self._zoom_canvas.sheet_to_canvas(sx, sy)
                cx2, cy2 = self._zoom_canvas.sheet_to_canvas(sx + sw, sy + sh)

                if (r, c) in self._selection:
                    # selected: green fill + thicker border
                    self._zoom_canvas.create_rectangle(
                        cx1, cy1, cx2, cy2,
                        outline=self.GR, fill=self.GR, stipple="gray25",
                        width=max(2, lw + 1), tags="sel"
                    )
                else:
                    self._zoom_canvas.create_rectangle(
                        cx1, cy1, cx2, cy2,
                        outline=self.AC, fill="", width=lw, tags="grid"
                    )

    # ── selection helpers ─────────────────────────────────────────────────────

    def _select_all(self):
        p = self._grid_params()
        if p is None:
            return
        ox, oy, sw, sh, gx, gy, cols, rows = p
        iw, ih = self._sheet_img.size
        self._selection = {
            (r, c)
            for r in range(rows)
            for c in range(cols)
            if ox + c * (sw + gx) + sw <= iw and oy + r * (sh + gy) + sh <= ih
        }
        self._redraw_grid()
        self._update_sel_label()

    def _clear_selection(self):
        self._selection.clear()
        self._redraw_grid()
        self._update_sel_label()

    # ── sheet mouse interaction ───────────────────────────────────────────────

    def _on_sheet_motion(self, event):
        if self._sheet_img is None:
            return
        sx, sy = self._zoom_canvas.canvas_to_sheet(event.x, event.y)
        iw, ih = self._sheet_img.size
        if 0 <= sx < iw and 0 <= sy < ih:
            px = self._sheet_img.getpixel((int(sx), int(sy)))
            self._hover_label.config(
                text=f"({int(sx)}, {int(sy)})  {rgb_to_hex(*px[:3])}  rgba{px}"
            )
        else:
            self._hover_label.config(text="")

        # update rubber-band while dragging
        if self._dragging and self._press_pos is not None:
            self._update_rubber(event.x, event.y)

    def _on_press(self, event):
        self._press_pos = (event.x, event.y)
        self._dragging  = False

    def _on_drag(self, event):
        if self._press_pos is None:
            return
        dx = abs(event.x - self._press_pos[0])
        dy = abs(event.y - self._press_pos[1])
        if not self._dragging and (dx > self.DRAG_THRESHOLD or dy > self.DRAG_THRESHOLD):
            self._dragging = True
        if self._dragging:
            self._update_rubber(event.x, event.y)

    def _on_release(self, event):
        if self._press_pos is None:
            return

        shift = (event.state & 0x0001) != 0

        if self._dragging:
            # finish rubber-band — select cells in rect
            self._zoom_canvas.delete("rubber")
            sx1, sy1 = self._zoom_canvas.canvas_to_sheet(*self._press_pos)
            sx2, sy2 = self._zoom_canvas.canvas_to_sheet(event.x, event.y)
            cells = self._cells_in_rect(sx1, sy1, sx2, sy2)
            if shift:
                self._selection |= cells
            else:
                self._selection = cells
        else:
            # treat as click
            sx, sy = self._zoom_canvas.canvas_to_sheet(event.x, event.y)
            cell = self._cell_at(int(sx), int(sy))

            if cell is not None:
                if shift:
                    # toggle
                    if cell in self._selection:
                        self._selection.discard(cell)
                    else:
                        self._selection.add(cell)
                else:
                    # replace selection
                    self._selection = {cell}
                self._show_sprite_at_cell(*cell)

            # pick color regardless
            iw, ih = self._sheet_img.size if self._sheet_img else (0, 0)
            if self._sheet_img and 0 <= sx < iw and 0 <= sy < ih:
                px = self._sheet_img.getpixel((int(sx), int(sy)))
                self._set_color(rgb_to_hex(*px[:3]))

        self._press_pos = None
        self._dragging  = False
        self._redraw_grid()
        self._update_sel_label()

    def _update_rubber(self, cx, cy):
        """Draw the rubber-band selection rectangle."""
        self._zoom_canvas.delete("rubber")
        if self._press_pos is None:
            return
        x1, y1 = self._press_pos
        self._rubber_id = self._zoom_canvas.create_rectangle(
            x1, y1, cx, cy,
            outline=self.YL, fill=self.YL, stipple="gray25",
            dash=(4, 3), width=1, tags="rubber"
        )

    # ── sprite preview ────────────────────────────────────────────────────────

    def _show_sprite_at_cell(self, row, col):
        if self._sheet_img is None:
            return
        p = self._grid_params()
        if p is None:
            return
        ox, oy, sw, sh, gx, gy, cols, rows = p
        x = ox + col * (sw + gx)
        y = oy + row * (sh + gy)
        iw, ih = self._sheet_img.size
        if x + sw > iw or y + sh > ih:
            return

        sprite = self._sheet_img.crop((x, y, x + sw, y + sh))
        for color in self.remove_colors:
            sprite = remove_color(sprite, color, self.tolerance.get())

        cw = self._sprite_canvas.winfo_width()  or 128
        ch = self._sprite_canvas.winfo_height() or 128
        scale = min(cw / sw, ch / sh)
        dw, dh = max(1, int(sw * scale)), max(1, int(sh * scale))
        preview = sprite.resize((dw, dh), Image.NEAREST if scale >= 1 else Image.LANCZOS)
        bg = Image.new("RGBA", (cw, ch), (24, 24, 37, 255))
        bg.paste(preview, ((cw - dw) // 2, (ch - dh) // 2), preview)

        self._sprite_tk = ImageTk.PhotoImage(bg)
        self._sprite_canvas.delete("all")
        self._sprite_canvas.create_image(0, 0, anchor="nw", image=self._sprite_tk)
        self._sprite_info.config(text=f"Row {row}, Col {col}\nSheet pos: ({x}, {y})\nSize: {sw} × {sh} px")

    # ── color management ──────────────────────────────────────────────────────

    def _set_color(self, hex_c):
        self.bg_color = hex_c
        self._color_preview.config(bg=hex_c)
        self._color_hex.delete(0, "end")
        self._color_hex.insert(0, hex_c)

    def _on_hex_typed(self, _=None):
        val = self._color_hex.get().strip()
        if not val.startswith("#"):
            val = "#" + val
        try:
            hex_to_rgb(val)
            self._set_color(val)
        except ValueError:
            pass

    def _start_pick(self):
        self._status.config(text="Click anywhere on the sheet to pick a color.", fg=self.C2)

    def _add_color(self):
        try:
            rgb = hex_to_rgb(self.bg_color)
        except ValueError:
            messagebox.showerror("Error", "Invalid hex color.")
            return
        if rgb not in self.remove_colors:
            self.remove_colors.append(rgb)
            self._rebuild_color_list()

    def _rebuild_color_list(self):
        for w in self._color_list.winfo_children():
            w.destroy()
        for i, rgb in enumerate(self.remove_colors):
            hex_c = rgb_to_hex(*rgb)
            row = tk.Frame(self._color_list, bg=self.C)
            row.pack(fill="x", pady=1)
            tk.Label(row, bg=hex_c, width=3, relief="flat").pack(side="left", ipady=6, padx=(0, 6))
            tk.Label(row, text=hex_c, **self._S()).pack(side="left")
            tk.Button(row, text="✕", bg=self.C, fg=self.RD, relief="flat",
                      font=("Segoe UI", 9), cursor="hand2",
                      command=lambda i=i: self._remove_color(i)).pack(side="right")

    def _remove_color(self, index):
        if 0 <= index < len(self.remove_colors):
            self.remove_colors.pop(index)
            self._rebuild_color_list()

    # ── extraction ────────────────────────────────────────────────────────────

    def _extract(self):
        if self._sheet_img is None:
            path = self.sheet_path.get().strip()
            if not path or not os.path.isfile(path):
                messagebox.showerror("Error", "Please select a valid sprite sheet.")
                return
            self._load_sheet(path)

        out = self.output_dir.get().strip()
        if not out:
            messagebox.showerror("Error", "Please select an output directory.")
            return

        p = self._grid_params()
        if p is None:
            messagebox.showerror("Error", "Sprite width and height must be > 0.")
            return

        try:
            ox, oy, sw, sh, gx, gy, cols, rows = p
            sheet = self._sheet_img.copy()
            for color in self.remove_colors:
                sheet = remove_color(sheet, color, self.tolerance.get())

            iw, ih = sheet.size
            os.makedirs(out, exist_ok=True)

            # if nothing selected, export everything
            export_set = self._selection if self._selection else None

            saved = skipped = 0
            prefix = self.prefix.get() or "sprite"

            for r in range(rows):
                for c in range(cols):
                    if export_set is not None and (r, c) not in export_set:
                        continue
                    x = ox + c * (sw + gx)
                    y = oy + r * (sh + gy)
                    if x + sw > iw or y + sh > ih:
                        continue
                    sprite = sheet.crop((x, y, x + sw, y + sh))
                    if self.skip_empty.get() and all(px[3] <= 4 for px in sprite.getdata()):
                        skipped += 1
                        continue
                    sprite.save(os.path.join(out, f"{prefix}_{r:02d}_{c:02d}_{r * cols + c:04d}.png"))
                    saved += 1

            scope = f"{len(export_set)} selected" if export_set else "all"
            msg = f"✓ Saved {saved} sprites ({scope})"
            if skipped:
                msg += f"\n({skipped} empty skipped)"
            self._status.config(text=msg, fg=self.GR)

        except Exception as e:
            messagebox.showerror("Extraction Error", str(e))


if __name__ == "__main__":
    app = App()
    app.mainloop()
