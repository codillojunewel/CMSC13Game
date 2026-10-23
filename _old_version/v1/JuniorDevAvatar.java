package com.cmsc13.frontend.ui;

import com.cmsc13.model.Persona;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.util.Duration;

/**
 * A little robot "Junior Dev" drawn entirely with JavaFX shapes and coloured
 * by its {@link Persona}. It idles (bobs and blinks) and can change expression.
 * The base drawing is 160 x 200 and is scaled to the requested size.
 */
public class JuniorDevAvatar extends Pane {

    public enum Mood { NEUTRAL, HAPPY, SMUG, SHOCKED, THINKING }

    private static final double BASE_W = 160;
    private static final double BASE_H = 200;

    private final Persona persona;
    private final Color color;
    private final Group robot = new Group();
    private final Group face = new Group();
    private final Circle leftEye = new Circle(54, 66, 8);
    private final Circle rightEye = new Circle(106, 66, 8);
    private final Rectangle visor;
    private final Timeline blink;
    private final TranslateTransition bob;
    private boolean active;

    public JuniorDevAvatar(Persona persona, double width) {
        this.persona = persona;
        this.color = Color.web(persona.getColor());
        double scale = width / BASE_W;
        setPrefSize(BASE_W * scale, BASE_H * scale);
        setMinSize(BASE_W * scale, BASE_H * scale);
        setMaxSize(BASE_W * scale, BASE_H * scale);

        // Antenna
        Line antenna = new Line(80, 14, 80, 30);
        antenna.setStroke(color);
        antenna.setStrokeWidth(3);
        Circle antennaTip = new Circle(80, 11, 5, color);
        antennaTip.setEffect(new DropShadow(10, color));

        // Head with visor
        Rectangle head = new Rectangle(28, 30, 104, 78);
        head.setArcWidth(30);
        head.setArcHeight(30);
        head.setFill(Color.web("#1a2238"));
        head.setStroke(color);
        head.setStrokeWidth(3);

        visor = new Rectangle(38, 44, 84, 50);
        visor.setArcWidth(18);
        visor.setArcHeight(18);
        visor.setFill(Color.web("#050a16"));
        visor.setStroke(color.deriveColor(0, 1, 1, 0.5));

        Circle earL = new Circle(26, 70, 7, color.darker());
        Circle earR = new Circle(134, 70, 7, color.darker());

        leftEye.setFill(color);
        rightEye.setFill(color);
        leftEye.setEffect(new DropShadow(8, color));
        rightEye.setEffect(new DropShadow(8, color));

        // Body
        Polygon body = new Polygon(42, 112, 118, 112, 128, 160, 32, 160);
        body.setFill(Color.web("#141b2e"));
        body.setStroke(color.deriveColor(0, 1, 1, 0.8));
        body.setStrokeWidth(2);
        Polygon tie = new Polygon(76, 112, 84, 112, 86, 140, 80, 148, 74, 140);
        tie.setFill(color);

        // Desk with name plate
        Rectangle desk = new Rectangle(8, 150, 144, 46);
        desk.setArcWidth(10);
        desk.setArcHeight(10);
        desk.setFill(Color.web("#0b1226"));
        desk.setStroke(color);
        desk.setStrokeWidth(2.5);
        desk.setEffect(new DropShadow(14, color.deriveColor(0, 1, 1, 0.6)));

        Label plate = new Label(persona.getDeskName());
        plate.setStyle("-fx-font-family: 'Consolas'; -fx-font-weight: bold; -fx-font-size: 17px;"
                + "-fx-text-fill: " + persona.getColor() + ";");
        StackPane plateBox = new StackPane(plate);
        plateBox.setLayoutX(8);
        plateBox.setLayoutY(150);
        plateBox.setPrefSize(144, 28);
        plateBox.setAlignment(Pos.CENTER);

        Label glyph = new Label(persona.getGlyph());
        glyph.setStyle("-fx-font-family: 'Consolas'; -fx-font-size: 12px; -fx-text-fill: #8aa0c8;");
        StackPane glyphBox = new StackPane(glyph);
        glyphBox.setLayoutX(8);
        glyphBox.setLayoutY(174);
        glyphBox.setPrefSize(144, 18);

        face.getChildren().addAll(leftEye, rightEye);
        robot.getChildren().addAll(antenna, antennaTip, earL, earR, head, visor, face, body, tie);
        Group root = new Group(robot, desk, plateBox, glyphBox);
        root.getTransforms().add(new javafx.scene.transform.Scale(scale, scale, 0, 0));
        getChildren().add(root);

        setMood(Mood.NEUTRAL);

        blink = new Timeline(
                new KeyFrame(Duration.seconds(0), e -> setEyesOpen(true)),
                new KeyFrame(Duration.seconds(3.2), e -> setEyesOpen(false)),
                new KeyFrame(Duration.seconds(3.35), e -> setEyesOpen(true)));
        blink.setCycleCount(Animation.INDEFINITE);
        blink.setDelay(Duration.millis(persona.ordinal() * 430));

        bob = new TranslateTransition(Duration.seconds(1.6 + persona.ordinal() * 0.13), robot);
        bob.setFromY(0);
        bob.setToY(-4);
        bob.setAutoReverse(true);
        bob.setCycleCount(Animation.INDEFINITE);

        sceneProperty().addListener((o, a, s) -> {
            if (s != null) {
                blink.play();
                bob.play();
            } else {
                blink.stop();
                bob.stop();
            }
        });
    }

