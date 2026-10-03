package com.dangernoodle.snake;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

import java.util.Random;

/**
 * Paints a theme's background scene into a low-resolution bitmap. The view scales it up without
 * filtering, so the scene comes out chunky and pixelated to match the rest of the art.
 */
final class Backdrop {
    private static final String[] PALM = {
        "..1111...1111..",
        ".11..111111..11",
        "1.....1111....1",
        "....11.11.11...",
        "...1...11...1..",
        "......11.......",
        "......11.......",
        ".....11........",
        ".....11........",
        ".....11........",
        "......11.......",
        "......11.......",
        ".....1111......",
    };

    private static final String[] BAT = {
        "1.........1",
        "11...1.1..11",
        "111.11111.111",
        ".11111111111.",
        "..111.1.111..",
        "...1.....1...",
    };

    private static final String[] TOMB = {
        "..1111..",
        ".111111.",
        "11111111",
        "11.11.11",
        "11111111",
        "11.1..11",
        "11111111",
        "11111111",
    };

    private static final String[] SAGUARO = {
        "...1...",
        "..111..",
        "..111.1",
        "1.111.1",
        "1.11111",
        "11111..",
        "..111..",
        "..111..",
        "..111..",
        "..111..",
    };

    private static final String[] FERN = {
        "....1....",
        "1...1...1",
        ".1..1..1.",
        "..1.1.1..",
        "1..111..1",
        ".11.1.11.",
        "...111...",
    };

    private static final String[] PTERO = {
        ".....11......",
        "1...1111.....",
        ".1111111111..",
        "..1111111.111",
        "...11...1....",
    };

    private static final String[] HEART = {
        ".11.11.",
        "1111111",
        "1111111",
        ".11111.",
        "..111..",
        "...1...",
    };

    private Backdrop() {}

