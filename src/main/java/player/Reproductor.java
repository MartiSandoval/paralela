package player;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import player.controllers.ReproductorController;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Punto de entrada del reproductor de video.
 * Garantiza que JavaFX se lanza una sola vez y permite abrir
 * multiples ventanas secuencialmente sin relanzar la plataforma.
 */
public class Reproductor extends Application {

    // JavaFX solo puede lanzarse una vez por proceso
    private static final AtomicBoolean lanzado       = new AtomicBoolean(false);
    // Indica si hay una ventana de reproductor abierta actualmente
    private static final AtomicBoolean enUso         = new AtomicBoolean(false);
    // Latch que bloquea el hilo llamante hasta que la ventana se cierre
    private static volatile CountDownLatch latchCierre;
    // Ruta del video a reproducir (se pasa antes de abrir la ventana)
    private static volatile String archivoInicial;

    /**
     * Lanza el reproductor de forma bloqueante:
     * - El hilo llamante queda bloqueado hasta que el usuario cierre la ventana.
     * - Si ya hay una ventana abierta, retorna inmediatamente sin hacer nada.
     * - Puede llamarse multiples veces; JavaFX solo se inicializa la primera vez.
     *
     * @param rutaArchivo Ruta absoluta al archivo de video.
     */
    public static void lanzar(String rutaArchivo) {
        // Evitar dos reproductores simultaneos
        if (!enUso.compareAndSet(false, true)) {
            System.out.println("[Reproductor] Ya hay un reproductor abierto. Espera a cerrarlo.");
            return;
        }

        archivoInicial = rutaArchivo;
        latchCierre    = new CountDownLatch(1);

        if (lanzado.compareAndSet(false, true)) {
            // Primera vez: lanzar la plataforma JavaFX
            // Esto bloquea hasta que la plataforma arranca, pero NO hasta que
            // la ventana se cierra, por eso usamos el latch.
            new Thread(() -> Application.launch(Reproductor.class), "javafx-launcher").start();
        } else {
            // JavaFX ya esta corriendo: abrir nueva ventana en el hilo de JavaFX
            Platform.runLater(() -> {
                try {
                    abrirVentana();
                } catch (IOException e) {
                    System.err.println("[Reproductor] Error al abrir ventana: " + e.getMessage());
                    latchCierre.countDown();
                    enUso.set(false);
                }
            });
        }

        // Bloquear el hilo llamante hasta que la ventana se cierre
        try {
            latchCierre.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void start(Stage stage) throws IOException {
        // Evitar que JavaFX cierre la plataforma al cerrar la unica ventana
        Platform.setImplicitExit(false);
        abrirVentana();
    }

    /** Crea y muestra la ventana del reproductor. */
    private static void abrirVentana() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Reproductor.class.getResource("/player/reproductor.fxml")
        );
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                Reproductor.class.getResource("/player/styles.css").toExternalForm()
        );

        ReproductorController controller = loader.getController();
        Stage stage = new Stage();
        controller.setStage(stage);

        if (archivoInicial != null) {
            controller.cargarVideo(archivoInicial);
        }

        stage.setTitle("StreamPlayer");
        stage.setMinWidth(800);
        stage.setMinHeight(520);
        stage.setScene(scene);

        stage.setOnCloseRequest(e -> {
            controller.detener();
            enUso.set(false);          // liberar el lock
            latchCierre.countDown();   // desbloquear el hilo llamante
        });

        stage.show();
    }
}