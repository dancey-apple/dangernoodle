package com.dangernoodle.snake;

import android.content.Context;
import android.content.SharedPreferences;
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
    static final int BG = 0xFFC7F0D8;
    static final int INK = 0xFF43523D;

    private static final int S_MENU = 0, S_PLAYING = 1, S_PAUSED = 2, S_DYING = 3,
            S_OVER = 4, S_NAME = 5, S_SCORES = 6;
    private static final int M_PLAY = 0, M_LEVEL = 1, M_WALLS = 2, M_SOUND = 3, M_SCORES = 4;
    private static final int COLS = 20;
    private static final int MIN_LEVEL = 1, MAX_LEVEL = 9;
    private static final long OVER_TAP_DELAY_MS = 600;

    private static final String[] BUG = {
        "#......#",
        ".######.",
        "##.##.##",
        ".######.",
        ".#....#.",
    };

    private final Paint ink = new Paint();
    private final Paint paper = new Paint();
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
    private final Rect[] menuRects = new Rect[5];
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
        String saved = HighScores.sanitize(prefs.getString("initials", "AAA"));
        for (int i = 0; i < 3; i++) initials[i] = saved.charAt(i);
        for (int i = 0; i < menuRects.length; i++) menuRects[i] = new Rect();
        for (int i = 0; i < letterRects.length; i++) letterRects[i] = new Rect();
        ink.setColor(INK);
        ink.setStyle(Paint.Style.FILL);
        paper.setColor(BG);
        paper.setStyle(Paint.Style.FILL);
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
        switch (item) {
            case M_PLAY:
                startGame();
                return;
            case M_LEVEL:
                changeLevel(x < menuRects[M_LEVEL].centerX() ? -1 : 1);
                return;
            case M_WALLS:
                walls = !walls;
                prefs.edit().putBoolean("walls", walls).apply();
                break;
            case M_SOUND:
                soundOn = !soundOn;
                prefs.edit().putBoolean("sound", soundOn).apply();
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
        u = Math.max(2, w / 120);
        frame = Math.max(2, u / 2);
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
        c.drawColor(BG);
        switch (state) {
            case S_MENU:
                drawMenu(c);
                break;
            case S_NAME:
                drawNameEntry(c);
                break;
            case S_SCORES:
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

    private void drawMenu(Canvas c) {
        int w = getWidth(), h = getHeight();
        int y = h / 10;

        int titlePx = PixelFont.fit("SNAKE", w * 7 / 10, 8 * u);
        PixelFont.drawCentered(c, "SNAKE", w / 2f, y, titlePx, ink);
        y += PixelFont.height(titlePx) + 3 * u;

        PixelFont.drawCentered(c, "DANGER NOODLE", w / 2f, y, u, ink);
        y += PixelFont.height(u) + 5 * u;

        drawMenuSnake(c, w / 2, y, 4 * u);
        y += 2 * (4 * u) + 6 * u;

        List<HighScores.Entry> top = scores.entries();
        String best = "BEST " + (top.isEmpty() ? "-" : String.valueOf(top.get(0).score));
        PixelFont.drawCentered(c, best, w / 2f, y, u, ink);
        y += PixelFont.height(u) + 6 * u;

        String[] labels = {
            "PLAY",
            "< LEVEL " + level + " >",
            "WALLS " + (walls ? "ON" : "OFF"),
            "SOUND " + (soundOn ? "ON" : "OFF"),
            "HIGH SCORES",
        };
        int px = PixelFont.fit("< LEVEL 9 >", w * 6 / 10, 2 * u);
        int boxW = w * 8 / 10;
        int boxH = PixelFont.height(px) + 4 * px;
        int gap = 2 * px;
        int footerH = 2 * PixelFont.height(u) + 10 * u;
        int needed = labels.length * boxH + (labels.length - 1) * gap;
        y = Math.max(y, Math.min(y + (h - footerH - y - needed) / 2, h - footerH - needed));
        for (int i = 0; i < labels.length; i++) {
            Rect r = menuRects[i];
            r.set((w - boxW) / 2, y, (w + boxW) / 2, y + boxH);
            drawButton(c, r, labels[i], px, i == M_PLAY);
            y += boxH + gap;
        }

        int fy = h - footerH + 2 * u;
        PixelFont.drawCentered(c, "SWIPE TO STEER", w / 2f, fy, u, ink);
        PixelFont.drawCentered(c, "TAP TO PAUSE", w / 2f, fy + PixelFont.height(u) + 2 * u, u, ink);
    }

    /** A little wavy snake chasing a food pellet, centred on cx. */
    private void drawMenuSnake(Canvas c, int cx, int top, int s) {
        int[][] segs = {{0, 1}, {1, 1}, {2, 1}, {2, 0}, {3, 0}, {4, 0}, {5, 0}, {5, 1}, {6, 1}, {7, 1}, {8, 1}};
        int width = 11 * s;
        int left = cx - width / 2;
        int g = Math.max(1, s / 8);
        for (int[] p : segs) {
            int x = left + p[0] * s, y = top + p[1] * s;
            c.drawRect(x + g, y + g, x + s - g, y + s - g, ink);
        }
        drawFood(c, left + 10 * s, top + s, s);
    }

    private void drawGame(Canvas c) {
        if (game == null) return;
        if (game.rows != fieldRows || cell == 0) layoutField(game.cols, game.rows);
        int fieldW = game.cols * cell, fieldH = game.rows * cell;

        // HUD: score on the left, bonus critter countdown on the right.
        PixelFont.draw(c, String.format(Locale.US, "%04d", game.score), fieldX - 2 * frame, hudTop, u, ink);
        if (game.bonus >= 0) {
            String t = String.format(Locale.US, "%02d", game.bonusTicks);
            int tw = PixelFont.width(t, u);
            int right = fieldX + fieldW + 2 * frame;
            PixelFont.draw(c, t, right - tw, hudTop, u, ink);
            int sub = Math.max(1, PixelFont.height(u) / 5);
            drawSprite(c, BUG, right - tw - 2 * u - 8 * sub, hudTop + (PixelFont.height(u) - 5 * sub) / 2, sub);
        }

        drawFrame(c, fieldX - 2 * frame, fieldY - 2 * frame,
                fieldX + fieldW + 2 * frame, fieldY + fieldH + 2 * frame);

        if (game.food >= 0) {
            drawFood(c, fieldX + (game.food % game.cols) * cell, fieldY + (game.food / game.cols) * cell, cell);
        }
        if (game.bonus >= 0) {
            int bx = fieldX + (game.bonus % game.cols) * cell, by = fieldY + (game.bonus / game.cols) * cell;
            int sub = Math.max(1, Math.min(2 * cell / 9, cell * 4 / 25));
            drawSprite(c, BUG, bx + (2 * cell - 8 * sub) / 2, by + (cell - 5 * sub) / 2, sub);
        }
        if (!blinkHidden) drawSnake(c);
    }

    private void drawFrame(Canvas c, int l, int t, int r, int b) {
        if (walls) {
            c.drawRect(l, t, r, t + frame, ink);
            c.drawRect(l, b - frame, r, b, ink);
            c.drawRect(l, t, l + frame, b, ink);
            c.drawRect(r - frame, t, r, b, ink);
            return;
        }
        // Dashed border means the edges wrap around.
        int dash = Math.max(frame, cell / 2);
        for (int x = l; x < r; x += 2 * dash) {
            int e = Math.min(r, x + dash);
            c.drawRect(x, t, e, t + frame, ink);
            c.drawRect(x, b - frame, e, b, ink);
        }
        for (int y = t; y < b; y += 2 * dash) {
            int e = Math.min(b, y + dash);
            c.drawRect(l, y, l + frame, e, ink);
            c.drawRect(r - frame, y, r, e, ink);
        }
    }

    private void drawSnake(Canvas c) {
        int g = Math.max(1, cell / 8);
        int n = game.length();
        for (int i = 0; i < n; i++) {
            int s = game.segment(i);
            int x = fieldX + (s % game.cols) * cell, y = fieldY + (s / game.cols) * cell;
            c.drawRect(x + g, y + g, x + cell - g, y + cell - g, ink);
        }
        // Eye on the head, offset forward and to the left of travel.
        int h = game.segment(0);
        int d = game.direction();
        float fx = SnakeGame.DX[d], fy = SnakeGame.DY[d];
        float cx = fieldX + (h % game.cols + 0.5f) * cell + fx * 0.15f * cell + fy * 0.2f * cell;
        float cy = fieldY + (h / game.cols + 0.5f) * cell + fy * 0.15f * cell - fx * 0.2f * cell;
        float e = Math.max(1, cell / 6);
        c.drawRect(cx - e / 2, cy - e / 2, cx + e / 2, cy + e / 2, paper);
    }

    /** Classic diamond-shaped food pellet occupying one cell. */
    private void drawFood(Canvas c, int x, int y, int size) {
        int sub = Math.max(1, (size - 2 * Math.max(1, size / 8)) / 3);
        int ox = x + (size - 3 * sub) / 2, oy = y + (size - 3 * sub) / 2;
        c.drawRect(ox + sub, oy, ox + 2 * sub, oy + sub, ink);
        c.drawRect(ox, oy + sub, ox + sub, oy + 2 * sub, ink);
        c.drawRect(ox + 2 * sub, oy + sub, ox + 3 * sub, oy + 2 * sub, ink);
        c.drawRect(ox + sub, oy + 2 * sub, ox + 2 * sub, oy + 3 * sub, ink);
    }

    private void drawSprite(Canvas c, String[] sprite, int x, int y, int px) {
        for (int r = 0; r < sprite.length; r++) {
            for (int col = 0; col < sprite[r].length(); col++) {
                if (sprite[r].charAt(col) == '#') {
                    c.drawRect(x + col * px, y + r * px, x + (col + 1) * px, y + (r + 1) * px, ink);
                }
            }
        }
    }

    private void drawDialog(Canvas c, String title, String line1, String line2) {
        int w = getWidth(), h = getHeight();
        int boxW = w * 8 / 10;
        int titlePx = PixelFont.fit(title, boxW - 8 * u, 2 * u);
        int px = PixelFont.fit(line2.length() > line1.length() ? line2 : line1, boxW - 8 * u, u);
        int boxH = PixelFont.height(titlePx) + 2 * PixelFont.height(px) + 18 * u;
        tmp.set((w - boxW) / 2, (h - boxH) / 2, (w + boxW) / 2, (h + boxH) / 2);
        c.drawRect(tmp, paper);
        outline(c, tmp, frame);
        int y = tmp.top + 4 * u;
        PixelFont.drawCentered(c, title, w / 2f, y, titlePx, ink);
        y += PixelFont.height(titlePx) + 5 * u;
        PixelFont.drawCentered(c, line1, w / 2f, y, px, ink);
        y += PixelFont.height(px) + 3 * u;
        PixelFont.drawCentered(c, line2, w / 2f, y, px, ink);
    }

    private void drawNameEntry(Canvas c) {
        int w = getWidth(), h = getHeight();
        int y = h / 8;
        int titlePx = PixelFont.fit("HIGH SCORE!", w * 8 / 10, 3 * u);
        PixelFont.drawCentered(c, "NEW", w / 2f, y, titlePx, ink);
        y += PixelFont.height(titlePx) + 2 * u;
        PixelFont.drawCentered(c, "HIGH SCORE!", w / 2f, y, titlePx, ink);
        y += PixelFont.height(titlePx) + 6 * u;
        PixelFont.drawCentered(c, String.valueOf(game.score), w / 2f, y, 2 * u, ink);
        y += PixelFont.height(2 * u) + 10 * u;

        int lp = 5 * u;
        int slot = 8 * lp;
        int left = (w - 3 * slot) / 2;
        int arrowPx = Math.max(1, lp / 2);
        int arrowH = PixelFont.height(arrowPx);
        for (int i = 0; i < 3; i++) {
            int sx = left + i * slot;
            Rect r = letterRects[i];
            r.set(sx, y, sx + slot, y + arrowH + PixelFont.height(lp) + arrowH + 6 * u);
            int ly = y + arrowH + 3 * u;
            String letter = String.valueOf(initials[i]);
            PixelFont.drawCentered(c, letter, r.centerX(), ly, lp, ink);
            if (i == cursor) {
                PixelFont.drawCentered(c, "^", r.centerX(), y, arrowPx, ink);
                PixelFont.drawCentered(c, "v", r.centerX(), r.bottom - arrowH, arrowPx, ink);
                int bw = PixelFont.width(letter, lp);
                c.drawRect(r.centerX() - bw / 2f, ly + PixelFont.height(lp) + u,
                        r.centerX() + bw / 2f, ly + PixelFont.height(lp) + u + frame, ink);
            }
        }
        y = letterRects[0].bottom + 8 * u;

        int px = PixelFont.fit("SWIPE LEFT/RIGHT: MOVE", w * 9 / 10, u);
        PixelFont.drawCentered(c, "SWIPE UP/DOWN: LETTER", w / 2f, y, px, ink);
        y += PixelFont.height(px) + 2 * u;
        PixelFont.drawCentered(c, "SWIPE LEFT/RIGHT: MOVE", w / 2f, y, px, ink);
        y += PixelFont.height(px) + 8 * u;

        int bpx = 2 * u;
        int bw = w / 2, bh = PixelFont.height(bpx) + 4 * bpx;
        saveRect.set((w - bw) / 2, y, (w + bw) / 2, y + bh);
        drawButton(c, saveRect, "SAVE", bpx, true);
    }

    private void drawScores(Canvas c) {
        int w = getWidth(), h = getHeight();
        int y = h / 12;
        int titlePx = PixelFont.fit("HIGH SCORES", w * 8 / 10, 3 * u);
        PixelFont.drawCentered(c, "HIGH SCORES", w / 2f, y, titlePx, ink);
        y += PixelFont.height(titlePx) + 8 * u;

        List<HighScores.Entry> list = scores.entries();
        int px = PixelFont.fit("10 AAA 00000 L9", w * 85 / 100, 2 * u);
        int rowH = PixelFont.height(px) + 3 * px;
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
                c.drawRect(x - 2 * px, y - px - px / 2, x + tw + 3 * px, y + PixelFont.height(px) + px + px / 2, ink);
                PixelFont.draw(c, row, x, y, px, paper);
            } else {
                PixelFont.draw(c, row, x, y, px, ink);
            }
            y += rowH;
        }

        int bpx = PixelFont.fit("SURE?", w * 3 / 10, 2 * u);
        int bh = PixelFont.height(bpx) + 4 * bpx;
        int bw = w * 38 / 100;
        int by = Math.max(y + 4 * u, h - bh - 8 * u);
        backRect.set(w / 2 - u - bw, by, w / 2 - u, by + bh);
        clearRect.set(w / 2 + u, by, w / 2 + u + bw, by + bh);
        drawButton(c, backRect, "BACK", bpx, true);
        drawButton(c, clearRect, confirmClear ? "SURE?" : "CLEAR", bpx, confirmClear);
    }

    private void drawButton(Canvas c, Rect r, String label, int px, boolean filled) {
        if (filled) c.drawRect(r, ink);
        else outline(c, r, frame);
        int ty = r.top + (r.height() - PixelFont.height(px)) / 2;
        PixelFont.drawCentered(c, label, r.centerX(), ty, px, filled ? paper : ink);
    }

    private void outline(Canvas c, Rect r, int t) {
        c.drawRect(r.left, r.top, r.right, r.top + t, ink);
        c.drawRect(r.left, r.bottom - t, r.right, r.bottom, ink);
        c.drawRect(r.left, r.top, r.left + t, r.bottom, ink);
        c.drawRect(r.right - t, r.top, r.right, r.bottom, ink);
    }
}
