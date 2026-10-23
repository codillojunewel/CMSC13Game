"""Synthesises every sound effect and the two music loops for the game (retro chiptune style)."""
import numpy as np, wave, sys, os

SR = 22050
OUT = sys.argv[1]
os.makedirs(OUT, exist_ok=True)
rng = np.random.default_rng(13)


def t(d):
    return np.arange(int(SR * d)) / SR


def env(n, a=0.005, r=0.1, total=None):
    """Attack/exponential-release envelope of n samples."""
    e = np.ones(n)
    na = max(1, int(a * SR))
    e[:na] = np.linspace(0, 1, na)
    tt = np.arange(n) / SR
    e *= np.exp(-tt / max(r, 1e-4))
    return e


def sine(f, d):
    return np.sin(2 * np.pi * f * t(d))


def square(f, d, duty=0.5):
    ph = (f * t(d)) % 1.0
    return np.where(ph < duty, 1.0, -1.0)


def saw(f, d):
    ph = (f * t(d)) % 1.0
    return 2 * ph - 1


def tri(f, d):
    ph = (f * t(d)) % 1.0
    return 2 * np.abs(2 * ph - 1) - 1


def glide(f0, f1, d, kind='sine'):
    n = int(SR * d)
    f = np.linspace(f0, f1, n)
    ph = np.cumsum(f) / SR
    if kind == 'sine':
        return np.sin(2 * np.pi * ph)
    if kind == 'square':
        return np.where(ph % 1 < 0.5, 1.0, -1.0)
    return 2 * (ph % 1) - 1


def noise(d):
    return rng.uniform(-1, 1, int(SR * d))


def lowpass(x, alpha):
    y = np.zeros_like(x)
    acc = 0.0
    for i, v in enumerate(x):
        acc += alpha * (v - acc)
        y[i] = acc
    return y


def mix_at(buf, x, start):
    s = int(start * SR)
    e = min(len(buf), s + len(x))
    buf[s:e] += x[:e - s]


