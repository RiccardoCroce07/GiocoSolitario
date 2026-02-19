package solitairegame;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * VIEW - Responsabile del rendering grafico e della costruzione della UI.
 * Delega la logica al Controller tramite listener registrati.
 */
public class GameView extends JFrame {

    // ── Costanti grafiche ────────────────────────────────────────────────────

    public static final int CARD_WIDTH   = 100;
    public static final int CARD_HEIGHT  = 145;
    public static final int CARD_SPACING = 15;
    public static final int PILE_OFFSET  = 30;
    public static final int TABLEAU_Y    = 220;

    public static final Color FELT_GREEN  = new Color(13, 110, 53);
    public static final Color DARK_GREEN  = new Color(10, 80, 40);
    public static final Color LIGHT_GREEN = new Color(20, 140, 70);
    public static final Color GOLD        = new Color(255, 215, 0);
    public static final Color DARK_GOLD   = new Color(218, 165, 32);

    // ── Componenti UI ────────────────────────────────────────────────────────

    private JLabel timerLabel;
    private JLabel movesLabel;
    private JLabel difficultyLabel;
    public  GamePanel gamePanel;

    // ── Immagini carte ───────────────────────────────────────────────────────

    private Map<String, BufferedImage> cardImages;
    private BufferedImage cardBackImage;

    // ── Costruttore ──────────────────────────────────────────────────────────

    public GameView() {
        setTitle("🎴 Solitario Klondike 🎴");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        loadCardImages();
        buildUI();
    }

