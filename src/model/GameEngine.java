/* ===========================================================
 * GameEngine ; motor de regras/turno (camada Model)
 * =========================================================== */

package model;

import java.util.List;
import java.util.Objects;
import model.api.dto.OwnableInfo;
import model.api.dto.Ownables; 
import model.api.dto.PlayerRef;
import model.api.dto.Transaction;

final class GameEngine {

    // Dependências e estado do turno 
    private final Board board;
    private final List<Player> players;
    private final Deck deck;
    private final EconomyService economy;

    private int currentPlayerIndex;
    private DiceRoll lastRoll;
    private int lastRollerIndex = -1;
    private int lastDrawedCardIndex = -1;
    private String lastLandedOwnableName = null;
    private boolean hasBuiltThisTurn = false;
    
    // Mock de dados para testes
    private Integer mockedDice1;
    private Integer mockedDice2;

    GameEngine(final Board board,
               final List<Player> players,
               final Deck deck,
               final EconomyService economy,
               final int startIndex) {
        this.board   = Objects.requireNonNull(board, "board");
        this.players = Objects.requireNonNull(players, "players");
        this.deck    = Objects.requireNonNull(deck, "deck");
        this.economy = Objects.requireNonNull(economy, "economy");
        this.currentPlayerIndex = startIndex;
    }

   // Início do turno: limpa estado do dado. 
    void beginTurn() {
        this.lastRoll = null;
        this.hasBuiltThisTurn = false;
    }
    
    // Aplica regras de saída da prisão (dupla ou cartão). 
    void applyJailRules(final DiceRoll roll) {
        final Player p = currentPlayer();
        if (!p.isInJail()) return;

        if (roll.isDouble()) {
            p.setInJail(false);
            return;
        }
        
        if (p.consumeGetOutOfJailCard()) {
            p.setInJail(false);
            deck.returnGetOutOfJailCardToBottom();
        }
        // Sem multa nesta edição; se não saiu, permanece preso.
    }

    // Move o jogador da vez pelo tabuleiro. Ignora se estiver preso. 
    void moveBy(final int steps) {
        final Player p = currentPlayer();
        if (p.isInJail()) return;

        final int from = p.getPosition();
        final int to = board.nextPosition(from, steps);

        // Se o movimento faz o jogador cruzar a linha de partida, credita o bônus antes de mover.
        // Detectamos crossing quando from + steps >= board.size().
        if ((from + steps) >= board.size()) {
            economy.creditPassStart(p);
        }

        p.moveTo(to);
    }

    // Resolve o efeito da casa onde o jogador parou. 
    void onLand() {
        final Player p = currentPlayer();
        final Square sq = board.squareAt(p.getPosition());
        // Registra o nome de uma ownable se for o caso (para notificação/visualização)
        if (sq instanceof OwnableSquare) {
            // armazenamos o nome da propriedade/companhia para a API
            this.lastLandedOwnableName = sq.name();
        } else {
            this.lastLandedOwnableName = null;
        }
        sq.onLand(p, this, economy);
    }
    
    // Tira uma carta do baralho e utiliza
    void drawAndUseCard(Player player) {
        final Card card = deck.draw();
        this.lastDrawedCardIndex = card.getId();
        card.applyEffect(player, this, economy);
  
    }
    
    // Envia o jogador para a prisão. 
    void sendToJail(final Player player) {
        player.setInJail(true);
        player.moveTo(board.jailIndex());
    }
    
    // ===== CHAMADAS PELA API =====
    // ===== CHAMADAS PELA API =====

    /* ===========================================================
     * Executa a jogada completa: rolar dados → aplicar prisão → mover → resolver casa.
     * =========================================================== */
    void rollAndResolve() {
        // Regra: bloqueia tentativa de rolar caso o jogador atual seja quem rolou por último.
        if (!isRollAllowed()) {
            return;
        }
        
        final Player p = currentPlayer();

        // Registra quem iniciou a rodada (rolou os dados)
        this.lastRollerIndex = currentPlayerIndex;

        // Rola os dados e guarda
        final DiceRoll roll = roll();
        applyJailRules(roll);

        // Se estiver preso, não move
        if (p.isInJail()) {
            return;
        }

        // Move o jogador
        moveBy(roll.getSum());

        // Resolve efeito da casa
        onLand();
    }

