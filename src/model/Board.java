/* ===========================================================
 * Board ; tabuleiro e navegação entre casas (Model).
 * Mantém a lista ordenada de Squares 
 * =========================================================== */

package model;

import java.util.List;
import java.util.Objects;

final class Board {

    private final List<Square> squares;
    private final int size;
    private final int jailIndex;
    
    Board(final List<Square> squares, final int jailIndex) {
        this.squares = List.copyOf(Objects.requireNonNull(squares, "squares"));
        if (squares.isEmpty()) throw new IllegalArgumentException("Board não pode ser vazio.");
        this.size = squares.size();
        if (jailIndex < 0 || jailIndex >= size) {
            throw new IllegalArgumentException("jailIndex fora do intervalo do board.");
        }
        this.jailIndex = jailIndex;
    }

    /* Próxima posição a partir de 'from' avançando 'steps' (wrap-around). */
    int nextPosition(final int from, final int steps) {
        if (from < 0 || from >= size) {
            throw new IllegalArgumentException("Posição 'from' inválida: " + from);
        }
        if (steps < 0) {
            throw new IllegalArgumentException("Steps deve ser não-negativo.");
        }
        return (from + steps) % size;
    }

    /* Retorna a Square na posição 'index'. */
    Square squareAt(final int index) {
        if (index < 0 || index >= size) {
            throw new IllegalArgumentException("Index fora do board: " + index);
        }
        return squares.get(index);
    }
    
    /* Índice da prisão no tabuleiro. */
    int jailIndex() { return jailIndex; }

    /* Tamanho do tabuleiro. */
    int size() { return size; }    
}
