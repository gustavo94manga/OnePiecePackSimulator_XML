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
    private boolean cardsHaveBeenLoaded = false; // Flag to control the one-time load
    private final Map<String, Image> imageCache = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
            return size() > 200;
        }
    };

    @Override
    public void start(Stage primaryStage) {
        // --- FAST STARTUP ---
        allCards = FXCollections.observableArrayList();
        filteredCards = new FilteredList<>(allCards, p -> true);

        // --- UI COMPONENTS ---
        tableView = createTableView();
        tableView.setItems(filteredCards);
        tableView.setPlaceholder(new Label("Please select a set from the dropdown menu to display cards."));

        setSelector = createSetSelector(); // Uses a hardcoded list for speed
        Button openPackButton = new Button("Open Pack");
        Button resetSetButton = new Button("Reset This Set");
        CheckBox missingOnlyCheckbox = new CheckBox("Show Only Missing Cards");

        // --- UI ACTIONS ---
        setSelector.setOnAction(e -> handleSetSelection());
        resetSetButton.setOnAction(e -> confirmReset());
        missingOnlyCheckbox.setOnAction(e -> applyMissingFilter(missingOnlyCheckbox.isSelected()));
        openPackButton.setOnAction(e -> openPackAction());

        // --- LAYOUT ---
        HBox controls = new HBox(10, setSelector, openPackButton, resetSetButton, missingOnlyCheckbox);
        controls.setPadding(new Insets(10));
        BorderPane root = new BorderPane(tableView);
        root.setTop(controls);

        // --- SHOW THE STAGE ---
        Scene scene = new Scene(root, 1200, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("One Piece Pack Simulator");
        primaryStage.setOnCloseRequest(e -> {
            if (allCards != null && !allCards.isEmpty()) {
                CardStorage.saveProgress(allCards);
            }
        });
        primaryStage.show();
    }

    /**
     * This method handles the logic for loading all cards into memory ONCE,
     * the very first time a set is selected.
     */
    private void handleSetSelection() {
        if (!cardsHaveBeenLoaded) {
            System.out.println("First-time selection detected. Lazy loading all card data into memory...");
            List<Card> loadedCards = CardLoader.loadCards("/OnePieceCards.xml");
            if (loadedCards.isEmpty()) {
                new Alert(Alert.AlertType.ERROR, "Failed to load card data. The application cannot proceed.").showAndWait();
                return;
            }
            allCards.setAll(loadedCards);
            CardStorage.loadProgress(allCards);
            cardsHaveBeenLoaded = true;
            System.out.println("Lazy loading complete. " + allCards.size() + " cards are now ready.");
        }
        // After the one-time load, this just filters the view, which is instantaneous.
        filterBySet();
    }

    /**
     * Filters the already-loaded card list based on the dropdown selection.
     */
    private void filterBySet() {
        String selectedSet = setSelector.getValue();
        if (selectedSet == null) return;
        
        filteredCards.setPredicate(card -> card.seriesNameProperty().get().equals(selectedSet));
    }

    private ComboBox<String> createSetSelector() {
        List<String> setNames = List.of(
            "ROMANCE DAWN- [OP-01]", "PARAMOUNT WAR- [OP-02]", 
            "PILLARS OF STRENGTH- [OP-03]", "KINGDOMS OF INTRIGUE- [OP-04]", 
            "AWAKENING OF THE NEW ERA- [OP-05]", "WINGS OF THE CAPTAIN- [OP-06]", 
            "500 YEARS IN THE FUTURE- [OP-07]", "TWO LEGENDS- [OP-08]", 
            "Starter Deck ANIMAL KINGDOM PIRATES- [ST-04]", "Starter Deck ONE PIECE FILM RED- [ST-05]", 
            "Starter Deck THE SEVEN WARLORDS OF THE SEA- [ST-03]", "Starter Deck WORST GENERATION- [ST-02]", 
            "Starter Deck STRAW HAT CREW- [ST-01]", "Starter Deck ZORO & SANJI- [ST-12]", 
            "Starter Deck THE THREE CAPTAINS- [ST-10]", "Starter Deck YAMATO- [ST-09]", 
            "Starter Deck BIG MOM PIRATES- [ST-07]", "Starter Deck NAVY- [ST-06]", 
            "Starter Deck UTA- [ST-11]", "Starter Deck LUFFY- [ST-08]"
        );
        
        List<String> sortedSetNames = setNames.stream().sorted().collect(Collectors.toList());
        
        ComboBox<String> comboBox = new ComboBox<>(FXCollections.observableArrayList(sortedSetNames));
        comboBox.setPromptText("Select a Set to Load Cards");
        return comboBox;
    }

    private void openPackAction() {
        if (!cardsHaveBeenLoaded) {
            new Alert(Alert.AlertType.INFORMATION, "Please select a set from the dropdown to load the card database first.").showAndWait();
            return;
        }
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
            List<Card> cardPool = allCards.stream().filter(card -> card.seriesNameProperty().get().replace(" ", "").contains("[" + selectedCode + "]")).toList();
            if (cardPool.isEmpty()) { new Alert(Alert.AlertType.WARNING, "No cards found for set: " + selectedCode).showAndWait(); return; }
            List<Card> pulledCards = new ArrayList<>();
            boolean isStarterDeck = selectedCode.startsWith("ST-");
            if (isStarterDeck) { pulledCards.addAll(cardPool); } else {
                Random random = new Random();
                for (int i = 0; i < 12 && !cardPool.isEmpty(); i++) { pulledCards.add(cardPool.get(random.nextInt(cardPool.size()))); }
            }
            PackPopupOpener.openPack("https://cdn.onepiece-cardgame.com/images/pack/thumbnail_OP-05.png", pulledCards, cards -> {
                for (Card c : cards) { c.incrementQuantity(); }
                tableView.refresh();
            });
        });
    }

    private void applyMissingFilter(boolean missingOnly) {
        if (!cardsHaveBeenLoaded) return;
        String selectedSet = setSelector.getValue();
        if (selectedSet == null) return;
        filteredCards.setPredicate(card -> {
            boolean inSet = card.seriesNameProperty().get().equals(selectedSet);
            boolean isMissing = card.quantityOwnedProperty().get() == 0;
            return missingOnly ? inSet && isMissing : inSet;
        });
    }
    
    private void confirmReset() {
        if (!cardsHaveBeenLoaded) return;
        String selectedSet = setSelector.getValue();
        if (selectedSet == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure? This will reset all cards in this set to 0.", new ButtonType("Yes, Reset"), ButtonType.CANCEL);
        alert.setHeaderText("Reset collection for set: " + selectedSet);
        alert.showAndWait().ifPresent(response -> {
            if (response.getText().equals("Yes, Reset")) {
                allCards.stream().filter(card -> card.seriesNameProperty().get().equals(selectedSet)).forEach(Card::resetQuantity);
                tableView.refresh();
            }
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

    public static void main(String[] args) {
        launch(args);
    }
}