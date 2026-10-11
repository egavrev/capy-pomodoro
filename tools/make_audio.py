"""
Synthesises Capy Pomodoro's seamless audio loops and chime from scratch (original material, no samples).

Every loop is built "circularly": noise is generated in the frequency domain at the exact loop length,
notes and reverb tails that run past the end wrap around to the start, and all modulations have periods
that divide the loop. So the file loops with no click or gap.

usage: python3 make_audio.py OUT_DIR
"""
import os
import subprocess
import sys

import numpy as np
from scipy.signal import butter, sosfilt, sosfiltfilt

SR = 44100
rng = np.random.default_rng(20261011)


def midi(m):
    return 440.0 * 2 ** ((m - 69) / 12)


# ---------- circular helpers -------------------------------------------------------------------

def add_wrapped(buf, sig, start):
    """Adds sig into buf (channels x N) at start, wrapping past the end."""
    n = buf.shape[-1]
    start %= n
    end = start + sig.shape[-1]
    if end <= n:
        buf[..., start:end] += sig
    else:
        first = n - start
        buf[..., start:] += sig[..., :first]
        rest = sig[..., first:]
        while rest.shape[-1] > 0:
            k = min(n, rest.shape[-1])
            buf[..., :k] += rest[..., :k]
            rest = rest[..., k:]


def periodic_noise(n, lo=20, hi=20000, slope=0.0):
    """Coloured noise that loops perfectly: random phase, |X(f)| ~ f^-slope inside [lo, hi]."""
    f = np.fft.rfftfreq(n, 1 / SR)
    fs = np.maximum(f, 1.0)
    mag = fs ** -slope / (1 + (lo / fs) ** 4) / (1 + (fs / hi) ** 4)
    mag[0] = 0.0
    phase = rng.uniform(0, 2 * np.pi, len(f))
    x = np.fft.irfft(mag * np.exp(1j * phase), n)
    return x / (np.std(x) + 1e-12)


def periodic_lfo(n, rates_hz, depth=1.0):
    """Smooth random-ish modulation built from sines whose periods divide the loop."""
    t = np.arange(n) / SR
    dur = n / SR
    out = np.zeros(n)
    for r in rates_hz:
        k = max(1, round(r * dur))  # whole cycles per loop
        out += np.sin(2 * np.pi * k * t / dur + rng.uniform(0, 2 * np.pi)) / len(rates_hz)
    return out * depth


def circular_reverb(x, seconds=1.8, decay=3.0, mix=0.25, predelay=0.02, bright=6000):
    """Convolution with a noise impulse response, done circularly so tails wrap round the loop."""
    n = x.shape[-1]
    ir_n = int(seconds * SR)
    t = np.arange(ir_n) / SR
    out = np.empty_like(x)
    for ch in range(x.shape[0]):
        ir = rng.standard_normal(ir_n) * np.exp(-decay * t)
        ir = sosfilt(butter(2, bright, 'low', fs=SR, output='sos'), ir)
        pd = int(predelay * SR)
        ir = np.concatenate([np.zeros(pd), ir])[:ir_n]
        ir /= np.sqrt(np.sum(ir ** 2)) + 1e-12
        wet = np.fft.irfft(np.fft.rfft(x[ch], n) * np.fft.rfft(ir, n), n)
        out[ch] = x[ch] * (1 - mix) + wet * mix * 1.6
    return out


def lowpass(x, cutoff, order=2):
    return sosfiltfilt(butter(order, cutoff, 'low', fs=SR, output='sos'), x, axis=-1)


def highpass(x, cutoff, order=2):
    return sosfiltfilt(butter(order, cutoff, 'high', fs=SR, output='sos'), x, axis=-1)


def bandpass(x, lo, hi, order=2):
    return sosfiltfilt(butter(order, [lo, hi], 'band', fs=SR, output='sos'), x, axis=-1)


def pan(sig, p):
    """Equal-power pan, p in -1..1 -> 2 x N."""
    a = (p + 1) * np.pi / 4
    return np.vstack([sig * np.cos(a), sig * np.sin(a)])


def env_adsr(n, a=0.005, r=0.08):
    e = np.ones(n)
    na, nr = int(a * SR), int(r * SR)
    if na:
        e[:na] = np.linspace(0, 1, na)
    if nr:
        e[-nr:] *= np.linspace(1, 0, nr)
    return e


