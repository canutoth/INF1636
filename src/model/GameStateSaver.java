/* ===========================================================
 * GameStateSaver ; salva o estado completo do jogo em CSV.
 * Exporta jogadores, propriedades, deck e configurações.
 * =========================================================== */

package model;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Classe responsável por serializar o estado completo do jogo em formato texto ASCII puro.
 * O arquivo salvo contém todas as informações necessárias para restaurar a partida.
 * Formato: arquivo .txt com charset US-ASCII.
 */
final class GameStateSaver {
    
    private GameStateSaver() {
        // Utility class
    }
    
    /**
     * Salva o estado completo do jogo em um arquivo CSV.
     * 
     * @param savePath caminho onde salvar o arquivo
     * @param players lista de jogadores
     * @param currentPlayerIndex índice do jogador atual
     * @param deck baralho de cartas
     * @param board tabuleiro
     * @throws IOException se houver erro ao escrever o arquivo
     */
    static void saveGame(Path savePath, 
                        List<Player> players, 
                        int currentPlayerIndex,
                        Deck deck,
                        Board board,
                        Bank bank) throws IOException {
        
        try (BufferedWriter writer = Files.newBufferedWriter(savePath, StandardCharsets.US_ASCII)) {
            // Seção: GAME_INFO
            writer.write("# GAME_INFO\n");
            writer.write("currentPlayerIndex," + currentPlayerIndex + "\n");
            writer.write("numberOfPlayers," + players.size() + "\n");
            writer.write("bankCash," + bank.getCash() + "\n");
            writer.newLine();
            
            // Seção: PLAYERS
            writer.write("# PLAYERS\n");
            writer.write("# id,name,color,money,position,inJail,getOutOfJailCards,alive\n");
            for (Player player : players) {
                writer.write(String.format("%s,%s,%s,%d,%d,%b,%d,%b\n",
                    player.getId(),
                    player.getName(),
                    player.getColor(),
                    player.getMoney(),
                    player.getPosition(),
                    player.isInJail(),
                    player.getGetOutOfJailCards(),
                    player.isAlive()
                ));
            }
            writer.newLine();
            
            // Seção: PROPERTIES - Streets
            writer.write("# PROPERTIES_STREETS\n");
            writer.write("# squareIndex,ownerId,houses,hasHotel\n");
            for (int i = 0; i < board.size(); i++) {
                Square square = board.squareAt(i);
                if (square instanceof StreetOwnableSquare) {
                    StreetOwnableSquare street = (StreetOwnableSquare) square;
                    if (street.hasOwner()) {
                        writer.write(String.format("%d,%s,%d,%b\n",
                            i,
                            street.getOwner().getId(),
                            street.getHouses(),
                            street.hasHotel()
                        ));
                    }
                }
            }
            writer.newLine();
            
            // Seção: PROPERTIES - Companies
            writer.write("# PROPERTIES_COMPANIES\n");
            writer.write("# squareIndex,ownerId\n");
            for (int i = 0; i < board.size(); i++) {
                Square square = board.squareAt(i);
                if (square instanceof CompanyOwnableSquare) {
                    CompanyOwnableSquare company = (CompanyOwnableSquare) square;
                    if (company.hasOwner()) {
                        writer.write(String.format("%d,%s\n",
                            i,
                            company.getOwner().getId()
                        ));
                    }
                }
            }
            writer.newLine();
            
            // Seção: DECK_STATE
            writer.write("# DECK_STATE\n");
            writer.write("# Ordem completa das cartas no baralho (do topo ao fim)\n");
            writer.write("# cardId,cardType,cardValue\n");
            List<Card> cardsInOrder = deck.getCardsInOrder();
            for (Card card : cardsInOrder) {
                writer.write(String.format("%d,%s,%d\n",
                    card.getId(),
                    card.type().name(),
                    card.value()
                ));
            }
            writer.newLine();
            
            // Cartas "Saia da Prisão" que estão com jogadores
            writer.write("# JAIL_CARDS_OUT\n");
            int jailCardsOut = 0;
            for (Player player : players) {
                jailCardsOut += player.getGetOutOfJailCards();
            }
            writer.write("getOutOfJailCardsOut," + jailCardsOut + "\n");
        }
    }
}
