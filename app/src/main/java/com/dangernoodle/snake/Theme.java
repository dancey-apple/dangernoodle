package com.dangernoodle.snake;

/**
 * Visual theme: colours, pixel-art sprites and which backdrop to paint.
 *
 * <p>Sprites are rows of characters: '.' is transparent and '1'..'9' index into {@link #palette}.
 * Head and body sprites are drawn facing right and rotated to the direction of travel.
 */
final class Theme {
    static final int DECOR_NONE = 0, DECOR_SYNTHWAVE = 1, DECOR_VAPORWAVE = 2,
            DECOR_CANDY = 3, DECOR_SPOOKY = 4, DECOR_SCIFI = 5, DECOR_NEON = 6, DECOR_WESTERN = 7,
            DECOR_DINOSAURS = 8, DECOR_CASTLES = 9, DECOR_PRINCESSES = 10, DECOR_UNICORNS = 11;

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

    static final Theme[] ALL = {
        classic(), synthwave(), vaporwave(), neon(), candy(), spooky(), scifi(),
        western(), dinosaurs(), castles(), princesses(), unicorns(),
    };

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

    private static Theme neon() {
        Theme t = new Theme("NEON");
        t.bgTop = 0xFF0A0606;
        t.bgBottom = 0xFF1C0E0E;
        t.ink = 0xFF39FF14;
        t.shadow = 0xFF0B5C05;
        t.accent = 0xFFFF2079;
        t.accentText = 0xFF0A0606;
        t.panel = 0xFF120A0A;
        t.frame = 0xFF00F0FF;
        t.fieldTint = 0xB8080505;
        t.body = new int[] {0xFFFF2079, 0xFFFFE700, 0xFF00F0FF, 0xFF39FF14, 0xFFB026FF};
        t.head = 0xFFFFFFFF;
        t.eye = 0xFF0A0606;
        t.glow = 0x40FF2079;
        t.decor = DECOR_NEON;
        //            1 pink      2 yellow    3 cyan      4 green     5 purple    6 white
        t.palette = new int[] {0xFFFF2079, 0xFFFFE700, 0xFF00F0FF, 0xFF39FF14, 0xFFB026FF, 0xFFFFFFFF};
        // Neon star.
        t.food = new String[] {
            "...2...",
            "..222..",
            "2222222",
            ".22622.",
            "..222..",
            ".22.22.",
            "2.....2",
        };
        // Neon shades.
        t.bonus = new String[] {
            "11111111111",
            "13331.13331",
            "13331.13331",
            ".111...111.",
        };
        return t;
    }

    private static Theme western() {
        Theme t = new Theme("WESTERN");
        t.bgTop = 0xFFF28C38;
        t.bgBottom = 0xFFF7D08A;
        t.ink = 0xFF4A2511;
        t.accent = 0xFFA0401E;
        t.accentText = 0xFFFFF1D6;
        t.panel = 0xFFF9E4BC;
        t.frame = 0xFF6B3E1F;
        t.fieldTint = 0x99F9E4BC;
        t.body = new int[] {0xFF8B5A2B};
        t.head = 0xFF6B3E1F;
        t.eye = 0xFFF2C14E;
        t.decor = DECOR_WESTERN;
        //            1 gold      2 dk gold   3 brown     4 tan       5 silver
        t.palette = new int[] {0xFFF2C14E, 0xFFB8860B, 0xFF4A2511, 0xFFB07840, 0xFFC0C0C0};
        // Rattlesnake diamonds.
        t.bodySprite = new String[] {
            "4444444",
            "4443444",
            "4433344",
            "4333334",
            "4433344",
            "4443444",
            "4444444",
        };
        // Gold coin.
        t.food = new String[] {
            "..111..",
            ".11111.",
            "1121211",
            "1112111",
            "1121211",
            ".11111.",
            "..111..",
        };
        // Sheriff's star.
        t.bonus = new String[] {
            "....1....",
            "...111...",
            "111111111",
            ".1112111.",
            "..11111..",
            ".1111111.",
            "111...111",
        };
        return t;
    }

    private static Theme dinosaurs() {
        Theme t = new Theme("DINOSAURS");
        t.bgTop = 0xFF9BD3E0;
        t.bgBottom = 0xFFE8F3C8;
        t.ink = 0xFF2D4A1E;
        t.accent = 0xFFE07A1F;
        t.accentText = 0xFFFFFFFF;
        t.panel = 0xFFF4F9E4;
        t.frame = 0xFF4F772D;
        t.fieldTint = 0x99F4F9E4;
        t.body = new int[] {0xFF5DAA3C};
        t.head = 0xFF5DAA3C;
        t.eye = 0xFF1B1B1B;
        t.decor = DECOR_DINOSAURS;
        //            1 green     2 dk green  3 white     4 egg       5 speckle   6 meat      7 bone      8 orange    9 black
        t.palette = new int[] {0xFF5DAA3C, 0xFF2F6B1F, 0xFFFFFFFF, 0xFFD9C78F, 0xFF6B8A40,
            0xFFA0522D, 0xFFF5F0E1, 0xFFE07A1F, 0xFF1B1B1B};
        // T. rex head, jaws open.
        t.rotateHead = false;
        t.headSprite = new String[] {
            ".1111..",
            "111911.",
            "1111111",
            "11.3.3.",
            "11.....",
            "113.3..",
            "11111..",
        };
        // Stegosaurus plates along the back.
        t.bodySprite = new String[] {
            ".8...8.",
            "888.888",
            "1111111",
            "1111111",
            "2121212",
            "1111111",
            "1111111",
        };
        // Speckled egg.
        t.food = new String[] {
            "..44..",
            ".4454.",
            "444444",
            "454444",
            "444454",
            "444444",
            ".4444.",
        };
        // Drumstick.
        t.bonus = new String[] {
            "..66666...",
            ".6666666..",
            ".66666666.",
            "..6666667.",
            "........77",
            ".......777",
        };
        return t;
    }

