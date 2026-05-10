package player;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.util.Duration;

public class ReproductorController {

    @FXML
    private Button btnPlay;

    @FXML
    private Label lblDuration;

    @FXML
    private MediaView mediaView;

    @FXML
    private Slider slider;

    private Media media;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private boolean usuarioMoviendoSlider = false;

    @FXML
    void btnPlay(MouseEvent event) {
        if (!isPlaying) {
            btnPlay.setText("Pausar");
            mediaPlayer.play();
            isPlaying = true;
        } else {
            btnPlay.setText("Reanudar");
            mediaPlayer.pause();
            isPlaying = false;
        }
    }

    public void cargarPelicula(String path) {
        media = new Media(path);
        mediaPlayer = new MediaPlayer(media);
        mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            if (!usuarioMoviendoSlider) {
                slider.setValue(newVal.toSeconds());
                lblDuration.setText(
                    formatear(newVal.toSeconds()) + " / " +
                    formatear(media.getDuration().toSeconds())
                );
            }
        });

        mediaPlayer.setOnReady(() -> {
            double total = media.getDuration().toSeconds();
            slider.setMin(0);
            slider.setMax(total);
            slider.setValue(0);
            lblDuration.setText("00:00 / " + formatear(total));
        });

        mediaPlayer.setOnEndOfMedia(() -> {
            isPlaying = false;
            btnPlay.setText("Reproducir");
            mediaPlayer.pause();
            mediaPlayer.seek(Duration.ZERO);
            slider.setValue(0);
        });

        slider.setOnMousePressed(e -> {
            usuarioMoviendoSlider = true;
            mediaPlayer.pause();
        });

        slider.setOnMouseDragged(e -> {
            mediaPlayer.seek(Duration.seconds(slider.getValue()));
            lblDuration.setText(
                formatear(slider.getValue()) + " / " +
                formatear(media.getDuration().toSeconds())
            );
        });

        slider.setOnMouseReleased(e -> {
            mediaPlayer.seek(Duration.seconds(slider.getValue()));
            usuarioMoviendoSlider = false;
            if (isPlaying) {
                mediaPlayer.play();
            }
        });

        Scene scene = mediaView.getScene();
        mediaView.fitWidthProperty().bind(scene.widthProperty());
        mediaView.fitHeightProperty().bind(scene.heightProperty());
        mediaPlayer.setAutoPlay(false);
    }

    private String formatear(double segundos) {
        int s = (int) segundos;
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}