    /** Renders the scene for a {@code w x h} view where each backdrop pixel covers {@code pixel} screen pixels. */
    static Bitmap render(Theme t, int w, int h, int pixel) {
        int bw = Math.max(1, (w + pixel - 1) / pixel);
        int bh = Math.max(1, (h + pixel - 1) / pixel);
        Bitmap bmp = Bitmap.createBitmap(bw, bh, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint();
        p.setShader(new LinearGradient(0, 0, 0, bh, t.bgTop, t.bgBottom, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, bw, bh, p);
        p.setShader(null);
        switch (t.decor) {
            case Theme.DECOR_SYNTHWAVE:
                retroSun(c, bw, bh, 0xFFFFE66D, 0xFFFF3CAC, t.bgBottom);
                floorGrid(c, bw, bh, 0xFF12002B, 0xFFFF3CAC, 0xFF00E5FF);
                break;
            case Theme.DECOR_VAPORWAVE:
                retroSun(c, bw, bh, 0xFFFFB86C, 0xFFFF71CE, 0xFFFFB6E6);
                floorGrid(c, bw, bh, 0xFFC9B3FF, 0xFFFFFFFF, 0xFFFF71CE);
                sprite(c, PALM, bw / 10, (int) (bh * 0.62f) - PALM.length * Math.max(1, bw / 45), Math.max(1, bw / 45), 0xFF2A1A40);
                sprite(c, PALM, bw - bw / 10 - PALM[0].length() * Math.max(1, bw / 60),
                        (int) (bh * 0.62f) - PALM.length * Math.max(1, bw / 60), Math.max(1, bw / 60), 0xFF2A1A40);
                break;
            case Theme.DECOR_CANDY:
                hearts(c, bw, bh);
                break;
            case Theme.DECOR_SPOOKY:
                spooky(c, bw, bh);
                break;
            case Theme.DECOR_SCIFI:
                space(c, bw, bh);
                break;
            case Theme.DECOR_NEON:
                neon(c, bw, bh);
                break;
            case Theme.DECOR_WESTERN:
                western(c, bw, bh);
                break;
            case Theme.DECOR_DINOSAURS:
                dinosaurs(c, bw, bh);
                break;
            case Theme.DECOR_CASTLES:
                clouds(c, bw, bh, 0xF0FFFFFF);
                hills(c, bw, bh, 0.80f, 0.05f, 0xFF7FB069, 0.0f);
                castle(c, bw, bh, 0.80f, 0xFF8E9AAF, 0xFF6B7690, 0xFFC0392B, false);
                hills(c, bw, bh, 0.88f, 0.04f, 0xFF5E9150, 2.0f);
                break;
            case Theme.DECOR_PRINCESSES:
                clouds(c, bw, bh, 0xD0FFFFFF);
                sparkles(c, bw, bh, 0xFFFFD23F, 23);
                hills(c, bw, bh, 0.80f, 0.05f, 0xFFE8C7F7, 1.0f);
                castle(c, bw, bh, 0.80f, 0xFFF7B2D9, 0xFF9B5DE5, 0xFFFFD23F, true);
                hills(c, bw, bh, 0.88f, 0.04f, 0xFFD9B3F0, 3.0f);
                break;
            case Theme.DECOR_UNICORNS:
                rainbow(c, bw, bh);
                clouds(c, bw, bh, 0xF8FFFFFF);
                sparkles(c, bw, bh, 0xFFFFFFFF, 5);
                hills(c, bw, bh, 0.86f, 0.05f, 0xFFBFF0C8, 0.5f);
                break;
            default:
                break;
        }
        return bmp;
    }

    private static float horizon(int bh) {
        return bh * 0.62f;
    }

    /** Striped 80s sunset sun sitting on the horizon. */
    private static void retroSun(Canvas c, int bw, int bh, int top, int bottom, int stripe) {
        float hz = horizon(bh);
        float r = Math.min(bw * 0.32f, bh * 0.2f);
        float cx = bw / 2f, cy = hz - r * 0.35f;
        Paint p = new Paint();
        p.setShader(new LinearGradient(0, cy - r, 0, cy + r, top, bottom, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);
        p.setColor(stripe);
        // Horizontal cut-outs that thicken towards the bottom.
        float y = cy + r * 0.05f;
        float gap = Math.max(1f, r * 0.06f);
        for (int i = 1; y < cy + r; i++) {
            float thick = Math.max(1f, r * 0.025f * i);
            c.drawRect(cx - r, y, cx + r, y + thick, p);
            y += thick + gap;
        }
    }

    /** Perspective grid floor below the horizon. */
    private static void floorGrid(Canvas c, int bw, int bh, int floor, int line, int horizonLine) {
        float hz = horizon(bh);
        Paint p = new Paint();
        p.setColor(floor);
        c.drawRect(0, hz, bw, bh, p);
        p.setColor(line);
        p.setStrokeWidth(1f);
        int rows = 9;
        for (int i = 1; i <= rows; i++) {
            float f = (float) i / rows;
            float y = hz + (bh - hz) * f * f;
            c.drawRect(0, y, bw, y + 1, p);
        }
        float cx = bw / 2f;
        for (int k = -12; k <= 12; k++) {
            c.drawLine(cx + k * bw * 0.02f, hz, cx + k * bw * 0.16f, bh, p);
        }
        p.setColor(horizonLine);
        c.drawRect(0, hz, bw, hz + 1, p);
    }

    private static void hearts(Canvas c, int bw, int bh) {
        int px = Math.max(1, bw / 100);
        int stepX = 18 * px, stepY = 16 * px;
        Random rng = new Random(14);
        for (int y = 0, row = 0; y < bh; y += stepY, row++) {
            for (int x = (row % 2) * stepX / 2; x < bw; x += stepX) {
                int color = rng.nextBoolean() ? 0x55FFFFFF : 0x40FF4F9A;
                sprite(c, HEART, x + rng.nextInt(4 * px), y + rng.nextInt(4 * px), px, color);
            }
        }
    }

    private static void spooky(Canvas c, int bw, int bh) {
        Random rng = new Random(31);
        Paint p = new Paint();
        for (int i = 0; i < bw * bh / 900; i++) {
            p.setColor(rng.nextBoolean() ? 0x60FFFFFF : 0x30FFFFFF);
            int x = rng.nextInt(bw), y = rng.nextInt(bh);
            c.drawRect(x, y, x + 1, y + 1, p);
        }
        // Harvest moon.
        // Tucked into the top-right corner, clear of the title.
        float r = Math.min(bw, bh) * 0.11f;
        float cx = bw - r * 0.35f, cy = r * 0.35f;
        p.setColor(0x30FFB347);
        c.drawCircle(cx, cy, r * 1.35f, p);
        p.setColor(0xFFFFD27A);
        c.drawCircle(cx, cy, r, p);
        p.setColor(0xFFF0B85A);
        c.drawCircle(cx - r * 0.35f, cy - r * 0.2f, r * 0.18f, p);
        c.drawCircle(cx + r * 0.3f, cy + r * 0.35f, r * 0.12f, p);
        c.drawCircle(cx + r * 0.1f, cy - r * 0.45f, r * 0.08f, p);
        int bp = Math.max(1, bw / 90);
        sprite(c, BAT, (int) (cx - r * 0.75f), (int) (cy + r * 0.05f), bp, 0xFF06040A);
        sprite(c, BAT, (int) (bw * 0.08f), (int) (bh * 0.05f), Math.max(1, bp - 1), 0xFF2A1636);
        sprite(c, BAT, (int) (bw * 0.18f), (int) (bh * 0.36f), bp, 0xFF2A1636);
        // Graveyard along the bottom.
        int tp = Math.max(1, bw / 70);
        p.setColor(0xFF120A18);
        c.drawRect(0, bh - 3 * tp, bw, bh, p);
        for (int x = tp * 2, i = 0; x < bw; x += TOMB[0].length() * tp * 3, i++) {
            sprite(c, TOMB, x, bh - 3 * tp - TOMB.length * tp + (i % 2) * tp, tp, 0xFF2E2238);
        }
    }

    private static void space(Canvas c, int bw, int bh) {
        Random rng = new Random(7);
        Paint p = new Paint();
        for (int i = 0; i < bw * bh / 120; i++) {
            int a = 0x30 + rng.nextInt(0xC0);
            p.setColor((a << 24) | (rng.nextInt(4) == 0 ? 0xB388FF : 0xFFFFFF));
            int x = rng.nextInt(bw), y = rng.nextInt(bh);
            int s = rng.nextInt(12) == 0 ? 2 : 1;
            c.drawRect(x, y, x + s, y + s, p);
        }
        // Ringed planet in the corner.
        float r = Math.min(bw, bh) * 0.2f;
        float cx = bw * 0.82f, cy = bh * 0.8f;
        p.setShader(new LinearGradient(cx - r, cy - r, cx + r, cy + r, 0xFF3A6FB0, 0xFF0B1E3A, Shader.TileMode.CLAMP));
        c.drawCircle(cx, cy, r, p);
        p.setShader(null);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, r * 0.06f));
        p.setColor(0xFF7FDBFF);
        c.save();
        c.rotate(-18, cx, cy);
        c.drawOval(new RectF(cx - r * 1.7f, cy - r * 0.35f, cx + r * 1.7f, cy + r * 0.35f), p);
        c.restore();
        p.setStyle(Paint.Style.FILL);
        // Small moon.
        p.setColor(0xFF9FB3C8);
        c.drawCircle(bw * 0.2f, bh * 0.18f, r * 0.22f, p);
    }

    private static void sprite(Canvas c, String[] rows, int x, int y, int px, int color) {
        Paint p = new Paint();
        p.setColor(color);
        for (int r = 0; r < rows.length; r++) {
            for (int col = 0; col < rows[r].length(); col++) {
                if (rows[r].charAt(col) != '.') {
                    c.drawRect(x + col * px, y + r * px, x + (col + 1) * px, y + (r + 1) * px, p);
                }
            }
        }
    }

    // ---- neon: brick wall with glowing signs

    private static void neon(Canvas c, int bw, int bh) {
        Paint p = new Paint();
        int brickH = Math.max(3, bh / 34), brickW = brickH * 3;
        p.setColor(0xFF2A1616);
        c.drawRect(0, 0, bw, bh, p);
        p.setColor(0xFF120909);
        for (int row = 0, y = 0; y < bh; row++, y += brickH) {
            c.drawRect(0, y, bw, y + 1, p);
            for (int x = (row % 2) * brickW / 2; x < bw; x += brickW) c.drawRect(x, y, x + 1, y + brickH, p);
        }
        float s = Math.min(bw, bh);
        // Heart.
        float hx = bw * 0.18f, hy = bh * 0.42f, hr = s * 0.08f;
        Path heart = new Path();
        heart.moveTo(hx, hy + hr * 1.1f);
        heart.cubicTo(hx - hr * 1.8f, hy - hr * 0.2f, hx - hr * 0.8f, hy - hr * 1.4f, hx, hy - hr * 0.4f);
        heart.cubicTo(hx + hr * 0.8f, hy - hr * 1.4f, hx + hr * 1.8f, hy - hr * 0.2f, hx, hy + hr * 1.1f);
        glowStroke(c, heart, 0xFFFF2079, s);
        // Lightning bolt.
        float bx = bw * 0.84f, by = bh * 0.4f, bs = s * 0.09f;
        Path bolt = new Path();
        bolt.moveTo(bx + bs * 0.3f, by - bs);
        bolt.lineTo(bx - bs * 0.4f, by + bs * 0.1f);
        bolt.lineTo(bx + bs * 0.1f, by + bs * 0.1f);
        bolt.lineTo(bx - bs * 0.3f, by + bs * 1.1f);
        bolt.lineTo(bx + bs * 0.5f, by - bs * 0.2f);
        bolt.lineTo(bx, by - bs * 0.2f);
        bolt.close();
        glowStroke(c, bolt, 0xFFFFE700, s);
        // Wavy tube and a ring.
        Path wave = new Path();
        float wy = bh * 0.78f;
        wave.moveTo(bw * 0.08f, wy);
        for (int i = 0; i < 4; i++) {
            float x0 = bw * (0.08f + i * 0.12f);
            wave.quadTo(x0 + bw * 0.03f, wy - s * 0.05f * (i % 2 == 0 ? 1 : -1), x0 + bw * 0.06f, wy);
            wave.quadTo(x0 + bw * 0.09f, wy + s * 0.05f * (i % 2 == 0 ? 1 : -1), x0 + bw * 0.12f, wy);
        }
        glowStroke(c, wave, 0xFF00F0FF, s);
        Path ring = new Path();
        ring.addCircle(bw * 0.8f, bh * 0.72f, s * 0.08f, Path.Direction.CW);
        glowStroke(c, ring, 0xFFB026FF, s);
    }

    private static void glowStroke(Canvas c, Path path, int color, float s) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeJoin(Paint.Join.ROUND);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setColor((color & 0x00FFFFFF) | 0x30000000);
        p.setStrokeWidth(Math.max(3f, s * 0.035f));
        c.drawPath(path, p);
        p.setColor((color & 0x00FFFFFF) | 0x70000000);
        p.setStrokeWidth(Math.max(2f, s * 0.018f));
        c.drawPath(path, p);
        p.setColor(color);
        p.setStrokeWidth(Math.max(1f, s * 0.008f));
        c.drawPath(path, p);
    }

