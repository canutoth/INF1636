/* ===========================================================
 * GameAPI ; fachada pública do Model.
 * Expõe operações seguras para Controller/View e orquestra o ciclo de jogo via GameEngine.
 * =========================================================== */

package model;

import java.nio.file.Path;
import java.util.*;

import model.api.dto.OwnableInfo;
import model.api.dto.Ownables;
import model.api.dto.PlayerColor;
import model.api.dto.PlayerRef;

public final class GameAPI {

    // ==== Estado principal mantido pela fachada ====
    private GameEngine engine;
    private boolean started;

    // ==== API pública ====

    /**
     * Inicia o jogo (boot do Model).
     * Cria regras, banco, dados, baralhos, tabuleiro, jogadores, economia e engine.
     * playersConfig configuração dos jogadores
     * boardCsvPath  caminho do CSV contendo o tabuleiro
     * deckCsvPaths  lista de caminhos para os CSVs de baralhos
     * initialPlayerMoney dinheiro inicial de cada jogador
     * initialBankCash dinheiro inicial do banco
     */
    
    public void startGame(final java.util.List<PlayerRef> playersConfig,
                          final Path boardCsvPath,
                          final Path deckCsvPath,
                          final int initialPlayerMoney,
                          final int initialBankCash) {
        ensureNotStarted();
        Objects.requireNonNull(playersConfig, "playersConfig não pode ser nulo");
        Objects.requireNonNull(boardCsvPath, "boardCsvPath não pode ser nulo");
        Objects.requireNonNull(deckCsvPath, "deckCsvPath não pode ser nulo");

        // 1) Banco e economia
        final Bank bank = new Bank(initialBankCash);
        final EconomyService economy = new EconomyService(bank);

        // 2) Baralhos
        final Deck deck = DeckFactory.fromCSV(deckCsvPath);

        // 3) Jogadores
        validatePlayerCount(playersConfig.size());
        List<Player> players = new ArrayList<>(playersConfig.size());
        for (PlayerRef spec : playersConfig) {
            players.add(new Player(
                spec.id(),
                spec.name(),
                spec.color(),
                initialPlayerMoney
            ));
        }

        // 4) Tabuleiro
        final Board board = BoardFactory.fromCSV(boardCsvPath);

        // 5) Engine
    this.engine = new GameEngine(board, players, deck, economy, 0);

        // 7) Boot concluído
        this.started = true;
    }
    
    /**
     * Inicia o jogo carregando o estado de um arquivo CSV salvo previamente.
     * 
     * @param loadPath caminho do arquivo salvo
     * @param boardCsvPath caminho do CSV do tabuleiro (necessário para recriar o board)
     * @param deckCsvPath caminho do CSV do deck (necessário para recriar o deck)
     * @param initialBankCash dinheiro inicial do banco
     * @throws IOException se houver erro ao ler o arquivo
     */
    public void loadGame(final Path loadPath,
                        final Path boardCsvPath,
                        final Path deckCsvPath,
                        final int initialBankCash) throws java.io.IOException {
        ensureNotStarted();
        
        // Carrega os dados salvos
        GameStateLoader.SavedGameData savedData = GameStateLoader.loadGame(loadPath);
        
        System.out.println("[LOAD] Loading game with " + savedData.players.size() + " players");
        System.out.println("[LOAD] Current player index: " + savedData.currentPlayerIndex);
        System.out.println("[LOAD] Bank cash: $" + savedData.bankCash);
        
        // 1) Banco e economia (usa o dinheiro salvo)
        final Bank bank = new Bank(savedData.bankCash);
        final EconomyService economy = new EconomyService(bank);
        
        // 2) Baralho (reconstrói com a ordem salva)
        final Deck deck;
        if (!savedData.deckCards.isEmpty()) {
            // Carrega a ordem exata das cartas salvas
            System.out.println("[LOAD] Restoring deck with " + savedData.deckCards.size() + " cards in saved order");
            List<Card> orderedCards = new ArrayList<>();
            for (GameStateLoader.CardData cardData : savedData.deckCards) {
                Card.CardType cardType = Card.CardType.valueOf(cardData.cardType);
                orderedCards.add(new Card(cardData.cardId, cardType, cardData.cardValue));
            }
            deck = DeckFactory.fromOrderedList(orderedCards);
        } else {
            // Fallback para compatibilidade com saves antigos
            System.out.println("[LOAD] No deck order saved, creating fresh deck and removing jail cards");
            deck = DeckFactory.fromCSV(deckCsvPath);
            // Remove cartas "sair da prisão" que estão com jogadores
            for (int i = 0; i < savedData.getOutOfJailCardsOut; i++) {
                deck.draw(); // Simula a remoção das cartas
            }
        }
        
        // 3) Jogadores
        List<Player> players = new ArrayList<>(savedData.players.size());
        for (GameStateLoader.PlayerData pData : savedData.players) {
            System.out.println("[LOAD] Creating player: " + pData.id + " (" + pData.name + ") at position " + pData.position + " with $" + pData.money);
            Player player = new Player(pData.id, pData.name, pData.color, pData.money);
            player.moveTo(pData.position);
            player.setInJail(pData.inJail);
            
            // Restaura cartas de sair da prisão
            for (int i = 0; i < pData.getOutOfJailCards; i++) {
                player.grantGetOutOfJailCard();
            }
            
            // Restaura status de vida
            if (!pData.alive) {
                player.setBankrupt();
            }
            
            players.add(player);
        }
        
        // 4) Tabuleiro
        final Board board = BoardFactory.fromCSV(boardCsvPath);
        
        // 5) Restaura propriedades de ruas
        System.out.println("[LOAD] Restoring " + savedData.streetProperties.size() + " street properties");
        for (GameStateLoader.StreetPropertyData propData : savedData.streetProperties) {
            Square sq = board.squareAt(propData.squareIndex);
            if (sq instanceof StreetOwnableSquare) {
                StreetOwnableSquare street = (StreetOwnableSquare) sq;
                
                // Encontra o dono
                Player owner = players.stream()
                    .filter(p -> p.getId().equals(propData.ownerId))
                    .findFirst()
                    .orElse(null);
                
                if (owner != null) {
                    System.out.println("[LOAD]   - " + street.name() + " owned by " + owner.getName() + " (houses: " + propData.houses + ", hotel: " + propData.hasHotel + ")");
                    street.setOwner(owner);
                    owner.addProperty(street);
                    
                    // Restaura construções
                    for (int i = 0; i < propData.houses; i++) {
                        street.buildHouse();
                    }
                    if (propData.hasHotel) {
                        street.buildHotel();
                    }
                }
            }
        }
        
        // 6) Restaura propriedades de companhias
        System.out.println("[LOAD] Restoring " + savedData.companyProperties.size() + " company properties");
        for (GameStateLoader.CompanyPropertyData propData : savedData.companyProperties) {
            Square sq = board.squareAt(propData.squareIndex);
            if (sq instanceof CompanyOwnableSquare) {
                CompanyOwnableSquare company = (CompanyOwnableSquare) sq;
                
                // Encontra o dono
                Player owner = players.stream()
                    .filter(p -> p.getId().equals(propData.ownerId))
                    .findFirst()
                    .orElse(null);
                
                if (owner != null) {
                    System.out.println("[LOAD]   - " + company.name() + " owned by " + owner.getName());
                    company.setOwner(owner);
                    owner.addProperty(company);
                }
            }
        }
        
        // 7) Engine
        this.engine = new GameEngine(board, players, deck, economy, savedData.currentPlayerIndex);
        
        // 8) Boot concluído
        this.started = true;
        
        // 9) Inicia o turno do jogador atual
        this.engine.beginTurn();
    }

