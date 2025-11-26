/* ===========================================================
 * DeckFactory ; cria um baralho de cartas de Sorte/Revés.
 * =========================================================== */

package model;

import java.nio.file.Path;
import java.util.*;

final class DeckFactory extends FactoryBase<Card> {

    private static final List<String> EXPECTED_HEADER = List.of("index", "type", "value");

    static Deck fromCSV(final Path csvPath) {
        DeckFactory factory = new DeckFactory();
        List<Card> cards = factory.readCSV(csvPath, EXPECTED_HEADER);

        if (cards.isEmpty())
            throw new IllegalArgumentException("Deck vazio: " + csvPath);

        Deck deck = new Deck(cards);
        deck.shuffle();
        return deck;
    }
    
    /**
     * Cria um deck a partir de uma lista de cartas já ordenada.
     * Usado ao carregar um jogo salvo para preservar a ordem das cartas.
     * 
     * @param cards lista de cartas já na ordem desejada
     * @return deck com as cartas na ordem fornecida (sem embaralhar)
     */
    static Deck fromOrderedList(final List<Card> cards) {
        if (cards.isEmpty())
            throw new IllegalArgumentException("Deck não pode ser vazio");
        return new Deck(cards);
    }

    @Override
    protected Card parseLine(String[] p) {
        int id = parseInt(p[0]);
        String typeStr = p[1].trim().toUpperCase(Locale.ROOT);
        int value = parseInt(p[2]);
        Card.CardType type = Card.CardType.valueOf(typeStr);
        return new Card(id, type, value);
    }
}
