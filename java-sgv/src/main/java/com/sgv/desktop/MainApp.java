package com.sgv.desktop;

import com.sgv.SgvApplication;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class MainApp extends Application {

    private static ConfigurableApplicationContext context;

    public static ConfigurableApplicationContext getContext() {
        return context;
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            context = new SpringApplicationBuilder(SgvApplication.class)
                    .web(WebApplicationType.NONE)
                    .run();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/styles/styles.css").toExternalForm());
            primaryStage.setTitle("SGV Desktop");
            primaryStage.setScene(scene);
            primaryStage.setWidth(420);
            primaryStage.setHeight(640);
            primaryStage.show();
        } catch (Throwable t) {
            try {
                java.io.PrintWriter pw = new java.io.PrintWriter("app-crash.log");
                t.printStackTrace(pw);
                pw.flush();
                pw.close();
            } catch (Exception e) {}
            throw new RuntimeException(t);
        }
    }

    @Override
    public void stop() throws Exception {
        if (context != null) {
            context.close();
        }
        super.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