    /**
     * Salva o estado atual do jogo em um arquivo CSV.
     * 
     * @param savePath caminho onde salvar o arquivo
     * @throws IOException se houver erro ao escrever o arquivo
     */
    public void saveGame(final Path savePath) throws java.io.IOException {
        ensureStarted();
        GameStateSaver.saveGame(savePath, 
                               engine.allPlayers(), 
                               engine.currentPlayerIndex(),
                               engine.getDeck(),
                               engine.getBoard(),
                               engine.getBank());
    }

    // ==== Métodos públicos ====

    /** Rola os dados e resolve tudo que não depende do usuário. */
    public void rollAndResolve() {
        ensureStarted();
        engine.rollAndResolve();
    }

    /** Solicita compra da propriedade atual (se aplicável). */
    public boolean  chooseBuy() {
        ensureStarted();
        return engine.chooseBuy();
    }

    /** Solicita construção de casa em propriedade do jogador. */
    public boolean chooseBuildHouse() {
        ensureStarted();
        return engine.chooseBuildHouse();
    }

    /** Solicita construção de hotel em propriedade do jogador. */
    public boolean chooseBuildHotel() {
        ensureStarted();
        return engine.chooseBuildHotel();
    }

    /** Encerra o turno atual e passa para o próximo jogador. */
    public void endTurn() {
        ensureStarted();
        engine.endTurn();
    }
    
    // ==== Métodos para obter informações do jogo ====
    
    /** Retorna o índice do jogador atual. */
    public int getCurrentPlayerIndex() {
        ensureStarted();
        return engine.currentPlayerIndex();
    }
    
    /** Retorna o número total de jogadores. */
    public int getNumberOfPlayers() {
        ensureStarted();
        return engine.allPlayers().size();
    }
    
    /** Retorna a posição de um jogador no tabuleiro. */
    public int getPlayerPosition(int playerIndex) {
        ensureStarted();
        return engine.allPlayers().get(playerIndex).getPosition();
    }
    
    /** Retorna o nome de um jogador. */
    public String getPlayerName(int playerIndex) {
        ensureStarted();
        return engine.allPlayers().get(playerIndex).getName();
    }
    
    /** Retorna o saldo de um jogador. */
    public int getPlayerMoney(int playerIndex) {
        ensureStarted();
        return engine.allPlayers().get(playerIndex).getMoney();
    }
    
    /** Retorna se um jogador está na prisão. */
    public boolean isPlayerInJail(int playerIndex) {
        ensureStarted();
        return engine.allPlayers().get(playerIndex).isInJail();
    }
    
