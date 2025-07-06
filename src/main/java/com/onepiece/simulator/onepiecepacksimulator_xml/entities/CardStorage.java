// The package for this file should be 'com.onepiece.simulator.onepiecepacksimulator_xml.data'
package com.onepiece.simulator.onepiecepacksimulator_xml.data;

import com.onepiece.simulator.onepiecepacksimulator_xml.entities.Card;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.collections.ObservableList;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CardStorage {

    private static final String SAVE_FILE = "collection_progress.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * Saves the quantity of each card to a JSON file.
     * @param cards The list of all cards in the application.
     */
    public static void saveProgress(List<Card> cards) {
        // Create a map of Card ID -> Quantity Owned
        Map<String, Integer> progress = new HashMap<>();
        for (Card card : cards) {
            if (card.quantityOwnedProperty().get() > 0) {
                progress.put(card.idProperty().get(), card.quantityOwnedProperty().get());
            }
        }

        try (FileWriter writer = new FileWriter(SAVE_FILE)) {
            gson.toJson(progress, writer);
            System.out.println("Progress saved successfully to " + SAVE_FILE);
        } catch (IOException e) {
            System.err.println("Error saving progress: " + e.getMessage());
        }
    }

    /**
     * Loads the card quantities from the JSON file and updates the main card list.
     * @param allCards The list of all cards to be updated.
     */
    public static void loadProgress(ObservableList<Card> allCards) {
        try (FileReader reader = new FileReader(SAVE_FILE)) {
            Type type = new TypeToken<Map<String, Integer>>() {}.getType();
            Map<String, Integer> progress = gson.fromJson(reader, type);

            if (progress != null) {
                // Create a map for quick lookups
                Map<String, Card> cardMap = new HashMap<>();
                for (Card card : allCards) {
                    cardMap.put(card.idProperty().get(), card);
                }

                // Update quantities from the loaded progress
                for (Map.Entry<String, Integer> entry : progress.entrySet()) {
                    Card card = cardMap.get(entry.getKey());
                    if (card != null) {
                        card.quantityOwnedProperty().set(entry.getValue());
                    }
                }
                System.out.println("Progress loaded successfully.");
            }
        } catch (IOException e) {
            // This is expected if the file doesn't exist yet, so we don't treat it as an error.
            System.out.println("No save file found. Starting with a fresh collection.");
        }
    }
}