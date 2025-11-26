/* ===========================================================
 * InitialWindow ; janela de configuração inicial do jogo.
 * Permite escolher o número de jogadores (3 a 6).
 * =========================================================== */

package view;

import controller.GameController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Janela inicial para configuração do jogo.
 * Permite ao usuário escolher o número de jogadores antes de iniciar.
 */
public class InitialWindow extends JFrame {
    
    private static final long serialVersionUID = 1L;
    
    private final GameController controller;
    private JComboBox<String> playerCountCombo;
    
    public InitialWindow(GameController controller) {
        this.controller = controller;
        initializeUI();
    }
    
    public InitialWindow() {
        this(GameController.getInstance());
    }
    
    private void initializeUI() {
        setTitle("Monopoly - Initial Setup");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(400, 250);
        setLocationRelativeTo(null); // Centraliza na tela
        setResizable(false);
        
        // Painel principal
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        mainPanel.setBackground(new Color(240, 248, 255));
        
        // Painel do título
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(new Color(240, 248, 255));
        JLabel titleLabel = new JLabel("WELCOME TO MONOPOLY!");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        titleLabel.setForeground(new Color(25, 25, 112));
        titlePanel.add(titleLabel);
        
        // Painel de seleção
        JPanel selectionPanel = new JPanel();
        selectionPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 10, 10));
        selectionPanel.setBackground(new Color(240, 248, 255));
        
        JLabel selectLabel = new JLabel("Number of players:");
        selectLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        
        String[] options = {"3", "4", "5", "6"};
        playerCountCombo = new JComboBox<>(options);
        playerCountCombo.setSelectedIndex(1); // Padrão: 4 jogadores
        playerCountCombo.setFont(new Font("Arial", Font.PLAIN, 14));
        
        selectionPanel.add(selectLabel);
        selectionPanel.add(playerCountCombo);
        
        // Painel de botões
        JPanel buttonPanel = new JPanel();
        buttonPanel.setBackground(new Color(240, 248, 255));
        
        JButton startButton = new JButton("START GAME");
        startButton.setFont(new Font("Arial", Font.BOLD, 14));
        startButton.setBackground(new Color(180, 180, 180));
        startButton.setForeground(Color.BLACK);
        startButton.setFocusPainted(false);
        startButton.setPreferredSize(new Dimension(150, 40));
        
        startButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startGame();
            }
        });
        
        JButton loadButton = new JButton("LOAD GAME");
        loadButton.setFont(new Font("Arial", Font.BOLD, 14));
        loadButton.setBackground(new Color(180, 180, 180));
        loadButton.setForeground(Color.BLACK);
        loadButton.setFocusPainted(false);
        loadButton.setPreferredSize(new Dimension(150, 40));
        
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadGame();
            }
        });
        
        buttonPanel.add(startButton);
        buttonPanel.add(Box.createHorizontalStrut(10));
        buttonPanel.add(loadButton);
        
        // Monta o layout
        mainPanel.add(titlePanel, BorderLayout.NORTH);
        mainPanel.add(selectionPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
    }
    
    /**
     * Inicia o jogo com o número de jogadores selecionado.
     */
    private void startGame() {
        try {
            int numberOfPlayers = Integer.parseInt((String) playerCountCombo.getSelectedItem());
            
            // Cria a janela principal
            GameWindow gameWindow = new GameWindow(numberOfPlayers);

            // Inicia o jogo através do controller
            controller.startNewGame(numberOfPlayers);

            // Fecha esta janela e mostra a janela principal
            this.dispose();
            gameWindow.setVisible(true);
            
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Error starting game: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
        }
    }
    
    /**
     * Carrega um jogo salvo de um arquivo CSV.
     */
    private void loadGame() {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Load Game State");
            fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Text Files (*.txt)", "txt"));
            
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                java.io.File file = fileChooser.getSelectedFile();
                java.nio.file.Path loadPath = file.toPath();
                
                // Primeiro carrega para descobrir o número de jogadores
                // Lê o arquivo diretamente aqui para evitar dependência de classes internas do model
                int numberOfPlayers = 4; // Padrão
                try (java.io.BufferedReader reader = java.nio.file.Files.newBufferedReader(loadPath)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("numberOfPlayers,")) {
                            numberOfPlayers = Integer.parseInt(line.split(",")[1]);
                            break;
                        }
                    }
                }
                
                // Cria a janela principal
                GameWindow gameWindow = new GameWindow(numberOfPlayers);
                
                // Carrega o jogo através do controller
                boolean success = controller.loadGame(loadPath);
                
                if (success) {
                    // Fecha esta janela e mostra a janela principal
                    this.dispose();
                    gameWindow.setVisible(true);
                } else {
                    gameWindow.dispose();
                    JOptionPane.showMessageDialog(
                        this,
                        "Failed to load game. Check the log for details.",
                        "Load Error",
                        JOptionPane.ERROR_MESSAGE
                    );
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(
                this,
                "Error loading game: " + ex.getMessage(),
                "Error",
                JOptionPane.ERROR_MESSAGE
            );
            ex.printStackTrace();
        }
    }
}
