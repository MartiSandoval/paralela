import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

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

    public static Stage getStage() {
        return stage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}