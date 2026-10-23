package com.cmsc13.frontend;

import com.cmsc13.audio.SoundManager;
import com.cmsc13.audio.SoundManager.Sound;
import com.cmsc13.frontend.SceneManager.Screen;
import com.cmsc13.frontend.ui.PixelText;
import com.cmsc13.model.GameSession;
import com.cmsc13.model.PlayerStats;
import com.cmsc13.model.Settings;

import javafx.animation.Animation;
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polyline;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Public Humiliation Protocol (mockup 2.3, webcam image). The live webcam feed
 * plays inside the red HUD frame from the mockup. The player presses the drawn
 * REC button, says the confession line, then STOP saves the video (with the
 * line burned in) to the "corporate database" folder. SNAPSHOT saves a photo.
 * Without a webcam, a simulated "camera offline" mode is used instead.
 */
public class ConfessionController implements ScreenController {

    private static final double MIN_SECONDS = 3.0;
    private static final double RECORD_FPS = 20.0;
    private static final double FEED_W = 926;
    private static final double FEED_H = 502;
    private static boolean openCvLoaded;

    @FXML private Label instructionLabel;
    @FXML private Label caseLabel;
    @FXML private Label savedLabel;
    @FXML private Canvas noiseCanvas;
    @FXML private ImageView cameraView;
    @FXML private Label cameraStatus;
    @FXML private Circle recDot;
    @FXML private Pane battery;
    @FXML private Pane brackets;
    @FXML private PixelText timecode;
    @FXML private Circle recRing;
    @FXML private HBox line1;
    @FXML private HBox line2;
    @FXML private Button stopButton;
    @FXML private Button recordButton;
    @FXML private Button snapshotButton;
    @FXML private Rectangle shutter;

    private GameSession session;
    private int confessionNo;
    private Path videoPath;

    // ---- camera thread state ----
    private volatile boolean running;
    private volatile boolean recording;
    private volatile boolean stopRequested;
    private volatile boolean snapshotRequested;
    private Thread cameraThread;
    private final Object writerLock = new Object();
    private VideoWriter writer;
    private final AtomicReference<byte[]> latestFrame = new AtomicReference<>();
    private volatile int frameW;
    private volatile int frameH;
    private WritableImage fxImage;

    private long recordStartNanos;
    private boolean submitted;
    private boolean simulated;
    private boolean cameraReady;
    private final Random random = new Random();
    private Timeline recBlink;
    private Timeline ringPulse;
    private String lastTimecode = "";

    private final AnimationTimer painter = new AnimationTimer() {
        @Override
        public void handle(long now) {
            byte[] rgb = latestFrame.getAndSet(null);
            if (rgb != null && frameW > 0) {
                if (fxImage == null || fxImage.getWidth() != frameW || fxImage.getHeight() != frameH) {
                    fxImage = new WritableImage(frameW, frameH);
                    cameraView.setImage(fxImage);
                    cameraView.setViewport(coverViewport(frameW, frameH));
                }
                fxImage.getPixelWriter().setPixels(0, 0, frameW, frameH,
                        PixelFormat.getByteRgbInstance(), rgb, 0, frameW * 3);
            }
            if (simulated || cameraView.getImage() == null) {
                drawNoise();
            }
            if (recording) {
                double secs = (now - recordStartNanos) / 1e9;
                String tc = formatTimecode(secs);
                if (!tc.equals(lastTimecode)) {
                    lastTimecode = tc;
                    timecode.setText(tc);
                }
                if (secs >= MIN_SECONDS && stopButton.isDisabled() && !submitted) {
                    stopButton.setDisable(false);
                    instructionLabel.setText("Good. Finish the line, then press STOP to submit your confession.");
                }
            }
        }
    };

    @FXML
    private void initialize() {
        buildHud();
    }

    @Override
    public void onShow() {
        session = AppState.getSession();
        SoundManager.get().stopMusic();
        confessionNo = PlayerStats.get().getConfessions() + 1;
        String name = session != null ? session.getPlayerName() : "Player Name";

        buildLine(name);
        caseLabel.setText(String.format("#%04d%nEmployee: %s%nCrashed at Level %d%n%nDisciplinary measure: mandatory video confession.",
                confessionNo, name, session != null ? session.getRung().number() : 1));
        stopButton.setDisable(true);

        recBlink = new Timeline(new KeyFrame(Duration.millis(500), e -> recDot.setVisible(!recDot.isVisible() || !recording)));
        recBlink.setCycleCount(Animation.INDEFINITE);
        recBlink.play();
        recDot.setOpacity(0.45);

        painter.start();
        if (Settings.get().isWebcamEnabled()) {
            instructionLabel.setText("HR is turning on your webcam. When the feed appears, press the red REC button "
                    + "and say the line below clearly (at least 3 seconds).");
            recordButton.setDisable(true);
            startCameraAsync();
        } else {
            enterSimulatedMode("Webcam confessions are disabled in Settings.");
        }
    }