    /* ===========================================================
     * Compra da propriedade atual (se aplicável).
     * =========================================================== */
    boolean chooseBuy() {
        final Player player = currentPlayer();
        
        final Square sq = board.squareAt(player.getPosition());
        
        if (!(sq instanceof OwnableSquare)) return false;
        
        final OwnableSquare property = (OwnableSquare) sq;
        
        final boolean hasPurchased = economy.attemptBuy(player, property);

        if (hasPurchased) {
            this.hasBuiltThisTurn = true;
        }

        return hasPurchased;
    }

    /* ===========================================================
     * Construção de casa em propriedade do jogador.
     * =========================================================== */
    boolean chooseBuildHouse() {
        // Regra: apenas 1 construção/compra por jogador por turno
        if (this.hasBuiltThisTurn) return false;

        final Player player = currentPlayer();
        
        final Square sq = board.squareAt(player.getPosition());
        
        if (!(sq instanceof StreetOwnableSquare)) return false;
        
        final StreetOwnableSquare property = (StreetOwnableSquare) sq;

        final boolean built = economy.attemptBuildHouse(player, property);

        if (built) {
            this.hasBuiltThisTurn = true;
        }

        return built;
    }

    /* ===========================================================
     * Construção de hotel em propriedade do jogador.
     * =========================================================== */
    boolean chooseBuildHotel() {
        // Regra: apenas 1 construção/compra por jogador por turno
        if (this.hasBuiltThisTurn) return false;

        final Player player = currentPlayer();
        
        final Square sq = board.squareAt(player.getPosition());
        
        if (!(sq instanceof StreetOwnableSquare)) return false;
        
        final StreetOwnableSquare property = (StreetOwnableSquare) sq;

        final boolean built = economy.attemptBuildHotel(player, property);

        if (built) {
            this.hasBuiltThisTurn = true;
        }

        return built;
    }

    /* ===========================================================
     * Vende a propriedade do indice enviado para o banco.
     * =========================================================== */
    void sellAtIndex(final int boardIndex) {
        final Square sq = board.squareAt(boardIndex);
        final OwnableSquare prop = (OwnableSquare) sq;
        final Player player = currentPlayer();
        economy.buybackPropertyToPlayer(prop, player);
    }

    /* ===========================================================
     * Finaliza o turno e retorna o índice do próximo jogador.
     * =========================================================== */
    int endTurn() {
        this.lastRoll = null;
        this.hasBuiltThisTurn = false;

    	int n = players.size();
        do {
            currentPlayerIndex = (currentPlayerIndex + 1) % n;
        } while (!players.get(currentPlayerIndex).isAlive());
        return currentPlayerIndex;
    }

    // ===== AUXILIARES =====
    // ===== AUXILIARES =====

    /* Retorna o resultado do dado rolado. */
    private DiceRoll roll() {
        // Se há valores mockados, usa-os e limpa
        if (mockedDice1 != null && mockedDice2 != null) {
            this.lastRoll = new DiceRoll(mockedDice1, mockedDice2);
            // Limpa os valores mockados após uso (single-use)
            this.mockedDice1 = null;
            this.mockedDice2 = null;
        } else {
            // Modo normal: aleatório
            this.lastRoll = new DiceRoll();
        }
        return lastRoll;
    }

    /* Retorna o resultado do último dado rolado. */
    DiceRoll lastRoll() { return lastRoll; }

    /* Retorna o jogador atual. */
    Player currentPlayer() { return players.get(currentPlayerIndex); }

    /* Retorna o índice da última carta retirada do baralho (ou -1). */
    int lastDrawedCardIndex() { return lastDrawedCardIndex; }

    /* Nome da última propriedade/companhia em que um jogador caiu (ou null). */
    String lastLandedOwnableName() { return lastLandedOwnableName; }

    // ===== AUXILIARES API =====
    // ===== AUXILIARES API =====