def save(name, x, peak=0.8):
    x = np.asarray(x, dtype=np.float64)
    m = np.max(np.abs(x)) or 1
    x = x / m * peak
    # tiny fades to avoid clicks
    f = min(len(x) // 4, int(0.004 * SR))
    if f > 0:
        x[:f] *= np.linspace(0, 1, f)
        x[-f:] *= np.linspace(1, 0, f)
    data = (x * 32767).astype('<i2').tobytes()
    with wave.open(os.path.join(OUT, name), 'wb') as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(data)


def note(n):
    """MIDI note number -> frequency."""
    return 440.0 * 2 ** ((n - 69) / 12)


# ---------------------------------------------------------------- SFX
d = 0.09
x = glide(700, 1400, d) * env(int(SR * d), 0.01, 0.03) * 0.5 + lowpass(noise(d), 0.15) * env(int(SR * d), 0.02, 0.03) * 0.4
save('hover.wav', x, 0.35)

d = 0.05
x = square(1800, d, 0.3) * env(int(SR * d), 0.001, 0.008) + noise(d) * env(int(SR * d), 0.001, 0.004) * 0.6
save('press.wav', x, 0.45)

d = 0.03
save('tick.wav', (square(2400, d) * 0.6 + noise(d) * 0.4) * env(int(SR * d), 0.001, 0.006), 0.5)

d = 0.025
save('type.wav', lowpass(noise(d), 0.5) * env(int(SR * d), 0.001, 0.005), 0.3)

# correct ping: bright two-note chime
buf = np.zeros(int(SR * 0.75))
for i, n in enumerate([88, 95]):
    d = 0.6
    tone = (sine(note(n), d) + 0.4 * sine(note(n) * 2, d) + 0.2 * sine(note(n) * 3.01, d)) * env(int(SR * d), 0.002, 0.18)
    mix_at(buf, tone, i * 0.09)
save('correct_ping.wav', buf, 0.7)

# wrong buzz: comical compile-error buzzer
d = 0.65
x = (saw(98, d) + saw(103.5, d) + 0.5 * square(49, d)) * env(int(SR * d), 0.005, 0.5)
x *= (1 + 0.35 * np.sin(2 * np.pi * 14 * t(d)))
save('wrong_buzz.wav', lowpass(x, 0.35), 0.75)

# lifeline: magical rising sweep with sparkles
d = 0.55
x = glide(400, 1600, d, 'square') * env(int(SR * d), 0.01, 0.25) * 0.4
for k in range(6):
    s = sine(note(84 + k * 2), 0.12) * env(int(SR * 0.12), 0.002, 0.04)
    pad = np.zeros(len(x))
    mix_at(pad, s, 0.05 + k * 0.07)
    x += pad * 0.5
save('lifeline.wav', lowpass(x, 0.5), 0.6)

# reboot: BIOS beep then power-up hum
buf = np.zeros(int(SR * 1.6))
mix_at(buf, square(1000, 0.22) * env(int(SR * 0.22), 0.002, 1.0) * 0.5, 0)
hum = glide(60, 240, 1.2, 'saw') * np.linspace(0, 1, int(SR * 1.2)) * 0.6
mix_at(buf, lowpass(hum, 0.08), 0.35)
save('reboot.wav', buf, 0.6)

# cinematic error: descending minor arpeggio, drone and crash
buf = np.zeros(int(SR * 3.2))
for i, n in enumerate([69, 65, 62, 57, 53]):
    d = 0.5
    tone = (square(note(n), d, 0.35) * 0.5 + saw(note(n) / 2, d) * 0.4) * env(int(SR * d), 0.005, 0.25)
    mix_at(buf, lowpass(tone, 0.3), i * 0.22)
drone = (saw(note(33), 2.4) + saw(note(33) * 1.01, 2.4)) * np.linspace(1, 0, int(SR * 2.4)) * 0.5
mix_at(buf, lowpass(drone, 0.06), 0.9)
crash = lowpass(noise(1.6), 0.6) * env(int(SR * 1.6), 0.002, 0.45) * 0.6
mix_at(buf, crash, 1.1)
save('cinematicerror.wav', buf, 0.8)

# cinematic celebration: fanfare
buf = np.zeros(int(SR * 3.8))
melody = [(60, 0.0, 0.18), (64, 0.18, 0.18), (67, 0.36, 0.18), (72, 0.54, 0.5),
          (67, 1.04, 0.16), (72, 1.2, 0.9)]
for n, st, du in melody:
    tone = (square(note(n + 12), du + 0.1, 0.25) * 0.5 + tri(note(n), du + 0.1) * 0.5) * env(int(SR * (du + 0.1)), 0.004, du * 0.8 + 0.05)
    mix_at(buf, tone, st)
for chord_t, root in [(1.2, 60), (2.1, 65), (2.6, 67), (3.0, 72)]:
    d = 0.8
    ch = sum(tri(note(root + iv), d) for iv in (0, 4, 7, 12)) * env(int(SR * d), 0.01, 0.35) * 0.35
    mix_at(buf, ch, chord_t)
for k in range(10):
    sp = sine(note(96 + (k % 5) * 2), 0.08) * env(int(SR * 0.08), 0.001, 0.03) * 0.3
    mix_at(buf, sp, 1.25 + k * 0.12)
save('cinematicceleb.wav', buf, 0.8)


# ---------------------------------------------------------------- music
def kick(d=0.25):
    return glide(140, 40, d) * env(int(SR * d), 0.001, 0.08)


def hat(d=0.05):
    return lowpass(noise(d), 0.9) * env(int(SR * d), 0.001, 0.012) - lowpass(noise(d), 0.2) * 0.0


def snare(d=0.18):
    return (noise(d) * 0.8 + sine(190, d) * 0.4) * env(int(SR * d), 0.001, 0.05)


def song(bpm, chords, lead_pattern, bass_style, length_bars, name, dark=False):
    beat = 60.0 / bpm
    total = length_bars * 4 * beat
    buf = np.zeros(int(SR * total) + SR)
    for bar in range(length_bars):
        root = chords[bar % len(chords)]
        bt = bar * 4 * beat
        # drums
        for b in range(4):
            mix_at(buf, kick() * 0.9, bt + b * beat) if (b % 2 == 0 or dark) else None
            if b % 2 == 1:
                mix_at(buf, snare() * (0.25 if dark else 0.45), bt + b * beat)
            for h in range(2):
                mix_at(buf, hat() * 0.18, bt + b * beat + h * beat / 2)
        # bass
        for e in range(8):
            d = beat / 2
            n = root - 24 + (12 if (bass_style == 'octave' and e % 2) else 0)
            tone = square(note(n), d * 0.9, 0.5) * env(int(SR * d * 0.9), 0.003, d * 0.6)
            mix_at(buf, lowpass(tone, 0.12) * 0.55, bt + e * d)
        # pad
        third = 3 if dark else (4 if root not in (57, 69) else 3)
        pd = 4 * beat
        pad = sum(tri(note(root + iv), pd) for iv in (0, third, 7)) * env(int(SR * pd), 0.2, 3.0) * 0.12
        mix_at(buf, pad, bt)
        # arpeggio lead
        for s in range(16):
            iv = lead_pattern[s % len(lead_pattern)]
            if iv is None:
                continue
            d = beat / 4
            n = root + 12 + iv
            tone = square(note(n), d * 0.95, 0.25) * env(int(SR * d * 0.95), 0.002, d * 0.5)
            mix_at(buf, tone * (0.16 if dark else 0.2), bt + s * d)
    buf = buf[:int(SR * total)]
    save(name, buf, 0.55)


# Main theme: upbeat game-show synth (Am - F - C - G)
song(112, [57, 53, 60, 55, 57, 53, 60, 55], [0, 7, 12, 7, 3, 7, 12, 15, 0, 7, 12, 7, 4, 7, 12, 16],
     'octave', 8, 'main_theme.wav')
# Audit theme: tense, minor, ticking (Dm - Bb - Gm - A)
song(100, [50, 46, 43, 45], [0, None, 7, None, 12, None, 7, 3, None, 3, None, 7, 0, None, None, 10],
     'pulse', 8, 'audit_theme.wav', dark=True)
print('done')
