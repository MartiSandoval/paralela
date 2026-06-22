import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.SVGPath;
import javafx.stage.Stage;
import javafx.util.Duration;

public class ReproductorController {
    private static final boolean MITIGACION_SEEK_ACTIVA = true;
    private static final Duration SEEK_MITIGACION = Duration.millis(200);

    private static final int MAX_INTENTOS = 3;
    private static final Duration TIMEOUT_POR_INTENTO = Duration.seconds(4);

    private static final Duration TIEMPO_INACTIVIDAD = Duration.seconds(3);
    private static final Duration DURACION_FADE = Duration.millis(200);

    public static final double VENTANA_ANCHO_MINIMO = 480.0;
    public static final double VENTANA_ALTO_MINIMO = 270.0;

    private static final String PATH_PLAY = "M8 5v14l11-7z";
    private static final String PATH_PAUSA = "M6 5h4v14H6zm8 0h4v14h-4z";
    private static final String PATH_FULLSCREEN_ENTRAR = "M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z";
    private static final String PATH_FULLSCREEN_SALIR = "M5 16h3v3h2v-5H5v2zm3-8H5v2h5V5H8v3zm6 11h2v-3h3v-2h-5v5zm2-11V5h-2v5h5V8h-3z";

    @FXML
    private StackPane root;

    @FXML
    private StackPane contenedorVideo;

    @FXML
    private MediaView mediaView;

    @FXML
    private VBox overlayControles;

    @FXML
    private HBox barraSuperior;

    @FXML
    private VBox barraInferior;

    @FXML
    private Button btnPlay;

    @FXML
    private SVGPath iconoPlay;

    @FXML
    private Button btnVolver;

    @FXML
    private Button btnImagenNoCarga;

    @FXML
    private Button btnFullscreen;

    @FXML
    private SVGPath iconoFullscreen;

    @FXML
    private Label lblDuration;

    @FXML
    private Slider slider;

    private String pathActual;
    private Media media;
    private MediaPlayer mediaPlayer;
    private boolean isPlaying = false;
    private boolean usuarioMoviendoSlider = false;

    private int intentoActual = 0;
    private PauseTransition watchdog;
    private boolean mitigacionAutomaticaYaAplicada = false;

    private PauseTransition timerInactividad;
    private FadeTransition fadeOverlay;
    private boolean overlayVisible = true;

    /** Se llama una sola vez cuando el video queda listo para reproducirse. */
    private Runnable onListo;
    /** Se llama si se agotan los reintentos sin lograr cargar el video. */
    private Runnable onError;
    /** Se llama al volver al catalogo: tanto si la pelicula termino sola como si el usuario presiono "Volver". */
    private Runnable onSalir;

    public void setOnListo(Runnable callback) {
        this.onListo = callback;
    }

    public void setOnError(Runnable callback) {
        this.onError = callback;
    }

    public void setOnSalir(Runnable callback) {
        this.onSalir = callback;
    }

    @FXML
    public void initialize() {
        configurarAutoOcultamiento();
    }

    @FXML
    void btnPlay(MouseEvent event) {
        alternarReproduccion();
    }

    private void alternarReproduccion() {
        if (!isPlaying) {
            mediaPlayer.play();
            isPlaying = true;
            iconoPlay.setContent(PATH_PAUSA);
        } else {
            mediaPlayer.pause();
            isPlaying = false;
            iconoPlay.setContent(PATH_PLAY);
        }
    }

    @FXML
    void btnVolver(MouseEvent event) {
        if (isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            iconoPlay.setContent(PATH_PLAY);

            Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
            confirmacion.setTitle("Volver al catalogo");
            confirmacion.setHeaderText(null);
            confirmacion.setContentText("La película se está reproduciendo. ¿Deseas salir y volver al catálogo?");

            confirmacion.showAndWait().ifPresentOrElse(boton -> {
                if (boton == ButtonType.OK) {
                    salir();
                } else {
                    mediaPlayer.play();
                    isPlaying = true;
                    iconoPlay.setContent(PATH_PAUSA);
                }
            }, () -> {
                mediaPlayer.play();
                isPlaying = true;
                iconoPlay.setContent(PATH_PAUSA);
            });
        } else {
            salir();
        }
    }

    @FXML
    void btnImagenNoCarga(MouseEvent event) {
        if (mediaPlayer == null) {
            return;
        }
        mitigacionAutomaticaYaAplicada = true;
        btnImagenNoCarga.setDisable(true);
        aplicarMitigacionSeek();
    }

    @FXML
    void btnFullscreen(MouseEvent event) {
        Stage stage = (Stage) root.getScene().getWindow();
        boolean nuevoEstado = !stage.isFullScreen();
        stage.setFullScreen(nuevoEstado);
        iconoFullscreen.setContent(nuevoEstado ? PATH_FULLSCREEN_SALIR : PATH_FULLSCREEN_ENTRAR);
    }

    private void salir() {
        liberar();
        if (onSalir != null) {
            onSalir.run();
        }
    }

    public void cargarPelicula(String path) {
        this.pathActual = path;
        this.intentoActual = 0;
        intentarCargar();
    }

    private void intentarCargar() {
        intentoActual++;
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer = null;
        }

        media = new Media(pathActual);
        mediaPlayer = new MediaPlayer(media);

        watchdog = new PauseTransition(TIMEOUT_POR_INTENTO);
        watchdog.setOnFinished(e -> {
            if (intentoActual < MAX_INTENTOS) {
                intentarCargar();
            } else {
                if (mediaPlayer != null) {
                    mediaPlayer.dispose();
                    mediaPlayer = null;
                }
                if (onError != null) {
                    onError.run();
                }
            }
        });

        mediaPlayer.setOnReady(() -> {
            watchdog.stop();
            configurarReproductor();
            if (onListo != null) {
                onListo.run();
            }
        });

