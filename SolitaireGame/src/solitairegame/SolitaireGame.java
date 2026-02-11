package solitairegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;
import javax.imageio.ImageIO;

public class SolitaireGame extends JFrame {
    private static final int CARD_WIDTH = 100;
    private static final int CARD_HEIGHT = 145;
    private static final int CARD_SPACING = 15;
    private static final int PILE_OFFSET = 30;
    
    // Colori tema migliorato
    private static final Color FELT_GREEN = new Color(13, 110, 53);
    private static final Color DARK_GREEN = new Color(10, 80, 40);
    private static final Color LIGHT_GREEN = new Color(20, 140, 70);
    private static final Color GOLD = new Color(255, 215, 0);
    private static final Color DARK_GOLD = new Color(218, 165, 32);
    
    private Deck deck;
    private List<Card> stockPile;
    private List<Card> wastePile;
    private List<List<Card>> foundations;
    private List<List<Card>> tableau;
    
    private Card selectedCard;
    private Point dragStart;
    private List<Card> draggedCards;
    private int sourceTableau = -1;
    private int sourceIndex = -1;
    
    private GamePanel gamePanel;
    private Map<String, BufferedImage> cardImages;
    private BufferedImage cardBackImage;
    
    // Timer e statistiche
    private javax.swing.Timer gameTimer;
    private int elapsedSeconds = 0;
    private int moveCount = 0;
    private JLabel timerLabel;
    private JLabel movesLabel;
    private JLabel difficultyLabel;
    private boolean gameStarted = false;
    
    // Difficoltà
    private enum Difficulty { FACILE, DIFFICILE }
    private Difficulty currentDifficulty = Difficulty.FACILE;
    private int cardsToDrawFromStock = 1;
    
