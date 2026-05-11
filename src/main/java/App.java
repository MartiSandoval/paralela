import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import player.ReproductorController;

public class App extends Application {
    private static final CountDownLatch jfxListo = new CountDownLatch(1);
    private static boolean jfxIniciado = false;

    @Override
    public void start(Stage stage) throws Exception {
        Platform.setImplicitExit(false);
        jfxListo.countDown();
    }

    public static void lanzar(String path) {
        String url = Paths.get(path).toUri().toString();
        if(!jfxIniciado) {
            jfxIniciado = true;
            Thread hilo = new Thread(() ->Application.launch(App.class));
            hilo.setDaemon(true);
            hilo.start();
            try {
                jfxListo.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        CountDownLatch ventanaCerrada = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                FXMLLoader fx = new FXMLLoader(Main.class.getResource("/player/reproductor.fxml"));
                Scene scene = new Scene(fx.load());
                ReproductorController c = fx.getController();
                c.cargarPelicula(url);
                Stage stage = new Stage();
                stage.setTitle("Reproductor");
                stage.setScene(scene);
                stage.setOnHidden(e -> ventanaCerrada.countDown());
                stage.show();
            } catch (Exception e) {
                System.err.println("Error al abrir el reproductor: " + e.getMessage());
                e.printStackTrace();
                ventanaCerrada.countDown();
            }
        });

        try {
            ventanaCerrada.await();
        } catch (Exception e) {
            Thread.currentThread().interrupt();
        }
    }
}
