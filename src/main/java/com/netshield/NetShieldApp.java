package com.netshield;

import com.netshield.ui.MainDashboardController;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.logging.Logger;

/**
 * NetShield - Production Desktop Application Entry Point
 * Offline-First Internet Failure Detection and Recovery Tool.
 */
public class NetShieldApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(NetShieldApp.class.getName());
    private MainDashboardController controller;

    @Override
    public void start(Stage primaryStage) {
        LOGGER.info("Starting NetShield Application...");

        controller = new MainDashboardController(primaryStage);
        BorderPane rootLayout = controller.buildMainLayout();

        Scene scene = new Scene(rootLayout, 1180, 840);

        // Attach modern dark cybersecurity stylesheet
        String cssPath = getClass().getResource("/com/netshield/css/netshield.css") != null
                ? getClass().getResource("/com/netshield/css/netshield.css").toExternalForm()
                : null;
        if (cssPath != null) {
            scene.getStylesheets().add(cssPath);
        }

        primaryStage.setTitle("NetShield — Offline-First Internet Failure Detection and Recovery Tool");
        primaryStage.setMinWidth(980);
        primaryStage.setMinHeight(680);
        primaryStage.setScene(scene);

        // Graceful clean shutdown hook
        primaryStage.setOnCloseRequest(e -> {
            LOGGER.info("Window close requested. Initiating clean shutdown...");
            controller.shutdown();
        });

        primaryStage.show();
        LOGGER.info("NetShield UI loaded and displayed.");
    }

    @Override
    public void stop() {
        if (controller != null) {
            controller.shutdown();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