    /* Retorna os DTOs das propriedades do jogador atual */
	 List<OwnableInfo> getCurrentPlayerPropertyData() {
	     final int[] indices = currentPlayer().getPropertiesIndex();
	     final java.util.List<OwnableInfo> out = new java.util.ArrayList<>(indices.length);
	
	     for (int idx : indices) {
	         final Square sq = board.squareAt(idx);
	
	         if (sq instanceof StreetOwnableSquare) {
	             final Ownables.Street dto = getStreetOwnableInfo(idx); 
	             if (dto != null) out.add(dto);
	         } else if (sq instanceof CompanyOwnableSquare) {
	             final Ownables.Company dto = getCompanyOwnableInfo(idx); 
	             if (dto != null) out.add(dto);
	         }
	     }
	
	     return out;
	 }
    
    /* Retorna a lista de todos os jogadores (imutável). */
    List<Player> allPlayers() {
        return List.copyOf(players);
    }

    /* Retorna o índice do jogador atual (sem alterar estado). */
    int currentPlayerIndex() { return currentPlayerIndex; }

    /* Retorna o número de jogadores ativos (não bankrupt). */
    int getAlivePlayerCount() {
        int cnt = 0;
        for (Player p : players) {
            if (p.isAlive()) cnt++;
        }
        return cnt;
    }

    /* Retorna os valores do último lance como um array int[3] {d1,d2,isDoubleFlag} */
    int[] lastRollValues() {
        return new int[] { lastRoll.getD1(), lastRoll.getD2(), lastRoll.isDouble() ? 1 : 0 };
    }

    /* Retorna se o jogador atual está autorizado a rolar os dados. */
    boolean isRollAllowed() {
        return this.lastRollerIndex != this.currentPlayerIndex;
    }

    /* Retorna o nome da square no índice dado. */
    String getSquareName(final int index) {
        return board.squareAt(index).name();
    }
    /* Retorna o tipo (classe simples) da square no índice dado. */
    String getSquareType(final int index) {
        return board.squareAt(index).type();
    }
 
