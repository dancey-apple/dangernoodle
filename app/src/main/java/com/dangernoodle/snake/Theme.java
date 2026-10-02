package com.dangernoodle.snake;

/**
 * Visual theme: colours, pixel-art sprites and which backdrop to paint.
 *
 * <p>Sprites are rows of characters: '.' is transparent and '1'..'9' index into {@link #palette}.
 * Head and body sprites are drawn facing right and rotated to the direction of travel.
 */
final class Theme {
    static final int DECOR_NONE = 0, DECOR_SYNTHWAVE = 1, DECOR_VAPORWAVE = 2,
            DECOR_CANDY = 3, DECOR_SPOOKY = 4, DECOR_SCIFI = 5;

    final String name;
    int bgTop, bgBottom;
    /** Text and outlines. */
    int ink;
    /** Offset text shadow for the neon looks; 0 for none. */
    int shadow;
    /** Filled buttons, highlighted rows. */
    int accent, accentText;
    /** Dialog box fill. */
    int panel;
    int frame;
    /** Drawn over the playfield (usually translucent) so the backdrop doesn't distract. */
    int fieldTint;
    /** Body segment colours, cycled from the head backwards. */
    int[] body;
    int head, eye;
    /** Translucent halo drawn behind each segment; 0 for none. */
    int glow;
    int decor = DECOR_NONE;
    int[] palette = new int[0];
    String[] headSprite, bodySprite, food, bonus;
    /** Whether the head sprite turns with the snake (false = always upright, mirrored when going left). */
    boolean rotateHead = true;

    private Theme(String name) {
        this.name = name;
    }

    int paletteColor(char c) {
        return palette[c - '1'];
    }

    static final Theme[] ALL = {classic(), synthwave(), vaporwave(), candy(), spooky(), scifi()};

    private static Theme classic() {
        Theme t = new Theme("CLASSIC");
        int bg = 0xFFC7F0D8, ink = 0xFF43523D;
        t.bgTop = t.bgBottom = t.panel = bg;
        t.ink = t.accent = t.frame = t.head = ink;
        t.accentText = t.eye = bg;
        t.body = new int[] {ink};
        t.palette = new int[] {ink};
        t.food = new String[] {
            ".1.",
            "1.1",
            ".1.",
        };
        t.bonus = new String[] {
            "1......1",
            ".111111.",
            "11.11.11",
            ".111111.",
            ".1....1.",
        };
        return t;
    }

    private static Theme synthwave() {
        Theme t = new Theme("SYNTHWAVE");
        t.bgTop = 0xFF0B0221;
        t.bgBottom = 0xFF3A0B5C;
        t.ink = 0xFFFF3CAC;
        t.shadow = 0xFF00E5FF;
        t.accent = 0xFFFF3CAC;
        t.accentText = 0xFF0B0221;
        t.panel = 0xFF1A0536;
        t.frame = 0xFF00E5FF;
        t.fieldTint = 0xB00B0221;
        t.body = new int[] {0xFFFF3CAC, 0xFFFF71CE};
        t.head = 0xFF00E5FF;
        t.eye = 0xFF0B0221;
        t.glow = 0x55FF3CAC;
        t.decor = DECOR_SYNTHWAVE;
        //            1 yellow    2 cyan      3 pink      4 dark
        t.palette = new int[] {0xFFFFE66D, 0xFF00E5FF, 0xFFFF3CAC, 0xFF0B0221};
        t.food = new String[] {
            "...2...",
            "..212..",
            ".21112.",
            "2111112",
            ".21112.",
            "..212..",
            "...2...",
        };
        // Cassette tape.
        t.bonus = new String[] {
            "3333333333",
            "3111111113",
            "3144114413",
            "3144114413",
            "3111111113",
            "3332222333",
            "3333333333",
        };
        return t;
    }

    private static Theme vaporwave() {
        Theme t = new Theme("VAPORWAVE");
        t.bgTop = 0xFFFFB6E6;
        t.bgBottom = 0xFF9AE8FF;
        t.ink = 0xFF6A3FA0;
        t.shadow = 0xFFFFFFFF;
        t.accent = 0xFF01CDFE;
        t.accentText = 0xFFFFFFFF;
        t.panel = 0xFFFFE3F5;
        t.frame = 0xFFB967FF;
        t.fieldTint = 0x99FFFFFF;
        t.body = new int[] {0xFFFF71CE, 0xFF01CDFE, 0xFFB967FF};
        t.head = 0xFF6A3FA0;
        t.eye = 0xFFFFFB96;
        t.decor = DECOR_VAPORWAVE;
        //            1 purple    2 white     3 grey      4 cyan      5 dark
        t.palette = new int[] {0xFF6A3FA0, 0xFFFFFFFF, 0xFFB0B0C8, 0xFF01CDFE, 0xFF2A1A40};
        // Floppy disk.
        t.food = new String[] {
            "1133311",
            "1133311",
            "1111111",
            "1222221",
            "1222221",
            "1222221",
            "1111111",
        };
        // Dolphin.
        t.bonus = new String[] {
            "....44........",
            "...444........",
            ".44444444....4",
            "4445444444444.",
            ".442222444.44.",
            "......44.....4",
        };
        return t;
    }

