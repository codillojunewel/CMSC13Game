package com.cmsc13.frontend.ui;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Retro pixel-font text like the titles, buttons and confession line in the
 * GUI mockups. Uses a hand-made 5x8 bitmap font (/fonts/pixel5x8.txt) drawn
 * into an image and scaled up without smoothing, so every pixel stays crisp.
 * Colour can be flat or a vertical gradient (top → bottom) with a neon glow.
 * <p>
 * FXML: {@code <PixelText text="PLAY GAME" pixel="3" bold="true" color="#39ff6a" glow="#39ff6a"/>}
 */
public class PixelText extends Region {

    private static final int GLYPH_W = 5;
    private static final int GLYPH_H = 8;
    private static final Map<Character, boolean[][]> FONT = loadFont();

    private final ImageView view = new ImageView();

    private final StringProperty text = new SimpleStringProperty(this, "text", "");
    private final DoubleProperty pixel = new SimpleDoubleProperty(this, "pixel", 3);
    /** Kept for FXML compatibility; the bitmap font has a fixed size (use {@code pixel} to scale). */
    private final DoubleProperty fontSize = new SimpleDoubleProperty(this, "fontSize", 10);
    private final BooleanProperty bold = new SimpleBooleanProperty(this, "bold", false);
    private final ObjectProperty<Color> color = new SimpleObjectProperty<>(this, "color", Color.WHITE);
    private final ObjectProperty<Color> bottomColor = new SimpleObjectProperty<>(this, "bottomColor", null);
    private final ObjectProperty<Color> glow = new SimpleObjectProperty<>(this, "glow", null);

    public PixelText() {
        view.setSmooth(false);
        getChildren().add(view);
        setMouseTransparent(true);
        text.addListener((o, a, b) -> render());
        pixel.addListener((o, a, b) -> render());
        bold.addListener((o, a, b) -> render());
        color.addListener((o, a, b) -> render());
        bottomColor.addListener((o, a, b) -> render());
        glow.addListener((o, a, b) -> applyGlow());
    }

    public PixelText(String text, double pixel, Color color) {
        this();
        setPixel(pixel);
        setColor(color);
        setText(text);
    }

    private static Map<Character, boolean[][]> loadFont() {
        Map<Character, boolean[][]> map = new HashMap<>();
        try (InputStream in = PixelText.class.getResourceAsStream("/fonts/pixel5x8.txt")) {
            if (in == null) {
                return map;
            }
            BufferedReader r = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = r.readLine()) != null) {
                String[] parts = line.trim().split("\\s+");
                if (parts.length != GLYPH_H + 1 || parts[0].length() != 1) {
                    continue;
                }
                boolean[][] g = new boolean[GLYPH_H][GLYPH_W];
                for (int y = 0; y < GLYPH_H; y++) {
                    for (int x = 0; x < GLYPH_W; x++) {
                        g[y][x] = parts[y + 1].charAt(x) == '#';
                    }
                }
                map.put(parts[0].charAt(0), g);
            }
        } catch (Exception ignored) {
        }
        boolean[][] dot = new boolean[GLYPH_H][GLYPH_W];
        dot[3][2] = dot[4][2] = true;
        map.put('·', dot); // middle dot
        return map;
    }

    private static boolean[][] glyph(char c) {
        if (c == ' ' || c == ' ') {
            return null;
        }
        boolean[][] g = FONT.get(c);
        if (g == null) {
            g = FONT.get(Character.toUpperCase(c));
        }
        return g == null ? FONT.get('?') : g;
    }

    private void render() {
        String s = text.get();
        if (s == null || s.isEmpty()) {
            view.setImage(null);
            setMinSize(0, 0);
            setPrefSize(0, 0);
            return;
        }
        int extra = bold.get() ? 1 : 0;
        int advance = GLYPH_W + 1 + extra;
        int w = s.length() * advance - 1;
        int h = GLYPH_H;
        WritableImage img = new WritableImage(w, h);
        PixelWriter pw = img.getPixelWriter();
        Color top = color.get() == null ? Color.WHITE : color.get();
        Color bottom = bottomColor.get() == null ? top : bottomColor.get();
        for (int i = 0; i < s.length(); i++) {
            boolean[][] g = glyph(s.charAt(i));
            if (g == null) {
                continue;
            }
            int ox = i * advance;
            for (int y = 0; y < GLYPH_H; y++) {
                Color rowColor = top.interpolate(bottom, y / (double) (GLYPH_H - 2));
                for (int x = 0; x < GLYPH_W; x++) {
                    if (g[y][x]) {
                        pw.setColor(ox + x, y, rowColor);
                        if (extra > 0) {
                            pw.setColor(ox + x + 1, y, rowColor);
                        }
                    }
                }
            }
        }
        view.setImage(img);
        double p = pixel.get();
        view.setFitWidth(w * p);
        view.setFitHeight(h * p);
        setMinSize(w * p, h * p);
        setPrefSize(w * p, h * p);
        setMaxSize(w * p, h * p);
        applyGlow();
        requestLayout();
    }

    private void applyGlow() {
        Color g = glow.get();
        view.setEffect(g == null ? null : new DropShadow(Math.max(6, pixel.get() * 4), g));
    }

    @Override
    protected void layoutChildren() {
        view.relocate(0, 0);
    }

    // ------------------------------------------------------------ properties

    public String getText() { return text.get(); }
    public void setText(String v) { text.set(v); }
    public StringProperty textProperty() { return text; }

    public double getPixel() { return pixel.get(); }
    public void setPixel(double v) { pixel.set(v); }
    public DoubleProperty pixelProperty() { return pixel; }

    public double getFontSize() { return fontSize.get(); }
    public void setFontSize(double v) { fontSize.set(v); }
    public DoubleProperty fontSizeProperty() { return fontSize; }

    public boolean isBold() { return bold.get(); }
    public void setBold(boolean v) { bold.set(v); }
    public BooleanProperty boldProperty() { return bold; }

    public Color getColor() { return color.get(); }
    public void setColor(Color v) { color.set(v); }
    public ObjectProperty<Color> colorProperty() { return color; }

    public Color getBottomColor() { return bottomColor.get(); }
    public void setBottomColor(Color v) { bottomColor.set(v); }
    public ObjectProperty<Color> bottomColorProperty() { return bottomColor; }

    public Color getGlow() { return glow.get(); }
    public void setGlow(Color v) { glow.set(v); }
    public ObjectProperty<Color> glowProperty() { return glow; }
}