    @Override
    public void onHide() {
        painter.stop();
        if (recBlink != null) {
            recBlink.stop();
        }
        if (ringPulse != null) {
            ringPulse.stop();
        }
        shutdownCamera();
    }

    // ------------------------------------------------------------ HUD

    private void buildHud() {
        Color red = Color.web("#ff3344");
        // battery icon (top-right of the feed)
        Rectangle body = new Rectangle(0, 0, 62, 26);
        body.setFill(Color.TRANSPARENT);
        body.setStroke(red);
        body.setStrokeWidth(2.5);
        body.setArcWidth(5);
        body.setArcHeight(5);
        Rectangle nub = new Rectangle(63, 8, 5, 10);
        nub.setFill(red);
        battery.getChildren().addAll(body, nub);
        for (int i = 0; i < 3; i++) {
            Rectangle bar = new Rectangle(6 + i * 18, 5, 14, 16);
            bar.setFill(red);
            battery.getChildren().add(bar);
        }
        // thin corner brackets inside the feed
        double[][] corners = {{105, 150, 1, 1}, {985, 150, -1, 1}, {105, 480, 1, -1}, {985, 480, -1, -1}};
        for (double[] c : corners) {
            Polyline l = new Polyline(c[0], c[1] + 42 * c[3], c[0], c[1], c[0] + 42 * c[2], c[1]);
            l.setStroke(red);
            l.setStrokeWidth(3);
            brackets.getChildren().add(l);
        }
    }

    private void buildLine(String name) {
        Color white = Color.web("#f2f2f2");
        Color red = Color.web("#ff3344");
        double px = name.length() > 10 ? 3 : 3.5;
        line1.getChildren().setAll(word("My name is ", px, white), word("[" + name + "]", px, red), word(", and", px, white));
        line2.getChildren().setAll(word("I am ", px, white), word("NOT", px, red), word(" smarter than a Junior Dev!", px, white));
    }

    private static PixelText word(String text, double px, Color c) {
        PixelText t = new PixelText(text.replace(' ', ' '), px, c);
        t.setFontSize(10);
        if (c.getRed() > 0.9 && c.getGreen() < 0.5) {
            t.setGlow(Color.web("#ff2a3c"));
        }
        return t;
    }

    // ------------------------------------------------------------ camera

    private void startCameraAsync() {
        running = true;
        cameraThread = new Thread(this::cameraLoop, "Confession-Camera");
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void cameraLoop() {
        VideoCapture camera = null;
        Mat frame = null;
        Mat display = null;
        Mat stamped = null;
        try {
            if (!openCvLoaded) {
                nu.pattern.OpenCV.loadLocally();
                openCvLoaded = true;
            }
            camera = new VideoCapture();
            boolean opened = System.getProperty("os.name").toLowerCase().contains("win")
                    ? camera.open(0, Videoio.CAP_DSHOW)
                    : camera.open(0);
            if (!opened) {
                camera.release();
                Platform.runLater(() -> enterSimulatedMode("No webcam was detected."));
                return;
            }
            camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 1280);
            camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);

            frame = new Mat();
            display = new Mat();
            stamped = new Mat();
            long nextWrite = 0;
            boolean announced = false;

            while (running) {
                if (!camera.read(frame) || frame.empty()) {
                    Thread.sleep(15);
                    continue;
                }
                if (!announced) {
                    announced = true;
                    Platform.runLater(this::onCameraReady);
                }
                synchronized (writerLock) {
                    if (recording && writer != null) {
                        long now = System.nanoTime();
                        if (nextWrite == 0) {
                            nextWrite = now;
                        }
                        if (now >= nextWrite) {
                            frame.copyTo(stamped);
                            burnInOverlay(stamped);
                            while (now >= nextWrite) {
                                writer.write(stamped);
                                nextWrite += (long) (1e9 / RECORD_FPS);
                            }
                        }
                    } else {
                        nextWrite = 0;
                    }
                }
                if (snapshotRequested) {
                    snapshotRequested = false;
                    saveSnapshot(frame);
                }
                if (stopRequested) {
                    stopRequested = false;
                    if (videoPath != null) {
                        Imgcodecs.imwrite(videoPath.toString().replace(".mp4", ".jpg"), frame);
                    }
                    finishRecording();
                }

                Core.flip(frame, display, 1); // mirror like a selfie camera
                Imgproc.cvtColor(display, display, Imgproc.COLOR_BGR2RGB);
                int w = display.cols();
                int h = display.rows();
                byte[] rgb = new byte[w * h * 3];
                display.get(0, 0, rgb);
                frameW = w;
                frameH = h;
                latestFrame.set(rgb);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            String msg = t.getMessage();
            Platform.runLater(() -> enterSimulatedMode("Webcam error: " + msg));
        } finally {
            finishRecording();
            if (camera != null) {
                camera.release();
            }
            for (Mat m : new Mat[]{frame, display, stamped}) {
                if (m != null) {
                    m.release();
                }
            }
        }
    }

