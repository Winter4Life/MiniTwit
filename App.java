import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("AdminControlPanel.fxml"));
        Parent root = loader.load();

        // Set the controller for the FXML file
        @SuppressWarnings("unused")
        AdminControl controller = loader.getController();

        Scene scene = new Scene(root); 
        primaryStage.setTitle("AdminControl Panel");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}