    // ---- western: sunset over the mesas

    private static void western(Canvas c, int bw, int bh) {
        Paint p = new Paint();
        float hz = bh * 0.7f;
        float r = Math.min(bw * 0.3f, bh * 0.16f);
        p.setColor(0x60FFE29A);
        c.drawCircle(bw * 0.5f, hz, r * 1.3f, p);
        p.setColor(0xFFFFE29A);
        c.drawCircle(bw * 0.5f, hz, r, p);
        // Far mesas, then near ones.
        mesa(c, bw * -0.05f, bw * 0.3f, hz, bh * 0.1f, 0xFFC9673A);
        mesa(c, bw * 0.62f, bw * 1.05f, hz, bh * 0.07f, 0xFFC9673A);
        mesa(c, bw * 0.12f, bw * 0.38f, hz, bh * 0.15f, 0xFFA4461F);
        mesa(c, bw * 0.7f, bw * 0.9f, hz, bh * 0.12f, 0xFFA4461F);
        p.setColor(0xFFE3B062);
        c.drawRect(0, hz, bw, bh, p);
        Random rng = new Random(5);
        p.setColor(0xFFC99A4E);
        for (int i = 0; i < bw * 3; i++) {
            int x = rng.nextInt(bw), y = (int) hz + rng.nextInt(Math.max(1, (int) (bh - hz)));
            c.drawRect(x, y, x + 1, y + 1, p);
        }
        int cp = Math.max(1, bw / 70);
        sprite(c, SAGUARO, (int) (bw * 0.08f), (int) (bh * 0.86f) - SAGUARO.length * cp, cp, 0xFF3E6B35);
        sprite(c, SAGUARO, (int) (bw * 0.84f), (int) (bh * 0.93f) - SAGUARO.length * cp * 3 / 2,
                cp * 3 / 2 > 0 ? cp * 3 / 2 : 1, 0xFF34592C);
        sprite(c, SAGUARO, (int) (bw * 0.45f), (int) hz + 2 - SAGUARO.length * Math.max(1, cp / 2),
                Math.max(1, cp / 2), 0xFF5C7A3A);
    }

