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
    private Boolean isPlaying = false;

    @FXML
    void btnPlay(MouseEvent event) {
        if(!isPlaying) {
            btnPlay.setText("Pausar");
            mediaPlayer.play();
            isPlaying = true;
        } else if(isPlaying) {
            btnPlay.setText("Reanudar");
            mediaPlayer.pause();
            isPlaying = false;
        }
    }

    public void cargarPelicula(String path) {
        media = new Media(path);
        mediaPlayer = new MediaPlayer(media);

        mediaView.setMediaPlayer(mediaPlayer);
        mediaPlayer.currentTimeProperty().addListener((observableValue, oldValue, newValue) -> {
            slider.setValue(newValue.toSeconds());
        });

        mediaPlayer.setOnReady(() -> {
            Duration total = media.getDuration();
            slider.setValue(total.toSeconds());
        });

        Scene scene = mediaView.getScene();
        mediaView.fitWidthProperty().bind(scene.widthProperty());
        mediaView.fitHeightProperty().bind(scene.heightProperty());

        mediaPlayer.setAutoPlay(false);
    }

    @FXML
    void sliderPressed(MouseEvent event) {
        mediaPlayer.seek(Duration.seconds(slider.getValue()));
    }
}