        mediaPlayer.setOnError(() -> {
            watchdog.stop();
            if (intentoActual < MAX_INTENTOS) {
                intentarCargar();
            } else {
                if (onError != null) {
                    onError.run();
                }
            }
        });

        mediaPlayer.setAutoPlay(false);
        watchdog.playFromStart();
    }

    private void configurarReproductor() {
        mediaView.setMediaPlayer(mediaPlayer);
        mitigacionAutomaticaYaAplicada = false;

        if (MITIGACION_SEEK_ACTIVA) {
            mediaPlayer.statusProperty().addListener((obs, oldStatus, newStatus) -> {
                if (newStatus == MediaPlayer.Status.PLAYING && !mitigacionAutomaticaYaAplicada) {
                    mitigacionAutomaticaYaAplicada = true;
                    aplicarMitigacionSeek();
                }
            });
        }

        mediaPlayer.currentTimeProperty().addListener((obs, oldVal, newVal) -> {
            if (!usuarioMoviendoSlider) {
                slider.setValue(newVal.toSeconds());
                lblDuration.setText(
                    formatear(newVal.toSeconds()) + " / " +
                    formatear(media.getDuration().toSeconds())
                );
                actualizarColorSlider();
            }
        });

        double total = media.getDuration().toSeconds();
        slider.setMin(0);
        slider.setMax(total);
        slider.setValue(0);
        lblDuration.setText("00:00 / " + formatear(total));
        actualizarColorSlider();
        Platform.runLater(this::actualizarColorSlider);

        mediaPlayer.setOnEndOfMedia(() -> {
            isPlaying = false;
            iconoPlay.setContent(PATH_PLAY);
            mediaPlayer.seek(Duration.ZERO);
            mediaPlayer.pause();
            slider.setValue(0);
            actualizarColorSlider();
            mostrarOverlay();
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
            actualizarColorSlider();
        });

        slider.setOnMouseReleased(e -> {
            mediaPlayer.seek(Duration.seconds(slider.getValue()));
            usuarioMoviendoSlider = false;
            if (isPlaying) {
                mediaPlayer.play();
            }
        });
        mediaView.fitWidthProperty().bind(root.widthProperty());
        mediaView.fitHeightProperty().bind(root.heightProperty());
        mediaView.setPreserveRatio(true);
    }

    private void aplicarMitigacionSeek() {
        Duration posicionOriginal = mediaPlayer.getCurrentTime();
        Duration duracionTotal = media.getDuration();
        Duration posicionTemporal = posicionOriginal.add(SEEK_MITIGACION);
        if (duracionTotal != null && posicionTemporal.greaterThan(duracionTotal)) {
            posicionTemporal = posicionOriginal.subtract(SEEK_MITIGACION);
            if (posicionTemporal.lessThan(Duration.ZERO)) {
                posicionTemporal = Duration.ZERO;
            }
        }

        mediaPlayer.seek(posicionTemporal);

        PauseTransition espera = new PauseTransition(Duration.millis(150));
        espera.setOnFinished(e -> {
            mediaPlayer.seek(posicionOriginal);
            if (btnImagenNoCarga != null) {
                btnImagenNoCarga.setDisable(false);
            }
        });
        espera.play();
    }

    private void actualizarColorSlider() {
        Region track = (Region) slider.lookup(".track");
        if (track == null) {
            return;
        }
        double rango = slider.getMax() - slider.getMin();
        double progreso = rango <= 0 ? 0 : (slider.getValue() - slider.getMin()) / rango;
        progreso = Math.max(0, Math.min(1, progreso));

        track.setStyle(
            "-fx-background-color: linear-gradient(to right, "
            + "#E8542E 0%, #E8542E " + (progreso * 100) + "%, "
            + "#3A383F " + (progreso * 100) + "%, #3A383F 100%);"
        );
    }

    private void configurarAutoOcultamiento() {
        timerInactividad = new PauseTransition(TIEMPO_INACTIVIDAD);
        timerInactividad.setOnFinished(e -> ocultarOverlay());

        root.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            mostrarOverlay();
            timerInactividad.playFromStart();
        });
        root.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
            mostrarOverlay();
            timerInactividad.playFromStart();
        });
    }

    private void mostrarOverlay() {
        timerInactividad.playFromStart();
        if (overlayVisible) {
            return;
        }
        overlayVisible = true;
        overlayControles.setMouseTransparent(false);
        if (fadeOverlay != null) {
            fadeOverlay.stop();
        }
        fadeOverlay = new FadeTransition(DURACION_FADE, overlayControles);
        fadeOverlay.setToValue(1.0);
        fadeOverlay.play();
        if (root.getScene() != null) {
            root.getScene().setCursor(Cursor.DEFAULT);
        }
    }

    private void ocultarOverlay() {
        if (!isPlaying) {
            return;
        }
        overlayVisible = false;
        if (fadeOverlay != null) {
            fadeOverlay.stop();
        }
        fadeOverlay = new FadeTransition(DURACION_FADE, overlayControles);
        fadeOverlay.setToValue(0.0);
        fadeOverlay.setOnFinished(e -> overlayControles.setMouseTransparent(true));
        fadeOverlay.play();
        if (root.getScene() != null) {
            root.getScene().setCursor(Cursor.NONE);
        }
    }

    public void liberar() {
        if (watchdog != null) {
            watchdog.stop();
        }
        if (timerInactividad != null) {
            timerInactividad.stop();
        }
        if (mediaPlayer != null) {
            mediaPlayer.dispose();
            mediaPlayer = null;
        }
    }

    private String formatear(double segundos) {
        int s = (int) segundos;
        return String.format("%02d:%02d", s / 60, s % 60);
    }
}