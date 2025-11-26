/* ===========================================================
 * BoardPanel ; painel customizado para renderizar o tabuleiro.
 * Usa Graphics2D para desenhar o tabuleiro e os componentes do jogo.
 * =========================================================== */

package view;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.swing.*;
import model.api.dto.Ownables;
import model.api.dto.PlayerColor;
import view.ui.PlayerColorAwt;
import model.api.dto.Transaction;
/**
 * Painel customizado que renderiza o tabuleiro do Monopoly.
 * Utiliza Graphics2D para desenhar casas, jogadores e dados.
 */
public class BoardPanel extends JPanel {
    
    private static final long serialVersionUID = 1L;
    
    // Dimensões do painel
    private static final int PANEL_WIDTH = 1000;
    private static final int PANEL_HEIGHT = 800;
    
    // Configuração geométrica do tabuleiro (imagem 700x700)
    private static final int TOTAL_SQUARES = 40; // casas no tabuleiro
    private static final int BOARD_SIZE   = 700;   // lado da imagem
    private static final int CORNER_SIZE  = 100;   // 4 cantos 100x100
    private static final double EDGE_STEP = (BOARD_SIZE - 2.0 * CORNER_SIZE) / 9.0; // 500/9

    // SQUARE_SIZE fica obsoleto para a imagem real; mantenho só para fallback procedural, se quiser.
    private static final int SQUARE_SIZE = 64; // usado apenas no fallback sem imagem

    
    // Posições dos jogadores
    private final int[] playerPositions;
    // Se o jogador está ativo/visível (não bankrupt). true = desenhar
    private final boolean[] playerAlive;
    private final Color[] playerColors;
    private int numberOfPlayers = 0;  // Número real de jogadores ativos
    
    // Valores dos dados
    private int dice1 = 0;
    private int dice2 = 0;
    // Carta atual a exibir (index 0-based); -1 = nenhuma
    private int cardIndex = -1;
    // Propriedade/companhia atual a exibir pelo nome (null = nenhuma)
    private String propertyName = null;
    private String propertyType = null; // "street" ou "company"
    
    // Dados da propriedade atual (se houver)
    private Ownables.Street currentStreetInfo = null;
    private Ownables.Company currentCompanyInfo = null;
    
    // Cache de imagens
    private Map<String, BufferedImage> imageCache;
    
    // Última transação a ser exibida e para qual jogador (nome)
    private Transaction lastTransaction = null;
    private String lastTransactionForPlayer = null; // ex: "Player 1"

