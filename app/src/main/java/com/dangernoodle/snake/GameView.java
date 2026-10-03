package com.dangernoodle.snake;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

import java.util.List;
import java.util.Locale;

/** Single custom view that draws every screen (menu, game, name entry, scores) and handles input. */
public final class GameView extends View {
    private static final int S_MENU = 0, S_PLAYING = 1, S_PAUSED = 2, S_DYING = 3,
            S_OVER = 4, S_NAME = 5, S_SCORES = 6;
    private static final int M_PLAY = 0, M_LEVEL = 1, M_WALLS = 2, M_SOUND = 3, M_THEME = 4, M_SCORES = 5;
    private static final int MENU_ITEMS = 6;
    private static final int COLS = 20;
    private static final int MIN_LEVEL = 1, MAX_LEVEL = 9;
    private static final long OVER_TAP_DELAY_MS = 600;

    private final Paint fill = new Paint();
    private final Paint pixelPaint = new Paint();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences prefs;
    private final HighScores scores;
    private final Sound sound = new Sound();
    private final float swipeThreshold;

    private int state = S_MENU;
    private SnakeGame game;
    private int level;
    private boolean walls;
    private boolean soundOn;
    private int themeIndex;
    private Theme theme;
    private Bitmap backdrop;

    private boolean blinkHidden;
    private int blinks;
    private long overAt;
    private final char[] initials = new char[3];
    private int cursor;
    private int highlightRank = -1;
    private boolean confirmClear;

    // Touch tracking.
    private float downX, downY;
    private boolean swiped;

    // Geometry, recomputed on size change.
    private int u = 4;
    private int frame = 2;
    private int cell, fieldX, fieldY, fieldRows, hudTop;

    // Hit areas, filled in while drawing.
    private final Rect[] menuRects = new Rect[MENU_ITEMS];
    private final Rect[] letterRects = new Rect[3];
    private final Rect saveRect = new Rect();
    private final Rect backRect = new Rect();
    private final Rect clearRect = new Rect();
    private final Rect tmp = new Rect();

