
package com.cmsc13.frontend;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.VideoWriter;
import org.opencv.videoio.Videoio;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;



public class Camera {

    @FXML private Pane cameraPane;
    @FXML private Button recordButton;
    @FXML private Button saveButton;
    @FXML private Button backButton;

    private final ImageView cameraView = new ImageView();

    private VideoCapture camera;
    private VideoWriter writer;
    private Thread cameraThread;

    private volatile boolean running = false;
    private volatile boolean recording = false;

    private final Object writerLock = new Object();

    private File temporaryVideo;
    private double recordingFps = 20.0;

    @FXML
    public void initialize() {
        cameraView.setPreserveRatio(true);
        cameraView.setSmooth(true);

        cameraView.fitWidthProperty()
                .bind(cameraPane.widthProperty());

        cameraView.fitHeightProperty()
                .bind(cameraPane.heightProperty());

        cameraPane.getChildren().add(cameraView);

        saveButton.setDisable(true);

        try {
            // Load OpenCV's native library.
            nu.pattern.OpenCV.loadLocally();

            // Start the laptop camera.
            startCamera();

        } catch (Exception | LinkageError e) {
            showError("Could not initialize OpenCV:\n" + e.getMessage());
        }
    }

    private void startCamera() {
        camera = new VideoCapture();

        boolean opened;

        if (System.getProperty("os.name")
                .toLowerCase().contains("win")) {
            opened = camera.open(0, Videoio.CAP_DSHOW);
        } else {
            opened = camera.open(0);
        }

        if (!opened) {
            camera.release();
            camera = null;
            showError("Could not open the laptop camera.");
            return;
        }

        camera.set(Videoio.CAP_PROP_FRAME_WIDTH, 1280);
        camera.set(Videoio.CAP_PROP_FRAME_HEIGHT, 720);

        double actualFps = camera.get(Videoio.CAP_PROP_FPS);

        if (actualFps > 0 && actualFps <= 120) {
            recordingFps = actualFps;
        }

        running = true;

        cameraThread = new Thread(this::cameraLoop, "Camera-Thread");
        cameraThread.setDaemon(true);
        cameraThread.start();
    }

    private void cameraLoop() {
        Mat frame = new Mat();

        try {
            while (running) {

                if (camera == null || !camera.isOpened()) {
                    break;
                }

                // Capture the next camera frame.
                if (!camera.read(frame) || frame.empty()) {
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }

                    continue;
                }

                // Write frames while recording.
                synchronized (writerLock) {
                    if (recording && writer != null) {
                        writer.write(frame);
                    }
                }

                // Convert OpenCV Mat into a JavaFX image.
                WritableImage image = convertToImage(frame);

                if (image != null) {
                    Platform.runLater(() -> {
                        if (running) {
                            cameraView.setImage(image);
                        }
                    });
                }
            }

        } finally {
            frame.release();
        }
    }

    private WritableImage convertToImage(Mat frame) {
        MatOfByte buffer = new MatOfByte();

        try {
            boolean success = Imgcodecs.imencode(
                    ".jpg",
                    frame,
                    buffer
            );

            if (!success) {
                return null;
            }

            BufferedImage bufferedImage = ImageIO.read(
                    new ByteArrayInputStream(buffer.toArray())
            );

            if (bufferedImage == null) {
                return null;
            }

            return SwingFXUtils.toFXImage(bufferedImage, null);

        } catch (IOException e) {
            e.printStackTrace();
            return null;

        } finally {
            buffer.release();
        }
    }

    @FXML
    private void handleRecord() {

        if (camera == null || !camera.isOpened()) {
            showError("The camera is not available.");
            return;
        }

        if (recording) {
            return;
        }

        try {
            int width = (int) camera.get(
                    Videoio.CAP_PROP_FRAME_WIDTH
            );

            int height = (int) camera.get(
                    Videoio.CAP_PROP_FRAME_HEIGHT
            );

            if (width <= 0 || height <= 0) {
                showError("Could not determine camera resolution.");
                return;
            }

            // Create a temporary file until the user saves it.
            temporaryVideo = Files.createTempFile(
                    "camera-recording-",
                    ".mp4"
            ).toFile();

            int codec = VideoWriter.fourcc(
                    'm', 'p', '4', 'v'
            );

            synchronized (writerLock) {
                writer = new VideoWriter(
                        temporaryVideo.getAbsolutePath(),
                        codec,
                        recordingFps,
                        new Size(width, height)
                );

                if (!writer.isOpened()) {
                    writer.release();
                    writer = null;

                    Files.deleteIfExists(temporaryVideo.toPath());
                    temporaryVideo = null;

                    showError("Could not create the MP4 video.");
                    return;
                }

                recording = true;
            }

            recordButton.setText("Recording...");
            recordButton.setDisable(true);
            saveButton.setDisable(false);

        } catch (IOException e) {
            showError("Could not start recording:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleSave() {

        if (temporaryVideo == null) {
            showError("There is no recording to save.");
            return;
        }

        // Stop recording and finalize the video.
        synchronized (writerLock) {
            recording = false;

            if (writer != null) {
                writer.release();
                writer = null;
            }
        }

        recordButton.setText("Record");
        recordButton.setDisable(false);
        saveButton.setDisable(true);

        if (!temporaryVideo.isFile()
                || temporaryVideo.length() == 0) {
            showError("The recording is empty.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Camera Recording");
        chooser.setInitialFileName("recording.mp4");

        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "MP4 Video",
                        "*.mp4"
                )
        );

        File destination = chooser.showSaveDialog(
                cameraPane.getScene().getWindow()
        );

        if (destination == null) {
            // Keep the recording if the dialog is cancelled.
            saveButton.setDisable(false);
            return;
        }

        if (!destination.getName().toLowerCase().endsWith(".mp4")) {
            destination = new File(
                    destination.getParentFile(),
                    destination.getName() + ".mp4"
            );
        }

        try {
            Files.copy(
                    temporaryVideo.toPath(),
                    destination.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );

            Files.deleteIfExists(temporaryVideo.toPath());
            temporaryVideo = null;

            showMessage(
                    "Video saved successfully:\n"
                            + destination.getAbsolutePath()
            );

        } catch (IOException e) {
            showError("Could not save the video:\n" + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        shutdownCamera();

        Stage stage = (Stage) backButton.getScene().getWindow();
        Scene quizScene = QuizController.getReturnScene();

        if (quizScene != null) {
            stage.setScene(quizScene);
            stage.show();
            return;
        }

        // Fallback if the camera screen was opened directly.
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/MainMenu.fxml")
            );

            stage.setScene(new Scene(loader.load()));
            stage.show();

        } catch (IOException | RuntimeException e) {
            showError(
                    "Could not return to the main menu.\n"
                    + e.getMessage()
            );
        }
    }

    private void shutdownCamera() {
        running = false;

        synchronized (writerLock) {
            recording = false;

            if (writer != null) {
                writer.release();
                writer = null;
            }
        }

        if (cameraThread != null) {
            try {
                cameraThread.join(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (camera != null) {
            camera.release();
            camera = null;
        }

        // Delete an unsaved temporary recording.
        if (temporaryVideo != null) {
            try {
                Files.deleteIfExists(temporaryVideo.toPath());
            } catch (IOException ignored) {
            }

            temporaryVideo = null;
        }
    }

    private void showMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Camera Recorder");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Camera Recorder");
        alert.setHeaderText("Camera Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
