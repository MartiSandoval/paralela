import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.stage.Stage;
import java.io.File;

public class ReproductorFX extends Application {
    
    // Variable estática para pasarle la ruta desde tu Main de consola
    public static String rutaArchivoBuffer = "";

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Cargar el archivo (puede ser el buffer que se está descargando)
            File archivoVideo = new File(rutaArchivoBuffer);
            Media media = new Media(archivoVideo.toURI().toString());

            // 2. Inicializar los componentes de JavaFX
            MediaPlayer mediaPlayer = new MediaPlayer(media);
            MediaView mediaView = new MediaView(mediaPlayer);

            // Ajustar el tamaño del reproductor
            mediaView.setFitWidth(800);
            mediaView.setFitHeight(600);

            // 3. Crear la interfaz gráfica
            StackPane root = new StackPane();
            root.getChildren().add(mediaView);
            Scene scene = new Scene(root, 800, 600);

            // 4. Configurar la ventana
            primaryStage.setTitle("Netflix Distribuido - Reproductor UDP");
            primaryStage.setScene(scene);
            
            // Al cerrar la ventana del video, detenemos la reproducción pero no matamos el cliente principal
            primaryStage.setOnCloseRequest(event -> {
                mediaPlayer.stop();
            });

            primaryStage.show();

            // ¡Play!
            mediaPlayer.play();

        } catch (Exception e) {
            System.err.println("Error al cargar JavaFX Media: " + e.getMessage());
        }
    }
    
    // Método auxiliar para lanzar la ventana desde tu clase Main
    public static void iniciar(String ruta) {
        rutaArchivoBuffer = ruta;
        // Platform.startup asegura que JavaFX inicie correctamente si es llamado desde otro hilo
        Platform.startup(() -> {
            try {
                new ReproductorFX().start(new Stage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}