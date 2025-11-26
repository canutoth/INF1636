package view;

import controller.GameController;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import model.api.dto.OwnableInfo;
import model.api.dto.Ownables;

public final class PlayerPropertiesWindow extends JDialog {

    private final GameController controller;
    private List<OwnableInfo> items; 
    private final JPanel listPanel;

    public PlayerPropertiesWindow(Frame owner, List<OwnableInfo> items) {
        this(owner, GameController.getInstance(), items);
    }

    public PlayerPropertiesWindow(Frame owner, GameController controller, List<OwnableInfo> items) {
        super(owner, "Player Properties", true);
        this.controller = controller;
        this.items = (items != null) ? items : List.of();

        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(300, 640);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(0, 0));

        // Área de listagem com scroll
        listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JScrollPane scroll = new JScrollPane(listPanel);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        reloadList();
    }

    /** Atualiza os itens e redesenha */
    public void updateItems(List<OwnableInfo> newItems) {
        this.items = (newItems != null) ? newItems : List.of();
        if (SwingUtilities.isEventDispatchThread()) {
            reloadList();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    reloadList();
                }
            });
        }
    }

    private void reloadList() {
        listPanel.removeAll();

        if (items.isEmpty()) {
            listPanel.add(renderEmptyState());
        } else {
            int n = 1;
            for (OwnableInfo it : items) {
                listPanel.add(buildPropertyCard(n++, it));
                listPanel.add(Box.createVerticalStrut(10));
            }
        }

        listPanel.revalidate();
        listPanel.repaint();
    }

    // ===== UI helpers =====

    private JComponent renderEmptyState() {
        JPanel empty = new JPanel(new BorderLayout());
        empty.setBorder(new EmptyBorder(40, 20, 40, 20));
        JLabel l = new JLabel("<html><div style='text-align:center;'>No properties owned.</div></html>", SwingConstants.CENTER);
        l.setFont(l.getFont().deriveFont(Font.PLAIN, 14f));
        empty.add(l, BorderLayout.CENTER);
        return empty;
    }

    private JComponent buildPropertyCard(int number, OwnableInfo it) {
        // Card: contêiner
        JPanel card = new JPanel(new BorderLayout(10, 10));
        card.setBorder(
        	    javax.swing.BorderFactory.createCompoundBorder(
        	        new javax.swing.border.LineBorder(new java.awt.Color(200, 200, 200), 1, true),
        	        new javax.swing.border.EmptyBorder(12, 12, 12, 12)
        	    )
        	);
        card.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, 160));

        card.setBackground(Color.WHITE);
        
        var core = it.core(); // info comum
        
        // Cabeçalho
        JLabel title = new JLabel("[" + number + "] " + core.propertyName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 15f));

        // Subtítulo com dados comuns
 
        String ownerText = (core.owner() != null)
                ? core.owner().id() + " (" + core.owner().color().name() + ")"
                : "—";
        JLabel subtitle = new JLabel(
                html(
                        "Index: <b>" + core.boardIndex() + "</b>  &nbsp;&nbsp;|&nbsp;&nbsp; " +
                        "Owner: <b>" + ownerText + "</b>"
                )
        );
        subtitle.setFont(subtitle.getFont().deriveFont(Font.PLAIN, 12f));
        subtitle.setForeground(new Color(80, 80, 80));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(title);
        header.add(Box.createVerticalStrut(4));
        header.add(subtitle);

        card.add(header, BorderLayout.NORTH);

        // Corpo com grid de atributos
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0; gc.gridy = 0;
        gc.anchor = GridBagConstraints.WEST;
        gc.insets = new Insets(2, 0, 2, 16);

        // Campos comuns
        addField(body, gc, "Price", "$" + core.propertyPrice());
        addField(body, gc, "Sell Value", "$" + core.propertySellValue());

        // Específicos por tipo
        if (it instanceof Ownables.Street s) {
            addField(body, gc, "Rent (now)", "$" + s.propertyActualRent());
            addField(body, gc, "Houses", String.valueOf(s.propertyHouseNumber()));
            addField(body, gc, "Hotel", s.propertyHasHotel() ? "yes" : "no");
        } else if (it instanceof Ownables.Company c) {
            addField(body, gc, "Multiplier", String.valueOf(c.propertyMultiplier()));
        }

        card.add(body, BorderLayout.CENTER);
        
        // Ações (lado direito)
        JButton sellButton = new JButton("Sell ($" + core.propertySellValue() + ")");
        sellButton.setFocusPainted(false);

        sellButton.addActionListener(new java.awt.event.ActionListener() {
            @Override
            public void actionPerformed(java.awt.event.ActionEvent e) {
                controller.attemptSell(core.boardIndex());
                dispose(); // Fecha após vender
            }
        });
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        actions.setOpaque(false);
        actions.add(sellButton);

        card.add(actions, BorderLayout.EAST);

        return card;
    }

    private static void addField(JPanel body, GridBagConstraints gc, String label, String value) {
        JLabel l = new JLabel(label + ": ");
        l.setFont(l.getFont().deriveFont(Font.BOLD, 12f));
        JLabel v = new JLabel(value);
        v.setFont(v.getFont().deriveFont(Font.PLAIN, 12f));

        // coluna 1: label
        gc.gridx = 0;
        body.add(l, gc);

        // coluna 2: valor
        gc.gridx = 1;
        body.add(v, gc);

        // próxima linha
        gc.gridy++;
    }

    private static String html(String s) { return "<html>" + s + "</html>"; }
}
