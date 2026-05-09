package player;

import java.io.IOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Reproductor extends Application {
    private static Scene scene;

    @Override
    public void start(Stage stage) throws IOException {
        scene = new Scene(loadFXML("testWindow"));
        stage.setScene(scene);
        stage.show();
    }
    
    private static Parent loadFXML(String fxml) throws IOException {

    System.out.println(
        Reproductor.class.getResource(fxml + ".fxml")
    );

    FXMLLoader fxmlLoader =
        new FXMLLoader(Reproductor.class.getResource(fxml + ".fxml"));

    return fxmlLoader.load();
}

    public static void main(String[] args) {
        launch();
    }
}