    public GameView(Context context) {
        super(context);
        prefs = context.getSharedPreferences("snake", Context.MODE_PRIVATE);
        scores = new HighScores(prefs);
        level = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, prefs.getInt("level", 5)));
        walls = prefs.getBoolean("walls", true);
        soundOn = prefs.getBoolean("sound", true);
        themeIndex = Math.max(0, Math.min(Theme.ALL.length - 1, prefs.getInt("theme", 0)));
        theme = Theme.ALL[themeIndex];
        String saved = HighScores.sanitize(prefs.getString("initials", "AAA"));
        for (int i = 0; i < 3; i++) initials[i] = saved.charAt(i);
        for (int i = 0; i < menuRects.length; i++) menuRects[i] = new Rect();
        for (int i = 0; i < letterRects.length; i++) letterRects[i] = new Rect();
        fill.setStyle(Paint.Style.FILL);
        pixelPaint.setFilterBitmap(false);
        swipeThreshold = 22 * context.getResources().getDisplayMetrics().density;
        setHapticFeedbackEnabled(true);
    }

    // ---------------------------------------------------------------- lifecycle / navigation

    /** Called when the activity pauses or loses focus. */
    void onHostPause() {
        pause();
    }

    void release() {
        handler.removeCallbacksAndMessages(null);
        sound.release();
    }

    /** Background colour for the window behind this view. */
    int backgroundColor() {
        return theme.bgBottom;
    }

    /** Returns true if the back press was consumed. */
    boolean onBack() {
        switch (state) {
            case S_MENU:
                return false;
            case S_PLAYING:
                pause();
                return true;
            case S_PAUSED:
                toMenu();
                return true;
            case S_OVER:
                leaveGameOver();
                return true;
            case S_NAME:
                saveName();
                return true;
            case S_SCORES:
                toMenu();
                return true;
            default:
                return true;
        }
    }

    private void toMenu() {
        handler.removeCallbacksAndMessages(null);
        state = S_MENU;
        game = null;
        confirmClear = false;
        setKeepScreenOn(false);
        invalidate();
    }

    private void startGame() {
        handler.removeCallbacksAndMessages(null);
        layoutField(COLS, 0);
        game = new SnakeGame(COLS, fieldRows, level, walls);
        blinkHidden = false;
        state = S_PLAYING;
        setKeepScreenOn(true);
        handler.postDelayed(tick, interval());
        invalidate();
    }

    private void pause() {
        if (state != S_PLAYING) return;
        handler.removeCallbacks(tick);
        state = S_PAUSED;
        setKeepScreenOn(false);
        invalidate();
    }

    private void resume() {
        if (state != S_PAUSED) return;
        state = S_PLAYING;
        setKeepScreenOn(true);
        handler.postDelayed(tick, interval());
        invalidate();
    }

    private void leaveGameOver() {
        if (game != null && scores.qualifies(game.score)) {
            cursor = 0;
            state = S_NAME;
        } else {
            toMenu();
        }
        invalidate();
    }

    private void saveName() {
        String name = new String(initials);
        prefs.edit().putString("initials", name).apply();
        highlightRank = scores.add(name, game.score, game.level);
        game = null;
        confirmClear = false;
        state = S_SCORES;
        invalidate();
    }

    private void setTheme(int index) {
        themeIndex = (index + Theme.ALL.length) % Theme.ALL.length;
        theme = Theme.ALL[themeIndex];
        prefs.edit().putInt("theme", themeIndex).apply();
        backdrop = null;
        getRootView().setBackgroundColor(theme.bgBottom);
        invalidate();
    }

    private long interval() {
        return 300 - (level - 1) * 30L;
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (state != S_PLAYING || game == null) return;
            int ev = game.step();
            if ((ev & SnakeGame.EV_DIED) != 0) {
                feedback(HapticFeedbackConstants.LONG_PRESS);
                if (soundOn) sound.die();
                state = S_DYING;
                blinks = 0;
                setKeepScreenOn(false);
                handler.postDelayed(blink, 180);
            } else {
                if ((ev & SnakeGame.EV_ATE_BONUS) != 0) {
                    feedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    if (soundOn) sound.bonus();
                } else if ((ev & SnakeGame.EV_ATE) != 0) {
                    feedback(HapticFeedbackConstants.KEYBOARD_TAP);
                    if (soundOn) sound.eat();
                }
                if ((ev & SnakeGame.EV_BONUS_SPAWNED) != 0 && soundOn) sound.bonusAppears();
                handler.postDelayed(this, interval());
            }
            invalidate();
        }
    };

    private final Runnable blink = new Runnable() {
        @Override
        public void run() {
            blinkHidden = !blinkHidden;
            if (++blinks < 7) {
                handler.postDelayed(this, 180);
            } else {
                blinkHidden = false;
                state = S_OVER;
                overAt = SystemClock.uptimeMillis();
            }
            invalidate();
        }
    };

    private void feedback(int constant) {
        performHapticFeedback(constant);
    }

    // ---------------------------------------------------------------- input

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downX = e.getX();
                downY = e.getY();
                swiped = false;
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dx = e.getX() - downX;
                float dy = e.getY() - downY;
                if (Math.abs(dx) > swipeThreshold || Math.abs(dy) > swipeThreshold) {
                    int dir = Math.abs(dx) > Math.abs(dy)
                            ? (dx > 0 ? SnakeGame.RIGHT : SnakeGame.LEFT)
                            : (dy > 0 ? SnakeGame.DOWN : SnakeGame.UP);
                    // Re-arm from the current point so one continuous drag can steer several turns.
                    downX = e.getX();
                    downY = e.getY();
                    swiped = true;
                    onSwipe(dir);
                }
                return true;
            }
            case MotionEvent.ACTION_UP:
                if (!swiped) onTap((int) e.getX(), (int) e.getY());
                return true;
            default:
                return true;
        }
    }

    private void onSwipe(int dir) {
        switch (state) {
            case S_PLAYING:
                game.queueDirection(dir);
                break;
            case S_PAUSED:
                resume();
                game.queueDirection(dir);
                break;
            case S_MENU:
                if (dir == SnakeGame.LEFT) changeLevel(-1);
                else if (dir == SnakeGame.RIGHT) changeLevel(1);
                break;
            case S_NAME:
                if (dir == SnakeGame.UP) changeLetter(1);
                else if (dir == SnakeGame.DOWN) changeLetter(-1);
                else if (dir == SnakeGame.LEFT) cursor = Math.max(0, cursor - 1);
                else cursor = Math.min(2, cursor + 1);
                invalidate();
                break;
            default:
                break;
        }
    }

    private void onTap(int x, int y) {
        switch (state) {
            case S_MENU:
                for (int i = 0; i < menuRects.length; i++) {
                    if (menuRects[i].contains(x, y)) {
                        onMenuItem(i, x);
                        break;
                    }
                }
                break;
            case S_PLAYING:
                pause();
                break;
            case S_PAUSED:
                resume();
                break;
            case S_OVER:
                if (SystemClock.uptimeMillis() - overAt >= OVER_TAP_DELAY_MS) leaveGameOver();
                break;
            case S_NAME:
                if (saveRect.contains(x, y)) {
                    click();
                    saveName();
                    return;
                }
                for (int i = 0; i < 3; i++) {
                    Rect r = letterRects[i];
                    if (r.contains(x, y)) {
                        if (cursor == i) changeLetter(y < r.centerY() ? 1 : -1);
                        else cursor = i;
                        invalidate();
                        break;
                    }
                }
                break;
            case S_SCORES:
                if (backRect.contains(x, y)) {
                    click();
                    toMenu();
                } else if (clearRect.contains(x, y)) {
                    click();
                    if (confirmClear) {
                        scores.clear();
                        highlightRank = -1;
                        confirmClear = false;
                    } else {
                        confirmClear = true;
                    }
                } else {
                    confirmClear = false;
                }
                invalidate();
                break;
            default:
                break;
        }
    }

    private void onMenuItem(int item, int x) {
        click();
        boolean left = x < menuRects[item].centerX();
        switch (item) {
            case M_PLAY:
                startGame();
                return;
            case M_LEVEL:
                changeLevel(left ? -1 : 1);
                return;
            case M_WALLS:
                walls = !walls;
                prefs.edit().putBoolean("walls", walls).apply();
                break;
            case M_SOUND:
                soundOn = !soundOn;
                prefs.edit().putBoolean("sound", soundOn).apply();
                break;
            case M_THEME:
                setTheme(themeIndex + (left ? -1 : 1));
                break;
            case M_SCORES:
                highlightRank = -1;
                confirmClear = false;
                state = S_SCORES;
                break;
            default:
                break;
        }
        invalidate();
    }

    private void changeLevel(int delta) {
        int next = Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level + delta));
        if (next == level) return;
        level = next;
        prefs.edit().putInt("level", level).apply();
        click();
        invalidate();
    }

    private void changeLetter(int delta) {
        initials[cursor] = (char) ('A' + ((initials[cursor] - 'A' + delta + 26) % 26));
        click();
    }

    private void click() {
        feedback(HapticFeedbackConstants.VIRTUAL_KEY);
        if (soundOn) sound.click();
    }

    // ---------------------------------------------------------------- layout

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        // One UI "pixel". Bounded by both dimensions so menus fit on wide and square screens too.
        u = Math.max(2, Math.min(w / 120, h / 220));
        frame = Math.max(2, u / 2);
        backdrop = null;
        if (game != null) layoutField(game.cols, game.rows);
        else layoutField(COLS, 0);
    }

    /** Sizes the playfield. With rows == 0, picks as many rows as fit (used for a new game). */
    private void layoutField(int cols, int rows) {
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;
        int margin = 2 * u;
        int hud = PixelFont.height(u) + 2 * u;
        int availW = w - 2 * margin - 4 * frame;
        int availH = h - 2 * margin - hud - 4 * frame;
        cell = Math.max(4, availW / cols);
        if (rows == 0) {
            rows = Math.max(8, availH / cell);
        } else {
            cell = Math.max(2, Math.min(cell, availH / rows));
        }
        fieldRows = rows;
        int fieldW = cols * cell, fieldH = rows * cell;
        int total = hud + 4 * frame + fieldH;
        hudTop = Math.max(margin, (h - total) / 2);
        fieldX = (w - fieldW) / 2;
        fieldY = hudTop + hud + 2 * frame;
    }

    // ---------------------------------------------------------------- drawing

    @Override
    protected void onDraw(Canvas c) {
        drawBackdrop(c);
        switch (state) {
            case S_MENU:
                drawMenu(c);
                break;
            case S_NAME:
                dimBackdrop(c);
                drawNameEntry(c);
                break;
            case S_SCORES:
                dimBackdrop(c);
                drawScores(c);
                break;
            default:
                drawGame(c);
                if (state == S_PAUSED) {
                    drawDialog(c, "PAUSED", "TAP TO RESUME", "BACK FOR MENU");
                } else if (state == S_OVER) {
                    String score = "SCORE " + game.score;
                    drawDialog(c, game.won ? "YOU WIN!" : "GAME OVER", score, "TAP TO CONTINUE");
                }
                break;
        }
    }

    private void drawBackdrop(Canvas c) {
        int w = getWidth(), h = getHeight();
        if (theme.decor == Theme.DECOR_NONE && theme.bgTop == theme.bgBottom) {
            c.drawColor(theme.bgTop);
            return;
        }
        if (backdrop == null) backdrop = Backdrop.render(theme, w, h, Math.max(1, u / 2));
        int pixel = Math.max(1, u / 2);
        tmp.set(0, 0, backdrop.getWidth() * pixel, backdrop.getHeight() * pixel);
        c.drawBitmap(backdrop, null, tmp, pixelPaint);
    }

    /** Calms the backdrop behind text-heavy screens. */
    private void dimBackdrop(Canvas c) {
        if (theme.fieldTint == 0) return;
        fill.setColor(theme.fieldTint);
        c.drawRect(0, 0, getWidth(), getHeight(), fill);
    }

    // ---- text helpers (theme ink plus optional offset shadow)

    private void text(Canvas c, String s, float x, float y, int px) {
        if (theme.shadow != 0) {
            fill.setColor(theme.shadow);
            int o = Math.max(1, px / 2);
            PixelFont.draw(c, s, x + o, y + o, px, fill);
        }
        fill.setColor(theme.ink);
        PixelFont.draw(c, s, x, y, px, fill);
    }

    private void textCentered(Canvas c, String s, float cx, float y, int px) {
        text(c, s, Math.round(cx - PixelFont.width(s, px) / 2f), y, px);
    }

    private void plainCentered(Canvas c, String s, float cx, float y, int px, int color) {
        fill.setColor(color);
        PixelFont.drawCentered(c, s, cx, y, px, fill);
    }

    // ---- menu

    private void drawMenu(Canvas c) {
        int w = getWidth(), h = getHeight();
        String[] labels = {
            "PLAY",
            "< LEVEL " + level + " >",
            "WALLS " + (walls ? "ON" : "OFF"),
            "SOUND " + (soundOn ? "ON" : "OFF"),
            theme.name,
            "HIGH SCORES",
        };

        // Header: title, subtitle, a little snake, best score.
        int titlePx = PixelFont.fit("SNAKE", w * 7 / 10, 6 * u);
        int snakeCell = 3 * u;
        int headerH = PixelFont.height(titlePx) + 3 * u + PixelFont.height(u) + 4 * u
                + 2 * snakeCell + 4 * u + PixelFont.height(u);
        int footerH = 2 * PixelFont.height(u) + 2 * u;
        int margin = 4 * u;

        // Buttons get whatever height is left, up to 2u per font pixel.
        int boxW = Math.min(w * 8 / 10, 110 * u);
        String longestName = "";
        for (Theme t : Theme.ALL) if (t.name.length() > longestName.length()) longestName = t.name;
        int px = PixelFont.fit("< " + longestName + " >", boxW - 4 * u, 2 * u);
        int availForItems = h - 2 * margin - headerH - footerH - 8 * u;
        // Items are 10 px tall (the two-line theme item 16) with 1.5 px gaps.
        int units2 = (MENU_ITEMS - 1) * 20 + 32 + (MENU_ITEMS - 1) * 3;
        px = Math.max(1, Math.min(px, availForItems * 2 / units2));
        int boxH = 10 * px;
        int themeH = 16 * px;
        int gap = 3 * px / 2;
        int itemsH = (MENU_ITEMS - 1) * boxH + themeH + (MENU_ITEMS - 1) * gap;

        int free = Math.max(0, h - 2 * margin - headerH - itemsH - footerH);
        int y = margin + free / 4;
        textCentered(c, "SNAKE", w / 2f, y, titlePx);
        y += PixelFont.height(titlePx) + 3 * u;
        textCentered(c, "DANGER NOODLE", w / 2f, y, u);
        y += PixelFont.height(u) + 4 * u;
        drawMenuSnake(c, w / 2, y, snakeCell);
        y += 2 * snakeCell + 4 * u;
        List<HighScores.Entry> top = scores.entries();
        textCentered(c, "BEST " + (top.isEmpty() ? "-" : String.valueOf(top.get(0).score)), w / 2f, y, u);
        y += PixelFont.height(u) + free / 2 + 4 * u;

        for (int i = 0; i < MENU_ITEMS; i++) {
            Rect r = menuRects[i];
            int bh = i == M_THEME ? themeH : boxH;
            r.set((w - boxW) / 2, y, (w + boxW) / 2, y + bh);
            if (i == M_THEME) drawThemeButton(c, r, px);
            else drawButton(c, r, labels[i], px, i == M_PLAY);
            y += bh + gap;
        }

        int fy = h - margin - footerH;
        textCentered(c, "SWIPE TO STEER", w / 2f, fy, u);
        textCentered(c, "TAP TO PAUSE", w / 2f, fy + PixelFont.height(u) + 2 * u, u);
    }

    /** Two-line button: a small "THEME" caption over the theme name, with arrows at the edges. */
    private void drawThemeButton(Canvas c, Rect r, int px) {
        fill.setColor(theme.panel);
        c.drawRect(r, fill);
        outline(c, r, frame, theme.frame);
        int capPx = Math.max(1, px * 3 / 5);
        textCentered(c, "THEME", r.centerX(), r.top + 2 * px, capPx);
        int ny = r.bottom - 2 * px - PixelFont.height(px);
        textCentered(c, theme.name, r.centerX(), ny, px);
        text(c, "<", r.left + 2 * px, ny, px);
        text(c, ">", r.right - 2 * px - PixelFont.width(">", px), ny, px);
    }

    /** A little wavy snake chasing a food pellet, centred on cx, drawn in the current theme. */
    private void drawMenuSnake(Canvas c, int cx, int top, int s) {
        int[][] segs = {{0, 1}, {1, 1}, {2, 1}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {5, 1}, {6, 1}, {7, 1}, {8, 1}};
        int left = cx - 11 * s / 2;
        int n = segs.length;
        for (int i = 0; i < n; i++) {
            int[] p = segs[i];
            int x = left + p[0] * s, y = top + p[1] * s;
            int fromHead = n - 1 - i;
            if (fromHead == 0) {
                drawHead(c, x, y, s, SnakeGame.RIGHT);
            } else {
                int[] towardHead = segs[i + 1];
                drawBody(c, x, y, s, fromHead, towardHead[1] == p[1]);
            }
        }
        drawSprite(c, theme.food, left + 10 * s, top + s, s, s, 0, false);
    }

    // ---- game

    private void drawGame(Canvas c) {
        if (game == null) return;
        if (game.rows != fieldRows || cell == 0) layoutField(game.cols, game.rows);
        int fieldW = game.cols * cell, fieldH = game.rows * cell;

        if (theme.fieldTint != 0) {
            fill.setColor(theme.fieldTint);
            c.drawRect(fieldX - 2 * frame, fieldY - 2 * frame, fieldX + fieldW + 2 * frame,
                    fieldY + fieldH + 2 * frame, fill);
        }

        // HUD: score on the left, bonus countdown on the right.
        text(c, String.format(Locale.US, "%04d", game.score), fieldX - 2 * frame, hudTop, u);
        if (game.bonus >= 0) {
            String t = String.format(Locale.US, "%02d", game.bonusTicks);
            int tw = PixelFont.width(t, u);
            int right = fieldX + fieldW + 2 * frame;
            text(c, t, right - tw, hudTop, u);
            int iconH = PixelFont.height(u), iconW = 2 * iconH;
            drawSprite(c, theme.bonus, right - tw - 2 * u - iconW, hudTop, iconW, iconH, 0, false);
        }

        drawFrame(c, fieldX - 2 * frame, fieldY - 2 * frame,
                fieldX + fieldW + 2 * frame, fieldY + fieldH + 2 * frame);

        int g = Math.max(1, cell / 10);
        if (game.food >= 0) {
            int fx = fieldX + (game.food % game.cols) * cell, fy = fieldY + (game.food / game.cols) * cell;
            drawSprite(c, theme.food, fx + g, fy + g, cell - 2 * g, cell - 2 * g, 0, false);
        }
        if (game.bonus >= 0) {
            int bx = fieldX + (game.bonus % game.cols) * cell, by = fieldY + (game.bonus / game.cols) * cell;
            drawSprite(c, theme.bonus, bx + g, by + g, 2 * cell - 2 * g, cell - 2 * g, 0, false);
        }
        if (!blinkHidden) drawSnake(c);
    }

    private void drawFrame(Canvas c, int l, int t, int r, int b) {
        fill.setColor(theme.frame);
        if (walls) {
            c.drawRect(l, t, r, t + frame, fill);
            c.drawRect(l, b - frame, r, b, fill);
            c.drawRect(l, t, l + frame, b, fill);
            c.drawRect(r - frame, t, r, b, fill);
            return;
        }
        // Dashed border means the edges wrap around.
        int dash = Math.max(frame, cell / 2);
        for (int x = l; x < r; x += 2 * dash) {
            int e = Math.min(r, x + dash);
            c.drawRect(x, t, e, t + frame, fill);
            c.drawRect(x, b - frame, e, b, fill);
        }
        for (int y = t; y < b; y += 2 * dash) {
            int e = Math.min(b, y + dash);
            c.drawRect(l, y, l + frame, e, fill);
            c.drawRect(r - frame, y, r, e, fill);
        }
    }

    private void drawSnake(Canvas c) {
        int n = game.length();
        int cols = game.cols;
        // Tail first so the head is drawn on top of any glow.
        for (int i = n - 1; i >= 0; i--) {
            int s = game.segment(i);
            int x = fieldX + (s % cols) * cell, y = fieldY + (s / cols) * cell;
            if (i == 0) {
                drawHead(c, x, y, cell, game.direction());
            } else {
                boolean horizontal = game.segment(i - 1) / cols == s / cols;
                drawBody(c, x, y, cell, i, horizontal);
            }
        }
    }

    private void drawGlow(Canvas c, int x, int y, int size) {
        if (theme.glow == 0) return;
        int e = Math.max(1, size / 6);
        fill.setColor(theme.glow);
        c.drawRect(x - e, y - e, x + size + e, y + size + e, fill);
    }

    /** Body segment {@code index} (1 = just behind the head). */
    private void drawBody(Canvas c, int x, int y, int size, int index, boolean horizontal) {
        drawGlow(c, x, y, size);
        int g = Math.max(1, size / 8);
        if (theme.bodySprite != null) {
            drawSprite(c, theme.bodySprite, x + g, y + g, size - 2 * g, size - 2 * g, horizontal ? 0 : 1, false);
            return;
        }
        fill.setColor(theme.body[(index - 1) % theme.body.length]);
        c.drawRect(x + g, y + g, x + size - g, y + size - g, fill);
    }

    private void drawHead(Canvas c, int x, int y, int size, int dir) {
        drawGlow(c, x, y, size);
        int g = Math.max(1, size / 8);
        if (theme.headSprite != null) {
            int rot = 0;
            boolean mirror = false;
            if (theme.rotateHead) rot = (dir - SnakeGame.RIGHT + 4) % 4;
            else mirror = dir == SnakeGame.LEFT;
            drawSprite(c, theme.headSprite, x + g, y + g, size - 2 * g, size - 2 * g, rot, mirror);
            return;
        }
        fill.setColor(theme.head);
        c.drawRect(x + g, y + g, x + size - g, y + size - g, fill);
        // Eye, offset forward and to the left of travel.
        float fx = SnakeGame.DX[dir], fy = SnakeGame.DY[dir];
        float cx = x + 0.5f * size + fx * 0.15f * size + fy * 0.2f * size;
        float cy = y + 0.5f * size + fy * 0.15f * size - fx * 0.2f * size;
        float e = Math.max(1, size / 6);
        fill.setColor(theme.eye);
        c.drawRect(cx - e / 2, cy - e / 2, cx + e / 2, cy + e / 2, fill);
    }

    /**
     * Draws a palette sprite as large as fits in the given box, centred.
     *
     * @param quarterTurns clockwise rotation in 90 degree steps
     */
    private void drawSprite(Canvas c, String[] rows, int left, int top, int boxW, int boxH,
                            int quarterTurns, boolean mirror) {
        int sw = 0;
        for (String r : rows) sw = Math.max(sw, r.length());
        int sh = rows.length;
        boolean sideways = (quarterTurns & 1) == 1;
        int dw = sideways ? sh : sw, dh = sideways ? sw : sh;
        int px = Math.max(1, Math.min(boxW / dw, boxH / dh));
        int ox = left + (boxW - dw * px) / 2, oy = top + (boxH - dh * px) / 2;
        for (int r = 0; r < sh; r++) {
            String row = rows[r];
            for (int col = 0; col < row.length(); col++) {
                char ch = row.charAt(col);
                if (ch == '.') continue;
                int sx = mirror ? sw - 1 - col : col;
                int dx, dy;
                switch (quarterTurns & 3) {
                    case 1: dx = sh - 1 - r; dy = sx; break;
                    case 2: dx = sw - 1 - sx; dy = sh - 1 - r; break;
                    case 3: dx = r; dy = sw - 1 - sx; break;
                    default: dx = sx; dy = r; break;
                }
                fill.setColor(theme.paletteColor(ch));
                c.drawRect(ox + dx * px, oy + dy * px, ox + (dx + 1) * px, oy + (dy + 1) * px, fill);
            }
        }
    }

    private void drawDialog(Canvas c, String title, String line1, String line2) {
        int w = getWidth(), h = getHeight();
        int boxW = Math.min(w * 8 / 10, 110 * u);
        int titlePx = PixelFont.fit(title, boxW - 8 * u, 2 * u);
        int px = PixelFont.fit(line2.length() > line1.length() ? line2 : line1, boxW - 8 * u, u);
        int boxH = PixelFont.height(titlePx) + 2 * PixelFont.height(px) + 18 * u;
        tmp.set((w - boxW) / 2, (h - boxH) / 2, (w + boxW) / 2, (h + boxH) / 2);
        fill.setColor(theme.panel);
        c.drawRect(tmp, fill);
        outline(c, tmp, frame, theme.frame);
        int y = tmp.top + 4 * u;
        textCentered(c, title, w / 2f, y, titlePx);
        y += PixelFont.height(titlePx) + 5 * u;
        textCentered(c, line1, w / 2f, y, px);
        y += PixelFont.height(px) + 3 * u;
        textCentered(c, line2, w / 2f, y, px);
    }

    // ---- name entry

    private void drawNameEntry(Canvas c) {
        int w = getWidth(), h = getHeight();
        int titlePx = PixelFont.fit("HIGH SCORE!", w * 8 / 10, 3 * u);
        int scorePx = 2 * u;
        int lp = Math.min(4 * u, w / 30);
        int arrowPx = Math.max(1, lp / 2);
        int arrowH = PixelFont.height(arrowPx);
        int hintPx = PixelFont.fit("SWIPE LEFT/RIGHT: MOVE", w * 9 / 10, u);
        int bpx = 2 * u;
        int bh = PixelFont.height(bpx) + 4 * bpx;
        int lettersH = arrowH + 3 * u + PixelFont.height(lp) + 3 * u + arrowH;
        int total = 2 * PixelFont.height(titlePx) + 2 * u + 6 * u + PixelFont.height(scorePx) + 8 * u
                + lettersH + 6 * u + 2 * PixelFont.height(hintPx) + 2 * u + 6 * u + bh;
        int y = Math.max(2 * u, (h - total) / 2);

        textCentered(c, "NEW", w / 2f, y, titlePx);
        y += PixelFont.height(titlePx) + 2 * u;
        textCentered(c, "HIGH SCORE!", w / 2f, y, titlePx);
        y += PixelFont.height(titlePx) + 6 * u;
        textCentered(c, String.valueOf(game.score), w / 2f, y, scorePx);
        y += PixelFont.height(scorePx) + 8 * u;

        int slot = 8 * lp;
        int left = (w - 3 * slot) / 2;
        for (int i = 0; i < 3; i++) {
            int sx = left + i * slot;
            Rect r = letterRects[i];
            r.set(sx, y, sx + slot, y + lettersH);
            int ly = y + arrowH + 3 * u;
            String letter = String.valueOf(initials[i]);
            textCentered(c, letter, r.centerX(), ly, lp);
            if (i == cursor) {
                textCentered(c, "^", r.centerX(), y, arrowPx);
                textCentered(c, "v", r.centerX(), r.bottom - arrowH, arrowPx);
                int bw = PixelFont.width(letter, lp);
                fill.setColor(theme.ink);
                c.drawRect(r.centerX() - bw / 2f, ly + PixelFont.height(lp) + u,
                        r.centerX() + bw / 2f, ly + PixelFont.height(lp) + u + frame, fill);
            }
        }
        y += lettersH + 6 * u;

        textCentered(c, "SWIPE UP/DOWN: LETTER", w / 2f, y, hintPx);
        y += PixelFont.height(hintPx) + 2 * u;
        textCentered(c, "SWIPE LEFT/RIGHT: MOVE", w / 2f, y, hintPx);
        y += PixelFont.height(hintPx) + 6 * u;

        int bw = Math.min(w / 2, 50 * u);
        saveRect.set((w - bw) / 2, y, (w + bw) / 2, y + bh);
        drawButton(c, saveRect, "SAVE", bpx, true);
    }

    // ---- high scores

    private void drawScores(Canvas c) {
        int w = getWidth(), h = getHeight();
        int titlePx = PixelFont.fit("HIGH SCORES", w * 8 / 10, 3 * u);
        int bw = Math.min(w * 38 / 100, 45 * u);
        int bpx = PixelFont.fit("CLEAR", bw - 6 * u, 2 * u);
        int bh = PixelFont.height(bpx) + 4 * bpx;
        int margin = 4 * u;
        int titleH = PixelFont.height(titlePx) + 6 * u;
        // Ten rows of 10 font pixels each must fit between the title and the buttons.
        int rowsAvail = h - 2 * margin - titleH - bh - 6 * u;
        int px = PixelFont.fit("10 AAA 00000 L9", w * 85 / 100, 2 * u);
        px = Math.max(1, Math.min(px, rowsAvail / (HighScores.MAX * 10)));
        int rowH = PixelFont.height(px) + 3 * px;
        int total = titleH + HighScores.MAX * rowH + 6 * u + bh;
        int y = Math.max(margin, (h - total) / 3);

        textCentered(c, "HIGH SCORES", w / 2f, y, titlePx);
        y += titleH;

        List<HighScores.Entry> list = scores.entries();
        for (int i = 0; i < HighScores.MAX; i++) {
            String row;
            if (i < list.size()) {
                HighScores.Entry e = list.get(i);
                row = String.format(Locale.US, "%2d %s %05d L%d", i + 1, e.name, e.score, e.level);
            } else {
                row = String.format(Locale.US, "%2d --- ----- --", i + 1);
            }
            int tw = PixelFont.width(row, px);
            int x = (w - tw) / 2;
            if (i == highlightRank) {
                fill.setColor(theme.accent);
                c.drawRect(x - 2 * px, y - px - px / 2, x + tw + 3 * px, y + PixelFont.height(px) + px + px / 2, fill);
                fill.setColor(theme.accentText);
                PixelFont.draw(c, row, x, y, px, fill);
            } else {
                text(c, row, x, y, px);
            }
            y += rowH;
        }

        int by = Math.max(y + 4 * u, Math.min(h - bh - margin, y + 12 * u));
        backRect.set(w / 2 - u - bw, by, w / 2 - u, by + bh);
        clearRect.set(w / 2 + u, by, w / 2 + u + bw, by + bh);
        drawButton(c, backRect, "BACK", bpx, true);
        drawButton(c, clearRect, confirmClear ? "SURE?" : "CLEAR", bpx, confirmClear);
    }

    private void drawButton(Canvas c, Rect r, String label, int px, boolean filled) {
        int ty = r.top + (r.height() - PixelFont.height(px)) / 2;
        if (filled) {
            fill.setColor(theme.accent);
            c.drawRect(r, fill);
            plainCentered(c, label, r.centerX(), ty, px, theme.accentText);
        } else {
            fill.setColor(theme.panel);
            c.drawRect(r, fill);
            outline(c, r, frame, theme.frame);
            textCentered(c, label, r.centerX(), ty, px);
        }
    }

    private void outline(Canvas c, Rect r, int t, int color) {
        fill.setColor(color);
        c.drawRect(r.left, r.top, r.right, r.top + t, fill);
        c.drawRect(r.left, r.bottom - t, r.right, r.bottom, fill);
        c.drawRect(r.left, r.top, r.left + t, r.bottom, fill);
        c.drawRect(r.right - t, r.top, r.right, r.bottom, fill);
    }
}