    private static Theme castles() {
        Theme t = new Theme("CASTLES");
        t.bgTop = 0xFF87BDE8;
        t.bgBottom = 0xFFDDEFFB;
        t.ink = 0xFF2C3E50;
        t.accent = 0xFF8E2C2C;
        t.accentText = 0xFFF5E6C8;
        t.panel = 0xFFF3ECDD;
        t.frame = 0xFF5D6D7E;
        t.fieldTint = 0x99F3ECDD;
        t.body = new int[] {0xFF3B8B4A};
        t.head = 0xFF3B8B4A;
        t.eye = 0xFF000000;
        t.decor = DECOR_CASTLES;
        //            1 gold      2 dk gold   3 green     4 dk green  5 wood      6 red       7 white     8 black     9 fire
        t.palette = new int[] {0xFFE0A800, 0xFFB8860B, 0xFF3B8B4A, 0xFF245C32, 0xFF7B4A26,
            0xFFC0392B, 0xFFFFFFFF, 0xFF000000, 0xFFFF8C1A};
        // A dragon guards the castle.
        t.rotateHead = false;
        t.headSprite = new String[] {
            "4....4.",
            "333333.",
            "3383333",
            "3333333",
            "33...99",
            "3333..9",
            ".333...",
        };
        t.bodySprite = new String[] {
            "4.4.4.4",
            "3333333",
            "3433343",
            "3333333",
            "3343334",
            "3333333",
            "1111111",
        };
        // Golden key.
        t.food = new String[] {
            ".11....",
            "1..1111",
            "1..1.1.",
            ".11....",
        };
        // Treasure chest.
        t.bonus = new String[] {
            ".5555555.",
            "555555555",
            "222212222",
            "555222555",
            "555525555",
            "555555555",
            "222222222",
        };
        return t;
    }

    private static Theme princesses() {
        Theme t = new Theme("PRINCESSES");
        t.bgTop = 0xFFFDE2FF;
        t.bgBottom = 0xFFFFD1E8;
        t.ink = 0xFF6A2C91;
        t.shadow = 0xFFFFFFFF;
        t.accent = 0xFFC2378F;
        t.accentText = 0xFFFFFFFF;
        t.panel = 0xFFFFF4FB;
        t.frame = 0xFFD4A017;
        t.fieldTint = 0x99FFF4FB;
        t.body = new int[] {0xFFF15BB5, 0xFF9B5DE5};
        t.head = 0xFFF15BB5;
        t.eye = 0xFF3A0CA3;
        t.decor = DECOR_PRINCESSES;
        //            1 purple    2 pink      3 gold      4 lt pink   5 eye       6 red       7 white
        t.palette = new int[] {0xFF9B5DE5, 0xFFF15BB5, 0xFFFFD23F, 0xFFFFB3DE, 0xFF3A0CA3,
            0xFFE63946, 0xFFFFFFFF};
        // A crowned head.
        t.rotateHead = false;
        t.headSprite = new String[] {
            ".3.3.3.",
            ".33333.",
            "2222222",
            "2252252",
            "2222222",
            "2226222",
            ".22222.",
        };
        // Pink jewel.
        t.food = new String[] {
            ".22222.",
            "2272222",
            "2222222",
            ".22222.",
            "..222..",
            "...2...",
        };
        // Crown.
        t.bonus = new String[] {
            "3...3...3",
            "33.333.33",
            "333333333",
            "323323323",
            "333333333",
            "333333333",
        };
        return t;
    }

    private static Theme unicorns() {
        Theme t = new Theme("UNICORNS");
        t.bgTop = 0xFFCDEBFF;
        t.bgBottom = 0xFFFFE3F6;
        t.ink = 0xFF6B4FA0;
        t.shadow = 0xFFFFFFFF;
        t.accent = 0xFFFF77C8;
        t.accentText = 0xFFFFFFFF;
        t.panel = 0xFFFFFFFF;
        t.frame = 0xFF9AD0FF;
        t.fieldTint = 0x80FFFFFF;
        t.body = new int[] {0xFFFF6B6B, 0xFFFFB86B, 0xFFFFE66B, 0xFF7BE495, 0xFF6BC4FF, 0xFFB48CFF};
        t.head = 0xFFFFFFFF;
        t.eye = 0xFF3A2E5C;
        t.decor = DECOR_UNICORNS;
        //            1 white     2 gold      3 pink      4 blue      5 eye       6 purple    7 yellow    8 cherry
        t.palette = new int[] {0xFFFFFFFF, 0xFFFFD23F, 0xFFFF77C8, 0xFF6BC4FF, 0xFF3A2E5C,
            0xFFB48CFF, 0xFFFFE66B, 0xFFFF4D6D};
        // Unicorn head with a golden horn and rainbow mane.
        t.rotateHead = false;
        t.headSprite = new String[] {
            ".....2.",
            "33..22.",
            "341111.",
            "3411511",
            "3411111",
            "34.1111",
            "3...11.",
        };
        // Cupcake.
        t.food = new String[] {
            "...8...",
            "..333..",
            ".33333.",
            "3333333",
            "6666666",
            ".66666.",
            ".66666.",
        };
        // Rainbow.
        t.bonus = new String[] {
            "...333333...",
            ".3377777733.",
            "377444444773",
            "374466664473",
            "3746....6473",
            "3746....6473",
        };
        return t;
    }
}
