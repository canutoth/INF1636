/* ===========================================================
 * GameController ; controlador principal do padrão MVC.
 * Coordena interações entre Model (GameAPI) e View.
 * Implementa o padrão Observer para notificar a View sobre mudanças.
 * =========================================================== */

package controller;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import model.GameAPI;
import model.api.dto.PlayerRef;
import model.api.dto.OwnableInfo;
import model.api.dto.Ownables;
import model.api.dto.PlayerColor;
/**
 * Controller principal da aplicação.
 * Gerencia o ciclo do jogo e coordena a comunicação entre Model e View.
 */
public class GameController {
    
    private final GameAPI gameAPI;
    private final List<GameObserver> observers;
    private boolean gameStarted;
    
    // Mock de dados para testes
    private Integer mockedDice1;
    private Integer mockedDice2;
    
    // Configurações padrão
    private static final int INITIAL_PLAYER_MONEY = 4000;
    private static final int INITIAL_BANK_CASH = 200000;
    private static final String BOARD_CSV = "assets/dados/board.csv";
    private static final String DECK_CSV = "assets/dados/deck.csv";
    
    // Cores padrão para os jogadores (definidas pelo enum PlayerColor)
    private static final PlayerColor[] PLAYER_COLORS = PlayerColor.values();
    
    private GameController() {
        this.gameAPI = new GameAPI();
        this.observers = new ArrayList<>();
        this.gameStarted = false;
    }

    // Instância única do GameController (Singleton)
    private static final GameController INSTANCE = new GameController();

    /**
     * Retorna a instância única de `GameController` (Singleton).
     */
    public static GameController getInstance() {
        return INSTANCE;
    }
    
    /**
     * Adiciona um observador para receber notificações de eventos.
     */
    public void addObserver(GameObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }
    
    /**
     * Remove um observador.
     */
    public void removeObserver(GameObserver observer) {
        observers.remove(observer);
    }
    
    /**
     * Notifica todos os observadores sobre o início de um turno.
     */
    private void notifyTurnStarted(int playerIndex, String playerName, PlayerColor firstPlayerColor, int playerMoney) {
        for (GameObserver observer : observers) {
            observer.onTurnStarted(playerIndex, playerName, firstPlayerColor, playerMoney);
        }
    }
    
    /**
     * Notifica todos os observadores sobre um lance de dados.
     */
    private void notifyDiceRolled(int dice1, int dice2, boolean isDouble) {
        for (GameObserver observer : observers) {
            observer.onDiceRolled(dice1, dice2, isDouble);
        }
    }
    
    /**
     * Notifica todos os observadores sobre movimento de jogador.
     */
    private void notifyPlayerMoved(int playerIndex, int fromPosition, int toPosition) {
        for (GameObserver observer : observers) {
            observer.onPlayerMoved(playerIndex, fromPosition, toPosition);
        }
    }

    /**
     * Notifica todos os observadores que um jogador caiu em uma casa específica.
     */
    private void notifySquareLanded(int playerIndex, int squareIndex, String squareName, String squareType) {
        for (GameObserver observer : observers) {
            observer.onSquareLanded(playerIndex, squareIndex, squareName, squareType);
        }
    }

    /** Notifica observers que o jogo terminou e entrega a lista de vencedores. */
    private void notifyGameEnded(java.util.List<PlayerRef> winners) {
        for (GameObserver observer : observers) {
            observer.onGameEnded(winners);
        }
    }