    /**
     * Retorna uma lista de PlayerRef representando o(s) vencedor(es) da partida.
     * O critério é o maior montante de dinheiro entre os jogadores (empates permitidos).
     */
    java.util.List<PlayerRef> getWinners() {
        int max = Integer.MIN_VALUE;
        for (Player p : players) {
            if (p.getMoney() > max) max = p.getMoney();
        }
        final java.util.List<PlayerRef> res = new java.util.ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            final Player p = players.get(i);
            if (p.getMoney() == max) {
                res.add(toPlayerRef(p));
            }
        }
        return java.util.Collections.unmodifiableList(res);
    }

    // ============ SUPORTE A LOG ============
    // ============ SUPORTE A LOG ============

    /** Retorna uma mensagem explicando por que a compra NÃO é permitida, ou null se permitida. */
    String buyNotAllowedReason() {
        final Player player = currentPlayer();
        final Square sq = board.squareAt(player.getPosition());
        if (!(sq instanceof OwnableSquare)) return "Not a buyable property";
        final OwnableSquare prop = (OwnableSquare) sq;
        if (prop.hasOwner()) return "Property already owned";
        if (!player.canAfford(prop.getPrice())) {
            final int missing = player.howMuchMissing(prop.getPrice());
            return "Insufficient funds: missing " + missing;
        }
        return null; // allowed
    }

    /** Retorna motivo pelo qual a construção de casa NÃO é permitida, ou null se permitida. */
    String buildHouseNotAllowedReason() {
        return buildNotAllowedReasonHelper(true);
    }

    /** Retorna motivo pelo qual a construção de hotel NÃO é permitida, ou null se permitida. */
    String buildHotelNotAllowedReason() {
        return buildNotAllowedReasonHelper(false);
    }

    /** Helper method para validar construção (house ou hotel). */
    private String buildNotAllowedReasonHelper(boolean isHouse) {
        final Player player = currentPlayer();
        final Square sq = board.squareAt(player.getPosition());
        if (!(sq instanceof StreetOwnableSquare)) return "Not a street (cannot build)";
        final StreetOwnableSquare street = (StreetOwnableSquare) sq;
        if (!street.hasOwner() || street.getOwner() != player) return "You don't own this property";
        if (this.hasBuiltThisTurn) return "Already built once this turn";
        
        if (isHouse) {
            if (!street.canBuildHouse()) return "Cannot build more houses (max 4 houses)";
            final int cost = street.getHouseCost();
            if (!player.canAfford(cost)) {
                final int missing = player.howMuchMissing(cost);
                return "Insufficient funds: missing " + missing;
            }
        } else {
            if (!street.canBuildHotel()) return "Cannot build hotel (need at least 1 house)";
            final int cost = street.getHotelCost();
            if (!player.canAfford(cost)) {
                final int missing = player.howMuchMissing(cost);
                return "Insufficient funds: missing " + missing;
            }
        }
        return null;
    }
    
    // ============ MONTAGEM DTO ============
    // ============ MONTAGEM DTO ============

    private PlayerRef toPlayerRef(final Player owner) {
    if (owner == null) return null;
    return new PlayerRef(owner.getId(), owner.getName(), owner.getColor());
    }

    /** Monta o Core comum (owner + price + sellValue). */
    private OwnableInfo.Core buildOwnableCore(final Player owner, final String propertyName, final int boardIndex, final int price, final int sellValue) {
        final PlayerRef pref = toPlayerRef(owner);

        return new OwnableInfo.Core(pref, propertyName ,boardIndex, price, sellValue);
    }

    Ownables.Street getStreetOwnableInfo(final int index) {
        final Square sq = board.squareAt(index);
        if (!(sq instanceof StreetOwnableSquare)) return null;
        final StreetOwnableSquare street = (StreetOwnableSquare) sq;

        // Parte comum
        final int sellValue = economy.evaluateSellValue(street);
        final OwnableInfo.Core core = buildOwnableCore(street.getOwner(), street.name(), street.index(), street.getPrice(), sellValue);

        // Parte específica (rua)
        final int rent   = street.calcRent(this);
        final int houses = street.getHouses();
        final boolean hotel = street.hasHotel();

        return new Ownables.Street(core, rent, houses, hotel);
    }


    Ownables.Company getCompanyOwnableInfo(final int index) {
        final Square sq = board.squareAt(index);
        if (!(sq instanceof CompanyOwnableSquare)) return null;
        final CompanyOwnableSquare company = (CompanyOwnableSquare) sq;

        // Parte comum
        final int sellValue = economy.evaluateSellValue(company);
        final OwnableInfo.Core core = buildOwnableCore(company.getOwner(), company.name(), company.index(), company.getPrice(), sellValue);

        // Parte específica (companhia)
        final int multiplier = company.getMultiplier();

        return new Ownables.Company(core, multiplier);
    }
    

    /**
     * Coleta (e limpa) as transações acumuladas na economia/banco. 
     */
    java.util.List<Transaction> collectTransactions() {
        return economy.drainTransactionLog();
    }

    // ============ MOCK DE DADOS (TESTES) ============
    // ============ MOCK DE DADOS (TESTES) ============

    /**
     * Define valores mockados para o próximo lance de dados.
     * Valores serão consumidos no próximo roll() e então limpos.
     */
    void setMockedDiceValues(final int d1, final int d2) {
        if (d1 < 1 || d1 > 6 || d2 < 1 || d2 > 6) {
            throw new IllegalArgumentException("Dice values must be between 1 and 6");
        }
        this.mockedDice1 = d1;
        this.mockedDice2 = d2;
    }
    
    /**
     * Remove valores mockados (volta ao modo normal/aleatório).
     */
    void clearMockedDiceValues() {
        this.mockedDice1 = null;
        this.mockedDice2 = null;
    }
    
    /**
     * Retorna o deck (para salvar estado).
     */
    Deck getDeck() {
        return deck;
    }
    
    /**
     * Retorna o board (para salvar estado).
     */
    Board getBoard() {
        return board;
    }
    
    /**
     * Retorna o banco (para salvar estado).
     */
    Bank getBank() {
        return economy.getBank();
    }
}