    public SolitaireGame() {
    setTitle("🎴 Solitario Klondike 🎴");
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    setResizable(false);
    
    loadCardImages();
    
    // Sposta initGame() DOPO la creazione dei componenti grafici
    // initGame(); <-- RIMUOVI DA QUI
    
    gamePanel = new GamePanel();
    add(gamePanel, BorderLayout.CENTER);
    
    // Panel superiore con statistiche - GRAFICA MIGLIORATA
    JPanel topPanel = new JPanel();
    topPanel.setBackground(DARK_GREEN);
    topPanel.setLayout(new BorderLayout());
    topPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 3, 0, GOLD),
        BorderFactory.createEmptyBorder(10, 15, 10, 15)
    ));
    
    // Panel per le statistiche
    JPanel statsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 5));
    statsPanel.setBackground(DARK_GREEN);
    
    // Timer con icona
    JPanel timerPanel = createStatPanel("⏱️", "00:00");
    timerLabel = (JLabel) ((JPanel) timerPanel.getComponent(1)).getComponent(0);
    statsPanel.add(timerPanel);
    
    // Mosse con icona
    JPanel movesPanel = createStatPanel("🎯", "0");
    movesLabel = (JLabel) ((JPanel) movesPanel.getComponent(1)).getComponent(0);
    statsPanel.add(movesPanel);
    
    // Difficoltà con icona
    JPanel diffPanel = createStatPanel("⭐", "Facile");
    difficultyLabel = (JLabel) ((JPanel) diffPanel.getComponent(1)).getComponent(0);
    statsPanel.add(diffPanel);
    
    topPanel.add(statsPanel, BorderLayout.CENTER);
    add(topPanel, BorderLayout.NORTH);
    
    // Panel inferiore con pulsanti - GRAFICA MIGLIORATA
    JPanel bottomPanel = new JPanel();
    bottomPanel.setBackground(DARK_GREEN);
    bottomPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 15, 12));
    bottomPanel.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(3, 0, 0, 0, GOLD),
        BorderFactory.createEmptyBorder(5, 0, 5, 0)
    ));
    
    // Selettore difficoltà con stile
    JLabel diffLabel = new JLabel("Difficoltà:");
    diffLabel.setForeground(GOLD);
    diffLabel.setFont(new Font("Arial", Font.BOLD, 15));
    bottomPanel.add(diffLabel);
    
    JComboBox<String> difficultyBox = new JComboBox<>(new String[]{"🟢 Facile (1 carta)", "🔴 Difficile (3 carte)"});
    difficultyBox.setFont(new Font("Arial", Font.BOLD, 13));
    difficultyBox.setBackground(new Color(230, 230, 230));
    difficultyBox.addActionListener(e -> {
        int selected = difficultyBox.getSelectedIndex();
        currentDifficulty = (selected == 0) ? Difficulty.FACILE : Difficulty.DIFFICILE;
        cardsToDrawFromStock = (selected == 0) ? 1 : 3;
        difficultyLabel.setText(selected == 0 ? "Facile" : "Difficile");
        initGame();
        gamePanel.repaint();
    });
    bottomPanel.add(difficultyBox);
    
    // Bottone nuova partita con stile
    JButton newGameButton = createStyledButton("🔄 Nuova Partita", new Color(50, 120, 200), Color.WHITE);
    newGameButton.addActionListener(e -> {
        initGame();
        gamePanel.repaint();
    });
    bottomPanel.add(newGameButton);
    
    // Bottone vittoria con stile dorato
    JButton winButton = createStyledButton("🏆 Vittoria!", GOLD, DARK_GREEN);
    winButton.addActionListener(e -> {
        showVictoryDialog();
    });
    bottomPanel.add(winButton);
    
    add(bottomPanel, BorderLayout.SOUTH);
    
    // Timer - crealo DOPO che timerLabel è stato inizializzato
    gameTimer = new javax.swing.Timer(1000, e -> {
        if (gameStarted) {
            elapsedSeconds++;
            updateTimerLabel();
        }
    });
    gameTimer.start();
    
    // Adesso chiama initGame() DOPO che tutti i componenti sono stati creati
    initGame();  // <-- SPOSTA QUI
    
    pack();
    setLocationRelativeTo(null);
    setVisible(true);
}
    
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
    
    private JButton createStyledButton(String text, Color bg, Color fg) {
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
            BorderFactory.createEmptyBorder(8, 20, 8, 20)
        ));
        
        // Effetto hover
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(bg.brighter());
            }
            
            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(bg);
            }
        });
        
        return button;
    }
    
    
    private void updateTimerLabel() {
        if (timerLabel != null) {  // <-- AGGIUNGI QUESTO CONTROLLO
            int minutes = elapsedSeconds / 60;
            int seconds = elapsedSeconds % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
    }
}
    
    
    private void updateMovesLabel() {
    if (movesLabel != null) {  // <-- AGGIUNGI QUESTO CONTROLLO
        movesLabel.setText(String.valueOf(moveCount));
    }
}
    
    private void incrementMoves() {
        if (!gameStarted) {
            gameStarted = true;
        }
        moveCount++;
        updateMovesLabel();
    }
    
    private void showVictoryDialog() {
        gameStarted = false;
        int minutes = elapsedSeconds / 60;
        int seconds = elapsedSeconds % 60;
        
        // Crea pannello personalizzato per la vittoria
        JPanel victoryPanel = new JPanel();
        victoryPanel.setLayout(new BoxLayout(victoryPanel, BoxLayout.Y_AXIS));
        victoryPanel.setBackground(Color.WHITE);
        victoryPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        
        // Titolo
        JLabel titleLabel = new JLabel("🎉 COMPLIMENTI! 🎉");
        titleLabel.setFont(new Font("Arial", Font.BOLD, 28));
        titleLabel.setForeground(GOLD);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        victoryPanel.add(titleLabel);
        
        victoryPanel.add(Box.createVerticalStrut(10));
        
        JLabel subtitleLabel = new JLabel("Hai completato il Solitario!");
        subtitleLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        victoryPanel.add(subtitleLabel);
        
        victoryPanel.add(Box.createVerticalStrut(20));
        
        // Statistiche
        addVictoryStat(victoryPanel, "⏱️ Tempo:", String.format("%02d:%02d", minutes, seconds));
        addVictoryStat(victoryPanel, "🎯 Mosse:", String.valueOf(moveCount));
        addVictoryStat(victoryPanel, "⭐ Difficoltà:", 
            currentDifficulty == Difficulty.FACILE ? "Facile" : "Difficile");
        
        JOptionPane.showMessageDialog(
            this,
            victoryPanel,
            "Vittoria!",
            JOptionPane.PLAIN_MESSAGE
        );
    }
    
    private void addVictoryStat(JPanel panel, String label, String value) {
        JPanel statPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        statPanel.setBackground(Color.WHITE);
        
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(new Font("Arial", Font.BOLD, 16));
        statPanel.add(labelComp);
        
        JLabel valueComp = new JLabel(value);
        valueComp.setFont(new Font("Arial", Font.PLAIN, 16));
        valueComp.setForeground(new Color(50, 50, 50));
        statPanel.add(valueComp);
        
        panel.add(statPanel);
    }
    
    private void loadCardImages() {
        cardImages = new HashMap<>();
        cardBackImage = null;
        
        Map<String, String> cardKeyMap = new HashMap<>();
        
        // Cuori
        cardKeyMap.put("ace_of_hearts.png", "A♥");
        cardKeyMap.put("2_of_hearts.png", "2♥");
        cardKeyMap.put("3_of_hearts.png", "3♥");
        cardKeyMap.put("4_of_hearts.png", "4♥");
        cardKeyMap.put("5_of_hearts.png", "5♥");
        cardKeyMap.put("6_of_hearts.png", "6♥");
        cardKeyMap.put("7_of_hearts.png", "7♥");
        cardKeyMap.put("8_of_hearts.png", "8♥");
        cardKeyMap.put("9_of_hearts.png", "9♥");
        cardKeyMap.put("10_of_hearts.png", "10♥");
        cardKeyMap.put("jack_of_hearts2.png", "J♥");
        cardKeyMap.put("queen_of_hearts2.png", "Q♥");
        cardKeyMap.put("king_of_hearts2.png", "K♥");
        
        // Quadri 
        cardKeyMap.put("ace_of_diamonds.png", "A♦");
        cardKeyMap.put("2_of_diamonds.png", "2♦");
        cardKeyMap.put("3_of_diamonds.png", "3♦");
        cardKeyMap.put("4_of_diamonds.png", "4♦");
        cardKeyMap.put("5_of_diamonds.png", "5♦");
        cardKeyMap.put("6_of_diamonds.png", "6♦");
        cardKeyMap.put("7_of_diamonds.png", "7♦");
        cardKeyMap.put("8_of_diamonds.png", "8♦");
        cardKeyMap.put("9_of_diamonds.png", "9♦");
        cardKeyMap.put("10_of_diamonds.png", "10♦");
        cardKeyMap.put("jack_of_diamonds2.png", "J♦");
        cardKeyMap.put("queen_of_diamonds2.png", "Q♦");
        cardKeyMap.put("king_of_diamonds2.png", "K♦");
        
        // Fiori
        cardKeyMap.put("ace_of_clubs.png", "A♣");
        cardKeyMap.put("2_of_clubs.png", "2♣");
        cardKeyMap.put("3_of_clubs.png", "3♣");
        cardKeyMap.put("4_of_clubs.png", "4♣");
        cardKeyMap.put("5_of_clubs.png", "5♣");
        cardKeyMap.put("6_of_clubs.png", "6♣");
        cardKeyMap.put("7_of_clubs.png", "7♣");
        cardKeyMap.put("8_of_clubs.png", "8♣");
        cardKeyMap.put("9_of_clubs.png", "9♣");
        cardKeyMap.put("10_of_clubs.png", "10♣");
        cardKeyMap.put("jack_of_clubs2.png", "J♣");
        cardKeyMap.put("queen_of_clubs2.png", "Q♣");
        cardKeyMap.put("king_of_clubs2.png", "K♣");
        
        // Picche
        cardKeyMap.put("ace_of_spades2.png", "A♠");
        cardKeyMap.put("2_of_spades.png", "2♠");
        cardKeyMap.put("3_of_spades.png", "3♠");
        cardKeyMap.put("4_of_spades.png", "4♠");
        cardKeyMap.put("5_of_spades.png", "5♠");
        cardKeyMap.put("6_of_spades.png", "6♠");
        cardKeyMap.put("7_of_spades.png", "7♠");
        cardKeyMap.put("8_of_spades.png", "8♠");
        cardKeyMap.put("9_of_spades.png", "9♠");
        cardKeyMap.put("10_of_spades.png", "10♠");
        cardKeyMap.put("jack_of_spades2.png", "J♠");
        cardKeyMap.put("queen_of_spades2.png", "Q♠");
        cardKeyMap.put("king_of_spades2.png", "K♠");
        
        File[] possibleDirs = {
            new File("cards_images"),
        };
        
        for (File dir : possibleDirs) {
            if (dir.exists() && dir.isDirectory()) {
                for (Map.Entry<String, String> entry : cardKeyMap.entrySet()) {
                    String fileName = entry.getKey();
                    String cardKey = entry.getValue();
                    
                    File imgFile = new File(dir, fileName);
                    if (imgFile.exists()) {
                        try {
                            BufferedImage img = ImageIO.read(imgFile);
                            if (img != null) {
                                cardImages.put(cardKey, img);
                            }
                        } catch (IOException e) {
                            System.err.println("❌ Errore: " + e);
                        }
                    }
                }
                
                File[] files = dir.listFiles((d, name) -> {
                    String lower = name.toLowerCase();
                    return lower.contains("back") || lower.contains("blue") || lower.contains("dorso");
                });
                
                if (files != null) {
                    for (File file : files) {
                        try {
                            cardBackImage = ImageIO.read(file);
                            break;
                        } catch (IOException e) {
                            System.err.println("❌ Errore dorso: " + e);
                        }
                    }
                }
                
                if (!cardImages.isEmpty()) {
                    break;
                }
            }
        }
    }
    
    private void initGame() {
        deck = new Deck();
        deck.shuffle();
        
        stockPile = new ArrayList<>();
        wastePile = new ArrayList<>();
        foundations = new ArrayList<>();
        tableau = new ArrayList<>();
        
        // Reset statistiche
        elapsedSeconds = 0;
        moveCount = 0;
        gameStarted = false;
        updateTimerLabel();
        updateMovesLabel();
        
        for (int i = 0; i < 4; i++) {
            foundations.add(new ArrayList<>());
        }
        
        for (int i = 0; i < 7; i++) {
            tableau.add(new ArrayList<>());
        }
        
        for (int col = 0; col < 7; col++) {
            for (int row = 0; row <= col; row++) {
                Card card = deck.draw();
                if (row == col) {
                    card.flip();
                }
                tableau.get(col).add(card);
            }
        }
        
        while (deck.hasCards()) {
            stockPile.add(deck.draw());
        }
        
        draggedCards = new ArrayList<>();
    }
    
    private class GamePanel extends JPanel {
        private Point dragOffset = new Point(0, 0);
        private Point currentMousePos = null;
        
        public GamePanel() {
            setPreferredSize(new Dimension(900, 720));
            setBackground(FELT_GREEN);
            
            MouseAdapter mouseHandler = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    handleMousePress(e);
                }
                
                @Override
                public void mouseDragged(MouseEvent e) {
                    handleMouseDrag(e);
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    handleMouseRelease(e);
                }
                
                @Override
                public void mouseClicked(MouseEvent e) {
                    handleMouseClick(e);
                }
            };
            
            addMouseListener(mouseHandler);
            addMouseMotionListener(mouseHandler);
        }
        
        private void handleMouseClick(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            
            int stockX = CARD_SPACING;
            int stockY = CARD_SPACING;
            if (x >= stockX && x <= stockX + CARD_WIDTH && 
                y >= stockY && y <= stockY + CARD_HEIGHT) {
                
                if (!stockPile.isEmpty()) {
                    int cardsToDraw = Math.min(cardsToDrawFromStock, stockPile.size());
                    for (int i = 0; i < cardsToDraw; i++) {
                        Card card = stockPile.remove(stockPile.size() - 1);
                        card.flip();
                        wastePile.add(card);
                    }
                    incrementMoves();
                } else if (!wastePile.isEmpty()) {
                    while (!wastePile.isEmpty()) {
                        Card card = wastePile.remove(wastePile.size() - 1);
                        card.flip();
                        stockPile.add(card);
                    }
                }
                repaint();
                return;
            }
        }
        
        private void handleMousePress(MouseEvent e) {
            int mouseX = e.getX();
            int mouseY = e.getY();
            
            draggedCards.clear();
            sourceTableau = -1;
            sourceIndex = -1;
            
            int wasteX = CARD_SPACING + CARD_WIDTH + CARD_SPACING;
            if (!wastePile.isEmpty()) {
                int cardsToShow = Math.min(3, wastePile.size());
                int topCardIndex = wastePile.size() - 1;
                int topCardOffset = (cardsToShow - 1) * 20;
                
                if (mouseX >= wasteX + topCardOffset && mouseX <= wasteX + topCardOffset + CARD_WIDTH &&
                    mouseY >= CARD_SPACING && mouseY <= CARD_SPACING + CARD_HEIGHT) {
                    
                    Card card = wastePile.get(topCardIndex);
                    draggedCards.add(card);
                    sourceTableau = -2;
                    dragStart = e.getPoint();
                    dragOffset.setLocation(mouseX - (wasteX + topCardOffset), mouseY - CARD_SPACING);
                    return;
                }
            }
            
            for (int i = 0; i < 4; i++) {
                int foundX = CARD_SPACING + (3 + i) * (CARD_WIDTH + CARD_SPACING);
                if (!foundations.get(i).isEmpty()) {
                    if (mouseX >= foundX && mouseX <= foundX + CARD_WIDTH &&
                        mouseY >= CARD_SPACING && mouseY <= CARD_SPACING + CARD_HEIGHT) {
                        
                        Card card = foundations.get(i).get(foundations.get(i).size() - 1);
                        draggedCards.add(card);
                        sourceTableau = -(i + 3);
                        dragStart = e.getPoint();
                        dragOffset.setLocation(mouseX - foundX, mouseY - CARD_SPACING);
                        return;
                    }
                }
            }
            
            for (int col = 0; col < 7; col++) {
                List<Card> pile = tableau.get(col);
                if (pile.isEmpty()) continue;
                
                int tableauX = CARD_SPACING + col * (CARD_WIDTH + CARD_SPACING);
                int tableauY = 220;
                
                for (int i = pile.size() - 1; i >= 0; i--) {
                    Card card = pile.get(i);
                    int cardY = tableauY + i * PILE_OFFSET;
                    
                    boolean isLastCard = (i == pile.size() - 1);
                    int cardHeight = isLastCard ? CARD_HEIGHT : PILE_OFFSET;
                    
                    if (mouseX >= tableauX && mouseX <= tableauX + CARD_WIDTH &&
                        mouseY >= cardY && mouseY <= cardY + cardHeight) {
                        
                        if (card.isFaceUp()) {
                            for (int j = i; j < pile.size(); j++) {
                                draggedCards.add(pile.get(j));
                            }
                            sourceTableau = col;
                            sourceIndex = i;
                            dragStart = e.getPoint();
                            dragOffset.setLocation(mouseX - tableauX, mouseY - cardY);
                            return;
                        }
                    }
                }
            }
        }
        
        private void handleMouseDrag(MouseEvent e) {
            if (!draggedCards.isEmpty() && dragStart != null) {
                currentMousePos = e.getPoint();
                repaint();
            }
        }
        
        private void handleMouseRelease(MouseEvent e) {
            if (draggedCards.isEmpty()) return;
            
            int mouseX = e.getX();
            int mouseY = e.getY();
            boolean placed = false;
            
            if (draggedCards.size() == 1) {
                Card card = draggedCards.get(0);
                for (int i = 0; i < 4; i++) {
                    int foundX = CARD_SPACING + (3 + i) * (CARD_WIDTH + CARD_SPACING);
                    if (mouseX >= foundX && mouseX <= foundX + CARD_WIDTH &&
                        mouseY >= CARD_SPACING && mouseY <= CARD_SPACING + CARD_HEIGHT) {
                        
                        if (canPlaceOnFoundation(card, i)) {
                            removeCardFromSource();
                            foundations.get(i).add(card);
                            placed = true;
                            incrementMoves();
                            
                            if (checkWin()) {
                                SwingUtilities.invokeLater(() -> showVictoryDialog());
                            }
                            break;
                        }
                    }
                }
            }
            
            if (!placed) {
                for (int col = 0; col < 7; col++) {
                    int tableauX = CARD_SPACING + col * (CARD_WIDTH + CARD_SPACING);
                    int tableauY = 220;
                    
                    List<Card> pile = tableau.get(col);
                    int targetY = pile.isEmpty() ? tableauY : tableauY + pile.size() * PILE_OFFSET;
                    
                    if (mouseX >= tableauX && mouseX <= tableauX + CARD_WIDTH &&
                        mouseY >= tableauY && mouseY <= targetY + CARD_HEIGHT) {
                        
                        Card topCard = draggedCards.get(0);
                        if (canPlaceOnTableau(topCard, col)) {
                            removeCardFromSource();
                            for (Card card : draggedCards) {
                                tableau.get(col).add(card);
                            }
                            placed = true;
                            incrementMoves();
                            break;
                        }
                    }
                }
            }
            
            draggedCards.clear();
            dragStart = null;
            currentMousePos = null;
            repaint();
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Disegna stock pile
            int stockX = CARD_SPACING;
            if (!stockPile.isEmpty()) {
                drawCardBack(g2d, stockX, CARD_SPACING);
            } else {
                drawEmptySlot(g2d, stockX, CARD_SPACING, "↻");
            }
            
            // Disegna waste pile con 3 carte visibili in modalità difficile
            int wasteX = CARD_SPACING + CARD_WIDTH + CARD_SPACING;
            if (!wastePile.isEmpty()) {
                int cardsToShow = Math.min(3, wastePile.size());
                int startIndex = wastePile.size() - cardsToShow;
                
                for (int i = 0; i < cardsToShow; i++) {
                    Card card = wastePile.get(startIndex + i);
                    boolean isDragging = draggedCards.contains(card);
                    if (!isDragging) {
                        int offset = i * 20;
                        drawCard(g2d, card, wasteX + offset, CARD_SPACING);
                    }
                }
            } else {
                drawEmptySlot(g2d, wasteX, CARD_SPACING, "");
            }
            
            // Disegna foundations
            String[] foundationSymbols = {"♥", "♦", "♣", "♠"};
            for (int i = 0; i < 4; i++) {
                int foundX = CARD_SPACING + (3 + i) * (CARD_WIDTH + CARD_SPACING);
                List<Card> foundation = foundations.get(i);
                
                if (foundation.isEmpty()) {
                    drawEmptySlot(g2d, foundX, CARD_SPACING, foundationSymbols[i]);
                } else {
                    Card topCard = foundation.get(foundation.size() - 1);
                    boolean isDragging = draggedCards.contains(topCard);
                    if (!isDragging) {
                        drawCard(g2d, topCard, foundX, CARD_SPACING);
                    }
                }
            }
            
            // Disegna tableau
            for (int col = 0; col < 7; col++) {
                int tableauX = CARD_SPACING + col * (CARD_WIDTH + CARD_SPACING);
                int tableauY = 220;
                List<Card> pile = tableau.get(col);
                
                if (pile.isEmpty()) {
                    drawEmptySlot(g2d, tableauX, tableauY, "K");
                } else {
                    for (int i = 0; i < pile.size(); i++) {
                        Card card = pile.get(i);
                        boolean isDragging = draggedCards.contains(card);
                        
                        if (!isDragging) {
                            int cardY = tableauY + i * PILE_OFFSET;
                            if (card.isFaceUp()) {
                                drawCard(g2d, card, tableauX, cardY);
                            } else {
                                drawCardBack(g2d, tableauX, cardY);
                            }
                        }
                    }
                }
            }
            
            // Disegna le carte trascinate con ombra migliorata
            if (!draggedCards.isEmpty() && dragStart != null && currentMousePos != null) {
                int offsetX = currentMousePos.x - dragStart.x;
                int offsetY = currentMousePos.y - dragStart.y;
                
                for (int i = 0; i < draggedCards.size(); i++) {
                    Card card = draggedCards.get(i);
                    int x = 0, y = 0;
                    
                    if (sourceTableau == -2) {
                        int cardsToShow = Math.min(3, wastePile.size());
                        x = wasteX + (cardsToShow - 1) * 20;
                        y = CARD_SPACING;
                    } else if (sourceTableau < -2) {
                        int foundIndex = -(sourceTableau + 3);
                        x = CARD_SPACING + (3 + foundIndex) * (CARD_WIDTH + CARD_SPACING);
                        y = CARD_SPACING;
                    } else if (sourceTableau >= 0) {
                        x = CARD_SPACING + sourceTableau * (CARD_WIDTH + CARD_SPACING);
                        y = 220 + sourceIndex * PILE_OFFSET + i * PILE_OFFSET;
                    }
                    
                    // Ombra migliorata con gradiente
                    g2d.setColor(new Color(0, 0, 0, 100));
                    g2d.fillRoundRect(x + offsetX + 5, y + offsetY + 5, CARD_WIDTH, CARD_HEIGHT, 12, 12);
                    g2d.setColor(new Color(0, 0, 0, 50));
                    g2d.fillRoundRect(x + offsetX + 8, y + offsetY + 8, CARD_WIDTH, CARD_HEIGHT, 12, 12);
                    
                    drawCard(g2d, card, x + offsetX, y + offsetY);
                }
            }
        }
        
        private void drawCard(Graphics2D g2d, Card card, int x, int y) {
            String key = card.getRank().toString() + card.getSuit().toString();
            BufferedImage img = cardImages.get(key);
            
            if (img != null) {
                // Bordo sottile per le carte
                g2d.setColor(new Color(200, 200, 200));
                g2d.fillRoundRect(x - 1, y - 1, CARD_WIDTH + 2, CARD_HEIGHT + 2, 12, 12);
                g2d.drawImage(img, x, y, CARD_WIDTH, CARD_HEIGHT, null);
            } else {
                // Fallback migliorato
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
                g2d.setColor(new Color(200, 200, 200));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 10, 10);
                
                Color suitColor = (card.getSuit() == Card.Suit.HEARTS || card.getSuit() == Card.Suit.DIAMONDS) 
                    ? new Color(200, 0, 0) : Color.BLACK;
                g2d.setColor(suitColor);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.drawString(card.getRank().toString(), x + 10, y + 40);
                g2d.drawString(card.getSuit().toString(), x + 10, y + 70);
            }
        }
        
        private void drawCardBack(Graphics2D g2d, int x, int y) {
            if (cardBackImage != null) {
                g2d.drawImage(cardBackImage, x, y, CARD_WIDTH, CARD_HEIGHT, null);
            } else {
                // Dorso elegante con pattern
                GradientPaint gradient = new GradientPaint(
                    x, y, new Color(20, 60, 140),
                    x + CARD_WIDTH, y + CARD_HEIGHT, new Color(40, 90, 180)
                );
                g2d.setPaint(gradient);
                g2d.fillRoundRect(x, y, CARD_WIDTH, CARD_HEIGHT, 12, 12);
                
                // Bordo dorato
                g2d.setColor(DARK_GOLD);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(x + 3, y + 3, CARD_WIDTH - 6, CARD_HEIGHT - 6, 10, 10);
                
                // Pattern decorativo
                g2d.setColor(new Color(255, 255, 255, 40));
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 5; j++) {
                        g2d.fillOval(x + 18 + i * 22, y + 18 + j * 28, 10, 10);
                    }
                }
                
                // Rombo centrale
                g2d.setColor(GOLD);
                int cx = x + CARD_WIDTH / 2;
                int cy = y + CARD_HEIGHT / 2;
                int[] xPoints = {cx, cx + 18, cx, cx - 18};
                int[] yPoints = {cy - 25, cy, cy + 25, cy};
                g2d.fillPolygon(xPoints, yPoints, 4);
            }
        }
        
        private void drawEmptySlot(Graphics2D g2d, int x, int y, String symbol) {
            // Slot vuoto migliorato
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
                int textX = x + (CARD_WIDTH - fm.stringWidth(symbol)) / 2;
                int textY = y + ((CARD_HEIGHT - fm.getHeight()) / 2) + fm.getAscent();
                g2d.drawString(symbol, textX, textY);
            }
        }
    }
    
    private void removeCardFromSource() {
        if (sourceTableau == -2) {
            if (!wastePile.isEmpty()) {
                wastePile.remove(wastePile.size() - 1);
            }
        } else if (sourceTableau < -2) {
            int foundIndex = -(sourceTableau + 3);
            if (!foundations.get(foundIndex).isEmpty()) {
                foundations.get(foundIndex).remove(foundations.get(foundIndex).size() - 1);
            }
        } else if (sourceTableau >= 0) {
            List<Card> pile = tableau.get(sourceTableau);
            for (Card card : draggedCards) {
                pile.remove(card);
            }
            if (!pile.isEmpty() && !pile.get(pile.size() - 1).isFaceUp()) {
                pile.get(pile.size() - 1).flip();
            }
        }
    }
    
    private boolean canPlaceOnFoundation(Card card, int foundationIndex) {
        List<Card> foundation = foundations.get(foundationIndex);
        
        if (foundation.isEmpty()) {
            return card.getRank() == Card.Rank.ACE;
        }
        
        Card topCard = foundation.get(foundation.size() - 1);
        return card.getSuit() == topCard.getSuit() && 
               card.getRank().ordinal() == topCard.getRank().ordinal() + 1;
    }
    
    private boolean canPlaceOnTableau(Card card, int column) {
        List<Card> pile = tableau.get(column);
        
        if (pile.isEmpty()) {
            return card.getRank() == Card.Rank.KING;
        }
        
        Card topCard = pile.get(pile.size() - 1);
        boolean cardIsRed = card.getSuit() == Card.Suit.HEARTS || card.getSuit() == Card.Suit.DIAMONDS;
        boolean topCardIsRed = topCard.getSuit() == Card.Suit.HEARTS || topCard.getSuit() == Card.Suit.DIAMONDS;
        
        return cardIsRed != topCardIsRed && 
               card.getRank().ordinal() == topCard.getRank().ordinal() - 1;
    }
    
    private boolean checkWin() {
        for (List<Card> foundation : foundations) {
            if (foundation.size() != 13) {
                return false;
            }
        }
        return true;
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                e.printStackTrace();
            }
            new SolitaireGame();
        });
    }
}