    /**
     * Função auxiliar que notifica os observadores sobre a casa em que o jogador caiu
     * e executa ações específicas baseadas no tipo da casa.
     */
    private void callSquareNotification(int playerIndex, int squareIndex, String squareName, String squareType) {
        // Sempre notifica o pouso na casa
        notifySquareLanded(playerIndex, squareIndex, squareName, squareType);

        notifyGameMessage("Player landed on: " + squareName + " // Position: " + squareIndex + " // Type: " + squareType);

        // Açoes específicas baseadas no tipo da casa
        switch (squareType) {
            case "ChanceSquare":
                notifyGameMessage("Drawing a chance card for " + gameAPI.getPlayerName(playerIndex));
                int cardIdx = gameAPI.getLastDrawedCardIndex();
                notifyChanceSquare(playerIndex, cardIdx);
                notifyGameMessage("Chance card drawn, index: " + cardIdx);
                break;
            case "GoToJailSquare":
                notifyGameMessage("GoToJailSquare landed: player will be sent to jail.");
                break;
            case "JailSquare":
                notifyGameMessage("JailSquare: visiting jail.");
                break;
            case "MoneySquare":
                notifyGameMessage("MoneySquare: money-related effect applies.");
                break;
            case "StreetOwnableSquare":
                notifyGameMessage("Ownable property landed: " + squareName);
       
                var streetDto = gameAPI.getStreetOwnableInfo(squareIndex);
                notifyStreetOwnable(playerIndex, squareName, streetDto);
                break;
            case "CompanyOwnableSquare":
                notifyGameMessage("Company landed: " + squareName);
                var companyDto = gameAPI.getCompanyOwnableInfo(squareIndex);
                notifyCompanyOwnable(playerIndex, squareName, companyDto);
                break;
            case "StartSquare":
                notifyGameMessage("Start square landed: collecting rewards if any.");
                break;
            default:
                notifyGameMessage("Landed on square type: " + squareType);
        }
    }

    private void notifyChanceSquare(int playerIndex, int cardIndex) {
        for (GameObserver observer : observers) {
            observer.onChanceSquareLand(playerIndex, cardIndex);
        }
    }

    private void notifyStreetOwnable(int playerIndex, String propertyName, Ownables.Street streetInfo) {
        notifyGameMessage("Street ownable landed: " + propertyName + " (player=" + playerIndex + ")");
        for (GameObserver observer : observers) {
            observer.onStreetOwnableLand(playerIndex, propertyName, streetInfo);
        }
    }

    private void notifyCompanyOwnable(int playerIndex, String companyName, Ownables.Company companyInfo) {
        notifyGameMessage("Company ownable landed: " + companyName + " (player=" + playerIndex + ")");
        for (GameObserver observer : observers) {
            observer.onCompanyOwnableLand(playerIndex, companyName, companyInfo);
        }
    }

    /**
     * Notifica atualização de uma rua (compra/construção)
     */
    private void notifyStreetOwnableUpdate(int playerIndex, Ownables.Street streetInfo) {
        for (GameObserver observer : observers) {
            observer.onStreetOwnableUpdate(playerIndex, streetInfo);
        }
    }

    /**
     * Notifica atualização de uma companhia (compra/efeito)
     */
    private void notifyCompanyOwnableUpdate(int playerIndex, Ownables.Company companyInfo) {
        for (GameObserver observer : observers) {
            observer.onCompanyOwnableUpdate(playerIndex, companyInfo);
        }
    }
    
    /**
     * Notifica todos os observadores sobre uma mensagem do jogo.
     */
    private void notifyGameMessage(String message) {
        for (GameObserver observer : observers) {
            observer.onGameMessage(message);
        }
    }

    /** Notifica sobre transações para todos os observers. */
    private void notifyTransactions(java.util.List<model.api.dto.Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) return;
        // Emite cada transação no log (controller usa onGameMessage para histórico)
        for (model.api.dto.Transaction t : transactions) {
            String from = t.fromId;
            String to = t.toId;
            // mensagem legível
            String msg = String.format("%s -> %s : %s%d | balances: %s=%d, %s=%d",
                    from, to,
                    (from.equals("BANK") ? "+" : ""), t.amount,
                    from, t.fromBalanceAfter,
                    to, t.toBalanceAfter
            );
            notifyGameMessage(msg);
        }

