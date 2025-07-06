package com.onepiece.simulator.onepiecepacksimulator_xml;

import com.onepiece.simulator.onepiecepacksimulator_xml.data.CardStorage;
import com.onepiece.simulator.onepiecepacksimulator_xml.entities.Card;
import com.onepiece.simulator.onepiecepacksimulator_xml.entities.CardLoader;
import com.onepiece.simulator.onepiecepacksimulator_xml.ui.PackPopupOpener;
import com.onepiece.simulator.onepiecepacksimulator_xml.ui.PackSelectView;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.*;
import java.util.stream.Collectors;

public class OnePieceApp extends Application {

    private ObservableList<Card> allCards;
    private TableView<Card> tableView;
    private ComboBox<String> setSelector;
    private FilteredList<Card> filteredCards;
    private final Map<String, Image> imageCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > 200;
        }
    };

    @Override
    public void start(Stage primaryStage) {
        // --- UI COMPONENTS (Create them all upfront with no data) ---
        tableView = createTableView();
        setSelector = createSetSelector(); // Now creates an empty ComboBox
        Button openPackButton = new Button("Open Pack");
        Button resetSetButton = new Button("Reset This Set");
        CheckBox missingOnlyCheckbox = new CheckBox("Show Only Missing Cards");

        // Disable controls until data is loaded
        setSelector.setDisable(true);
        openPackButton.setDisable(true);
        resetSetButton.setDisable(true);
        missingOnlyCheckbox.setDisable(true);

        // --- LAYOUT ---
        HBox controls = new HBox(10, setSelector, openPackButton, resetSetButton, missingOnlyCheckbox);
        controls.setPadding(new Insets(10));

        ProgressIndicator loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(100, 100);

        BorderPane root = new BorderPane();
        root.setTop(controls);
        root.setCenter(loadingIndicator);

        // --- BACKGROUND DATA LOADING TASK ---
        Task<List<Card>> loadCardsTask = new Task<>() {
            @Override
            protected List<Card> call() {
                System.out.println("Loading cards in background...");
                List<Card> loadedCards = CardLoader.loadCards("/OnePieceCards.xml");
                System.out.println("Card loading complete.");
                return loadedCards;
            }
        };

        // --- ACTIONS AFTER TASK COMPLETES ---
        loadCardsTask.setOnSucceeded(e -> {
            allCards = FXCollections.observableArrayList(loadCardsTask.getValue());
            CardStorage.loadProgress(allCards);
            
            filteredCards = new FilteredList<>(allCards, p -> true);
            tableView.setItems(filteredCards);

            // **FIX**: Populate the set selector now that allCards exists
            Set<String> setNames = allCards.stream()
                    .map(card -> card.seriesNameProperty().get())
                    .collect(Collectors.toCollection(TreeSet::new));
            List<String> sortedSets = new ArrayList<>(setNames);
            sortedSets.add(0, "All Sets");
            setSelector.setItems(FXCollections.observableArrayList(sortedSets));
            setSelector.setValue("All Sets");

            // Re-enable the UI
            root.setCenter(tableView);
            setSelector.setDisable(false);
            openPackButton.setDisable(false);
            resetSetButton.setDisable(false);
            missingOnlyCheckbox.setDisable(false);
            System.out.println("UI updated with loaded cards.");
            
            // Wire up event handlers that need the data
            setSelector.setOnAction(event -> filterBySet());
            resetSetButton.setOnAction(event -> confirmReset());
            missingOnlyCheckbox.setOnAction(event -> applyMissingFilter(missingOnlyCheckbox.isSelected()));
            openPackButton.setOnAction(event -> openPackAction());
        });

        loadCardsTask.setOnFailed(e -> {
            Throwable error = loadCardsTask.getException();
            System.err.println("Failed to load cards:");
            error.printStackTrace();
            root.setCenter(new Label("Error: Could not load card data. Check logs for details."));
        });

        // --- SHOW THE STAGE & START THE TASK ---
        Scene scene = new Scene(root, 1200, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("One Piece Pack Simulator");
        primaryStage.setOnCloseRequest(e -> {
            if (allCards != null) {
                CardStorage.saveProgress(allCards);
            }
        });
        primaryStage.show();

        new Thread(loadCardsTask).start();
    }
    
    // **FIX**: This method now just creates an empty ComboBox
    private ComboBox<String> createSetSelector() {
        ComboBox<String> comboBox = new ComboBox<>();
        comboBox.setPromptText("Loading Sets...");
        return comboBox;
    }
    
    // **NEW**: Extracted the openPack button's logic into its own method
    private void openPackAction() {
        Map<String, String> packLabels = new TreeMap<>();
        for (Card card : allCards) {
            String fullName = card.seriesNameProperty().get();
            if (!packLabels.containsKey(fullName)) {
                String code = fullName.replaceAll(".*\\[(.*)]", "$1").trim();
                String label = code + "\n" + fullName.replaceAll("-\\s*\\[.*]", "").trim();
                packLabels.put(code, label);
            }
        }
        
        PackSelectView.show(packLabels, selectedCode -> {
            List<Card> cardPool = allCards.stream()
                    .filter(card -> card.seriesNameProperty().get().replace(" ", "")
                            .contains("[" + selectedCode + "]"))
                    .toList();
    
            if (cardPool.isEmpty()) {
                new Alert(Alert.AlertType.WARNING, "No cards found for set: " + selectedCode).showAndWait();
                return;
            }
    
            List<Card> pulledCards = new ArrayList<>();
            boolean isStarterDeck = selectedCode.startsWith("ST-");
    
            if (isStarterDeck) {
                pulledCards.addAll(cardPool);
            } else {
                Random random = new Random();
                for (int i = 0; i < 12 && !cardPool.isEmpty(); i++) {
                    pulledCards.add(cardPool.get(random.nextInt(cardPool.size())));
                }
            }
    
            String packImageUrl = "https://cdn.onepiece-cardgame.com/images/pack/thumbnail_OP-05.png";
            PackPopupOpener.openPack(packImageUrl, pulledCards, cards -> {
                for (Card c : cards) {
                    c.incrementQuantity();
                }
                tableView.refresh();
            });
        });
    }

    private TableView<Card> createTableView() {
        TableView<Card> table = new TableView<>();
        // This is fine, as table items will be set later.
        TableColumn<Card, String> imageCol = new TableColumn<>("Card Image");
        imageCol.setCellValueFactory(data -> data.getValue().imageUrlProperty());
        imageCol.setCellFactory(col -> new TableCell<>() {
            private final ImageView imageView = new ImageView();
            private final StackPane clickablePane = new StackPane(imageView);
            private String currentImageUrl = null;

            {
                imageView.setFitHeight(90);
                imageView.setFitWidth(60);
                imageView.setPreserveRatio(true);
                imageView.setCache(true);

                clickablePane.setOnMouseClicked(event -> {
                    if (currentImageUrl != null && !currentImageUrl.isEmpty()) {
                        Image fullImage = new Image(currentImageUrl, true);
                        ImageView expandedView = new ImageView(fullImage);
                        expandedView.setPreserveRatio(true);
                        expandedView.setFitWidth(400);

                        StackPane pane = new StackPane(expandedView);
                        Scene scene = new Scene(pane, 420, 600);

                        Stage popup = new Stage();
                        popup.setTitle("Card View");
                        popup.setScene(scene);
                        popup.initModality(Modality.APPLICATION_MODAL);
                        popup.showAndWait();
                    }
                });
            }

            @Override
            protected void updateItem(String imageUrl, boolean empty) {
                super.updateItem(imageUrl, empty);
                if (empty || imageUrl == null || imageUrl.isEmpty()) {
                    setGraphic(null);
                    currentImageUrl = null;
                } else {
                    currentImageUrl = imageUrl;
                    Image image = imageCache.get(imageUrl);
                    if (image == null) {
                        image = new Image(imageUrl, 60, 90, true, true, true);
                        imageCache.put(imageUrl, image);
                    }
                    imageView.setImage(image);
                    setGraphic(clickablePane);
                }
            }
        });

        TableColumn<Card, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(data -> data.getValue().nameProperty());
        TableColumn<Card, String> rarityCol = new TableColumn<>("Rarity");
        rarityCol.setCellValueFactory(data -> data.getValue().rarityProperty());
        TableColumn<Card, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> data.getValue().typeProperty());
        TableColumn<Card, String> colorCol = new TableColumn<>("Color");
        colorCol.setCellValueFactory(data -> data.getValue().colorProperty());
        TableColumn<Card, Number> quantityCol = new TableColumn<>("Quantity Owned");
        quantityCol.setCellValueFactory(data -> data.getValue().quantityOwnedProperty());

        table.getColumns().setAll(List.of(imageCol, nameCol, rarityCol, typeCol, colorCol, quantityCol));
        return table;
    }

    private void filterBySet() {
        String selectedSet = setSelector.getValue();
        if (selectedSet == null || "All Sets".equals(selectedSet)) {
            filteredCards.setPredicate(card -> true);
        } else {
            filteredCards.setPredicate(card -> card.seriesNameProperty().get().equals(selectedSet));
        }
    }

    private void applyMissingFilter(boolean missingOnly) {
        String selectedSet = setSelector.getValue();
        if (selectedSet == null) return;
        
        filteredCards.setPredicate(card -> {
            boolean inSet = "All Sets".equals(selectedSet) || card.seriesNameProperty().get().equals(selectedSet);
            boolean isMissing = card.quantityOwnedProperty().get() == 0;
            return missingOnly ? inSet && isMissing : inSet;
        });
    }
    
    private void confirmReset() {
        String selectedSet = setSelector.getValue();
        if (selectedSet == null || "All Sets".equals(selectedSet)) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, 
            "Are you sure? This will reset all cards in this set to 0.", 
            new ButtonType("Yes, Reset"), 
            ButtonType.CANCEL);
        alert.setTitle("Reset Confirmation");
        alert.setHeaderText("Reset collection for set: " + selectedSet);

        alert.showAndWait().ifPresent(response -> {
            if (response.getText().equals("Yes, Reset")) {
                allCards.stream()
                        .filter(card -> card.seriesNameProperty().get().equals(selectedSet))
                        .forEach(Card::resetQuantity);
                tableView.refresh();
            }
        });
    }

    public static void main(String[] args) {
        launch(args);
    }
}