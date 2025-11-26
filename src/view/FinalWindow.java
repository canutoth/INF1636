/* ===========================================================
 * FinalWindow ; janela final exibindo o(s) vencedor(es) da partida.
 * =========================================================== */

package view;

import model.api.dto.PlayerRef;
import view.ui.PlayerColorAwt;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class FinalWindow extends JFrame {

    private static final long serialVersionUID = 1L;

    public FinalWindow(List<PlayerRef> winners) {
        setTitle("Finished Game");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(400, 300);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel main = new JPanel();
        main.setLayout(new BorderLayout(10, 10));
        main.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        JLabel title = new JLabel("Finished Game");
        title.setFont(new Font("Arial", Font.BOLD, 20));
        title.setHorizontalAlignment(SwingConstants.CENTER);
        main.add(title, BorderLayout.NORTH);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        String subtitleText = "The winners are:";
        if (winners != null && winners.size() == 1) subtitleText = "The winner is:";
        JLabel subtitle = new JLabel(subtitleText);
        subtitle.setFont(new Font("Arial", Font.PLAIN, 14));
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        center.add(subtitle);
        center.add(Box.createVerticalStrut(12));

        if (winners == null || winners.isEmpty()) {
            JLabel none = new JLabel("(no winners)");
            none.setAlignmentX(Component.CENTER_ALIGNMENT);
            center.add(none);
        } else {
            for (PlayerRef pr : winners) {
                String text = pr.name();
                JLabel lbl = new JLabel(text);
                lbl.setFont(new Font("Arial", Font.BOLD, 16));
                Color c = PlayerColorAwt.toColor(pr.color());
                if (c != null) lbl.setForeground(c);
                lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
                center.add(lbl);
                center.add(Box.createVerticalStrut(6));
            }
        }

        main.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
        bottom.setLayout(new FlowLayout(FlowLayout.CENTER));
        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent ev) {
                // Termina a aplicação
                System.exit(0);
            }
        });
        bottom.add(closeBtn);

        main.add(bottom, BorderLayout.SOUTH);

        add(main);
    }
}
