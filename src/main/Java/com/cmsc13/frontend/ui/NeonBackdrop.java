package com.cmsc13.frontend.ui;

import javafx.animation.AnimationTimer;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;

import java.util.Random;

/**
 * Animated "neon server room" background used by every screen: a dark
 * gradient, a perspective grid floor, circuit traces, drifting data particles
 * and a slow scan line. Drawn on a canvas by an {@link AnimationTimer} that uses
 * delta time, so speed is identical on fast and slow machines.
 * <p>
 * Usable from FXML: {@code <NeonBackdrop accent="#ff3b5c"/>}.
 */
public class NeonBackdrop extends Pane {

    private static final int PARTICLES = 46;

    private final Canvas canvas = new Canvas();
    private final ObjectProperty<Color> accent = new SimpleObjectProperty<>(Color.web("#2f8cff"));
    private final double[] px = new double[PARTICLES];
    private final double[] py = new double[PARTICLES];
    private final double[] speed = new double[PARTICLES];
    private final Random random = new Random(7);

    private double time;
    private long lastTime;

    private final AnimationTimer timer = new AnimationTimer() {
        @Override
        public void handle(long now) {
            if (lastTime == 0) {
                lastTime = now;
            }
            double delta = Math.min((now - lastTime) / 1e9, 0.1);
            lastTime = now;
            time += delta;
            for (int i = 0; i < PARTICLES; i++) {
                py[i] -= speed[i] * delta;
                if (py[i] < -10) {
                    py[i] = getHeight() + 10;
                    px[i] = random.nextDouble() * getWidth();
                }
            }
            draw();
        }
    };

    public NeonBackdrop() {
        getChildren().add(canvas);
        setMouseTransparent(true);
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());
        for (int i = 0; i < PARTICLES; i++) {
            px[i] = random.nextDouble() * 1280;
            py[i] = random.nextDouble() * 720;
            speed[i] = 8 + random.nextDouble() * 28;
        }
        accent.addListener((o, a, b) -> draw());
        sceneProperty().addListener((o, oldScene, newScene) -> {
            if (newScene != null) {
                lastTime = 0;
                timer.start();
            } else {
                timer.stop();
            }
        });
    }

    public Color getAccent() { return accent.get(); }
    public void setAccent(Color c) { accent.set(c); }
    public ObjectProperty<Color> accentProperty() { return accent; }

    private void draw() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) {
            return;
        }
        GraphicsContext g = canvas.getGraphicsContext2D();
        Color a = accent.get();

        // Base gradient
        g.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#050914")), new Stop(0.6, Color.web("#081028")), new Stop(1, Color.web("#03050c"))));
        g.fillRect(0, 0, w, h);

        // Central glow
        g.setFill(new RadialGradient(0, 0, w / 2, h * 0.42, w * 0.55, false, CycleMethod.NO_CYCLE,
                new Stop(0, a.deriveColor(0, 1, 1, 0.16)), new Stop(1, Color.TRANSPARENT)));
        g.fillRect(0, 0, w, h);

        // Perspective floor grid
        double horizon = h * 0.70;
        g.setLineWidth(1);
        for (int i = 0; i <= 12; i++) {
            double t = i / 12.0;
            double y = horizon + (h - horizon) * t * t;
            g.setStroke(a.deriveColor(0, 1, 1, 0.05 + 0.18 * t));
            g.strokeLine(0, y, w, y);
        }
        for (int i = -16; i <= 16; i++) {
            double xTop = w / 2 + i * 40;
            double xBottom = w / 2 + i * 150;
            g.setStroke(a.deriveColor(0, 1, 1, 0.12));
            g.strokeLine(xTop, horizon, xBottom, h);
        }

        // Circuit traces on both sides
        g.setStroke(a.deriveColor(0, 1, 1, 0.22));
        g.setLineWidth(1.4);
        for (int i = 0; i < 6; i++) {
            double y = 70 + i * 52;
            g.strokeLine(0, y, 40 + i * 9, y);
            g.strokeLine(40 + i * 9, y, 60 + i * 9, y + 20);
            g.strokeOval(58 + i * 9, y + 18, 5, 5);
            g.strokeLine(w, y + 10, w - 44 - i * 9, y + 10);
            g.strokeLine(w - 44 - i * 9, y + 10, w - 64 - i * 9, y + 30);
            g.strokeOval(w - 70 - i * 9, y + 28, 5, 5);
        }

        // Data particles (tiny binary rain)
        for (int i = 0; i < PARTICLES; i++) {
            double flicker = 0.25 + 0.25 * Math.sin(time * 2 + i);
            g.setFill(a.deriveColor(0, 1, 1.3, flicker));
            g.fillRect(px[i], py[i], 2, 2 + (i % 3) * 2);
        }

        // Slow scan line
        double scanY = (time * 60) % (h + 200) - 100;
        g.setFill(new LinearGradient(0, scanY - 40, 0, scanY + 40, false, CycleMethod.NO_CYCLE,
                new Stop(0, Color.TRANSPARENT), new Stop(0.5, a.deriveColor(0, 1, 1, 0.06)), new Stop(1, Color.TRANSPARENT)));
        g.fillRect(0, scanY - 40, w, 80);

        // CRT scanlines
        g.setFill(Color.rgb(0, 0, 0, 0.18));
        for (double y = 0; y < h; y += 3) {
            g.fillRect(0, y, w, 1);
        }
    }
}
