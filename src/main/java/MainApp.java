import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Punto de entrada de la aplicacion. Reemplaza al par App.java/Main.java
 * original: ahora la app nace directamente en JavaFX, mostrando el catalogo
 * como pantalla inicial en lugar de un menu de consola.
 */
public class MainApp extends Application {

    private static Stage stage;

    @Override
    public void start(Stage primaryStage) throws Exception {
        MainApp.stage = primaryStage;

        FXMLLoader fx = new FXMLLoader(getClass().getResource("/player/catalogo.fxml"));
        Scene scene = new Scene(fx.load());

        primaryStage.setTitle("Streaming");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Stage principal de la aplicacion, compartido por todos los controllers
     * para que puedan cambiar el contenido de la unica ventana en vez de abrir
     * Stages nuevos.
     */
    public static Stage getStage() {
        return stage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}