# ---------- instruments ------------------------------------------------------------------------

def piano(freq, dur, vel=0.6):
    """Soft piano: slightly inharmonic partials, faster decay for higher partials, gentle hammer."""
    n = int((dur + 1.5) * SR)
    t = np.arange(n) / SR
    B = 0.00035
    x = np.zeros(n)
    for k in range(1, 10):
        fk = freq * k * np.sqrt(1 + B * k * k)
        if fk > 12000:
            break
        amp = (1 / k ** 1.4) * (0.55 + 0.45 * vel)
        dec = 0.9 + 0.55 * k + freq / 900
        x += amp * np.sin(2 * np.pi * fk * t + rng.uniform(0, 6.28)) * np.exp(-dec * t)
    # damper after note-off
    off = int(dur * SR)
    damp = np.ones(n)
    damp[off:] = np.exp(-np.arange(n - off) / SR * 7)
    x *= damp * env_adsr(n, 0.004, 0.05)
    return x * vel


def epiano(freq, dur, vel=0.6):
    """Rhodes-ish electric piano (simple two-operator FM) for the lo-fi loop."""
    n = int((dur + 1.2) * SR)
    t = np.arange(n) / SR
    idx = 1.6 * np.exp(-t * 3.5) + 0.25
    mod = np.sin(2 * np.pi * freq * t) * idx
    x = np.sin(2 * np.pi * freq * t + mod) * np.exp(-t * 1.1)
    x += 0.25 * np.sin(2 * np.pi * freq * 2 * t) * np.exp(-t * 2.5)
    x *= 1 + 0.08 * np.sin(2 * np.pi * 4.5 * t)  # tremolo
    off = int(dur * SR)
    damp = np.ones(n)
    damp[off:] = np.exp(-np.arange(n - off) / SR * 5)
    return x * damp * env_adsr(n, 0.006, 0.05) * vel


def music_box(freq, vel=0.6):
    """Plucked comb tine: bright attack, a high inharmonic mode, ~2s ring."""
    n = int(2.4 * SR)
    t = np.arange(n) / SR
    x = np.sin(2 * np.pi * freq * t) * np.exp(-t * 1.7)
    x += 0.35 * np.sin(2 * np.pi * freq * 2.0 * t) * np.exp(-t * 3.5)
    x += 0.18 * np.sin(2 * np.pi * freq * 5.4 * t) * np.exp(-t * 9)
    x += 0.10 * np.sin(2 * np.pi * freq * 8.9 * t) * np.exp(-t * 16)
    return x * env_adsr(n, 0.0015, 0.1) * vel


def bell(freq, dur=3.0, vel=0.7):
    n = int(dur * SR)
    t = np.arange(n) / SR
    partials = [(1.0, 1.0, 1.1), (2.0, 0.45, 1.8), (2.76, 0.32, 2.4), (4.07, 0.18, 3.4), (5.4, 0.10, 4.6)]
    x = sum(a * np.sin(2 * np.pi * freq * r * t) * np.exp(-d * t) for r, a, d in partials)
    return x * env_adsr(n, 0.002, 0.3) * vel


def soft_kick(vel=0.5):
    n = int(0.35 * SR)
    t = np.arange(n) / SR
    f = 45 + 70 * np.exp(-t * 30)
    ph = 2 * np.pi * np.cumsum(f) / SR
    return np.sin(ph) * np.exp(-t * 11) * vel


def brush_hat(vel=0.15):
    n = int(0.12 * SR)
    t = np.arange(n) / SR
    x = highpass(rng.standard_normal(n), 6000) * np.exp(-t * 40)
    return x * vel


# ---------- tracks -----------------------------------------------------------------------------