    private static Rectangle2D coverViewport(int w, int h) {
        double target = FEED_W / FEED_H;
        double ratio = (double) w / h;
        if (ratio > target) {
            double vw = h * target;
            return new Rectangle2D((w - vw) / 2, 0, vw, h);
        }
        double vh = w / target;
        return new Rectangle2D(0, (h - vh) / 2, w, vh);
    }

    private void burnInOverlay(Mat m) {
        String name = session != null ? session.getPlayerName() : "Player";
        String line = "My name is " + name + ", and I am NOT smarter than a Junior Dev!";
        double scale = Math.max(0.45, m.cols() / 1280.0 * 0.85);
        int font = Imgproc.FONT_HERSHEY_SIMPLEX;
        int barH = (int) (60 * scale / 0.85);
        Imgproc.rectangle(m, new Point(0, m.rows() - barH), new Point(m.cols(), m.rows()), new Scalar(10, 5, 30), -1);
        Imgproc.putText(m, line, new Point(16, m.rows() - barH / 3.0), font, scale, new Scalar(255, 255, 255), 2);
        String header = String.format("REC  CONFESSION #%04d  %s", confessionNo,
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        Imgproc.putText(m, header, new Point(16, 36 * scale / 0.85), font, scale * 0.8, new Scalar(68, 51, 255), 2);
    }

    private void saveSnapshot(Mat frame) {
        try {
            Files.createDirectories(Settings.confessionsFolder());
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path p = Settings.confessionsFolder().resolve("snapshot_" + stamp + ".jpg");
            Imgcodecs.imwrite(p.toString(), frame);
            Platform.runLater(() -> savedLabel.setText("Snapshot saved:\n" + p.getFileName()));
        } catch (IOException ignored) {
        }
    }

    private void onCameraReady() {
        if (simulated) {
            return;
        }
        cameraReady = true;
        cameraStatus.setVisible(false);
        noiseCanvas.setVisible(false);
        recordButton.setDisable(false);
        recordButton.requestFocus();
        instructionLabel.setText("Camera is live. Press the red REC button, look into the camera and say the line below.");
    }

    @FXML
    private void record() {
        if (recording || submitted) {
            return;
        }
        if (simulated) {
            startRecordingUi();
            instructionLabel.setText("Say the line OUT LOUD now. Press STOP when you're done (at least 3 seconds).");
            return;
        }
        if (!cameraReady) {
            return;
        }
        try {
            Files.createDirectories(Settings.confessionsFolder());
        } catch (IOException e) {
            enterSimulatedMode("Could not create the confessions folder.");
            return;
        }
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeName = (session != null ? session.getPlayerName() : "player").replaceAll("[^A-Za-z0-9_-]", "_");
        videoPath = Settings.confessionsFolder().resolve("confession_" + stamp + "_" + safeName + ".mp4");

        int w = frameW > 0 ? frameW : 1280;
        int h = frameH > 0 ? frameH : 720;
        synchronized (writerLock) {
            writer = new VideoWriter(videoPath.toString(), VideoWriter.fourcc('m', 'p', '4', 'v'),
                    RECORD_FPS, new Size(w, h));
            if (!writer.isOpened()) {
                writer.release();
                writer = null;
                enterSimulatedMode("Could not start the video recorder.");
                return;
            }
        }
        startRecordingUi();
        instructionLabel.setText("Recording... say the line now!");
    }

    private void startRecordingUi() {
        recording = true;
        recordStartNanos = System.nanoTime();
        recDot.setOpacity(1);
        recordButton.setDisable(true);
        SoundManager.get().play(Sound.PRESS);
        ringPulse = new Timeline(
                new KeyFrame(Duration.ZERO, e -> recRing.setOpacity(1)),
                new KeyFrame(Duration.millis(450), e -> recRing.setOpacity(0.3)));
        ringPulse.setCycleCount(Animation.INDEFINITE);
        ringPulse.play();
    }

    @FXML
    private void stop() {
        if (!recording || submitted) {
            if (!submitted) {
                instructionLabel.setText("HR refuses to continue until your confession is recorded. Press REC first.");
            }
            return;
        }
        submitted = true;
        stopButton.setDisable(true);
        if (ringPulse != null) {
            ringPulse.stop();
        }
        recRing.setOpacity(0);
        recording = false;
        if (simulated) {
            logConfession("Verbal confession logged.");
            return;
        }
        stopRequested = true;
        PauseTransition settle = new PauseTransition(Duration.millis(400));
        settle.setOnFinished(e -> {
            finishRecording();
            boolean ok = videoPath != null && Files.exists(videoPath);
            logConfession(ok ? "Video logged:\n" + videoPath.getFileName() : "Confession logged.");
        });
        settle.play();
    }

    @FXML
    private void snapshot() {
        shutter.setOpacity(0.8);
        FadeTransition f = new FadeTransition(Duration.millis(350), shutter);
        f.setToValue(0);
        f.play();
        SoundManager.get().play(Sound.PRESS);
        if (cameraReady && !simulated) {
            snapshotRequested = true;
        } else {
            savedLabel.setText("No camera: nothing to snapshot.");
        }
    }

    private void finishRecording() {
        synchronized (writerLock) {
            if (writer != null) {
                writer.release();
                writer = null;
            }
        }
    }

    private void shutdownCamera() {
        running = false;
        recording = false;
        finishRecording();
        if (cameraThread != null) {
            try {
                cameraThread.join(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            cameraThread = null;
        }
        if (!submitted && videoPath != null) {
            try {
                Files.deleteIfExists(videoPath);
            } catch (IOException ignored) {
            }
        }
    }

    // ------------------------------------------------------------ simulated mode

    private void enterSimulatedMode(String reason) {
        if (simulated || submitted) {
            return;
        }
        simulated = true;
        running = false;
        recording = false;
        finishRecording();
        cameraView.setImage(null);
        noiseCanvas.setVisible(true);
        cameraStatus.setVisible(true);
        cameraStatus.setText("CAMERA OFFLINE  ·  AUDIO CONFESSION MODE");
        recordButton.setDisable(false);
        instructionLabel.setText(reason + " Press REC, read the line OUT LOUD, then press STOP.");
    }

    private void drawNoise() {
        GraphicsContext g = noiseCanvas.getGraphicsContext2D();
        double w = noiseCanvas.getWidth();
        double h = noiseCanvas.getHeight();
        g.setFill(Color.web("#07070b"));
        g.fillRect(0, 0, w, h);
        for (int i = 0; i < 1100; i++) {
            g.setFill(Color.gray(random.nextDouble(), 0.3 + 0.4 * random.nextDouble()));
            g.fillRect(random.nextDouble() * w, random.nextDouble() * h, 3, 2);
        }
        g.setFill(Color.rgb(255, 59, 92, 0.08));
        g.fillRect(0, (System.nanoTime() / 4_000_000L) % h, w, 18);
    }

    // ------------------------------------------------------------ finish

    private void logConfession(String headline) {
        int n = PlayerStats.get().recordConfession();
        writeTextLog(n);
        recDot.setOpacity(0.45);
        savedLabel.setText("✓ " + headline + "\n(#" + n + " in the corporate database)");
        instructionLabel.setText("Confession archived. Initiating forced reboot...");
        SoundManager.get().play(Sound.LIFELINE);
        PauseTransition go = new PauseTransition(Duration.seconds(2.4));
        go.setOnFinished(e -> SceneManager.show(Screen.REBOOT));
        go.play();
    }

    private void writeTextLog(int n) {
        try {
            Files.createDirectories(Settings.confessionsFolder());
            Path log = Settings.confessionsFolder().resolve("hr_database.log");
            String name = session != null ? session.getPlayerName() : "Player";
            String level = session != null ? String.valueOf(session.getRung().number()) : "?";
            String entry = String.format("[%s] #%04d  %s crashed at Level %s: \"My name is %s, and I am NOT smarter than a Junior Dev!\"%s%n",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), n, name, level, name,
                    videoPath != null && !simulated ? "  video=" + videoPath.getFileName() : "  (verbal)");
            Files.writeString(log, entry, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
        }
    }

    private static String formatTimecode(double secs) {
        int total = (int) secs;
        int frames = (int) ((secs - total) * 30);
        return String.format("%02d:%02d:%02d:%02d", total / 3600, (total / 60) % 60, total % 60, frames);
    }
}
