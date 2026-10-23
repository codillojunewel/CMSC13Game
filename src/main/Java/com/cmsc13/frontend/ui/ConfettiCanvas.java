package com.cmsc13.frontend.ui;

import javafx.animation.AnimationTimer;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

import java.util.Random;

/** Celebration confetti and floating code glyphs for the victory screen. */
public class ConfettiCanvas extends Canvas {

    private static final Color[] COLORS = {
            Color.web("#39ff88"), Color.web("#2f8cff"), Color.web("#ffb000"),
            Color.web("#ff5c8a"), Color.web("#c86bff"), Color.web("#39d0ff")
    };
    private static final String[] GLYPHS = {"</>", "{ }", "λ", "true", "++", "?-", "()", ";"};

    private final int count = 140;
    private final double[] x = new double[count];
    private final double[] y = new double[count];
    private final double[] vx = new double[count];
    private final double[] vy = new double[count];
    private final double[] rot = new double[count];
    private final double[] spin = new double[count];
    private final int[] kind = new int[count];
    private final Random random = new Random();
    private long lastTime;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastTime == 0) {
                lastTime = now;
            }
            double dt = Math.min((now - lastTime) / 1e9, 0.05);
            lastTime = now;
            step(dt);
        }
    };

    public ConfettiCanvas() {
        this(1280, 720);
    }

    public ConfettiCanvas(double w, double h) {
        super(w, h);
        setMouseTransparent(true);
        for (int i = 0; i < count; i++) {
            reset(i, true);
        }
        sceneProperty().addListener((o, a, s) -> {
            if (s != null) {
                lastTime = 0;
                timer.start();
            } else {
                timer.stop();
            }
        });
    }

    private void reset(int i, boolean anywhere) {
        x[i] = random.nextDouble() * getWidth();
        y[i] = anywhere ? -random.nextDouble() * getHeight() : -20;
        vx[i] = -30 + random.nextDouble() * 60;
        vy[i] = 60 + random.nextDouble() * 120;
        rot[i] = random.nextDouble() * 360;
        spin[i] = -200 + random.nextDouble() * 400;
        kind[i] = random.nextInt(10);
    }

    private void step(double dt) {
        GraphicsContext g = getGraphicsContext2D();
        g.clearRect(0, 0, getWidth(), getHeight());
        for (int i = 0; i < count; i++) {
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
            rot[i] += spin[i] * dt;
            if (y[i] > getHeight() + 20) {
                reset(i, false);
            }
            Color c = COLORS[i % COLORS.length];
            g.save();
            g.translate(x[i], y[i]);
            g.rotate(rot[i]);
            if (kind[i] == 0) {
                g.setFill(c);
                g.setFont(javafx.scene.text.Font.font("Consolas", 14));
                g.fillText(GLYPHS[i % GLYPHS.length], 0, 0);
            } else {
                g.setFill(c);
                g.fillRect(-5, -3, 10, 6);
            }
            g.restore();
        }
    }
}