    public BoardPanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(200, 230, 200)); // Verde claro
        
        // Inicializa posições dos jogadores (máximo 6)
        playerPositions = new int[6];
        playerAlive = new boolean[6];
        // Inicializa cores dos jogadores a partir do enum PlayerColor usando o adaptador da view
        model.api.dto.PlayerColor[] pcs = model.api.dto.PlayerColor.values();
        playerColors = new Color[Math.min(pcs.length, 6)];
        for (int i = 0; i < playerColors.length; i++) {
            playerColors[i] = view.ui.PlayerColorAwt.toColor(pcs[i]);
        }
        // Inicialmente todos os jogadores estão vivos/visíveis
        for (int i = 0; i < playerAlive.length; i++) playerAlive[i] = true;
        
        // Inicializa cache de imagens
        imageCache = new HashMap<>();
        loadImages();
    }

    /**
     * Carrega as imagens dos assets.
     */
    private void loadImages() {
        try {
            // Carrega tabuleiro
            File boardFile = new File("src/view/assets/tabuleiro.png");
            if (boardFile.exists()) {
                imageCache.put("board", ImageIO.read(boardFile));
            } else {
                System.err.println("Board not found: " + boardFile.getAbsolutePath());
            }
            
            // Carrega dados (1-6)
            for (int i = 1; i <= 6; i++) {
                File diceFile = new File("src/view/assets/dados/die_face_" + i + ".png");
                if (diceFile.exists()) {
                    imageCache.put("dice" + i, ImageIO.read(diceFile));
                }
            }
            
            // Carrega piões (0-5)
            for (int i = 0; i < 6; i++) {
                File pinFile = new File("src/view/assets/pinos/pin" + i + ".png");
                if (pinFile.exists()) {
                    imageCache.put("pin" + i, ImageIO.read(pinFile));
                }
            }

            // Carrega cartas de Sorte/Reves (1..30)
            for (int i = 1; i <= 30; i++) {
                File chanceFile = new File("src/view/assets/sorteReves/chance" + i + ".png");
                if (chanceFile.exists()) {
                    imageCache.put("chance" + i, ImageIO.read(chanceFile));
                }
            }

        } catch (IOException e) {
            System.err.println("Error loading images: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Atualiza a posição de um jogador.
     */
    public void movePlayer(int playerIndex, int position) {
        if (playerIndex >= 0 && playerIndex < playerPositions.length) {
            playerPositions[playerIndex] = position % TOTAL_SQUARES;
            
            // Atualiza o número de jogadores se necessário
            if (playerIndex >= numberOfPlayers) {
                numberOfPlayers = playerIndex + 1;
            }
            
            repaint();
        }
    }
    
    /**
     * Define o número de jogadores no jogo.
     */
    public void setNumberOfPlayers(int count) {
        if (count >= 0 && count <= 6) {
            this.numberOfPlayers = count;
            // Ajusta flags de visibilidade: ativa apenas os primeiros `count` jogadores
            for (int i = 0; i < playerAlive.length; i++) {
                playerAlive[i] = (i < count);
                if (!playerAlive[i]) playerPositions[i] = -1;
            }
            repaint();
        }
    }

    /**
     * Define se o jogador deve ser desenhado no tabuleiro (true = visível).
     */
    public void setPlayerAlive(int playerIndex, boolean alive) {
        if (playerIndex >= 0 && playerIndex < playerAlive.length) {
            playerAlive[playerIndex] = alive;
            // Se estiver morto, esconde sua peça movendo-a para -1
            if (!alive) {
                playerPositions[playerIndex] = -1;
            }
            repaint();
        }
    }
    
    /**
     * Define os valores dos dados exibidos.
     */
    public void setDiceValues(int d1, int d2) {
        this.dice1 = d1;
        this.dice2 = d2;
        repaint();
    }

    /**
     * Define a transação a ser exibida no topo do painel.
     * @param tx transação (pode ser null para limpar)
     * @param currentPlayerName nome do jogador da vez (ex: "Player 1")
     */
    public void setTransaction(Transaction tx, String currentPlayerName) {
        this.lastTransaction = tx;
        this.lastTransactionForPlayer = currentPlayerName;
        repaint();
    }
    
    /**
     * Define qual carta deve ser exibida (index 0-based). -1 para não exibir.
     */
    public void setCard(int cardIndex) {
        this.cardIndex = cardIndex;
        repaint();
    }

    /** Define a propriedade/companhia atual a exibir (nome) e redesenha. */
    public void setPropertyInfo(String name, String type) {
        this.propertyName = name;
        this.propertyType = type;
        repaint();
    }
    
    /** Define os dados da rua a exibir (limpa dados de companhia). */
    public void setStreetInfo(Ownables.Street info) {
        this.currentStreetInfo = info;
        this.currentCompanyInfo = null; // garante exclusividade visual
        repaint();
    }

    /** Define os dados da companhia a exibir (limpa dados de rua). */
    public void setCompanyInfo(Ownables.Company info) {
        this.currentCompanyInfo = info;
        this.currentStreetInfo = null; // garante exclusividade visual
        repaint();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        
        // Ativa anti-aliasing para melhor qualidade
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        
        // Centraliza o tabuleiro
        int offsetX = (PANEL_WIDTH - BOARD_SIZE) / 2;
        int offsetY = (PANEL_HEIGHT - BOARD_SIZE) / 2;
        // Desenha caixa de transação no topo (se houver)
        drawTransactionBox(g2d, offsetX, offsetY);
        
        // Desenha o tabuleiro
        drawBoard(g2d, offsetX, offsetY);
        
        // Desenha os jogadores
        drawPlayers(g2d, offsetX, offsetY);
        
        // Desenha os dados
        drawDice(g2d, offsetX, offsetY);

        // Se houver propriedade, desenha a caixa de informações entre os dados e a carta
        if (propertyName != null && !propertyName.isBlank()) {
            drawOwnableStreetInfo(g2d, offsetX, offsetY);
            // Desenha uma segunda caixa menor por cima (overlay)
            drawOwnableCompanyInfo(g2d, offsetX, offsetY);
        }

        // Desenha a carta da vez (se houver)
        BufferedImage imgChance = getChanceCard();
        if (imgChance != null)
        {
        	drawCard(g2d, offsetX, offsetY, imgChance);
        }
        
        // Desenha a propriedade/companhia atual (se houver)
        BufferedImage imgProperty = getPropertyCard();
        if (imgProperty != null)
        {
        	drawCard(g2d, offsetX, offsetY, imgProperty);
        	
            if (currentStreetInfo != null) {
                drawOwnableStreetInfo(g2d, offsetX, offsetY);
            } else if (currentCompanyInfo != null) {
                drawOwnableCompanyInfo(g2d, offsetX, offsetY);
            }
            
        }
    }

    /** Desenha a caixa de transação no topo do tabuleiro. */
    private void drawTransactionBox(Graphics2D g2d, int offsetX, int offsetY) {
        if (lastTransaction == null || lastTransactionForPlayer == null) return;

        int boxW = BOARD_SIZE;
        int boxH = 32; 
        int x = offsetX;
        int y = offsetY - boxH - 10; // um pouco acima do tabuleiro

        // Fundo branco e borda
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(x, y, boxW, boxH, 12, 12);
        Stroke old = g2d.getStroke();
        g2d.setStroke(new BasicStroke(2));
        g2d.setColor(Color.GRAY);
        g2d.drawRoundRect(x, y, boxW, boxH, 12, 12);
        g2d.setStroke(old);

        // Padding e áreas
        int pad = 12;
        int leftStart = x + pad;
        int rightEnd = x + boxW - pad;

        // Fonte única construída para caber em uma linha
        Font nameFont = new Font("Arial", Font.BOLD, 13);
        Font amtFont = new Font("Arial", Font.BOLD, 13);
        Font smallFont = new Font("Arial", Font.PLAIN, 12);
        g2d.setFont(nameFont);
        FontMetrics fm = g2d.getFontMetrics();

        // Dados
        boolean isPayer = lastTransactionForPlayer.equals(lastTransaction.fromId);
        String playerName = lastTransactionForPlayer;
        java.awt.Color playerColor = (isPayer ? PlayerColorAwt.toColor(lastTransaction.fromColor) : PlayerColorAwt.toColor(lastTransaction.toColor));
        if (playerColor == null) playerColor = Color.BLACK;

        int amt = lastTransaction.amount;
        String signedForPlayer = (isPayer ? "-" : "+") + amt + "$";
        java.awt.Color amtColorForPlayer = isPayer ? Color.RED : new Color(0, 140, 20);

        // Lado direito: contraparte
        String counterName = lastTransaction.toId.equals(lastTransactionForPlayer) ? lastTransaction.fromId : lastTransaction.toId;
        model.api.dto.PlayerColor counterColorEnum = counterName.equals(lastTransaction.toId) ? lastTransaction.toColor : lastTransaction.fromColor;
        Color counterColor = (counterColorEnum == null) ? Color.BLACK : PlayerColorAwt.toColor(counterColorEnum);
        boolean isCounterReceiver = counterName.equals(lastTransaction.toId);
        String signedForCounter = (isCounterReceiver ? "+" : "-") + amt + "$";

        // Saldos
        int playerBalance = isPayer ? lastTransaction.fromBalanceAfter : lastTransaction.toBalanceAfter;
        int counterBalance = counterName.equals(lastTransaction.fromId) ? lastTransaction.fromBalanceAfter : lastTransaction.toBalanceAfter;

        // Compor segmentos à esquerda e desenhar em uma única linha: saldo final, nome, valor
        int baselineY = y + (boxH / 2) + (fm.getAscent() / 2) - 2;

        int cursorX = leftStart;

        // Saldo do jogador (pequeno, cinza) — mostrado primeiro
        g2d.setFont(smallFont);
        g2d.setColor(Color.DARK_GRAY);
        String balText = "$" + playerBalance;
        g2d.drawString(balText, cursorX, baselineY);
        cursorX += g2d.getFontMetrics().stringWidth(balText) + 8;

        // Nome do jogador (colorido)
        g2d.setFont(nameFont);
        g2d.setColor(playerColor);
        g2d.drawString(playerName, cursorX, baselineY);
        cursorX += g2d.getFontMetrics().stringWidth(playerName) + 8;

        // Valor para o jogador (colorido), mostrado por último
        g2d.setFont(amtFont);
        g2d.setColor(amtColorForPlayer);
        g2d.drawString(signedForPlayer, cursorX, baselineY);

        // Compor segmentos da direita e desenhar alinhado à borda direita
        // texto direito: signedForCounter + " " + counterName + " (" + $counterBalance + ")"
        g2d.setFont(amtFont);
        int wAmt = g2d.getFontMetrics().stringWidth(signedForCounter);
        g2d.setFont(nameFont);
        int wName = g2d.getFontMetrics().stringWidth(counterName);
        g2d.setFont(smallFont);
        String counterBalText = " $" + counterBalance;
        int wBal = g2d.getFontMetrics().stringWidth(counterBalText);

        int totalRightW = wAmt + 6 + wName + 6 + wBal;
        int rightCursor = rightEnd - totalRightW;

        // Desenha valor (colorido)
        g2d.setFont(amtFont);
        g2d.setColor(isCounterReceiver ? new Color(0, 140, 20) : Color.RED);
        g2d.drawString(signedForCounter, rightCursor, baselineY);
        rightCursor += wAmt + 6;

        // Desenha nome do contra‑partido (colorido ou preto para BANK)
        g2d.setFont(nameFont);
        g2d.setColor(counterColor);
        g2d.drawString(counterName, rightCursor, baselineY);
        rightCursor += wName + 6;

        // Desenha saldo do contra‑partido (pequeno, cinza)
        g2d.setFont(smallFont);
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString(counterBalText, rightCursor, baselineY);
    }

    /**
     * Retorna a carta atual a exibir (se card != -1). 
     */
	private BufferedImage getChanceCard() {
	    if (cardIndex == -1) return null;
	
	    String key = "chance" + (cardIndex + 1);
	
	    BufferedImage img = imageCache.get(key);

	    if (img == null) {
	    	
		     throw new IllegalStateException("Imagem da carta não encontrada para '" + cardIndex + "'");
		 }
	    
	    return img;
	 }
	
	/** 
	 * Retorna a imagem da propriedade/companhia atual a exibir (se property != null).
	*/
	private BufferedImage getPropertyCard() {
	 if (propertyName == null || propertyType == null) return null;
	
	 // Normaliza o nome
	 String normalized = propertyName.toLowerCase(Locale.ROOT); // Minúsculas
	 normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD).replaceAll("\\p{M}", "");  // Remove acentos
	 normalized = normalized.replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");  // Substitui espaços por underscore e remove caracteres indesejados 
	
	 // Tenta cache usando a chave normalizada
	 BufferedImage img = imageCache.get(normalized);
	 
     // Tenta disco (nomes exatos transformados)
     if (img == null) {
   
         String[] paths;
         if (propertyType.equals("street")) {
             paths = new String[] { "src/view/assets/territorios/" + normalized + ".png" };
         } else {
             paths = new String[] { "src/view/assets/companhias/" + normalized + ".png" };
         }
        	
		 for (String p : paths) {
             try {
                 File f = new File(p);
                 if (f.exists()) {
                     img = ImageIO.read(f);
                     imageCache.put(normalized, img); // cacheia com a chave normalizada
                     break;
                 }
             } catch (IOException ignore) { /* silencioso */ }
         }
     }
	
	
	 if (img == null) {
	
	     throw new IllegalStateException("Imagem da propriedade não encontrada para '" + propertyName + "'");
	 }
	 
	 return img;
	 
	}
	
	/** Desenha a imagem da carta de chance ou propriedade centralizada horizontalmente
	 *  e posicionada verticalmente em y ≈ 5/8 do tabuleiro.
	 */
	private void drawCard(Graphics2D g2d, int offsetX, int offsetY, BufferedImage img) {
	    int imgW = img.getWidth();
	    int imgH = img.getHeight();

	    int maxW = 200;
	    if (imgW > maxW) {
	        double scale = (double) maxW / imgW;
	        imgW = maxW;
	        imgH = (int) Math.round(imgH * scale);
	    }

	    int x = offsetX + (BOARD_SIZE - imgW) / 2;
	    int y = offsetY + (int) Math.round(BOARD_SIZE * (2.0 / 3.0) - imgH / 2.0);
        
	    g2d.drawImage(img, x, y, imgW, imgH, null);
	}

	/**
	 * Desenha uma caixa de informações com título "Owned By" centralizado
	 * e linhas "label : value" com valores alinhados à direita.
	 */
	private void drawOwnableBox(Graphics2D g2d, int offsetX, int offsetY, int boxWidth, int boxHeight, String ownedBy, PlayerColor borderColor, String[][] rows) {
	    // Mesma ancoragem vertical usada antes (entre dados e carta)
	    int boxCenterY = offsetY + (int) Math.round(BOARD_SIZE * 7.0 / 16.0) - 28;
	    int x = offsetX + (BOARD_SIZE - boxWidth) / 2;
	    int y = boxCenterY - boxHeight / 2;

	    // Caixa branca
	    g2d.setColor(Color.WHITE);
	    g2d.fillRoundRect(x, y, boxWidth, boxHeight, 10, 10);

	    // Borda
	    Stroke oldStroke = g2d.getStroke();
	    g2d.setStroke(new BasicStroke(3));
	    
	    if (borderColor != null) {
	    	 g2d.setColor(PlayerColorAwt.toColor(borderColor));
	    }else {
	         g2d.setColor(Color.GRAY);
	    }  
	   
	    g2d.drawRoundRect(x, y, boxWidth, boxHeight, 10, 10);
	    g2d.setStroke(oldStroke);

	    // Texto
	    g2d.setColor(Color.BLACK);
	    g2d.setFont(new Font("Arial", Font.PLAIN, 14));
	    FontMetrics fm = g2d.getFontMetrics();

	    int padding = 12;
	    int tx = x + padding;
	    int maxValueX = x + boxWidth - padding;

	    // Owned By centralizado
	    String owned = "Owned By: " + (ownedBy == null || ownedBy.isBlank() ? "None" : ownedBy);
	    int ownedW = fm.stringWidth(owned);
	    int cx = x + (boxWidth - ownedW) / 2;
	    int ty = y + padding + fm.getAscent();
	    g2d.drawString(owned, cx, ty);

	    // Linhas label/value
	    for (String[] row : rows) {
	        ty += fm.getHeight();
	        String label = row[0];
	        String value = row[1];
	        g2d.drawString(label, tx, ty);
	        int valW = fm.stringWidth(value);
	        g2d.drawString(value, maxValueX - valW, ty);
	    }
	}
	
	/** Desenha info de Street usando StreetOwnableInfo (DTO). */
	private void drawOwnableStreetInfo(Graphics2D g2d, int offsetX, int offsetY) {
	    if (currentStreetInfo == null) return;

	    // Extrai dados do DTO
	    var core = currentStreetInfo.core();
	    
	    String ownerName = "None";
	    PlayerColor ownerColor = null;	   
	    
	    if ( core.owner() != null ){
	        ownerName = core.owner().id();
	        ownerColor = core.owner().color();
	    }
	    
	    String price = core.propertyPrice() + "$";
	    String rent = currentStreetInfo.propertyActualRent() + "$";
	    String houses = String.valueOf(currentStreetInfo.propertyHouseNumber());
	    String hotel = currentStreetInfo.propertyHasHotel() ? "Yes" : "No";

	    // Linhas label/value
	    String[][] rows = new String[][]{
	        {"Price:", price},
	        {"Actual Rent:", rent},
	        {"Houses:", houses},
	        {"Hotel:", hotel}
	    };

	    int boxWidth = 260;
	    int boxHeight = 110;
	    drawOwnableBox(g2d, offsetX, offsetY, boxWidth, boxHeight, ownerName, ownerColor, rows);
	}

	/** Desenha info de Company usando CompanyOwnableInfo (DTO). */
	private void drawOwnableCompanyInfo(Graphics2D g2d, int offsetX, int offsetY) {
	    if (currentCompanyInfo == null) return;

	    var core = currentCompanyInfo.core();
	    
	    String ownerName = "None";
	    PlayerColor ownerColor = null;	   
	    
	    if ( core.owner() != null ) {
	        ownerName = core.owner().id();
	        ownerColor = core.owner().color();
	    }
	    
	    String price = core.propertyPrice() + "$";
	    String mult = currentCompanyInfo.propertyMultiplier() + "x";

	    String[][] rows = new String[][]{
	        {"Price:", price},
	        {"Multiplier:", mult}
	    };

	    int boxWidth = 220;
	    int boxHeight = 70;
	    drawOwnableBox(g2d, offsetX, offsetY, boxWidth, boxHeight, ownerName, ownerColor, rows);
	}

    /**
     * Desenha o tabuleiro (40 casas em formato quadrado).
     */
    private void drawBoard(Graphics2D g2d, int offsetX, int offsetY) {
        // Usa a imagem do tabuleiro se disponível
        if (imageCache.containsKey("board")) {
            BufferedImage boardImg = imageCache.get("board");
            // Desenha a imagem do tabuleiro ajustada ao tamanho
            g2d.drawImage(boardImg, offsetX, offsetY, BOARD_SIZE, BOARD_SIZE, null);
        } else {
            // Fallback: Desenha o tabuleiro proceduralmente
            
            // Fundo central
            g2d.setColor(new Color(220, 255, 220));
            int centerSize = BOARD_SIZE - 2 * SQUARE_SIZE;
            g2d.fillRect(offsetX + SQUARE_SIZE, offsetY + SQUARE_SIZE, centerSize, centerSize);
            
            // Desenha as 40 casas
            for (int i = 0; i < TOTAL_SQUARES; i++) {
                Point pos = getSquarePosition(i, offsetX, offsetY);
                drawSquare(g2d, pos.x, pos.y, i);
            }
        }
    }
    
    /**
     * Desenha uma casa individual do tabuleiro.
     */
    private void drawSquare(Graphics2D g2d, int x, int y, int squareIndex) {
        // Borda da casa
        g2d.setColor(Color.BLACK);
        g2d.drawRect(x, y, SQUARE_SIZE, SQUARE_SIZE);
        
        // Cor de fundo varia por posição
        Color bgColor = getSquareColor(squareIndex);
        g2d.setColor(bgColor);
        g2d.fillRect(x + 1, y + 1, SQUARE_SIZE - 2, SQUARE_SIZE - 2);
        
        // Número da casa
        g2d.setColor(Color.BLACK);
        g2d.setFont(new Font("Arial", Font.BOLD, 10));
        String label = String.valueOf(squareIndex);
        FontMetrics fm = g2d.getFontMetrics();
        int labelWidth = fm.stringWidth(label);
        g2d.drawString(label, x + (SQUARE_SIZE - labelWidth) / 2, y + SQUARE_SIZE - 5);
    }
    
    /**
     * Retorna a cor de fundo de uma casa baseada em sua posição.
     */
    private Color getSquareColor(int index) {
        // Casas especiais
        if (index == 0) return new Color(255, 200, 200); // Início (vermelho claro)
        if (index == 10) return new Color(220, 220, 220); // Prisão (cinza)
        if (index == 20) return new Color(255, 255, 200); // Estacionamento (amarelo claro)
        if (index == 30) return new Color(200, 200, 255); // Vá para prisão (azul claro)
        
        // Casas normais (tons variados)
        int group = (index / 10) % 4;
        switch (group) {
            case 0: return new Color(255, 240, 240);
            case 1: return new Color(240, 255, 240);
            case 2: return new Color(240, 240, 255);
            case 3: return new Color(255, 255, 240);
            default: return Color.WHITE;
        }
    }

    /**
     * Retorna o retângulo (x,y,w,h) exato da casa idx (0..39),
     * alinhado à imagem 700x700 com cantos 100x100 e passos 500/9.
     */
    private Rectangle getCellRect(int idx, int offsetX, int offsetY) {
        final int BS = BOARD_SIZE;
        final int C  = CORNER_SIZE;
        final double STEP = EDGE_STEP;

        // Cantos (quadrados 100x100)
        if (idx == 0)  return new Rectangle(offsetX + BS - C, offsetY + BS - C, C, C); // inf-dir
        if (idx == 10) return new Rectangle(offsetX,          offsetY + BS - C, C, C); // inf-esq
        if (idx == 20) return new Rectangle(offsetX,          offsetY,          C, C); // sup-esq
        if (idx == 30) return new Rectangle(offsetX + BS - C, offsetY,          C, C); // sup-dir

        // Lado inferior (1..9) — direita -> esquerda
        if (idx < 10) {
            int k = 10 - idx;                // k = 9..1 (distância a partir do canto direito)
            int x1 = offsetX + C + (int)Math.round((k - 1) * STEP);
            int x2 = offsetX + C + (int)Math.round(k * STEP);
            int y  = offsetY + BS - C;
            return new Rectangle(x1, y, x2 - x1, C);
        }

        // Lado esquerdo (11..19) — baixo -> cima
        if (idx < 20) {
            int k = 20 - idx;                // k = 9..1 (de baixo pra cima)
            int y1 = offsetY + C + (int)Math.round((k - 1) * STEP);
            int y2 = offsetY + C + (int)Math.round(k * STEP);
            int x  = offsetX;
            return new Rectangle(x, y1, C, y2 - y1);
        }

        // Lado superior (21..29) — esquerda -> direita
        if (idx < 30) {
            int k = idx - 20;                // k = 1..9
            int x1 = offsetX + C + (int)Math.round((k - 1) * STEP);
            int x2 = offsetX + C + (int)Math.round(k * STEP);
            int y  = offsetY;
            return new Rectangle(x1, y, x2 - x1, C);
        }

        // Lado direito (31..39) — cima -> baixo
        int k = idx - 30;                    // k = 1..9
        int y1 = offsetY + C + (int)Math.round((k - 1) * STEP);
        int y2 = offsetY + C + (int)Math.round(k * STEP);
        int x  = offsetX + BS - C;
        return new Rectangle(x, y1, C, y2 - y1);
    }
    
    private Point getSquarePosition(int squareIndex, int offsetX, int offsetY) {
        Rectangle r = getCellRect(squareIndex, offsetX, offsetY);
        return new Point(r.x, r.y); // canto superior-esquerdo da casa real
    }
    
    /**
     * Desenha os peões dos jogadores centralizados num grid 2x3 dentro da casa real.
     */
    private void drawPlayers(Graphics2D g2d, int offsetX, int offsetY) {
        final int pinSize = 25;
        final int spacing = 5;

        for (int i = 0; i < numberOfPlayers; i++) {
            // Se o jogador foi marcado como não vivo/visível, pula
            if (i < 0 || i >= playerAlive.length) continue;
            if (!playerAlive[i]) continue;
            int pos = playerPositions[i];
            if (pos < 0) continue;
            int idx = pos % TOTAL_SQUARES;
            Rectangle r = getCellRect(idx, offsetX, offsetY);

            // Tamanho do grid 2x3
            int totalGridW = 2 * pinSize + spacing;
            int totalGridH = 3 * pinSize + 2 * spacing;

            // Base centralizada dentro do retângulo da casa
            int baseX = r.x + Math.max(0, (r.width  - totalGridW) / 2);
            int baseY = r.y + Math.max(0, (r.height - totalGridH) / 2);

            // Posição do peão i no grid
            int col = i % 2;        // 0..1
            int row = i / 2;        // 0..2  (até 6 jogadores)

            int px = baseX + col * (pinSize + spacing);
            int py = baseY + row * (pinSize + spacing);

            // Desenha a imagem do pino ou círculo fallback
            BufferedImage pinImg = imageCache.get("pin" + i);
            if (pinImg != null) {
                g2d.drawImage(pinImg, px, py, pinSize, pinSize, null);
            } else {
                g2d.setColor(playerColors[i]);
                g2d.fillOval(px, py, pinSize, pinSize);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(px, py, pinSize, pinSize);
            }
        }
    }


    
    /**
     * Desenha os dados no centro do tabuleiro.
     */
    private void drawDice(Graphics2D g2d, int offsetX, int offsetY) {
        if (dice1 == 0 || dice2 == 0) return;
        
    int centerX = offsetX + BOARD_SIZE / 2;
    // Centraliza verticalmente na metade superior do tabuleiro
    int centerY = offsetY + BOARD_SIZE / 4;
        
        int diceSize = 60;
        int spacing = 15;
        
        // Usa imagens dos dados se disponíveis
        if (imageCache.containsKey("dice" + dice1) && imageCache.containsKey("dice" + dice2)) {
            BufferedImage dice1Img = imageCache.get("dice" + dice1);
            BufferedImage dice2Img = imageCache.get("dice" + dice2);
            
            // Desenha o primeiro dado
            g2d.drawImage(dice1Img, centerX - diceSize - spacing/2, centerY - diceSize/2, diceSize, diceSize, null);
            
            // Desenha o segundo dado
            g2d.drawImage(dice2Img, centerX + spacing/2, centerY - diceSize/2, diceSize, diceSize, null);
        } else {
            // Fallback: desenha dados proceduralmente
            drawSingleDice(g2d, centerX - diceSize - spacing/2, centerY - diceSize/2, diceSize, dice1);
            drawSingleDice(g2d, centerX + spacing/2, centerY - diceSize/2, diceSize, dice2);
        }
    }
    
    /**
     * Desenha um único dado com seus pontos.
     */
    private void drawSingleDice(Graphics2D g2d, int x, int y, int size, int value) {
        // Fundo branco com borda preta
        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(x, y, size, size, 10, 10);
        g2d.setColor(Color.BLACK);
        g2d.drawRoundRect(x, y, size, size, 10, 10);
        
        // Desenha os pontos baseado no valor
        g2d.setColor(Color.BLACK);
        int dotSize = 8;
        int offset = size / 4;
        
        // Posições padrão (grid 3x3)
        int cx = x + size / 2;
        int cy = y + size / 2;
        int left = x + offset;
        int right = x + size - offset;
        int top = y + offset;
        int bottom = y + size - offset;
        
        // Padrão de pontos para cada valor (índices: 0=canto superior esq, 1=canto superior dir, etc.)
        // Formato: cada linha é [posX, posY] dos pontos a desenhar
        int[][][] dicePatterns = {
            {},  // 0 (não usado)
            {{cx, cy}},  // 1: centro
            {{left, top}, {right, bottom}},  // 2: diagonal
            {{left, top}, {cx, cy}, {right, bottom}},  // 3: diagonal + centro
            {{left, top}, {right, top}, {left, bottom}, {right, bottom}},  // 4: quatro cantos
            {{left, top}, {right, top}, {cx, cy}, {left, bottom}, {right, bottom}},  // 5: quatro cantos + centro
            {{left, top}, {right, top}, {left, cy}, {right, cy}, {left, bottom}, {right, bottom}}  // 6: dois por coluna
        };
        
        // Desenha os pontos do padrão correspondente
        if (value >= 1 && value <= 6) {
            for (int[] dot : dicePatterns[value]) {
                g2d.fillOval(dot[0] - dotSize/2, dot[1] - dotSize/2, dotSize, dotSize);
            }
        }
    }
}