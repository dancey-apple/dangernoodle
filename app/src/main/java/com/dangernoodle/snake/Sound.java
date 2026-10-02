package com.dangernoodle.snake;

import android.media.AudioManager;
import android.media.ToneGenerator;

/** Phone-style beeps via ToneGenerator. Silently does nothing if the device refuses a generator. */
final class Sound {
    private ToneGenerator tones;

    Sound() {
        try {
            tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
        } catch (RuntimeException e) {
            tones = null;
        }
    }

    void eat() {
        play(ToneGenerator.TONE_PROP_BEEP, 40);
    }

    void bonus() {
        play(ToneGenerator.TONE_PROP_ACK, 120);
    }

    void bonusAppears() {
        play(ToneGenerator.TONE_PROP_BEEP2, 80);
    }

    void die() {
        play(ToneGenerator.TONE_PROP_NACK, 300);
    }

    void click() {
        play(ToneGenerator.TONE_PROP_BEEP, 20);
    }

    private void play(int tone, int ms) {
        if (tones != null) tones.startTone(tone, ms);
    }

    void release() {
        if (tones != null) {
            tones.release();
            tones = null;
        }
    }
}
