package com.dangernoodle.snake;

import android.graphics.Canvas;
import android.graphics.Paint;

/** A 5x7 bitmap font drawn as solid squares, for the old monochrome LCD look. */
final class PixelFont {
    static final int GLYPH_W = 5, GLYPH_H = 7, ADVANCE = 6;

    private static final String[] GLYPHS = {
        "A .###. #...# #...# ##### #...# #...# #...#",
        "B ####. #...# #...# ####. #...# #...# ####.",
        "C .###. #...# #.... #.... #.... #...# .###.",
        "D ####. #...# #...# #...# #...# #...# ####.",
        "E ##### #.... #.... ####. #.... #.... #####",
        "F ##### #.... #.... ####. #.... #.... #....",
        "G .###. #...# #.... #.### #...# #...# .####",
        "H #...# #...# #...# ##### #...# #...# #...#",
        "I .###. ..#.. ..#.. ..#.. ..#.. ..#.. .###.",
        "J ..### ...#. ...#. ...#. ...#. #..#. .##..",
        "K #...# #..#. #.#.. ##... #.#.. #..#. #...#",
        "L #.... #.... #.... #.... #.... #.... #####",
        "M #...# ##.## #.#.# #.#.# #...# #...# #...#",
        "N #...# #...# ##..# #.#.# #..## #...# #...#",
        "O .###. #...# #...# #...# #...# #...# .###.",
        "P ####. #...# #...# ####. #.... #.... #....",
        "Q .###. #...# #...# #...# #.#.# #..#. .##.#",
        "R ####. #...# #...# ####. #.#.. #..#. #...#",
        "S .#### #.... #.... .###. ....# ....# ####.",
        "T ##### ..#.. ..#.. ..#.. ..#.. ..#.. ..#..",
        "U #...# #...# #...# #...# #...# #...# .###.",
        "V #...# #...# #...# #...# #...# .#.#. ..#..",
        "W #...# #...# #...# #.#.# #.#.# #.#.# .#.#.",
        "X #...# #...# .#.#. ..#.. .#.#. #...# #...#",
        "Y #...# #...# .#.#. ..#.. ..#.. ..#.. ..#..",
        "Z ##### ....# ...#. ..#.. .#... #.... #####",
        "0 .###. #...# #..## #.#.# ##..# #...# .###.",
        "1 ..#.. .##.. ..#.. ..#.. ..#.. ..#.. .###.",
        "2 .###. #...# ....# ...#. ..#.. .#... #####",
        "3 ##### ...#. ..#.. ...#. ....# #...# .###.",
        "4 ...#. ..##. .#.#. #..#. ##### ...#. ...#.",
        "5 ##### #.... ####. ....# ....# #...# .###.",
        "6 ..##. .#... #.... ####. #...# #...# .###.",
        "7 ##### ....# ...#. ..#.. .#... .#... .#...",
        "8 .###. #...# #...# .###. #...# #...# .###.",
        "9 .###. #...# #...# .#### ....# ...#. .##..",
        ": ..... .##.. .##.. ..... .##.. .##.. .....",
        "- ..... ..... ..... ##### ..... ..... .....",
        ". ..... ..... ..... ..... ..... .##.. .##..",
        "! ..#.. ..#.. ..#.. ..#.. ..#.. ..... ..#..",
        "? .###. #...# ....# ...#. ..#.. ..... ..#..",
        "< ...#. ..#.. .#... #.... .#... ..#.. ...#.",
        "> .#... ..#.. ...#. ....# ...#. ..#.. .#...",
        "/ ..... ....# ...#. ..#.. .#... #.... .....",
        "_ ..... ..... ..... ..... ..... ..... #####",
        "^ ..#.. .###. #.#.# ..#.. ..#.. ..#.. ..#..",
        "v ..#.. ..#.. ..#.. ..#.. #.#.# .###. ..#..",
    };

    private static final int[][] TABLE = new int[128][];

    static {
        for (String g : GLYPHS) {
            String[] rows = g.substring(2).split(" ");
            int[] bits = new int[GLYPH_H];
            for (int r = 0; r < GLYPH_H; r++) {
                for (int c = 0; c < GLYPH_W; c++) {
                    if (rows[r].charAt(c) == '#') bits[r] |= 1 << (GLYPH_W - 1 - c);
                }
            }
            TABLE[g.charAt(0)] = bits;
        }
    }

    private PixelFont() {}

    static int width(String s, int px) {
        return s.isEmpty() ? 0 : (s.length() * ADVANCE - 1) * px;
    }

    static int height(int px) {
        return GLYPH_H * px;
    }

    /** Draws {@code s} with its top-left corner at (x, y). Unknown characters render as blanks. */
    static void draw(Canvas canvas, String s, float x, float y, int px, Paint paint) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            int[] bits = ch < 128 ? TABLE[ch] : null;
            if (bits == null && ch < 128) {
                char upper = Character.toUpperCase(ch);
                bits = upper < 128 ? TABLE[upper] : null;
            }
            if (bits != null) {
                float gx = x + i * ADVANCE * px;
                for (int r = 0; r < GLYPH_H; r++) {
                    for (int c = 0; c < GLYPH_W; c++) {
                        if ((bits[r] & (1 << (GLYPH_W - 1 - c))) != 0) {
                            float left = gx + c * px, top = y + r * px;
                            canvas.drawRect(left, top, left + px, top + px, paint);
                        }
                    }
                }
            }
        }
    }

    /** Draws {@code s} horizontally centred on {@code cx} with its top at {@code y}. */
    static void drawCentered(Canvas canvas, String s, float cx, float y, int px, Paint paint) {
        draw(canvas, s, Math.round(cx - width(s, px) / 2f), y, px, paint);
    }

    /** Largest pixel size (at most {@code max}) at which {@code s} fits in {@code maxWidth}. */
    static int fit(String s, int maxWidth, int max) {
        int px = max;
        while (px > 1 && width(s, px) > maxWidth) px--;
        return px;
    }
}