    /** Retorna os valores do último lance de dados (após rollAndResolve). */
    public DiceData getLastDiceData() {
        ensureStarted();
        final int[] vals = engine.lastRollValues();
        return new DiceData(vals[0], vals[1], vals[2] == 1);
    }

    /** Retorna true se o jogador atual está autorizado a rolar os dados. */
    public boolean isRollAllowed() {
        ensureStarted();
        return engine.isRollAllowed();
    }       
    
    /** Retorna string com motivo pelo qual a compra não é permitida, ou null se permitida. */
    public String getBuyNotAllowedReason() {
        ensureStarted();
        return engine.buyNotAllowedReason();
    }

    /** Retorna string com motivo pelo qual a construção de casa não é permitida, ou null se permitida. */
    public String getBuildHouseNotAllowedReason() {
        ensureStarted();
        return engine.buildHouseNotAllowedReason();
    }

    /** Retorna string com motivo pelo qual a construção de hotel não é permitida, ou null se permitida. */
    public String getBuildHotelNotAllowedReason() {
        ensureStarted();
        return engine.buildHotelNotAllowedReason();
    }

    /** Retorna a cor (string) de um jogador. */
    public PlayerColor getPlayerColor(int playerIndex) {
        ensureStarted();
        return engine.allPlayers().get(playerIndex).getColor();
    }

    /** Retorna o nome da square no índice fornecido. */
    public String getSquareName(final int index) {
        ensureStarted();
        return engine.getSquareName(index);
    }

    /** Retorna o tipo (classe simples) da square no índice fornecido. */
    public String getSquareType(final int index) {
        ensureStarted();
        return engine.getSquareType(index);
    }

    /** Retorna o índice da última carta retirada do baralho (ou -1). */
    public int getLastDrawedCardIndex() {
        ensureStarted();
        return engine.lastDrawedCardIndex();
    }

    /** Retorna se o jogador no índice fornecido está ativo/no jogo (não bankrupt). */
    public boolean isPlayerAlive(final int playerIndex) {
        ensureStarted();
        return engine.allPlayers().get(playerIndex).isAlive();
    }

    /** Retorna o nome da última propriedade/companhia em que um jogador caiu (ou null). */
    public String getLastLandedOwnableName() {
        ensureStarted();
        return engine.lastLandedOwnableName();
    }

    /** Retorna informações detalhadas de uma StreetOwnable no índice dado (ou null se não for Street). */
    public Ownables.Street getStreetOwnableInfo(final int index) {
        ensureStarted();
        return engine.getStreetOwnableInfo(index);
    }

    /** Retorna informações detalhadas de uma CompanyOwnable no índice dado (ou null se não for Company). */
    public Ownables.Company getCompanyOwnableInfo(final int index) {
        ensureStarted();
        return engine.getCompanyOwnableInfo(index);
    }
    
    /** Retorna os DTOs OwnableInfo (Street/Company) do jogador da vez, prontos para a View/Controller. */
    public List<OwnableInfo> getCurrentPlayerPropertyData() {
        ensureStarted();
        return engine.getCurrentPlayerPropertyData();
    }
    
    /** Vende a propriedade indicada (índice do board) do jogador da vez para o banco. */
    public void sellAtIndex(final int boardIndex) {
        ensureStarted();
        engine.sellAtIndex(boardIndex);
    }

    /** Retorna e limpa as transações ocorridas desde a última leitura. */
    public java.util.List<model.api.dto.Transaction> fetchAndClearTransactions() {
        ensureStarted();
        return engine.collectTransactions();
    }
    
    /** Retorna a(s) referência(s) do(s) vencedor(es) da partida. */
    public java.util.List<PlayerRef> getWinners() {
        ensureStarted();
        return engine.getWinners();
    }

    /** Retorna o número de jogadores atualmente "vivos" (ativos, não bankrupt). */
    public int getAlivePlayerCount() {
        ensureStarted();
        return engine.getAlivePlayerCount();
    }
    
    /**
     * Define valores para o próximo lance de dados (modo de teste).
     */
    public void setMockedDiceValues(final int d1, final int d2) {
        ensureStarted();
        engine.setMockedDiceValues(d1, d2);
    }
    
    /**
     * Remove valores dos dados (volta ao modo normal/aleatório).
     */
    public void clearMockedDiceValues() {
        ensureStarted();
        engine.clearMockedDiceValues();
    }
    
    // ==== Auxiliares internas ====

    private void ensureStarted() {
        if (!started)
            throw new IllegalStateException("Jogo ainda não foi iniciado. Chame startGame().");
    }

    private void ensureNotStarted() {
        if (started)
            throw new IllegalStateException("Jogo já iniciado.");
    }

    private void validatePlayerCount(final int n) {
        if (n < 2 || n > 6)
            throw new IllegalArgumentException("Quantidade de jogadores inválida (precisa ser entre 2 e 6).");
    }

    // ==== Tipos auxiliares ====

    /** Retorna os valores do último lance de dados em um pequeno DTO. */
    public record DiceData(int d1, int d2, boolean isDouble) {}
}
