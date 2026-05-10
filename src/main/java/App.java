import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fx = new FXMLLoader(Main.class.getResource("reproductor.fxml"));
        Scene scene = new Scene(fx.load());
        stage.setTitle("Reproductor");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
    
}
