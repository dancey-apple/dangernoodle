package com.dangernoodle.snake;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
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
}
