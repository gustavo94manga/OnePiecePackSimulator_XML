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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class PackPopupOpener {

    public static void openPack(String packImageUrl, List<Card> pulledCards, Consumer<List<Card>> onPackFinished) {
        boolean isStarterDeck = pulledCards.stream()
                .anyMatch(c -> c.idProperty().get().startsWith("ST"));

        if (isStarterDeck) {
            showAllCardsInGrid(pulledCards, onPackFinished);
        } else {
            // This is the stable, corrected flow for booster packs
            boolean userClickedOpen = showBoosterPackUI(packImageUrl);
            if (userClickedOpen) {
                // Only if the user clicked "Open", we proceed to show the next window.
                showCardsOneByOneUI(pulledCards, onPackFinished);
            }
        }
    }

    /**
     * Shows the pack art. This method now returns 'true' if the user clicks "Open Pack".
     * This is a more robust way to handle the UI flow.
     */
    private static boolean showBoosterPackUI(String packImageUrl) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Opening Pack...");

        AtomicBoolean openClicked = new AtomicBoolean(false);

        ImageView packImageView = new ImageView(new Image(packImageUrl, 300, 420, true, true));
        packImageView.setPreserveRatio(true);

        Button openButton = new Button("Open Pack");
        openButton.setOnAction(e -> {
            openClicked.set(true);
            stage.close();
        });

        VBox layout = new VBox(20, packImageView, openButton);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 500, 600);
        stage.setScene(scene);
        stage.showAndWait(); // This blocks until the window is closed

        return openClicked.get();
    }

    /**
     * Shows cards one by one in a new, separate window.
     * This is guaranteed to display correctly.
     */
    private static void showCardsOneByOneUI(List<Card> cards, Consumer<List<Card>> onDone) {
        if (cards == null || cards.isEmpty()) {
            return;
        }

        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Card 1 of " + cards.size());

        final int[] currentIndex = {0};

        ImageView cardView = new ImageView();
        cardView.setPreserveRatio(true);
        cardView.setFitWidth(300);
        cardView.setCursor(Cursor.HAND);

        Label instructionLabel = new Label("Click card to reveal next");

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

        cardView.setOnMouseClicked(e -> {
            currentIndex[0]++;
            if (currentIndex[0] >= cards.size()) {
                stage.close();
                onDone.accept(cards);
            } else {
                showCurrentCard.run();
            }
        });

        VBox layout = new VBox(10, cardView, instructionLabel);
        layout.setAlignment(Pos.CENTER);
        Scene scene = new Scene(layout, 500, 600);
        stage.setScene(scene);
        stage.showAndWait(); // Use showAndWait to keep focus
    }

    // Starter Deck and Full Card view methods are unchanged and stable
    private static void showAllCardsInGrid(List<Card> cards, Consumer<List<Card>> onDone) {
        Stage stage = new Stage();
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setTitle("Starter Deck Contents");
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.setAlignment(Pos.CENTER);
        int columns = 5;
        int col = 0, row = 0;
        for (Card card : cards) {
            ImageView view = new ImageView(new Image(card.imageUrlProperty().get(), 120, 168, true, true));
            view.setCursor(Cursor.HAND);
            view.setOnMouseClicked(e -> showFullCard(card));
            grid.add(view, col, row);
            col++;
            if (col >= columns) {
                col = 0;
                row++;
            }
        }
        Button closeBtn = new Button("Add to Collection");
        closeBtn.setOnAction(e -> {
            stage.close();
            onDone.accept(cards);
        });
        VBox layout = new VBox(15, grid, closeBtn);
        layout.setAlignment(Pos.CENTER);
        layout.setPadding(new Insets(10));
        ScrollPane scrollPane = new ScrollPane(layout);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        Scene scene = new Scene(scrollPane, 800, 700);
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