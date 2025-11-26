/* ===========================================================
 * Main ; ponto de entrada da aplicação.
 * Inicia a janela de configuração inicial.
 * =========================================================== */

package view;

import javax.swing.*;

/**
 * Classe principal que inicia a aplicação Monopoly.
 */
public class Main {
    
    public static void main(String[] args) {
        // Configura o Look and Feel do sistema operacional
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Error setting Look and Feel: " + e.getMessage());
        }
        
        // Inicia a aplicação na Event Dispatch Thread
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // Abre a janela inicial
                InitialWindow initialWindow = new InitialWindow();
                initialWindow.setVisible(true);
            }
        });
    }
}
