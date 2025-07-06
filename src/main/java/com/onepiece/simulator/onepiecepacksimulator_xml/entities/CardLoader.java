/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.onepiece.simulator.onepiecepacksimulator_xml.entities;

/**
 *
 * @author angelgarza
 */


import com.onepiece.simulator.onepiecepacksimulator_xml.entities.Card;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class CardLoader {
    
      public static List<Card> loadCards(String xmlPath) {
        List<Card> cards = new ArrayList<>();

        try {
            InputStream inputStream = CardLoader.class.getResourceAsStream(xmlPath);
            if (inputStream == null) {
                System.err.println("Failed to load XML: " + xmlPath);
                return cards;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            NodeList cardNodes = doc.getElementsByTagName("card");

            for (int i = 0; i < cardNodes.getLength(); i++) {
                Element cardElement = (Element) cardNodes.item(i);
                cards.add(new Card(cardElement));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return cards;
    }
}