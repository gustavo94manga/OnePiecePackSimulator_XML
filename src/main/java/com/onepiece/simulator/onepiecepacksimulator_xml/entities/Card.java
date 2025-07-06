package com.onepiece.simulator.onepiecepacksimulator_xml.entities;

/**
 *
 * @author angelgarza
 */

import javafx.beans.property.*;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class Card {
    private final StringProperty id = new SimpleStringProperty();
    private final StringProperty name = new SimpleStringProperty();
    private final StringProperty rarity = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final StringProperty attribute = new SimpleStringProperty();
    private final IntegerProperty power = new SimpleIntegerProperty();
    private final IntegerProperty counter = new SimpleIntegerProperty();
    private final StringProperty color = new SimpleStringProperty();
    private final StringProperty cardType = new SimpleStringProperty();
    private final StringProperty effect = new SimpleStringProperty();
    private final StringProperty imageUrl = new SimpleStringProperty();
    private final BooleanProperty alternateArt = new SimpleBooleanProperty();
    private final StringProperty seriesId = new SimpleStringProperty();
    private final StringProperty seriesName = new SimpleStringProperty();
    private final IntegerProperty quantityOwned = new SimpleIntegerProperty(0); // Tracks how many user has

    public Card(Element element) {
        this.id.set(get(element, "id"));
        this.name.set(get(element, "name"));
        this.rarity.set(get(element, "rarity"));
        this.type.set(get(element, "type"));
        this.attribute.set(get(element, "attribute", "-"));
        this.power.set(parseInt(get(element, "power")));
        this.counter.set(parseInt(get(element, "counter")));
        this.color.set(get(element, "color"));
        this.cardType.set(get(element, "cardtype", "-"));
        this.effect.set(get(element, "effect"));
        this.imageUrl.set(get(element, "imageurl"));
        this.alternateArt.set(Boolean.parseBoolean(get(element, "alternateart")));
        this.seriesId.set(get(element, "seriesid"));
        this.seriesName.set(get(element, "seriesname"));
    }

    // Properties (for TableView bindings and filtering)
    public StringProperty idProperty() { return id; }
    public StringProperty nameProperty() { return name; }
    public StringProperty rarityProperty() { return rarity; }
    public StringProperty typeProperty() { return type; }
    public StringProperty attributeProperty() { return attribute; }
    public IntegerProperty powerProperty() { return power; }
    public IntegerProperty counterProperty() { return counter; }
    public StringProperty colorProperty() { return color; }
    public StringProperty cardTypeProperty() { return cardType; }
    public StringProperty effectProperty() { return effect; }
    public StringProperty imageUrlProperty() { return imageUrl; }
    public BooleanProperty alternateArtProperty() { return alternateArt; }
    public StringProperty seriesIdProperty() { return seriesId; }
    public StringProperty seriesNameProperty() { return seriesName; }
    public IntegerProperty quantityOwnedProperty() { return quantityOwned; }

    // Logic for collection handling
    public void incrementQuantity() {
        quantityOwned.set(quantityOwned.get() + 1);
    }

    public void resetQuantity() {
        quantityOwned.set(0);
    }

    private String get(Element parent, String tag) {
        return get(parent, tag, "");
    }

    private String get(Element parent, String tag, String defaultVal) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() > 0) {
            String text = nodes.item(0).getTextContent().trim();
            return (text.equals("-") || text.equalsIgnoreCase("nan")) ? defaultVal : text;
        }
        return defaultVal;
    }

    private int parseInt(String text) {
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public String toString() {
        return name.get() + " (" + id.get() + ", " + rarity.get() + ")";
    }
}