class Card {
    enum Suit {
        HEARTS("♥"), DIAMONDS("♦"), CLUBS("♣"), SPADES("♠");
        private final String symbol;
        Suit(String symbol) { this.symbol = symbol; }
        @Override
        public String toString() { return symbol; }
    }
    
    enum Rank {
        ACE("A"), TWO("2"), THREE("3"), FOUR("4"), FIVE("5"), SIX("6"), 
        SEVEN("7"), EIGHT("8"), NINE("9"), TEN("10"), JACK("J"), QUEEN("Q"), KING("K");
        private final String symbol;
        Rank(String symbol) { this.symbol = symbol; }
        @Override
        public String toString() { return symbol; }
    }
    
    private final Suit suit;
    private final Rank rank;
    private boolean faceUp;
    
    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
        this.faceUp = false;
    }
    
    public Suit getSuit() { return suit; }
    public Rank getRank() { return rank; }
    public boolean isFaceUp() { return faceUp; }
    public void flip() { faceUp = !faceUp; }
}

class Deck {
    private List<Card> cards;
    
    public Deck() {
        cards = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                cards.add(new Card(suit, rank));
            }
        }
    }
    
    public void shuffle() {
        Collections.shuffle(cards);
    }
    
    public Card draw() {
        return cards.isEmpty() ? null : cards.remove(cards.size() - 1);
    }
    
    public boolean hasCards() {
        return !cards.isEmpty();
    }
}