    public Persona getPersona() {
        return persona;
    }

    private void setEyesOpen(boolean open) {
        leftEye.setScaleY(open ? 1 : 0.15);
        rightEye.setScaleY(open ? 1 : 0.15);
    }

    /** Highlights the proctor that is currently on duty. */
    public void setActive(boolean active) {
        this.active = active;
        setOpacity(active ? 1.0 : 0.45);
        setEffect(active ? new DropShadow(26, color) : null);
    }

    public boolean isActive() {
        return active;
    }

    public void setMood(Mood mood) {
        face.getChildren().setAll(leftEye, rightEye);
        leftEye.setRadius(8);
        rightEye.setRadius(8);
        leftEye.setCenterY(66);
        rightEye.setCenterY(66);

        switch (mood) {
            case HAPPY -> {
                Arc smile = arc(80, 76, 18, 10, 180, 180);
                face.getChildren().add(smile);
            }
            case SMUG -> {
                leftEye.setRadius(6);
                rightEye.setRadius(6);
                Line brow = new Line(46, 54, 64, 58);
                brow.setStroke(color);
                brow.setStrokeWidth(3);
                Line mouth = new Line(68, 84, 96, 80);
                mouth.setStroke(color);
                mouth.setStrokeWidth(3);
                mouth.setStrokeLineCap(StrokeLineCap.ROUND);
                face.getChildren().addAll(brow, mouth);
            }
            case SHOCKED -> {
                leftEye.setRadius(10);
                rightEye.setRadius(10);
                Circle mouth = new Circle(80, 84, 6);
                mouth.setFill(Color.TRANSPARENT);
                mouth.setStroke(color);
                mouth.setStrokeWidth(3);
                face.getChildren().add(mouth);
            }
            case THINKING -> {
                leftEye.setCenterY(62);
                rightEye.setCenterY(62);
                Line mouth = new Line(72, 84, 88, 84);
                mouth.setStroke(color);
                mouth.setStrokeWidth(3);
                Label dots = new Label("...");
                dots.setStyle("-fx-text-fill: " + persona.getColor() + "; -fx-font-size: 14px; -fx-font-weight: bold;");
                dots.setLayoutX(100);
                dots.setLayoutY(70);
                face.getChildren().addAll(mouth, dots);
            }
            default -> {
                Line mouth = new Line(70, 84, 90, 84);
                mouth.setStroke(color);
                mouth.setStrokeWidth(3);
                mouth.setStrokeLineCap(StrokeLineCap.ROUND);
                face.getChildren().add(mouth);
            }
        }
    }

    private Arc arc(double cx, double cy, double rx, double ry, double start, double length) {
        Arc a = new Arc(cx, cy, rx, ry, start, length);
        a.setType(ArcType.OPEN);
        a.setFill(Color.TRANSPARENT);
        a.setStroke(color);
        a.setStrokeWidth(3);
        a.setStrokeLineCap(StrokeLineCap.ROUND);
        return a;
    }
}
