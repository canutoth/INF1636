/* ===========================================================
 * Deck ; baralho de Sorte/Revés.
 * =========================================================== */

package model;

import java.util.*;

final class Deck {

    private final Deque<Card> cards;

    Deck(final List<Card> initialCards) {
        if (initialCards.isEmpty()) throw new IllegalArgumentException("Deck não pode ser vazio.");
        this.cards = new ArrayDeque<>(initialCards);
    }

    Card draw() {
        Card c = cards.pollFirst();
        
        // Se for carta sair da prisão, ela sai do baralho
        if (c.type() == Card.CardType.GET_OUT_OF_JAIL) {
            return c;
        }
        
        cards.addLast(c); // volta pro fim depois de usada
        return c;
    }

    void returnGetOutOfJailCardToBottom() {
        cards.addLast(new Card(0, Card.CardType.GET_OUT_OF_JAIL, 0));
    }

    void shuffle() {
        List<Card> tmp = new ArrayList<>(cards);
        Collections.shuffle(tmp);
        cards.clear();
        cards.addAll(tmp);
    }
    
    /**
     * Retorna a lista ordenada de cartas no deck (do topo para o fim).
     * Usado para salvar o estado do baralho.
     */
    List<Card> getCardsInOrder() {
        return new ArrayList<>(cards);
    }
}
