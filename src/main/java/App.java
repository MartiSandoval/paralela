import java.nio.file.Paths;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import player.ReproductorController;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fx = new FXMLLoader(Main.class.getResource("/player/reproductor.fxml"));
        Scene scene = new Scene(fx.load());
        ReproductorController c = fx.getController();
        c.cargarPelicula(Paths.get("src/main/resources/peliculas/Bee-movie.mp4").toUri().toString());
        stage.setTitle("Reproductor");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
