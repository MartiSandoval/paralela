import java.util.ArrayList;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

public class CatalogoController {

    @FXML
    private FlowPane grillaPeliculas;

    @FXML
    private Label lblEstado;

    @FXML
    private VBox overlayCarga;

    @FXML
    private Label lblOverlayCarga;

    /** Escena del catalogo, guardada para poder volver a ella tras ver una pelicula. */
    private Scene escenaCatalogo;

    /** Tamano minimo original del Stage (el del catalogo), para restaurarlo al volver. */
    private double minAnchoCatalogo;
    private double minAltoCatalogo;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            escenaCatalogo = grillaPeliculas.getScene();
            minAnchoCatalogo = MainApp.getStage().getMinWidth();
            minAltoCatalogo = MainApp.getStage().getMinHeight();
        });
        cargarCatalogoEnSegundoPlano();
    }

    /**
     * Pide el catalogo al servidor en un hilo aparte (la llamada es bloqueante
     * por sockets), y vuelve al hilo de JavaFX solo para actualizar la UI.
     */
    private void cargarCatalogoEnSegundoPlano() {
        Thread hilo = new Thread(() -> {
            ArrayList<Pelicula> peliculas = ClienteRed.solicitarCatalogo();
            Platform.runLater(() -> mostrarCatalogo(peliculas));
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    private void mostrarCatalogo(ArrayList<Pelicula> peliculas) {
        if (peliculas == null || peliculas.isEmpty()) {
            lblEstado.setText("No se pudo conectar con el catalogo. Verifica que ServidorCatalogo este corriendo.");
            return;
        }

        lblEstado.setText(peliculas.size() + " peliculas disponibles");
        grillaPeliculas.getChildren().clear();
        for (Pelicula p : peliculas) {
            grillaPeliculas.getChildren().add(crearTarjeta(p));
        }
    }

    private VBox crearTarjeta(Pelicula p) {
        Label titulo = new Label(p.getTitulo());
        titulo.getStyleClass().add("tarjeta-titulo");

        Label meta = new Label(p.getAño() + " · " + String.join(", ", p.getDirector()));
        meta.getStyleClass().add("tarjeta-meta");

        Label generos = new Label(String.join(" / ", p.getGeneros()));
        generos.getStyleClass().add("tarjeta-generos");

        VBox tarjeta = new VBox(6, titulo, meta, generos);
        tarjeta.getStyleClass().add("tarjeta-pelicula");
        tarjeta.setOnMouseClicked(e -> seleccionarPelicula(p));
        return tarjeta;
    }

    private void mostrarOverlay(String texto) {
        lblOverlayCarga.setText(texto);
        overlayCarga.setVisible(true);
    }

    private void ocultarOverlay() {
        overlayCarga.setVisible(false);
    }

    /**
     * Inicia el streaming de la pelicula elegida en un hilo aparte (bloqueante),
     * y cuando el archivo termina de descargarse, intenta abrir el reproductor
     * en la misma ventana. El overlay de carga permanece visible durante toda
     * la descarga UDP y mientras el reproductor termina de inicializarse.
     */
    private void seleccionarPelicula(Pelicula p) {
        mostrarOverlay("Descargando " + p.getTitulo() + "...");

        Thread hilo = new Thread(() -> {
            String rutaDescargada = ClienteRed.iniciarStreaming(p.getPath(), null);
            Platform.runLater(() -> {
                if (rutaDescargada != null) {
                    mostrarOverlay("Preparando reproductor...");
                    prepararReproductor(rutaDescargada);
                } else {
                    ocultarOverlay();
                    lblEstado.setText("No se pudo iniciar el streaming de " + p.getTitulo());
                }
            });
        });
        hilo.setDaemon(true);
        hilo.start();
    }

    /**
     * Carga el FXML del reproductor y comienza a cargar el video. El cambio de
     * escena solo ocurre cuando el reproductor confirma que esta listo
     * (ReproductorController.setOnListo), nunca antes. Si se agotan los
     * reintentos internos del reproductor, se vuelve a mostrar el catalogo
     * con un mensaje de error en vez de quedarse en una pantalla en blanco.
     */
    private void prepararReproductor(String rutaArchivo) {
        try {
            FXMLLoader fx = new FXMLLoader(getClass().getResource("/player/reproductor.fxml"));
            Scene escenaReproductor = new Scene(fx.load());
            ReproductorController controller = fx.getController();

            controller.setOnListo(() -> {
                ocultarOverlay();
                MainApp.getStage().setMinWidth(ReproductorController.VENTANA_ANCHO_MINIMO);
                MainApp.getStage().setMinHeight(ReproductorController.VENTANA_ALTO_MINIMO);
                MainApp.getStage().setScene(escenaReproductor);
            });

            controller.setOnError(() -> {
                ocultarOverlay();
                lblEstado.setText("No se pudo cargar el video tras varios intentos.");
            });

            controller.setOnSalir(this::volverAlCatalogo);

            controller.cargarPelicula(java.nio.file.Paths.get(rutaArchivo).toUri().toString());

        } catch (Exception e) {
            ocultarOverlay();
            lblEstado.setText("Error al preparar el reproductor: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void volverAlCatalogo() {
        MainApp.getStage().setMinWidth(minAnchoCatalogo);
        MainApp.getStage().setMinHeight(minAltoCatalogo);
        MainApp.getStage().setScene(escenaCatalogo);
        lblEstado.setText(grillaPeliculas.getChildren().size() + " peliculas disponibles");
    }
}