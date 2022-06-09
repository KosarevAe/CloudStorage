package ru.geekbrains.netty.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainClient extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/main_window.fxml"));
        Parent root = fxmlLoader.load();
        primaryStage.setTitle("Client");
        primaryStage.setScene(new Scene(root, 1280, 1024));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
