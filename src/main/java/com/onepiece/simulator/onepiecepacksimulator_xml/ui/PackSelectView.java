/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.onepiece.simulator.onepiecepacksimulator_xml.ui;

/**
 *
 * @author angelgarza
 */
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.Map;
import java.util.function.Consumer;
import javafx.scene.control.Button;

public class PackSelectView {

    public static void show(Map<String, String> packLabels, Consumer<String> onPackSelected) {
        Stage window = new Stage();
        window.initModality(Modality.APPLICATION_MODAL);
        window.setTitle("Select a Pack to Open");

        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(20);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER);

        int col = 0;
        int row = 0;
        for (Map.Entry<String, String> entry : packLabels.entrySet()) {
            String setCode = entry.getKey();
            String label = entry.getValue();

            Button placeholderButton = new Button(label);
            placeholderButton.setPrefWidth(160);
            placeholderButton.setPrefHeight(80);
            placeholderButton.setWrapText(true);
            placeholderButton.setOnAction(e -> {
                window.close();
                onPackSelected.accept(setCode);
            });

            VBox vbox = new VBox(5, placeholderButton);
            vbox.setAlignment(Pos.CENTER);

            grid.add(vbox, col, row);

            col++;
            if (col > 2) {
                col = 0;
                row++;
            }
        }

        ScrollPane scrollPane = new ScrollPane(grid);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 720, 640);
        window.setScene(scene);
        window.showAndWait();
    }
}