    private static void mesa(Canvas c, float x0, float x1, float base, float height, int color) {
        Paint p = new Paint();
        p.setColor(color);
        float inset = (x1 - x0) * 0.15f;
        Path m = new Path();
        m.moveTo(x0, base);
        m.lineTo(x0 + inset, base - height);
        m.lineTo(x1 - inset, base - height);
        m.lineTo(x1, base);
        m.close();
        c.drawPath(m, p);
    }

    // ---- dinosaurs: volcano, jungle hills, a pterodactyl

    private static void dinosaurs(Canvas c, int bw, int bh) {
        Paint p = new Paint();
        float base = bh * 0.74f;
        // Smoke.
        p.setColor(0x80B0B0B0);
        float vx = bw * 0.74f, top = bh * 0.52f;
        c.drawCircle(vx, top - bh * 0.03f, bw * 0.05f, p);
        c.drawCircle(vx + bw * 0.04f, top - bh * 0.08f, bw * 0.07f, p);
        c.drawCircle(vx - bw * 0.02f, top - bh * 0.14f, bw * 0.06f, p);
        // Volcano with a lava cap.
        Path v = new Path();
        v.moveTo(bw * 0.45f, base);
        v.lineTo(vx - bw * 0.06f, top);
        v.lineTo(vx + bw * 0.06f, top);
        v.lineTo(bw * 1.0f, base);
        v.close();
        p.setColor(0xFF6B4F3A);
        c.drawPath(v, p);
        Path lava = new Path();
        lava.moveTo(vx - bw * 0.06f, top);
        lava.lineTo(vx + bw * 0.06f, top);
        lava.lineTo(vx + bw * 0.08f, top + bh * 0.03f);
        lava.lineTo(vx + bw * 0.03f, top + bh * 0.02f);
        lava.lineTo(vx + bw * 0.01f, top + bh * 0.08f);
        lava.lineTo(vx - bw * 0.02f, top + bh * 0.025f);
        lava.lineTo(vx - bw * 0.08f, top + bh * 0.03f);
        lava.close();
        p.setColor(0xFFE8521E);
        c.drawPath(lava, p);
        hills(c, bw, bh, 0.76f, 0.05f, 0xFF7DB45A, 0.7f);
        hills(c, bw, bh, 0.86f, 0.04f, 0xFF4F8A3A, 2.4f);
        int fp = Math.max(1, bw / 90);
        for (int i = 0; i < 5; i++) {
            sprite(c, FERN, (int) (bw * (0.02f + i * 0.21f)), bh - FERN.length * fp - fp, fp, 0xFF2F6B1F);
        }
        int pp = Math.max(1, bw / 90);
        sprite(c, PTERO, (int) (bw * 0.15f), (int) (bh * 0.3f), pp, 0xFF4A3B5C);
        sprite(c, PTERO, (int) (bw * 0.32f), (int) (bh * 0.36f), Math.max(1, pp - 1), 0xFF6A5B7C);
    }

