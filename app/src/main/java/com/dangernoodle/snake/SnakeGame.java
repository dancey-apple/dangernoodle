package com.dangernoodle.snake;

import java.util.Random;

/** Pure game logic: grid, snake, food, bonus critter and scoring. No Android dependencies. */
final class SnakeGame {
    static final int UP = 0, RIGHT = 1, DOWN = 2, LEFT = 3;
    static final int[] DX = {0, 1, 0, -1};
    static final int[] DY = {-1, 0, 1, 0};

    /** Bit flags returned by {@link #step()}. */
    static final int EV_ATE = 1, EV_ATE_BONUS = 2, EV_DIED = 4, EV_BONUS_SPAWNED = 8;

    static final int BONUS_EVERY = 5;
    static final int BONUS_TICKS = 40;
    private static final int START_LENGTH = 4;

    final int cols, rows, level;
    final boolean walls;

    // Snake body is a ring buffer of cell indexes (y * cols + x); body[head] is the head.
    private final int[] body;
    private final boolean[] occupied;
    private int head;
    private int length;

    private int dir = RIGHT;
    private final int[] queue = new int[3];
    private int queued;

    int food = -1;
    /** Left cell of the two-cell bonus critter, or -1 when none is on the board. */
    int bonus = -1;
    int bonusTicks;
    int score;
    int eaten;
    boolean dead;
    boolean won;

    private final Random rng = new Random();

    SnakeGame(int cols, int rows, int level, boolean walls) {
        this.cols = cols;
        this.rows = rows;
        this.level = level;
        this.walls = walls;
        int cap = cols * rows;
        body = new int[cap];
        occupied = new boolean[cap];
        int y = rows / 2;
        for (int i = 0; i < START_LENGTH; i++) {
            int cell = y * cols + (2 + i);
            body[i] = cell;
            occupied[cell] = true;
        }
        head = START_LENGTH - 1;
        length = START_LENGTH;
        placeFood();
    }

    int length() {
        return length;
    }

    /** Cell of the i-th segment, 0 being the head. */
    int segment(int i) {
        return body[(head - i + body.length) % body.length];
    }

    int direction() {
        return dir;
    }

    /** Buffers a turn. Ignores repeats and 180-degree reversals of the last buffered direction. */
    void queueDirection(int d) {
        int last = queued > 0 ? queue[queued - 1] : dir;
        if (d == last || d == (last + 2) % 4 || queued == queue.length) return;
        queue[queued++] = d;
    }

    /** Advances one move and returns EV_* flags. */
    int step() {
        if (dead) return 0;
        int events = 0;
        if (queued > 0) {
            dir = queue[0];
            System.arraycopy(queue, 1, queue, 0, --queued);
        }

        int hc = body[head];
        int x = hc % cols + DX[dir];
        int y = hc / cols + DY[dir];
        if (x < 0 || y < 0 || x >= cols || y >= rows) {
            if (walls) return die();
            x = (x + cols) % cols;
            y = (y + rows) % rows;
        }
        int next = y * cols + x;

        boolean ateFood = next == food;
        boolean ateBonus = bonus >= 0 && (next == bonus || next == bonus + 1);
        boolean grow = ateFood;
        int tail = segment(length - 1);
        if (occupied[next] && (grow || next != tail)) return die();

        if (!grow) occupied[tail] = false;
        else length++;
        head = (head + 1) % body.length;
        body[head] = next;
        occupied[next] = true;

        if (bonus >= 0) {
            if (ateBonus) {
                score += level * (5 + bonusTicks / 4);
                bonus = -1;
                events |= EV_ATE_BONUS;
            } else if (--bonusTicks <= 0) {
                bonus = -1;
            }
        }
        if (ateFood) {
            score += level;
            eaten++;
            events |= EV_ATE;
            if (length == cols * rows) {
                won = true;
                return events | die();
            }
            placeFood();
            if (eaten % BONUS_EVERY == 0 && bonus < 0 && placeBonus()) events |= EV_BONUS_SPAWNED;
        }
        return events;
    }

    private int die() {
        dead = true;
        return EV_DIED;
    }

    private boolean isFree(int cell) {
        return !occupied[cell] && cell != food && (bonus < 0 || (cell != bonus && cell != bonus + 1));
    }

    private void placeFood() {
        int free = 0;
        for (int c = 0; c < occupied.length; c++) if (isFree(c)) free++;
        if (free == 0) {
            food = -1;
            return;
        }
        int pick = rng.nextInt(free);
        for (int c = 0; c < occupied.length; c++) {
            if (isFree(c) && pick-- == 0) {
                food = c;
                return;
            }
        }
    }

    private boolean placeBonus() {
        for (int attempt = 0; attempt < 200; attempt++) {
            int x = rng.nextInt(cols - 1);
            int y = rng.nextInt(rows);
            int c = y * cols + x;
            if (isFree(c) && isFree(c + 1)) {
                bonus = c;
                bonusTicks = BONUS_TICKS;
                return true;
            }
        }
        return false;
    }
}
