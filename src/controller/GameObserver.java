/* ===========================================================
 * GameObserver ; interface para o padrão Observer.
 * Permite que a View seja notificada sobre mudanças no Model.
 * =========================================================== */

package controller;

import java.util.List;
import model.api.dto.OwnableInfo;
import model.api.dto.Ownables;
import model.api.dto.PlayerColor;

/**
 * Interface para objetos que observam mudanças no estado do jogo.
 * Implementa o padrão Observer para desacoplar Model da View.
 */
public interface GameObserver {
    
    /**
     * Notifica que um novo turno começou.
     * @param playerIndex índice do jogador atual
     * @param playerName nome do jogador atual
     */
    void onTurnStarted(int playerIndex, String playerName, PlayerColor firstPlayerColor, int playerMoney);
    
    /**
     * Notifica que os dados foram lançados.
     * @param dice1 valor do primeiro dado
     * @param dice2 valor do segundo dado
     * @param isDouble se os dados são iguais
     */
    void onDiceRolled(int dice1, int dice2, boolean isDouble);
    
    /**
     * Notifica que um jogador se moveu no tabuleiro.
     * @param playerIndex índice do jogador
     * @param fromPosition posição anterior
     * @param toPosition nova posição
     */
    void onPlayerMoved(int playerIndex, int fromPosition, int toPosition);
    
    /**
     * Notifica que um jogador caiu em uma casa.
     * @param playerIndex índice do jogador
     * @param squareIndex índice da casa
     * @param squareName nome da casa
     * @param squareType tipo/classe da casa (ex.: ChanceSquare, GoToJailSquare, JailSquare, MoneySquare, StreetOwnableSquare, StartSquare)
     */
    void onSquareLanded(int playerIndex, int squareIndex, String squareName, String squareType);

    /**
     * Notifica que um jogador caiu em uma ChanceSquare com a carta sorteada.
     * @param playerIndex índice do jogador
     * @param cardIndex índice da carta sorteada (0-based)
     */
    void onChanceSquareLand(int playerIndex, int cardIndex);
    
    /** Notifica que o jogador caiu em uma rua própria (nome da propriedade).
     * @param playerIndex índice do jogador
     * @param propertyName nome da propriedade
     * @param streetInfo DTO com informações da rua
    */
    void onStreetOwnableLand(int playerIndex, String propertyName, Ownables.Street streetInfo);

    /** Notifica que o jogador caiu em uma companhia própria (informações).
     * @param playerIndex índice do jogador
     * @param companyInfo DTO com informações da companhia
     * @param companyName nome da companhia
    */
    void onCompanyOwnableLand(int playerIndex, String companyName, Ownables.Company companyInfo);

    /**
     * Notifica que houve uma atualização em uma rua (compra ou construção).
     * @param playerIndex índice do jogador que realizou a ação (pode ser o dono)
     * @param streetInfo DTO com informações atualizadas da rua
     */
    void onStreetOwnableUpdate(int playerIndex, Ownables.Street streetInfo);

    /**
     * Notifica que houve uma atualização em uma companhia (compra ou efeito).
     * @param playerIndex índice do jogador que realizou a ação (pode ser o dono)
     * @param companyInfo DTO com informações atualizadas da companhia
     */
    void onCompanyOwnableUpdate(int playerIndex, Ownables.Company companyInfo);

    /**
     * Notifica que o turno terminou.
     */
    void onTurnEnded();
    
    /**
     * Notifica sobre uma mensagem/evento do jogo.
     * @param message mensagem a exibir
     */
    void onGameMessage(String message);

    /**
     * Notifica que a lista de propriedades do jogador atual foi atualizada.
     * @param items lista atualizada de propriedades
     */
    void onCurrentPlayerPropertyDataUpdated(List<OwnableInfo> items);

    /**
     * Notifica que o jogo acabou e fornece a lista de vencedores.
     * Implementações de UI devem exibir a tela final (ou equivalente).
     * @param winners lista de PlayerRef representando os vencedores
     */
    void onGameEnded(java.util.List<model.api.dto.PlayerRef> winners);

    /**
    * Notifica que uma propriedade foi vendida.
    * @param playerIndex índice do jogador que vendeu a propriedade
    */
    void onPropertySold(int playerIndex);

    /**
     * Notifica que uma ou mais transações financeiras ocorreram.
     * A lista poderá conter múltiplas transações (ex.: pay-to-all).
     */
    void onTransactionsUpdated(java.util.List<model.api.dto.Transaction> transactions);

    /**
     * Notifica que um jogador faliu (foi declarado bankrupt) e deve ser removido/ocultado da UI.
     * @param playerIndex índice do jogador que faliu
     */
    void onPlayerBankrupt(int playerIndex);

}
