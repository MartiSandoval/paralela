package player.controllers;

import com.sun.jna.NativeLibrary;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import uk.co.caprica.vlcj.binding.support.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.javafx.videosurface.ImageViewVideoSurface;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.base.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ReproductorController implements Initializable {

    // ── FXML ────────────────────────────────────────────────────────────────
    @FXML private StackPane rootPane;
    @FXML private ImageView videoView;
    @FXML private VBox      controlesBox;
    @FXML private Label     lblTitulo;
    @FXML private Label     lblTiempoActual;
    @FXML private Label     lblTiempoTotal;
    @FXML private Slider    sliderProgreso;
    @FXML private Slider    sliderVolumen;
    @FXML private Button    btnPlayPause;
    @FXML private Button    btnDetener;
    @FXML private Button    btnAbrir;
    @FXML private Button    btnMute;
    @FXML private Button    btnPantallaCompleta;
    @FXML private VBox      barraControles;
    @FXML private ProgressBar bufferingBar;
    @FXML private Label     lblEstado;

    // ── VLCJ ────────────────────────────────────────────────────────────────
    private MediaPlayerFactory   factory;
    private EmbeddedMediaPlayer  mediaPlayer;
    private boolean              vlcDisponible = false;

    // ── Estado ──────────────────────────────────────────────────────────────
    private Stage   stage;
    private boolean arrastrandoProgreso = false;
    private boolean muteado             = false;
    private boolean pantallaCompleta    = false;

    // ── Controles visibles/ocultos ──────────────────────────────────────────
    // true  = controles visibles de forma fija (toggle por click)
    // false = controles en modo auto (aparecen con movimiento, desaparecen solos)
    private boolean controlesAnclados   = false;
    private PauseTransition  tempOcultar;
    private FadeTransition   fadeIn;
    private FadeTransition   fadeOut;

    // ── Iconos ───────────────────────────────────────────────────────────────
    private static final String ICONO_PLAY  = "\u25B6";
    private static final String ICONO_PAUSE = "\u23F8";
    private static final String ICONO_MUTE  = "\uD83D\uDD07";
    private static final String ICONO_VOL   = "\uD83D\uDD0A";

    // ── Rutas candidatas VLC ─────────────────────────────────────────────────
    private static final List<String> RUTAS_WIN = List.of(
            "C:\\Program Files\\VideoLAN\\VLC",
            "C:\\Program Files (x86)\\VideoLAN\\VLC",
            System.getenv("LOCALAPPDATA") != null
                    ? System.getenv("LOCALAPPDATA") + "\\Programs\\VideoLAN\\VLC" : ""
    );
    private static final List<String> RUTAS_LINUX = List.of(
            "/usr/lib", "/usr/lib/x86_64-linux-gnu",
            "/usr/lib/aarch64-linux-gnu", "/usr/local/lib"
    );
    private static final List<String> RUTAS_MAC = List.of(
            "/Applications/VLC.app/Contents/MacOS/lib", "/usr/local/lib"
    );

    // ════════════════════════════════════════════════════════════════════════
    //  INICIALIZACION
    // ════════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarAnimacionesControles();
        configurarEventosRaton();

        vlcDisponible = inicializarVLCJ();

        if (vlcDisponible) {
            configurarSliderProgreso();
            configurarSliderVolumen();
            configurarVideoView();
        } else {
            deshabilitarControles();
            mostrarDialogoVLCNoEncontrado();
        }
    }

    // ── Animaciones de los controles ─────────────────────────────────────────

    private void configurarAnimacionesControles() {
        fadeIn = new FadeTransition(Duration.millis(200), controlesBox);
        fadeIn.setToValue(1.0);

        fadeOut = new FadeTransition(Duration.millis(400), controlesBox);
        fadeOut.setToValue(0.0);

        // Timer que oculta los controles 2.5s despues del ultimo movimiento
        tempOcultar = new PauseTransition(Duration.millis(2500));
        tempOcultar.setOnFinished(e -> {
            if (!controlesAnclados && vlcDisponible && mediaPlayer.status().isPlaying()) {
                fadeOut.playFromStart();
            }
        });

        // Empezar invisibles
        controlesBox.setOpacity(0.0);
    }

    private void configurarEventosRaton() {
        // Movimiento del raton: mostrar controles y reiniciar el timer de ocultamiento
        rootPane.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (!controlesAnclados) mostrarControlesTemp();
        });

        // Click en el area de video (fuera de los controles): alternar anclar/desanclar
        rootPane.addEventFilter(MouseEvent.MOUSE_CLICKED, e -> {
            // Si el click fue sobre los propios controles, no alternar
            if (controlesBox.contains(controlesBox.sceneToLocal(e.getSceneX(), e.getSceneY()))) return;

            controlesAnclados = !controlesAnclados;
            if (controlesAnclados) {
                tempOcultar.stop();
                fadeIn.playFromStart();
            } else {
                mostrarControlesTemp();
            }
        });

        // Al salir el raton de la ventana, ocultar si no estan anclados
        rootPane.setOnMouseExited(e -> {
            if (!controlesAnclados) {
                tempOcultar.stop();
                fadeOut.playFromStart();
            }
        });
    }

    /** Muestra los controles y programa su ocultamiento automatico. */
    private void mostrarControlesTemp() {
        fadeOut.stop();
        fadeIn.playFromStart();
        tempOcultar.playFromStart(); // reinicia el countdown
    }

    // ── VLCJ ─────────────────────────────────────────────────────────────────

    private boolean inicializarVLCJ() {
        String rutaExplicita = System.getProperty("VLC_HOME");
        if (rutaExplicita == null || rutaExplicita.isBlank())
            rutaExplicita = System.getenv("VLC_HOME");

        if (rutaExplicita != null && !rutaExplicita.isBlank()) {
            if (intentarCargar(rutaExplicita)) return true;
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        List<String> candidatas = os.contains("win") ? RUTAS_WIN
                                : os.contains("mac") ? RUTAS_MAC
                                : RUTAS_LINUX;

        for (String ruta : candidatas) {
            if (ruta != null && !ruta.isBlank() && intentarCargar(ruta)) {
                System.out.println("[Reproductor] libVLC cargada desde: " + ruta);
                return true;
            }
        }
        System.err.println("[Reproductor] No se encontro VLC.");
        return false;
    }

    private boolean intentarCargar(String ruta) {
        try {
            NativeLibrary.addSearchPath(RuntimeUtil.getLibVlcLibraryName(), ruta);
            factory     = new MediaPlayerFactory();
            mediaPlayer = factory.mediaPlayers().newEmbeddedMediaPlayer();
            mediaPlayer.videoSurface().set(new ImageViewVideoSurface(videoView));
            registrarEventosVLCJ();
            return true;
        } catch (Throwable t) {
            factory     = null;
            mediaPlayer = null;
            return false;
        }
    }

    private void registrarEventosVLCJ() {
        mediaPlayer.events().addMediaPlayerEventListener(new MediaPlayerEventAdapter() {

            @Override public void playing(MediaPlayer mp) {
                Platform.runLater(() -> {
                    btnPlayPause.setText(ICONO_PAUSE);
                    setEstado("Reproduciendo");
                    bufferingBar.setVisible(false);
                    actualizarTiempoTotal();
                });
            }

            @Override public void paused(MediaPlayer mp) {
                Platform.runLater(() -> {
                    btnPlayPause.setText(ICONO_PLAY);
                    setEstado("Pausado");
                    // Al pausar, mostrar controles y anclarlos
                    controlesAnclados = true;
                    fadeIn.playFromStart();
                });
            }

            @Override public void stopped(MediaPlayer mp) {
                Platform.runLater(() -> {
                    btnPlayPause.setText(ICONO_PLAY);
                    sliderProgreso.setValue(0);
                    lblTiempoActual.setText("00:00:00");
                    setEstado("Detenido");
                    controlesAnclados = true;
                    fadeIn.playFromStart();
                });
            }

            @Override public void finished(MediaPlayer mp) {
                Platform.runLater(() -> {
                    btnPlayPause.setText(ICONO_PLAY);
                    sliderProgreso.setValue(0);
                    setEstado("Finalizado");
                    controlesAnclados = true;
                    fadeIn.playFromStart();
                });
            }

            @Override public void buffering(MediaPlayer mp, float pct) {
                Platform.runLater(() -> {
                    if (pct < 100f) {
                        bufferingBar.setVisible(true);
                        bufferingBar.setProgress(pct / 100.0);
                        setEstado("Cargando " + (int) pct + "%");
                    } else {
                        bufferingBar.setVisible(false);
                    }
                });
            }

            @Override public void timeChanged(MediaPlayer mp, long nuevoTiempo) {
                Platform.runLater(() -> {
                    if (!arrastrandoProgreso) {
                        long duracion = mediaPlayer.media().info().duration();
                        if (duracion > 0)
                            sliderProgreso.setValue((double) nuevoTiempo / duracion * 100.0);
                        lblTiempoActual.setText(formatearTiempo(nuevoTiempo));
                    }
                });
            }

            @Override public void error(MediaPlayer mp) {
                Platform.runLater(() -> setEstado("Error al cargar el medio"));
            }
        });
    }

    // ── Sliders y video view ─────────────────────────────────────────────────

    private void configurarSliderProgreso() {
        sliderProgreso.setMin(0);
        sliderProgreso.setMax(100);
        sliderProgreso.setValue(0);

        sliderProgreso.addEventHandler(MouseEvent.MOUSE_PRESSED,  e -> arrastrandoProgreso = true);
        sliderProgreso.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> {
            arrastrandoProgreso = false;
            long duracion = mediaPlayer.media().info().duration();
            if (duracion > 0)
                mediaPlayer.controls().setTime((long)(sliderProgreso.getValue() / 100.0 * duracion));
        });
    }

    private void configurarSliderVolumen() {
        sliderVolumen.setMin(0);
        sliderVolumen.setMax(100);
        sliderVolumen.setValue(80);
        mediaPlayer.audio().setVolume(80);

        sliderVolumen.valueProperty().addListener((obs, oldVal, newVal) -> {
            int vol = newVal.intValue();
            mediaPlayer.audio().setVolume(vol);
            muteado = (vol == 0);
            btnMute.setText(muteado ? ICONO_MUTE : ICONO_VOL);
        });
    }

    private void configurarVideoView() {
        videoView.fitWidthProperty().bind(rootPane.widthProperty());
        videoView.fitHeightProperty().bind(rootPane.heightProperty());
        videoView.setPreserveRatio(true);
    }

    private void deshabilitarControles() {
        btnPlayPause.setDisable(true);
        btnDetener.setDisable(true);
        sliderProgreso.setDisable(true);
        sliderVolumen.setDisable(true);
        btnMute.setDisable(true);
        controlesBox.setOpacity(1.0); // mostrar aunque este deshabilitado
        setEstado("VLC no disponible");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  API PUBLICA
    // ════════════════════════════════════════════════════════════════════════

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void cargarVideo(String ruta) {
        if (!vlcDisponible || ruta == null || ruta.isBlank()) return;

        String titulo = new File(ruta).getName();
        lblTitulo.setText(titulo);
        if (stage != null) stage.setTitle("StreamPlayer - " + titulo);

        bufferingBar.setVisible(true);
        bufferingBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);

        // Al cargar video, desanclar controles para que se oculten solos
        controlesAnclados = false;
        mediaPlayer.media().play(ruta);
    }

    public void detener() {
        if (mediaPlayer != null) {
            mediaPlayer.controls().stop();
            mediaPlayer.release();
        }
        if (factory != null) factory.release();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MANEJADORES FXML
    // ════════════════════════════════════════════════════════════════════════

    @FXML private void onPlayPause() {
        if (!vlcDisponible) return;
        if (mediaPlayer.status().isPlaying()) {
            mediaPlayer.controls().pause();
        } else {
            // Al reanudar, desanclar para que los controles se vuelvan a ocultar solos
            controlesAnclados = false;
            mediaPlayer.controls().play();
        }
    }

    @FXML private void onDetener()    { if (vlcDisponible) mediaPlayer.controls().stop(); }
    @FXML private void onRetroceder() { if (vlcDisponible) mediaPlayer.controls().skipTime(-10_000); }
    @FXML private void onAdelantar()  { if (vlcDisponible) mediaPlayer.controls().skipTime( 10_000); }

    @FXML private void onMute() {
        if (!vlcDisponible) return;
        muteado = !muteado;
        mediaPlayer.audio().setMute(muteado);
        btnMute.setText(muteado ? ICONO_MUTE : ICONO_VOL);
    }

    @FXML private void onPantallaCompleta() {
        if (stage == null) return;
        pantallaCompleta = !pantallaCompleta;
        stage.setFullScreen(pantallaCompleta);
    }

    @FXML private void onAbrir() {
        if (!vlcDisponible) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Abrir archivo de video");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Video",
                        "*.mp4","*.mkv","*.avi","*.mov","*.wmv","*.flv","*.webm","*.m4v"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*")
        );
        File archivo = fc.showOpenDialog(stage);
        if (archivo != null) cargarVideo(archivo.getAbsolutePath());
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ════════════════════════════════════════════════════════════════════════

    private void actualizarTiempoTotal() {
        lblTiempoTotal.setText(formatearTiempo(mediaPlayer.media().info().duration()));
    }

    private String formatearTiempo(long ms) {
        if (ms < 0) ms = 0;
        long s = ms / 1000;
        return String.format("%02d:%02d:%02d", s / 3600, (s / 60) % 60, s % 60);
    }

    private void setEstado(String msg) { lblEstado.setText(msg); }

    private void mostrarDialogoVLCNoEncontrado() {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("VLC no encontrado");
            alert.setHeaderText("No se pudo cargar libVLC");
            alert.setContentText(
                "El reproductor requiere VLC Media Player instalado.\n\n" +
                "Opciones:\n" +
                "  1. Instalar VLC desde videolan.org\n" +
                "  2. Si esta en una ruta personalizada, define:\n" +
                "     VLC_HOME=C:\\ruta\\a\\VLC\n\n" +
                "Luego reinicia la aplicacion."
            );
            ButtonType btnDescargar = new ButtonType("Descargar VLC");
            ButtonType btnCerrar    = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(btnDescargar, btnCerrar);
            alert.showAndWait().ifPresent(r -> {
                if (r == btnDescargar) {
                    try { Desktop.getDesktop().browse(new URI("https://www.videolan.org/vlc/")); }
                    catch (Exception ignored) {}
                }
            });
        });
    }
}