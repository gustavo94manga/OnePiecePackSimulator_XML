package com.onepiece.simulator.onepiecepacksimulator_xml.ui;

import com.onepiece.simulator.onepiecepacksimulator_xml.entities.Card;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class PackPopupOpener {

    public static void openPack(String packImageUrl, List<Card> pulledCards, Consumer<List<Card>> onPackFinished) {
        boolean isStarterDeck = pulledCards.stream().anyMatch(c -> c.idProperty().get().startsWith("ST"));

        if (isStarterDeck) {
            showStarterDeck(pulledCards, onPackFinished);
        } else {
            // Use the stable UI flow for booster packs
            Stage boosterStage = new Stage();
            boosterStage.initModality(Modality.APPLICATION_MODAL);
            showBoosterPackUI(boosterStage, packImageUrl, pulledCards, onPackFinished);
        }
    }

    private static void showBoosterPackUI(Stage stage, String packImageUrl, List<Card> pulledCards, Consumer<List<Card>> onDone) {
        stage.setTitle("Opening Pack...");
        ImageView packImageView = new ImageView(new Image(packImageUrl, 300, 420, true, true));
        packImageView.setPreserveRatio(true);

        Button openButton = new Button("Open Pack");
        openButton.setOnAction(e -> showCardsOneByOneUI(stage, pulledCards, onDone));

        VBox layout = new VBox(20, packImageView, openButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 500, 600);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static void showCardsOneByOneUI(Stage stage, List<Card> cards, Consumer<List<Card>> onDone) {
        if (cards == null || cards.isEmpty()) {
            stage.close();
            return;
        }
        stage.setTitle("Card 1 of " + cards.size());
        final int[] currentIndex = {0};
        ImageView cardView = new ImageView();
        cardView.setPreserveRatio(true);
        cardView.setFitWidth(300);
        cardView.setCursor(Cursor.HAND); // Make it look clickable

        Label instructionLabel = new Label("Click card to reveal next");

        // Function to update the image and title
        Runnable showCurrentCard = () -> {
            Card card = cards.get(currentIndex[0]);
            Image image = new Image(card.imageUrlProperty().get(), true);
            cardView.setImage(image);
            stage.setTitle("Card " + (currentIndex[0] + 1) + " of " + cards.size());
            if (currentIndex[0] == cards.size() - 1) {
                instructionLabel.setText("Click card to finish");
            }
        };

        showCurrentCard.run();

        // The click action for the image itself
        cardView.setOnMouseClicked(e -> {
            currentIndex[0]++;
            if (currentIndex[0] >= cards.size()) {
                stage.close();
                onDone.accept(cards); // This triggers the save
            } else {
                showCurrentCard.run();
            }
        });

        VBox layout = new VBox(10, cardView, instructionLabel);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 500, 600);
        stage.setScene(scene);
    }
    
    // showStarterDeck and showFullCard methods remain the same...
    private static void showStarterDeck(List<Card> cards, Consumer<List<Card>> onDone) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Starter Deck Contents");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER);

        int col = 0, row = 0;
        for (Card card : cards) {
            ImageView view = new ImageView(new Image(card.imageUrlProperty().get(), 120, 168, true, true));
            view.setOnMouseClicked(e -> showFullCard(card));

            grid.add(view, col, row);
            col++;
            if (col >= 5) {
                col = 0;
                row++;
            }
        }

        Button closeBtn = new Button("Add to Collection");
        closeBtn.setOnAction(e -> {
            stage.close();
            onDone.accept(cards);
        });

        VBox layout = new VBox(10, grid, closeBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(10));
        
        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 800, 600);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static void showFullCard(Card card) {
        Stage popup = new Stage();
        popup.initModality(Modality.APPLICATION_MODAL);
        popup.setTitle(card.nameProperty().get());

        ImageView imageView = new ImageView(new Image(card.imageUrlProperty().get(), 400, 560, true, true));
        imageView.setPreserveRatio(true);

        StackPane root = new StackPane(imageView);
        Scene scene = new Scene(root);
        popup.setScene(scene);
        popup.showAndWait();
    }
}