    // ---- shared scenery

    /** Rolling hills along {@code baseFrac} of the height. */
    private static void hills(Canvas c, int bw, int bh, float baseFrac, float ampFrac, int color, float phase) {
        Paint p = new Paint();
        p.setColor(color);
        Path h = new Path();
        float base = bh * baseFrac, amp = bh * ampFrac;
        h.moveTo(0, bh);
        for (int x = 0; x <= bw; x++) {
            double t = (double) x / bw;
            float y = (float) (base - amp * (0.5 + 0.3 * Math.sin(t * 7 + phase) + 0.2 * Math.sin(t * 17 + phase * 2)));
            h.lineTo(x, y);
        }
        h.lineTo(bw, bh);
        h.close();
        c.drawPath(h, p);
    }

    private static void clouds(Canvas c, int bw, int bh, int color) {
        Paint p = new Paint();
        p.setColor(color);
        float[][] at = {{0.18f, 0.12f, 1f}, {0.75f, 0.08f, 0.8f}, {0.55f, 0.24f, 0.6f}, {0.1f, 0.38f, 0.7f}, {0.88f, 0.42f, 0.9f}};
        for (float[] a : at) {
            float cx = bw * a[0], cy = bh * a[1], r = bw * 0.05f * a[2];
            c.drawCircle(cx, cy, r, p);
            c.drawCircle(cx - r * 1.1f, cy + r * 0.3f, r * 0.75f, p);
            c.drawCircle(cx + r * 1.1f, cy + r * 0.3f, r * 0.8f, p);
            c.drawRect(cx - r * 1.8f, cy + r * 0.3f, cx + r * 1.8f, cy + r * 0.9f, p);
        }
    }