    private static Theme candy() {
        Theme t = new Theme("CANDY");
        t.bgTop = 0xFFFFE0EF;
        t.bgBottom = 0xFFFFB8D9;
        t.ink = 0xFFC2185B;
        t.accent = 0xFFFF4F9A;
        t.accentText = 0xFFFFFFFF;
        t.panel = 0xFFFFF0F6;
        t.frame = 0xFFFF4F9A;
        t.fieldTint = 0x80FFFFFF;
        t.body = new int[] {0xFFE0245E, 0xFFFF8FB8};
        t.head = 0xFFB0003A;
        t.eye = 0xFFFFFFFF;
        t.decor = DECOR_CANDY;
        //            1 red       2 white     3 dark pink
        t.palette = new int[] {0xFFE0245E, 0xFFFFFFFF, 0xFFC2185B};
        t.food = new String[] {
            ".11.11.",
            "1211111",
            "1111111",
            "1111111",
            ".11111.",
            "..111..",
            "...1...",
        };
        // Love letter.
        t.bonus = new String[] {
            "3333333333",
            "3322222233",
            "3232112323",
            "3221111223",
            "3222112223",
            "3222222223",
            "3333333333",
        };
        return t;
    }

    private static Theme spooky() {
        Theme t = new Theme("SPOOKY");
        t.bgTop = 0xFF06040A;
        t.bgBottom = 0xFF1C0F24;
        t.ink = 0xFFFF7A00;
        t.shadow = 0xFF5B1A7A;
        t.accent = 0xFFFF7A00;
        t.accentText = 0xFF06040A;
        t.panel = 0xFF140B1A;
        t.frame = 0xFFFF7A00;
        t.fieldTint = 0xA006040A;
        t.body = new int[] {0xFFEDE6D6};
        t.head = 0xFFEDE6D6;
        t.eye = 0xFF06040A;
        t.decor = DECOR_SPOOKY;
        //            1 bone      2 black     3 orange    4 ghost     5 green
        t.palette = new int[] {0xFFEDE6D6, 0xFF06040A, 0xFFFF7A00, 0xFFF4F4FF, 0xFF3A7D2C};
        // Skelly snake: skull head, vertebra body.
        t.rotateHead = false;
        t.headSprite = new String[] {
            ".11111.",
            "1111111",
            "1221221",
            "1221221",
            "1111111",
            ".11211.",
            ".1.1.1.",
        };
        t.bodySprite = new String[] {
            "..1.1..",
            "..1.1..",
            ".11111.",
            "1111111",
            ".11111.",
            "..1.1..",
            "..1.1..",
        };
        // Ghost.
        t.food = new String[] {
            "..444..",
            ".44444.",
            "4424244",
            "4424244",
            "4444444",
            "4444444",
            "4.4.4.4",
        };
        // Jack-o'-lantern.
        t.bonus = new String[] {
            "....55....",
            ".33333333.",
            "3322332233",
            "3333333333",
            "3322222233",
            "3332323333",
            ".33333333.",
        };
        return t;
    }

    private static Theme scifi() {
        Theme t = new Theme("SCI-FI");
        t.bgTop = 0xFF01020A;
        t.bgBottom = 0xFF081A33;
        t.ink = 0xFF3CFFB4;
        t.shadow = 0xFF0A5C48;
        t.accent = 0xFF3CFFB4;
        t.accentText = 0xFF01020A;
        t.panel = 0xFF061226;
        t.frame = 0xFF3CFFB4;
        t.fieldTint = 0x9001020A;
        t.body = new int[] {0xFF00D1FF, 0xFF0096C7};
        t.head = 0xFFE0FFFF;
        t.eye = 0xFF01020A;
        t.glow = 0x4000D1FF;
        t.decor = DECOR_SCIFI;
        //            1 purple    2 white     3 grey      4 cyan      5 yellow
        t.palette = new int[] {0xFFB388FF, 0xFFFFFFFF, 0xFF90A4AE, 0xFF00D1FF, 0xFFFFE66D};
        // Energy crystal.
        t.food = new String[] {
            "...1...",
            "..121..",
            ".11211.",
            "1112111",
            ".11111.",
            "..111..",
            "...1...",
        };
        // UFO.
        t.bonus = new String[] {
            "....4444....",
            "...444444...",
            ".3333333333.",
            "335335335333",
            ".3333333333.",
            "...5....5...",
        };
        return t;
    }
}
