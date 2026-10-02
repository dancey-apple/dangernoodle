package com.dangernoodle.snake;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Top-ten table persisted in SharedPreferences as "NAME,score,level;..." */
final class HighScores {
    static final int MAX = 10;
    private static final String KEY = "high_scores";

    static final class Entry {
        final String name;
        final int score;
        final int level;

        Entry(String name, int score, int level) {
            this.name = name;
            this.score = score;
            this.level = level;
        }
    }

    private final SharedPreferences prefs;
    private final List<Entry> entries = new ArrayList<Entry>();

    HighScores(SharedPreferences prefs) {
        this.prefs = prefs;
        String raw = prefs.getString(KEY, "");
        for (String row : raw.split(";")) {
            String[] parts = row.split(",");
            if (parts.length != 3) continue;
            try {
                entries.add(new Entry(parts[0], Integer.parseInt(parts[1]), Integer.parseInt(parts[2])));
            } catch (NumberFormatException ignored) {
                // Skip a corrupt row rather than losing the whole table.
            }
        }
        while (entries.size() > MAX) entries.remove(entries.size() - 1);
    }

    List<Entry> entries() {
        return Collections.unmodifiableList(entries);
    }

    boolean qualifies(int score) {
        return score > 0 && (entries.size() < MAX || score > entries.get(MAX - 1).score);
    }

    /** Inserts the score and returns its 0-based rank, or -1 if it did not make the table. */
    int add(String name, int score, int level) {
        if (!qualifies(score)) return -1;
        int rank = 0;
        while (rank < entries.size() && entries.get(rank).score >= score) rank++;
        entries.add(rank, new Entry(sanitize(name), score, level));
        while (entries.size() > MAX) entries.remove(entries.size() - 1);
        save();
        return rank;
    }

    void clear() {
        entries.clear();
        save();
    }

    static String sanitize(String name) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < name.length() && sb.length() < 3; i++) {
            char c = Character.toUpperCase(name.charAt(i));
            if (c >= 'A' && c <= 'Z') sb.append(c);
        }
        while (sb.length() < 3) sb.append('A');
        return sb.toString();
    }

    private void save() {
        StringBuilder sb = new StringBuilder();
        for (Entry e : entries) {
            if (sb.length() > 0) sb.append(';');
            sb.append(e.name).append(',').append(e.score).append(',').append(e.level);
        }
        prefs.edit().putString(KEY, sb.toString()).apply();
    }
}