        // Notifica observers com objetos para UI (BoardPanel)
        for (GameObserver observer : observers) {
            observer.onTransactionsUpdated(transactions);
        }
    }
    
    /**
     * Notifica todos os observadores sobre o fim do turno.
     */
    private void notifyTurnEnded() {
        for (GameObserver observer : observers) {
            observer.onTurnEnded();
        }
    }

    /**
     * Notifica observadores para limpar transações (lista vazia).
     */
    private void notifyClearTransactions() {
        java.util.List<model.api.dto.Transaction> empty = java.util.List.of();
        for (GameObserver observer : observers) {
            observer.onTransactionsUpdated(empty);
        }
    }

    /** Notifica observadores que um jogador faliu e deve ser removido/ocultado da UI. */
    private void notifyPlayerBankrupt(int playerIndex) {
        for (GameObserver observer : observers) {
            observer.onPlayerBankrupt(playerIndex);
        }
    }
    

    /** Notificar observers com a lista de propriedades prontas */
    private void notifyPropertyDataUpdated(List<OwnableInfo> items) {
        for (GameObserver observer : observers) {
            observer.onCurrentPlayerPropertyDataUpdated(items);
        }
    }

    /** Notificar observers sobre venda de propriedade */
    private void notifyPropertySold(int playerIndex) {
        for (GameObserver observer : observers) {
            observer.onPropertySold(playerIndex);
        }
    }
    
    /**
     * Inicia um novo jogo com o número especificado de jogadores.
     * @param numberOfPlayers número de jogadores (3 a 6)
     */
    public void startNewGame(int numberOfPlayers) {
        if (numberOfPlayers < 3 || numberOfPlayers > 6) {
            throw new IllegalArgumentException("Number of players must be between 3 and 6");
        }
        
        // Cria lista de jogadores (DTO PlayerRef)
        List<PlayerRef> playerSpecs = new ArrayList<>();
        for (int i = 0; i < numberOfPlayers; i++) {
            PlayerColor color = PLAYER_COLORS[i];
            playerSpecs.add(PlayerRef.of(i + 1, color, "Player " + (i + 1)));
        }
        // Emit debug messages listando os jogadores criados (nome e cor)
        for (int i = 0; i < numberOfPlayers; i++) {
            String pname = "Player " + (i + 1);
            PlayerColor color = PLAYER_COLORS[i];
            notifyGameMessage("Created player " + (i + 1) + ": " + pname + " (" + color.name() + ")");
        }
        // Caminhos dos arquivos de configuração
        Path boardPath = Paths.get(BOARD_CSV);
        Path deckPath = Paths.get(DECK_CSV);
        
        try {
            
            // Inicia o jogo através da API
            gameAPI.startGame(playerSpecs, boardPath, deckPath, INITIAL_PLAYER_MONEY, INITIAL_BANK_CASH);
            gameStarted = true;
            
            // Atualiza as posições iniciais de todos os jogadores
            for (int i = 0; i < numberOfPlayers; i++) {
                int position = gameAPI.getPlayerPosition(i);
                notifyPlayerMoved(i, -1, position); // -1 indica inicialização
            }
            
            notifyGameMessage("Game started with " + numberOfPlayers + " players!");
            
            // Notifica o primeiro jogador (inclui cor)
            int firstPlayer = gameAPI.getCurrentPlayerIndex();
            String firstPlayerName = gameAPI.getPlayerName(firstPlayer);
            PlayerColor firstPlayerColor = gameAPI.getPlayerColor(firstPlayer);
            int firstPlayerMoney = gameAPI.getPlayerMoney(firstPlayer);
            notifyTurnStarted(firstPlayer, firstPlayerName, firstPlayerColor, firstPlayerMoney);
                notifyGameMessage("=== Turn of " + firstPlayerName + " ===");
            notifyPropertyDataUpdated(gameAPI.getCurrentPlayerPropertyData());
            
        } catch (Exception e) {
            System.err.println("ERROR starting game:");
            e.printStackTrace();
            notifyGameMessage("Error starting game: " + e.getMessage());
            throw new RuntimeException("Failed to start game", e);
        }
    }
    
    /**
     * Executa um turno completo: rola dados e resolve todas as ações.
     * Este é o método principal que coordena a jogada.
     */
    public void rollDiceAndPlay() {
        ensureGameStarted();
        
        try {
            int currentPlayer = gameAPI.getCurrentPlayerIndex();
            if (!gameAPI.isRollAllowed()) {
                String pname = gameAPI.getPlayerName(currentPlayer);
                notifyGameMessage(" '" + pname + "' tried to roll again, but was the last to play. Action blocked.");
                return;
            }

            // Obtém informações do jogador atual antes da jogada
            int positionBefore = gameAPI.getPlayerPosition(currentPlayer);
            
            // Se há valores mockados, aplica-os ao GameAPI antes do roll
            if (hasMockedDiceValues()) {
                gameAPI.setMockedDiceValues(mockedDice1, mockedDice2);
                // Limpa os valores após aplicá-los (single-use no controller também)
                clearMockedDiceValues();
            }
            
            // Executa a jogada através da API (move o jogador de verdade)
            gameAPI.rollAndResolve();
            
            // Obtém os valores dos dados reais que foram lançados via GameAPI
            GameAPI.DiceData lastRoll = gameAPI.getLastDiceData();
            int dice1 = lastRoll.d1();
            int dice2 = lastRoll.d2();
            boolean isDouble = lastRoll.isDouble();
            
            // Notifica sobre o lance de dados
            notifyDiceRolled(dice1, dice2, isDouble);

            notifyGameMessage("Dice rolled: " + dice1 + " and " + dice2 + (isDouble ? " (DOUBLE!)" : ""));
            
            // Obtém a posição real após o movimento
            int positionAfter = gameAPI.getPlayerPosition(currentPlayer);
            
            // Notifica sobre o movimento real
            notifyPlayerMoved(currentPlayer, positionBefore, positionAfter);

            notifyGameMessage("Player moved from position " + positionBefore + " to " + positionAfter);

            // Notifica sobre a casa em que o jogador caiu
            String squareName = gameAPI.getSquareName(positionAfter);
            String squareType = gameAPI.getSquareType(positionAfter);
            
            // Usa a função auxiliar para notificar e tratar efeitos
            callSquareNotification(currentPlayer, positionAfter, squareName, squareType);

            // Coleta transações ocorridas durante a jogada e as notifica
            var transactions = gameAPI.fetchAndClearTransactions();
            notifyTransactions(transactions);

            // Após a jogada (roll & resolve), verifique se o jogador que rolou faliu.
            if (!gameAPI.isPlayerAlive(currentPlayer)) {
                notifyGameMessage("PLAYER BANKRUPTCY: " + gameAPI.getPlayerName(currentPlayer) + " has gone bankrupt!");
                notifyPlayerBankrupt(currentPlayer);
            }

            // Verifica automaticamente se o jogo terminou (apenas um jogador vivo, condição de termino 2)
            checkAutoEndCondition();
        } catch (Exception e) {
            notifyGameMessage("Error during turn: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Finaliza o turno atual e passa para o próximo jogador.
     */
    public void endTurn() {
        ensureGameStarted();
        
        try {
            // Finaliza o turno e obtém o próximo jogador
            gameAPI.endTurn();
            notifyTurnEnded();
            // Limpa transações visuais ao fim do turno
            notifyClearTransactions();

            notifyGameMessage("Turn ended.");
            
            // Obtém informações do próximo jogador
            int nextPlayerIndex = gameAPI.getCurrentPlayerIndex();
            String nextPlayerName = gameAPI.getPlayerName(nextPlayerIndex);
            PlayerColor nextPlayerColor = gameAPI.getPlayerColor(nextPlayerIndex);
            int nextPlayerMoney = gameAPI.getPlayerMoney(nextPlayerIndex);

            notifyTurnStarted(nextPlayerIndex, nextPlayerName, nextPlayerColor, nextPlayerMoney);
            notifyGameMessage("Now it's " + nextPlayerName + "'s turn");
            notifyPropertyDataUpdated(gameAPI.getCurrentPlayerPropertyData());
            // Coleta transações geradas pela compra e notifica
            var transactions = gameAPI.fetchAndClearTransactions();
            notifyTransactions(transactions);
            
        } catch (Exception e) {
            notifyGameMessage("Error ending turn: " + e.getMessage());
            e.printStackTrace();
        }
    }
    /**
     * Verifica se o jogo já foi iniciado.
     */
    public boolean isGameStarted() {
        return gameStarted;
    }

    /**
     * Retorna a lista de vencedores da partida (PlayerRef).
     * Se o jogo não foi iniciado, retorna lista vazia.
     */
    public java.util.List<model.api.dto.PlayerRef> getWinners() {
        if (!gameStarted) return java.util.List.of();
        return gameAPI.getWinners();
    }

    /**
     * Força o encerramento do jogo e notifica os observers com os vencedores.
     * Usado pela View quando o usuário clica em "Finish Game".
     */
    public void finishGame() {
        if (!gameStarted) {
            notifyGameMessage("Finish requested but game not started.");
            return;
        }

        java.util.List<PlayerRef> winners = gameAPI.getWinners();
        notifyGameEnded(winners);
        gameStarted = false;
    }

    /**
     * Garante que o jogo foi iniciado; lança IllegalStateException caso contrário.
     */
    private void ensureGameStarted() {
        if (!gameStarted) {
            throw new IllegalStateException("Game has not been started yet");
        }
    }

    /**
     * Retorna a quantidade de dinheiro de um jogador (acesso de conveniência para a view).
     */
    public int getPlayerMoney(int playerIndex) {
        return gameAPI.getPlayerMoney(playerIndex);
    }

    /**
     * Tenta comprar a propriedade onde o jogador atual está.
     * Se não for possível, envia uma mensagem de debug explicando o motivo.
     */
    public void attemptBuy() {
        ensureGameStarted();

        try {
            final int currentPlayer = gameAPI.getCurrentPlayerIndex();

            if (!gameAPI.chooseBuy()) {
                String reason = gameAPI.getBuyNotAllowedReason();
                if (reason == null) reason = "Unknown reason";
                notifyGameMessage("Buy blocked: " + reason);
                return;
            }

            int pos = gameAPI.getPlayerPosition(currentPlayer);
            String propName = gameAPI.getSquareName(pos);

            notifyGameMessage(gameAPI.getPlayerName(currentPlayer) + " bought " + propName);

            String squareType = gameAPI.getSquareType(pos);

            // Se for uma propriedade comprada, notifica a atualização adequada
            if (squareType.equals("StreetOwnableSquare")) {
                Ownables.Street streetInfo = gameAPI.getStreetOwnableInfo(pos);
                notifyStreetOwnableUpdate(currentPlayer, streetInfo);
            } else {
                Ownables.Company companyInfo = gameAPI.getCompanyOwnableInfo(pos);
                notifyCompanyOwnableUpdate(currentPlayer, companyInfo);
            }
            notifyPropertyDataUpdated(gameAPI.getCurrentPlayerPropertyData());
            // Coleta transações geradas pela construção e notifica
            var transactions = gameAPI.fetchAndClearTransactions();
            notifyTransactions(transactions);
            
        } catch (Exception e) {
            notifyGameMessage("Error while attempting buy: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tenta construir uma casa na propriedade atual. 
     * Emite debug se não for possível.
     */
    public void attemptBuildHouse() {
        ensureGameStarted();

        try {
            final int currentPlayer = gameAPI.getCurrentPlayerIndex();

            if (!gameAPI.chooseBuildHouse()) {
                String reason = gameAPI.getBuildHouseNotAllowedReason();
                if (reason == null) reason = "Unknown reason";
                notifyGameMessage("Build House blocked: " + reason);
                return;
            }

            int pos = gameAPI.getPlayerPosition(currentPlayer);
            String propName = gameAPI.getSquareName(pos);
            notifyGameMessage(gameAPI.getPlayerName(currentPlayer) + " built a house on " + propName);
            Ownables.Street streetInfo = gameAPI.getStreetOwnableInfo(pos);
            notifyStreetOwnableUpdate(currentPlayer, streetInfo);
            notifyPropertyDataUpdated(gameAPI.getCurrentPlayerPropertyData());
            // Coleta transações geradas pela construção e notifica
            var transactions = gameAPI.fetchAndClearTransactions();
            notifyTransactions(transactions);
        } catch (Exception e) {
            notifyGameMessage("Error while attempting to build house: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Tenta construir um hotel na propriedade atual. 
     * Emite debug se não for possível.
     */
    public void attemptBuildHotel() {
        ensureGameStarted();

        try {
            final int currentPlayer = gameAPI.getCurrentPlayerIndex();

            if (!gameAPI.chooseBuildHotel()) {
                String reason = gameAPI.getBuildHotelNotAllowedReason();
                if (reason == null) reason = "Unknown reason";
                notifyGameMessage("Build Hotel blocked: " + reason);
                return;
            }

            int pos = gameAPI.getPlayerPosition(currentPlayer);
            String propName = gameAPI.getSquareName(pos);
            notifyGameMessage(gameAPI.getPlayerName(currentPlayer) + " built a hotel on " + propName);
            Ownables.Street streetInfo = gameAPI.getStreetOwnableInfo(pos);
            notifyStreetOwnableUpdate(currentPlayer, streetInfo);
            notifyPropertyDataUpdated(gameAPI.getCurrentPlayerPropertyData());
            // Coleta transações geradas pela construção e notifica
            var transactions = gameAPI.fetchAndClearTransactions();
            notifyTransactions(transactions);
        } catch (Exception e) {
            notifyGameMessage("Error while attempting to build hotel: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // Venda acionada pela View
    public void attemptSell(final int boardIndex) {
        ensureGameStarted();
        try {
            final int currentPlayer = gameAPI.getCurrentPlayerIndex();
            final String name = gameAPI.getSquareName(boardIndex);

            gameAPI.sellAtIndex(boardIndex);

            notifyGameMessage(gameAPI.getPlayerName(currentPlayer) + " sold " + name);
            notifyPropertySold(currentPlayer);

            // Se for uma propriedade vendida, notifica a atualização adequada
            int pos = boardIndex;
            String squareType = gameAPI.getSquareType(pos);

            if (squareType.equals("StreetOwnableSquare")) {
                Ownables.Street streetInfo = gameAPI.getStreetOwnableInfo(pos);
                notifyStreetOwnableUpdate(currentPlayer, streetInfo);
            } else {
                Ownables.Company companyInfo = gameAPI.getCompanyOwnableInfo(pos);
                notifyCompanyOwnableUpdate(currentPlayer, companyInfo);
            }

            notifyPropertyDataUpdated(gameAPI.getCurrentPlayerPropertyData());
            // Coleta transações pendentes e notifica
            var transactions = gameAPI.fetchAndClearTransactions();
            notifyTransactions(transactions);

        } catch (Exception e) {
            notifyGameMessage("Error while attempting sell: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Define valores mockados para os dados (modo de teste).
     * Quando definidos, o próximo rollDiceAndPlay usará estes valores.
     */
    public void setMockedDiceValues(int dice1, int dice2) {
        if (dice1 < 1 || dice1 > 6 || dice2 < 1 || dice2 > 6) {
            throw new IllegalArgumentException("Dice values must be between 1 and 6");
        }
        this.mockedDice1 = dice1;
        this.mockedDice2 = dice2;
        notifyGameMessage("[TEST MODE] Forcing dice: " + dice1 + " and " + dice2);
    }
    
    /**
     * Remove valores mockados dos dados (volta ao modo normal/aleatório).
     */
    public void clearMockedDiceValues() {
        this.mockedDice1 = null;
        this.mockedDice2 = null;
    }
    
    /**
     * Verifica se há valores mockados definidos.
     */
    private boolean hasMockedDiceValues() {
        return mockedDice1 != null && mockedDice2 != null;
    }
    
    /**
     * Retorna a cor do jogador atual.
     */
    private PlayerColor getCurrentPlayerColor() {
        if (!gameStarted) return null;
        int currentPlayerIdx = gameAPI.getCurrentPlayerIndex();
        return gameAPI.getPlayerColor(currentPlayerIdx);
    }
    
    /**
     * Salva o estado atual do jogo em um arquivo CSV.
     * 
     * @param savePath caminho onde salvar o arquivo
     * @return true se salvou com sucesso, false caso contrário
     */
    public boolean saveGame(Path savePath) {
        if (!gameStarted) {
            notifyGameMessage("Cannot save: game has not started.");
            return false;
        }
        
        try {
            gameAPI.saveGame(savePath);
            notifyGameMessage("Game saved successfully to: " + savePath.toString());
            return true;
        } catch (Exception e) {
            notifyGameMessage("Error saving game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Carrega um jogo salvo de um arquivo CSV e inicia a partida.
     * 
     * @param loadPath caminho do arquivo salvo
     * @return true se carregou com sucesso, false caso contrário
     */
    public boolean loadGame(Path loadPath) {
        if (gameStarted) {
            notifyGameMessage("Cannot load: game already started.");
            return false;
        }
        
        try {
            Path boardPath = Paths.get(BOARD_CSV);
            Path deckPath = Paths.get(DECK_CSV);
            
            gameAPI.loadGame(loadPath, boardPath, deckPath, INITIAL_BANK_CASH);
            gameStarted = true;
            
            // Notifica o início do turno do jogador atual
            int currentPlayerIdx = gameAPI.getCurrentPlayerIndex();
            String playerName = gameAPI.getPlayerName(currentPlayerIdx);
            PlayerColor playerColor = getCurrentPlayerColor();
            int playerMoney = gameAPI.getPlayerMoney(currentPlayerIdx);
            
            // Atualiza as posições e status (vivo/morto) dos jogadores no tabuleiro
            for (int i = 0; i < gameAPI.getNumberOfPlayers(); i++) {
                int position = gameAPI.getPlayerPosition(i);
                boolean alive = gameAPI.isPlayerAlive(i);
                notifyPlayerMoved(i, 0, position);
                if (!alive) {
                    notifyPlayerBankrupt(i);
                }
            }
            
            notifyTurnStarted(currentPlayerIdx, playerName, playerColor, playerMoney);
            notifyPropertyDataUpdated(gameAPI.getCurrentPlayerPropertyData());
            notifyGameMessage("Game loaded successfully from: " + loadPath.toString());
            
            return true;
        } catch (Exception e) {
            notifyGameMessage("Error loading game: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica automaticamente a condição de fim de jogo: se apenas um jogador permanecer vivo,
     * declara o(s) vencedor(es), notifica observers e desliga o estado de `gameStarted`.
     */
    private void checkAutoEndCondition() {
        if (!gameStarted) return;

        // Delegates counting of active players to the Model (GameAPI)
        int alive = gameAPI.getAlivePlayerCount();

        if (alive <= 1) {
            // Obtém vencedores antes de marcar o jogo como encerrado
            java.util.List<PlayerRef> winners = gameAPI.getWinners();

            // Notifica observers para que a UI abra a janela final (mesma ação do botão "Finish Game")
            notifyGameEnded(winners);

            // Marca jogo como terminado (a UI já recebeu os vencedores)
            gameStarted = false;
        }
    }
}