    private void buildUI() {
        gamePanel = new GamePanel();
        add(gamePanel, BorderLayout.CENTER);
        add(buildTopPanel(),    BorderLayout.NORTH);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    // ── Top panel ────────────────────────────────────────────────────────────

    private JPanel buildTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(DARK_GREEN);
        topPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 3, 0, GOLD),
            BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 5));
        statsPanel.setBackground(DARK_GREEN);

        JPanel timerPanel = createStatPanel("⏱️", "00:00");
        timerLabel = (JLabel) ((JPanel) timerPanel.getComponent(1)).getComponent(0);
        statsPanel.add(timerPanel);

        JPanel movesPanel = createStatPanel("🎯", "0");
        movesLabel = (JLabel) ((JPanel) movesPanel.getComponent(1)).getComponent(0);
        statsPanel.add(movesPanel);

        JPanel diffPanel = createStatPanel("⭐", "Facile");
        difficultyLabel = (JLabel) ((JPanel) diffPanel.getComponent(1)).getComponent(0);
        statsPanel.add(diffPanel);

        topPanel.add(statsPanel, BorderLayout.CENTER);
        return topPanel;
    }

    // ── Bottom panel ─────────────────────────────────────────────────────────

    /** Il controller chiama questo metodo DOPO aver aggiunto i listener. */
    public JPanel buildBottomPanel() {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        bottomPanel.setBackground(DARK_GREEN);
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(3, 0, 0, 0, GOLD),
            BorderFactory.createEmptyBorder(5, 0, 5, 0)
        ));

        JLabel diffLabel = new JLabel("Difficoltà:");
        diffLabel.setForeground(GOLD);
        diffLabel.setFont(new Font("Arial", Font.BOLD, 15));
        bottomPanel.add(diffLabel);

        // Il controller si registra su questi componenti
        JComboBox<String> difficultyBox = new JComboBox<>(
            new String[]{"🟢 Facile (1 carta)", "🔴 Difficile (3 carte)"});
        difficultyBox.setName("difficultyBox");
        difficultyBox.setFont(new Font("Arial", Font.BOLD, 13));
        difficultyBox.setBackground(new Color(230, 230, 230));
        bottomPanel.add(difficultyBox);

        JButton newGameButton = createStyledButton("🔄 Nuova Partita", new Color(50, 120, 200), Color.WHITE);
        newGameButton.setName("newGameButton");
        bottomPanel.add(newGameButton);

        JButton winButton = createStyledButton("🏆 Vittoria!", GOLD, DARK_GREEN);
        winButton.setName("winButton");
        bottomPanel.add(winButton);

        return bottomPanel;
    }

    // ── Aggiornamento etichette statistiche ──────────────────────────────────

    public void updateTimerLabel(int elapsedSeconds) {
        if (timerLabel != null)
            timerLabel.setText(String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60));
    }

    public void updateMovesLabel(int moveCount) {
        if (movesLabel != null)
            movesLabel.setText(String.valueOf(moveCount));
    }

    public void updateDifficultyLabel(String text) {
        if (difficultyLabel != null)
            difficultyLabel.setText(text);
    }

    // ── Dialogo vittoria ─────────────────────────────────────────────────────

    public void showVictoryDialog(int elapsedSeconds, int moveCount, GameModel.Difficulty difficulty) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titleLabel = new JLabel("🎉 COMPLIMENTI! 🎉");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(GOLD);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(10));

        JLabel sub = new JLabel("Hai completato il Solitario!");
        sub.setFont(new Font("Arial", Font.PLAIN, 16));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(sub);
        panel.add(Box.createVerticalStrut(20));

        addVictoryStat(panel, "⏱️ Tempo:",
            String.format("%02d:%02d", elapsedSeconds / 60, elapsedSeconds % 60));
        addVictoryStat(panel, "🎯 Mosse:", String.valueOf(moveCount));
        addVictoryStat(panel, "⭐ Difficoltà:",
            difficulty == GameModel.Difficulty.FACILE ? "Facile" : "Difficile");

        JOptionPane.showMessageDialog(this, panel, "Vittoria!", JOptionPane.PLAIN_MESSAGE);
    }

    private void addVictoryStat(JPanel panel, String label, String value) {
        JPanel statPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statPanel.setBackground(Color.WHITE);
        JLabel l = new JLabel(label); l.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel v = new JLabel(value); v.setFont(new Font("Arial", Font.PLAIN, 16));
        v.setForeground(new Color(50, 50, 50));
        statPanel.add(l); statPanel.add(v);
        panel.add(statPanel);
    }

    // ── Utility grafiche ─────────────────────────────────────────────────────

    private JPanel createStatPanel(String icon, String initialValue) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        panel.setBackground(DARK_GREEN);
        JLabel iconLabel = new JLabel(icon);
        iconLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));
        panel.add(iconLabel);
        JPanel textPanel = new JPanel(new GridLayout(0, 1));
        textPanel.setBackground(DARK_GREEN);
        JLabel valueLabel = new JLabel(initialValue);
        valueLabel.setFont(new Font("Arial", Font.BOLD, 22));
        valueLabel.setForeground(GOLD);
        textPanel.add(valueLabel);
        panel.add(textPanel);
        return panel;
    }

    public JButton createStyledButton(String text, Color bg, Color fg) {
        JButton button = new JButton(text);
        button.setFont(new Font("Arial", Font.BOLD, 14));
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(fg, 2),
            BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) { button.setBackground(bg.brighter()); }
            @Override public void mouseExited (java.awt.event.MouseEvent e) { button.setBackground(bg); }
        });
        return button;
    }

    // ── Caricamento immagini ─────────────────────────────────────────────────

    private void loadCardImages() {
        cardImages    = new HashMap<>();
        cardBackImage = null;

        Map<String, String> map = new LinkedHashMap<>();
        String[] suits  = {"hearts","diamonds","clubs","spades"};
        String[] ranks  = {"ace","2","3","4","5","6","7","8","9","10","jack","queen","king"};
        String[] symbols= {"♥","♦","♣","♠"};
        String[] rankSym= {"A","2","3","4","5","6","7","8","9","10","J","Q","K"};

        for (int s = 0; s < suits.length; s++) {
            for (int r = 0; r < ranks.length; r++) {
                String suffix = (r >= 10) ? "2" : "";   // jack/queen/king have "2" suffix
                String fname  = (r < 10)
                    ? ranks[r] + "_of_" + suits[s] + ".png"
                    : ranks[r] + "_of_" + suits[s] + "2.png";
                // special cases already handled by original mapping:
                map.put(fname, rankSym[r] + symbols[s]);
            }
        }
        // Fix ace of spades
        map.put("ace_of_spades2.png", "A♠");
        map.put("ace_of_spades.png",  "A♠");

        File dir = new File("cards_images");
        if (dir.exists() && dir.isDirectory()) {
            for (Map.Entry<String, String> entry : map.entrySet()) {
                File f = new File(dir, entry.getKey());
                if (f.exists()) {
                    try {
                        BufferedImage img = ImageIO.read(f);
                        if (img != null) cardImages.put(entry.getValue(), img);
                    } catch (IOException e) { System.err.println("❌ " + e.getMessage()); }
                }
            }
            File[] backs = dir.listFiles((d, n) -> {
                String l = n.toLowerCase();
                return l.contains("back") || l.contains("blue") || l.contains("dorso");
            });
            if (backs != null) for (File f : backs) {
                try { cardBackImage = ImageIO.read(f); break; }
                catch (IOException e) { System.err.println("❌ dorso: " + e.getMessage()); }
            }
        }
    }

    public Map<String, BufferedImage> getCardImages()  { return cardImages;    }
    public BufferedImage              getCardBackImage(){ return cardBackImage; }

    // ── GamePanel ─────────────────────────────────────────────────────────────

    /**
     * Pannello di disegno. Riceve i dati necessari dal Controller tramite
     * i setter e si occupa solo del rendering.
     */
    public class GamePanel extends JPanel {

        // Dati ricevuti dal controller per il rendering del drag
        private List<GameModel.Card> draggedCards  = new ArrayList<>();
        private Point                dragStart     = null;
        private Point                currentMousePos = null;
        private int                  sourceTableau = -1;
        private int                  sourceIndex   = -1;

        // Dati del modello (riferimento live)
        private GameModel model;

        public GamePanel() {
            setPreferredSize(new Dimension(900, 720));
            setBackground(FELT_GREEN);
        }

        public void setModel(GameModel model) { this.model = model; }

        public void setDragState(List<GameModel.Card> dragged, Point start,
                                 Point currentPos, int srcTableau, int srcIndex) {
            this.draggedCards   = dragged;
            this.dragStart      = start;
            this.currentMousePos = currentPos;
            this.sourceTableau  = srcTableau;
            this.sourceIndex    = srcIndex;
        }

        // ── paintComponent ───────────────────────────────────────────────────

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (model == null) return;

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            List<GameModel.Card> stock      = model.getStockPile();
            List<GameModel.Card> waste      = model.getWastePile();
            List<List<GameModel.Card>> founds  = model.getFoundations();
            List<List<GameModel.Card>> tab     = model.getTableau();

            int stockX = CARD_SPACING;
            int wasteX = CARD_SPACING + CARD_WIDTH + CARD_SPACING;

            // Stock
            if (!stock.isEmpty()) drawCardBack(g2d, stockX, CARD_SPACING);
            else                  drawEmptySlot(g2d, stockX, CARD_SPACING, "↻");

            // Waste
            if (!waste.isEmpty()) {
                int cardsToShow = Math.min(3, waste.size());
                int startIdx    = waste.size() - cardsToShow;
                for (int i = 0; i < cardsToShow; i++) {
                    GameModel.Card card = waste.get(startIdx + i);
                    if (!draggedCards.contains(card))
                        drawCard(g2d, card, wasteX + i * 20, CARD_SPACING);
                }
            } else {
                drawEmptySlot(g2d, wasteX, CARD_SPACING, "");
            }

            // Foundations
            String[] fSymbols = {"♥","♦","♣","♠"};
            for (int i = 0; i < 4; i++) {
                int fx = CARD_SPACING + (3 + i) * (CARD_WIDTH + CARD_SPACING);
                List<GameModel.Card> f = founds.get(i);
                if (f.isEmpty()) {
                    drawEmptySlot(g2d, fx, CARD_SPACING, fSymbols[i]);
                } else {
                    GameModel.Card top = f.get(f.size() - 1);
                    if (!draggedCards.contains(top)) drawCard(g2d, top, fx, CARD_SPACING);
                }
            }

            // Tableau
            for (int col = 0; col < 7; col++) {
                int tx = CARD_SPACING + col * (CARD_WIDTH + CARD_SPACING);
                List<GameModel.Card> pile = tab.get(col);
                if (pile.isEmpty()) {
                    drawEmptySlot(g2d, tx, TABLEAU_Y, "K");
                } else {
                    for (int i = 0; i < pile.size(); i++) {
                        GameModel.Card card = pile.get(i);
                        if (draggedCards.contains(card)) continue;
                        int cy = TABLEAU_Y + i * PILE_OFFSET;
                        if (card.isFaceUp()) drawCard(g2d, card, tx, cy);
                        else                 drawCardBack(g2d, tx, cy);
                    }
                }
            }

            // Drag shadow + cards
            if (!draggedCards.isEmpty() && dragStart != null && currentMousePos != null) {
                int offX = currentMousePos.x - dragStart.x;
                int offY = currentMousePos.y - dragStart.y;

                for (int i = 0; i < draggedCards.size(); i++) {
                    int x = 0, y = 0;
                    if (sourceTableau == -2) {
                        int show = Math.min(3, waste.size());
                        x = wasteX + (show - 1) * 20;
                        y = CARD_SPACING;
                    } else if (sourceTableau < -2) {
                        int fi = -(sourceTableau + 3);
                        x = CARD_SPACING + (3 + fi) * (CARD_WIDTH + CARD_SPACING);
                        y = CARD_SPACING;
                    } else if (sourceTableau >= 0) {
                        x = CARD_SPACING + sourceTableau * (CARD_WIDTH + CARD_SPACING);
                        y = TABLEAU_Y + sourceIndex * PILE_OFFSET + i * PILE_OFFSET;
                    }
                    // Ombra
                    g2d.setColor(new Color(0, 0, 0, 100));
                    g2d.fillRoundRect(x + offX + 5, y + offY + 5, CARD_WIDTH, CARD_HEIGHT, 12, 12);
                    g2d.setColor(new Color(0, 0, 0, 50));
                    g2d.fillRoundRect(x + offX + 8, y + offY + 8, CARD_WIDTH, CARD_HEIGHT, 12, 12);

                    drawCard(g2d, draggedCards.get(i), x + offX, y + offY);
                }
            }
        }

        // ── Metodi di disegno ────────────────────────────────────────────────

        private void drawCard(Graphics2D g2d, GameModel.Card card, int x, int y) {
            String key = card.getRank().toString() + card.getSuit().toString();
            BufferedImage img = cardImages.get(key);
            if (img != null) {
                g2d.setColor(new Color(200, 200, 200));
                g2d.fillRoundRect(x - 1, y - 1, CARD_WIDTH + 2, CARD_HEIGHT + 2, 12, 12);
                g2d.drawImage(img, x, y, CARD_WIDTH, CARD_HEIGHT, null);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
                g2d.setColor(new Color(200, 200, 200));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
                boolean red = card.getSuit() == GameModel.Card.Suit.HEARTS
                           || card.getSuit() == GameModel.Card.Suit.DIAMONDS;
                g2d.setColor(red ? new Color(200, 0, 0) : Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.drawString(card.getRank().toString(), x + 10, y + 40);
                g2d.drawString(card.getSuit().toString(), x + 10, y + 70);
            }
        }

        private void drawCardBack(Graphics2D g2d, int x, int y) {
            if (cardBackImage != null) {
                g2d.drawImage(cardBackImage, x, y, CARD_WIDTH, CARD_HEIGHT, null);
            } else {
                GradientPaint gp = new GradientPaint(
                    x, y, new Color(20, 60, 140),
                    x + CARD_WIDTH, y + CARD_HEIGHT, new Color(40, 90, 180));
                g2d.setPaint(gp);
                g2d.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 12, 12);
                g2d.setColor(DARK_GOLD);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(x + 3, y + 3, CARD_WIDTH - 6, CARD_HEIGHT - 6, 10, 10);
                g2d.setColor(new Color(255, 255, 255, 40));
                for (int i = 0; i < 4; i++)
                    for (int j = 0; j < 5; j++)
                        g2d.fillOval(x + 18 + i * 22, y + 18 + j * 28, 10, 10);
                g2d.setColor(GOLD);
                int cx = x + CARD_WIDTH / 2, cy = y + CARD_HEIGHT / 2;
                g2d.fillPolygon(new int[]{cx, cx+18, cx, cx-18}, new int[]{cy-25, cy, cy+25, cy}, 4);
            }
        }

        private void drawEmptySlot(Graphics2D g2d, int x, int y, String symbol) {
            g2d.setColor(DARK_GREEN);
            g2d.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 12, 12);
            g2d.setColor(LIGHT_GREEN);
            g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                0, new float[]{8, 6}, 0));
            g2d.drawRoundRect(x + 4, y + 4, CARD_WIDTH - 8, CARD_HEIGHT - 8, 10, 10);
            if (!symbol.isEmpty()) {
                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(symbol,
                    x + (CARD_WIDTH  - fm.stringWidth(symbol)) / 2,
                    y + ((CARD_HEIGHT - fm.getHeight()) / 2) + fm.getAscent());
            }
        }
    }
}