def track_rain():
    """Lo-fi rain: gentle electric-piano chords, soft bass and brushed beat under steady rain."""
    bpm = 72
    beat = 60 / bpm
    bars = 16
    n = int(round(bars * 4 * beat * SR))
    music = np.zeros((2, n))
    prog = [(53, [53, 57, 60, 64]), (52, [52, 55, 59, 62]), (50, [50, 53, 57, 60]), (48, [48, 52, 55, 59])]
    for bar in range(bars):
        root, chord = prog[(bar // 2) % 4]
        t0 = bar * 4 * beat
        if bar % 2 == 0:
            for i, m in enumerate(chord):
                s = epiano(midi(m), 3.6 * beat, 0.32 + 0.04 * rng.random())
                add_wrapped(music, pan(s, -0.3 + 0.2 * i), int((t0 + 0.012 * i) * SR))
            bass = epiano(midi(root - 12), 1.8 * beat, 0.55)
            add_wrapped(music, pan(lowpass(bass, 500), 0), int(t0 * SR))
        else:
            for i, m in enumerate(chord[1:]):
                s = epiano(midi(m + 12 if i == 2 else m), 1.4 * beat, 0.22)
                add_wrapped(music, pan(s, 0.25 - 0.2 * i), int((t0 + 2.5 * beat + 0.015 * i) * SR))
            bass = epiano(midi(root - 12), 1.5 * beat, 0.45)
            add_wrapped(music, pan(lowpass(bass, 500), 0), int((t0 + 2 * beat) * SR))
        # very soft beat
        for b in (0, 2.5):
            add_wrapped(music, pan(soft_kick(0.38), 0), int((t0 + b * beat) * SR))
        for b in (1, 3):
            add_wrapped(music, pan(brush_hat(0.06), 0.3), int((t0 + b * beat) * SR))
        for b in (0.5, 1.5, 2.5, 3.5):
            add_wrapped(music, pan(brush_hat(0.035), -0.3), int((t0 + (b + 0.03) * beat) * SR))
    music = lowpass(music, 4200)  # lo-fi warmth
    music = circular_reverb(music, 1.6, 3.2, 0.22)

    rain = np.vstack([periodic_noise(n, 500, 9000, 0.4), periodic_noise(n, 500, 9000, 0.4)])
    rain += 0.6 * np.vstack([periodic_noise(n, 60, 500, 1.0), periodic_noise(n, 60, 500, 1.0)])
    rain *= 1 + 0.12 * periodic_lfo(n, [0.07, 0.13])
    drops = np.zeros((2, n))
    for _ in range(int(n / SR * 35)):
        L = int(0.006 * SR)
        d = rng.standard_normal(L) * np.exp(-np.arange(L) / (0.0015 * SR))
        add_wrapped(drops, pan(bandpass(d, 1500, 7000) * rng.uniform(0.3, 1.2), rng.uniform(-0.8, 0.8)), rng.integers(n))
    crackle = np.zeros((2, n))
    for _ in range(int(n / SR * 6)):
        crackle[:, rng.integers(n)] += rng.uniform(0.2, 0.6)
    crackle = highpass(crackle, 2000)
    return mix_norm([(music, 1.0), (rain, 0.11), (drops, 0.18), (crackle, 0.05)])


def piano_progression(prog, bpm, bars, arp_patterns, melody=None, vel=0.4):
    beat = 60 / bpm
    n = int(round(bars * 4 * beat * SR))
    out = np.zeros((2, n))
    for bar in range(bars):
        root, chord = prog[(bar // 2) % len(prog)] if len(prog) * 2 <= bars else prog[bar % len(prog)]
        t0 = bar * 4 * beat
        if bar % 2 == 0:
            add_wrapped(out, pan(piano(midi(root - 12), 7.6 * beat, vel * 0.9), -0.25), int(t0 * SR))
            add_wrapped(out, pan(piano(midi(root), 7.6 * beat, vel * 0.6), -0.15), int((t0 + 0.01) * SR))
        pattern = arp_patterns[(bar // 8) % len(arp_patterns)]
        for i, idx in enumerate(pattern):
            m = chord[idx % len(chord)] + 12 * (idx // len(chord))
            v = vel * (0.62 + 0.12 * rng.random()) * (1.0 if i % 2 == 0 else 0.8)
            jitter = rng.normal(0, 0.008)
            add_wrapped(out, pan(piano(midi(m), 0.9 * beat, v), 0.1 + 0.05 * (idx % 3)), int((t0 + i * 0.5 * beat + jitter) * SR))
    if melody:
        t = 0.0
        for m, beats in melody:
            if m:
                add_wrapped(out, pan(piano(midi(m), beats * beat * 0.95, vel * 0.85), 0.2), int(t * beat * SR))
            t += beats
    return out


def track_piano():
    """Soft piano: slow C – G/B – Am – F arpeggios, a simple melody on the second pass."""
    prog = [(48, [60, 64, 67]), (47, [59, 62, 67]), (45, [57, 60, 64]), (41, [57, 60, 65])]
    up = [0, 1, 2, 3, 4, 3, 2, 1]
    wave = [0, 2, 1, 3, 2, 4, 3, 1]
    melody = [(None, 32),
              (76, 3), (74, 1), (72, 4), (71, 3), (72, 1), (74, 4),
              (72, 3), (71, 1), (69, 4), (69, 2), (72, 2), (67, 4)]
    x = piano_progression(prog, 60, 16, [up, wave], melody, vel=0.42)
    x = circular_reverb(x, 2.4, 2.4, 0.3)
    return mix_norm([(x, 1.0)])


def track_cafe():
    """Café murmur: distant overlapping chatter, cups and spoons, a faint piano in the corner."""
    n = int(60 * SR)
    babble = np.zeros((2, n))
    for v in range(10):
        f0 = rng.uniform(110, 240)
        src = bandpass(periodic_noise(n, 90, 4000, 0.6), f0, f0 * 9)
        # formant-ish colouring that wanders slowly
        form = bandpass(src, 400, 1800) * (1 + 0.5 * periodic_lfo(n, [0.3, 0.5]))
        syll = np.clip(0.5 + 0.9 * periodic_lfo(n, [3.1, 4.3, 5.2, 2.2]), 0, None) ** 1.5
        phrase = np.clip(0.4 + periodic_lfo(n, [0.11, 0.19, 0.27]), 0, None) ** 1.2
        voice = (0.6 * src + form) * syll * phrase
        babble += pan(voice, rng.uniform(-0.8, 0.8)) * rng.uniform(0.5, 1.0)
    babble = lowpass(babble, 2600)
    babble = circular_reverb(babble, 2.0, 2.2, 0.55, bright=3000)
    room = np.vstack([periodic_noise(n, 60, 1200, 1.0), periodic_noise(n, 60, 1200, 1.0)])
    clinks = np.zeros((2, n))
    for _ in range(46):
        f = rng.uniform(2200, 4800)
        L = int(0.5 * SR)
        t = np.arange(L) / SR
        c = (np.sin(2 * np.pi * f * t) + 0.5 * np.sin(2 * np.pi * f * 2.7 * t)) * np.exp(-t * rng.uniform(12, 25))
        start = rng.integers(n)
        add_wrapped(clinks, pan(c * rng.uniform(0.15, 0.5), rng.uniform(-0.9, 0.9)), start)
        if rng.random() < 0.4:  # a second tap
            add_wrapped(clinks, pan(c * 0.25, rng.uniform(-0.9, 0.9)), start + int(rng.uniform(0.12, 0.3) * SR))
    clinks = circular_reverb(clinks, 1.2, 4.0, 0.4)
    prog = [(50, [62, 65, 69, 72]), (55, [59, 62, 65, 69]), (48, [60, 64, 67, 71]), (45, [57, 60, 64, 67])]
    beat = 60 / 80
    pianox = np.zeros((2, n))
    bar_len = 4 * beat
    for bar in range(int(60 / bar_len)):
        root, chord = prog[bar % 4]
        t0 = bar * bar_len
        for i, m in enumerate(chord):
            add_wrapped(pianox, pan(piano(midi(m), 3 * beat, 0.25), 0.5), int((t0 + 0.02 * i) * SR))
        add_wrapped(pianox, pan(piano(midi(root - 12), 3 * beat, 0.3), 0.5), int(t0 * SR))
    pianox = circular_reverb(lowpass(pianox, 2500), 2.0, 2.5, 0.5)
    return mix_norm([(babble, 1.0), (room, 0.06), (clinks, 0.35), (pianox, 0.22)])


def chirp(f_from, f_to, dur, vel=0.4):
    n = int(dur * SR)
    t = np.arange(n) / SR
    f = np.geomspace(f_from, f_to, n)
    ph = 2 * np.pi * np.cumsum(f) / SR
    e = np.sin(np.pi * t / dur) ** 2
    return (np.sin(ph) + 0.15 * np.sin(2 * ph)) * e * vel


def bird_phrase(kind):
    parts = []
    if kind == 0:  # descending tweets
        base = rng.uniform(3800, 5200)
        for _ in range(rng.integers(3, 6)):
            parts.append(chirp(base, base * 0.62, 0.07))
            parts.append(np.zeros(int(0.06 * SR)))
    elif kind == 1:  # trill
        f = rng.uniform(2800, 3600)
        n = int(rng.uniform(0.35, 0.6) * SR)
        t = np.arange(n) / SR
        am = 0.5 + 0.5 * np.sin(2 * np.pi * rng.uniform(22, 30) * t)
        parts.append(np.sin(2 * np.pi * f * t) * am * np.sin(np.pi * t / t[-1]) * 0.35)
    else:  # two-note whistle
        f = rng.uniform(2200, 2800)
        parts += [chirp(f, f * 1.05, 0.18, 0.35), np.zeros(int(0.05 * SR)), chirp(f * 1.3, f * 1.22, 0.25, 0.35)]
    return np.concatenate(parts)


def track_birds():
    """Birdsong & piano: unhurried F major piano with birds calling across the stereo field."""
    prog = [(41, [60, 65, 69]), (40, [60, 64, 67]), (38, [57, 62, 65]), (46, [58, 62, 65])]
    pattern = [0, 1, 2, 4, 3, 2, 1, 2]
    melody = [(None, 32), (77, 2), (76, 2), (72, 4), (74, 3), (72, 1), (69, 4),
              (70, 2), (69, 2), (65, 4), (67, 4), (69, 4)]
    p = piano_progression(prog, 66, 16, [pattern], melody, vel=0.38)
    p = circular_reverb(p, 2.4, 2.4, 0.32)
    n = p.shape[-1]
    birds = np.zeros((2, n))
    t = 0.3
    while t < n / SR:
        kind = rng.integers(0, 3)
        side = rng.uniform(-0.9, 0.9)
        phrase = bird_phrase(kind)
        add_wrapped(birds, pan(phrase * rng.uniform(0.4, 1.0), side), int(t * SR))
        if rng.random() < 0.5:  # answer from the other side
            add_wrapped(birds, pan(bird_phrase(kind) * 0.5, -side), int((t + rng.uniform(0.8, 1.6)) * SR))
        t += rng.uniform(1.6, 4.5)
    birds = circular_reverb(birds, 1.4, 3.0, 0.35, bright=9000)
    leaves = np.vstack([periodic_noise(n, 300, 6000, 0.8), periodic_noise(n, 300, 6000, 0.8)])
    leaves *= 0.6 + 0.4 * periodic_lfo(n, [0.05, 0.09])
    return mix_norm([(p, 1.0), (birds, 0.28), (leaves, 0.025)])


def track_ocean():
    """Ocean waves: slow swells that break and wash back, about one every ten seconds."""
    n = int(60 * SR)
    t = np.arange(n) / SR
    dark = np.vstack([periodic_noise(n, 40, 900, 1.0), periodic_noise(n, 40, 900, 1.0)])
    bright = np.vstack([periodic_noise(n, 400, 9000, 0.3), periodic_noise(n, 400, 9000, 0.3)])
    env = np.zeros((2, n))
    for ch in range(2):
        for w in range(6):
            start = w * 10 + rng.uniform(-0.8, 0.8) + ch * 0.35
            rise, fall = rng.uniform(2.5, 3.5), rng.uniform(4.5, 6.0)
            seg_t = np.arange(int((rise + fall) * SR)) / SR
            e = np.where(seg_t < rise, (seg_t / rise) ** 2, np.exp(-(seg_t - rise) / (fall / 3)))
            add_wrapped(env[ch:ch + 1], e[None, :] * rng.uniform(0.75, 1.0), int(start * SR))
    env = 0.18 + env
    x = dark * env + bright * env ** 2.2 * 0.5
    rumble = np.vstack([periodic_noise(n, 25, 120, 1.0)] * 2)
    return mix_norm([(x, 1.0), (rumble, 0.25)])


def track_music_box():
    """Music box: an original lullaby waltz in C major, twinkly and a little sleepy."""
    bpm = 84
    beat = 60 / bpm
    melody = [
        [(76, 1), (79, 1), (84, 1)], [(83, 2), (79, 1)], [(81, 1), (79, 1), (76, 1)], [(79, 3)],
        [(77, 1), (81, 1), (86, 1)], [(84, 2), (81, 1)], [(79, 1), (77, 1), (74, 1)], [(76, 3)],
        [(76, 1), (79, 1), (84, 1)], [(86, 2), (84, 1)], [(83, 1), (81, 1), (79, 1)], [(81, 3)],
        [(77, 1), (76, 1), (74, 1)], [(79, 2), (71, 1)], [(74, 1), (76, 1), (74, 1)], [(72, 3)],
    ]
    chords = [(48, [64, 67]), (43, [62, 67]), (45, [64, 69]), (48, [64, 67]),
              (50, [65, 69]), (41, [65, 69]), (43, [65, 71]), (48, [64, 67]),
              (48, [64, 67]), (43, [62, 67]), (43, [62, 65]), (41, [65, 69]),
              (50, [65, 69]), (43, [62, 67]), (43, [65, 71]), (48, [64, 67])]
    bars = len(melody)
    n = int(round(bars * 3 * beat * SR))
    out = np.zeros((2, n))
    for bar in range(bars):
        t0 = bar * 3 * beat
        t = t0
        for m, b in melody[bar]:
            add_wrapped(out, pan(music_box(midi(m), 0.55 + 0.1 * rng.random()), 0.15), int((t + rng.normal(0, 0.006)) * SR))
            t += b * beat
        root, upper = chords[bar]
        add_wrapped(out, pan(music_box(midi(root + 12), 0.4), -0.2), int(t0 * SR))
        for i, m in enumerate(upper):
            add_wrapped(out, pan(music_box(midi(m), 0.22), -0.1), int((t0 + (1 + i) * beat) * SR))
    out = circular_reverb(out, 2.0, 2.6, 0.3, bright=9000)
    return mix_norm([(out, 1.0)])


def chime():
    """Soft two-note bell for the end of a phase (not a loop)."""
    n = int(3.2 * SR)
    out = np.zeros((2, n))
    out[:, :int(3.0 * SR)] += pan(bell(midi(88), 3.0, 0.6), -0.1)  # E6
    start = int(0.38 * SR)
    b2 = bell(midi(84), 2.8, 0.55)  # C6
    out[:, start:start + len(b2)] += pan(b2, 0.1)
    # small linear reverb (not circular: this one has a beginning and an end)
    ir_n = int(1.2 * SR)
    ir = rng.standard_normal(ir_n) * np.exp(-4 * np.arange(ir_n) / SR)
    ir /= np.sqrt(np.sum(ir ** 2))
    wet = np.vstack([np.convolve(out[c], ir)[:n] for c in range(2)])
    out = out * 0.8 + wet * 0.25
    out *= np.linspace(1, 0, n) ** 0.5
    out /= np.max(np.abs(out)) / 0.7
    return out


def mix_norm(layers, rms_db=-19.0):
    x = sum(sig * g for sig, g in layers)
    x = highpass(x, 30)
    x *= 10 ** (rms_db / 20) / (np.sqrt(np.mean(x ** 2)) + 1e-12)
    return np.tanh(x * 1.1) / np.tanh(1.1)  # gentle soft clip, keeps peaks under 0 dBFS


def write_ogg(x, path, quality=4):
    pcm = (np.clip(x, -1, 1).T * 32767).astype('<i2').tobytes()
    subprocess.run(
        ['ffmpeg', '-y', '-loglevel', 'error', '-f', 's16le', '-ar', str(SR), '-ac', '2', '-i', '-',
         '-c:a', 'libvorbis', '-q:a', str(quality), path],
        input=pcm, check=True,
    )


if __name__ == '__main__':
    out_dir = sys.argv[1]
    os.makedirs(out_dir, exist_ok=True)
    jobs = {
        'focus_lofi_rain.ogg': track_rain,
        'focus_soft_piano.ogg': track_piano,
        'focus_cafe_murmur.ogg': track_cafe,
        'break_birdsong_piano.ogg': track_birds,
        'break_ocean_waves.ogg': track_ocean,
        'break_music_box.ogg': track_music_box,
        'chime.ogg': chime,
    }
    only = sys.argv[2:] or list(jobs)
    for name in only:
        x = jobs[name]()
        write_ogg(x, os.path.join(out_dir, name), quality=3 if name != 'chime.ogg' else 5)
        print(f'{name}: {x.shape[-1] / SR:.1f}s peak {np.max(np.abs(x)):.2f}')