    private static void sparkles(Canvas c, int bw, int bh, int color, long seed) {
        Paint p = new Paint();
        p.setColor(color);
        Random rng = new Random(seed);
        for (int i = 0; i < 26; i++) {
            int x = rng.nextInt(bw), y = rng.nextInt((int) (bh * 0.7f));
            int s = 1 + rng.nextInt(2);
            c.drawRect(x - s, y, x + s + 1, y + 1, p);
            c.drawRect(x, y - s, x + 1, y + s + 1, p);
        }
    }

    /** A castle standing on the hills: crenellated towers, or fairy-tale cone roofs when {@code cones}. */
    private static void castle(Canvas c, int bw, int bh, float baseFrac, int wall, int roof, int flag, boolean cones) {
        Paint p = new Paint();
        float base = bh * baseFrac;
        float s = Math.min(bw, bh * 0.6f);
        float cx = bw * 0.5f;
        float[][] parts = {
            // x offset, width, height (fractions of s)
            {-0.30f, 0.22f, 0.20f},
            {-0.20f, 0.12f, 0.34f},
            {-0.07f, 0.14f, 0.46f},
            {0.08f, 0.12f, 0.34f},
            {0.30f, 0.22f, 0.20f},
        };
        // Curtain wall.
        p.setColor(wall);
        c.drawRect(cx - s * 0.36f, base - s * 0.16f, cx + s * 0.36f, base + 2, p);
        for (float[] t : parts) {
            float l = cx + s * t[0] - s * t[1] / 2, r = l + s * t[1], top = base - s * t[2];
            p.setColor(wall);
            c.drawRect(l, top, r, base + 2, p);
            if (cones) {
                Path cone = new Path();
                cone.moveTo(l - s * 0.015f, top);
                cone.lineTo((l + r) / 2, top - s * t[1] * 1.3f);
                cone.lineTo(r + s * 0.015f, top);
                cone.close();
                p.setColor(roof);
                c.drawPath(cone, p);
                drawFlag(c, (l + r) / 2, top - s * t[1] * 1.3f, s, flag);
            } else {
                float m = Math.max(1f, s * 0.02f);
                for (float x = l; x < r - m * 0.5f; x += 2 * m) c.drawRect(x, top - m, Math.min(r, x + m), top, p);
                if (t[2] > 0.3f) drawFlag(c, (l + r) / 2, top - m, s, flag);
            }
            // Window.
            p.setColor(roof);
            float wwid = Math.max(1f, s * t[1] * 0.25f);
            c.drawRect((l + r) / 2 - wwid / 2, top + s * 0.06f, (l + r) / 2 + wwid / 2, top + s * 0.06f + wwid * 1.6f, p);
        }
        // Gate.
        p.setColor(roof);
        float gw = s * 0.08f;
        c.drawRect(cx - gw / 2, base - s * 0.12f, cx + gw / 2, base + 2, p);
        c.drawCircle(cx, base - s * 0.12f, gw / 2, p);
    }

    private static void drawFlag(Canvas c, float x, float topY, float s, int color) {
        Paint p = new Paint();
        p.setColor(0xFF4A4A4A);
        float poleH = s * 0.08f;
        c.drawRect(x, topY - poleH, x + Math.max(1f, s * 0.006f), topY, p);
        p.setColor(color);
        Path f = new Path();
        f.moveTo(x, topY - poleH);
        f.lineTo(x + s * 0.06f, topY - poleH + s * 0.02f);
        f.lineTo(x, topY - poleH + s * 0.04f);
        f.close();
        c.drawPath(f, p);
    }

    private static void rainbow(Canvas c, int bw, int bh) {
        int[] bands = {0xFFFF6B6B, 0xFFFFB86B, 0xFFFFE66B, 0xFF7BE495, 0xFF6BC4FF, 0xFFB48CFF};
        float cx = bw * 0.5f, cy = bh * 0.68f;
        float outer = Math.min(bw * 0.46f, bh * 0.28f);
        float band = outer * 0.07f;
        Paint p = new Paint();
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(band + 1);
        for (int i = 0; i < bands.length; i++) {
            p.setColor((bands[i] & 0x00FFFFFF) | 0xC0000000);
            float r = outer - i * band;
            c.drawArc(new RectF(cx - r, cy - r, cx + r, cy + r), 180, 180, false, p);
        }
    }
}
