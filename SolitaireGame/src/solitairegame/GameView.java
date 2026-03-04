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
 */
public class GameView extends JFrame {

    // ── Costanti grafiche ────────────────────────────────────────────────────
    public static final int LARGHEZZA_CARTA = 100;
    public static final int ALTEZZA_CARTA = 145;
    public static final int SPAZIATURA_CARTE = 15;
    public static final int OFFSET_PILA = 30;
    public static final int Y_TAVOLO = 220;

    // Alias per compatibilità col controller
    public static final int CARD_WIDTH = LARGHEZZA_CARTA;
    public static final int CARD_HEIGHT = ALTEZZA_CARTA;
    public static final int CARD_SPACING = SPAZIATURA_CARTE;
    public static final int PILE_OFFSET = OFFSET_PILA;
    public static final int TABLEAU_Y = Y_TAVOLO;

    public static final Color VERDE_FELTRO = new Color(13, 110, 53);
    public static final Color VERDE_SCURO = new Color(10, 80, 40);
    public static final Color VERDE_CHIARO = new Color(20, 140, 70);
    public static final Color ORO = new Color(255, 215, 0);
    public static final Color ORO_SCURO = new Color(218, 165, 32);

    // Alias colori per compatibilità
    public static final Color FELT_GREEN = VERDE_FELTRO;
    public static final Color DARK_GREEN = VERDE_SCURO;
    public static final Color LIGHT_GREEN = VERDE_CHIARO;
    public static final Color GOLD = ORO;
    public static final Color DARK_GOLD = ORO_SCURO;

    // ── Componenti UI ────────────────────────────────────────────────────────
    private JLabel etichettaTimer;
    private JLabel etichettaMovimenti;
    private JLabel etichettaDifficolta;
    public PannelloGioco gamePanel;

    // ── Immagini carte ───────────────────────────────────────────────────────
    private Map<String, BufferedImage> immaginiCarte;
    private BufferedImage immagineRetro;

    // ── Costruttore ──────────────────────────────────────────────────────────
    public GameView() {
        setTitle("Solitario");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        caricaImmaginiCarte();
        costruisciUI();
    }

    private void costruisciUI() {
        gamePanel = new PannelloGioco();
        add(gamePanel, BorderLayout.CENTER);
        add(costruisciPannelloSuperiore(), BorderLayout.NORTH);
        add(buildBottomPanel(), BorderLayout.SOUTH);
    }

    // ── Dialogo scelta difficoltà all'avvio ──────────────────────────────────
    /**
     * Mostra un dialogo per scegliere la difficoltà prima di iniziare la partita.
     * Ritorna la difficoltà scelta (default FACILE se si chiude senza scegliere).
     * @return 
     */
    public GameModel.Difficulty mostraDialogoDifficolta() {
        JPanel pannello = new JPanel();
        pannello.setLayout(new BoxLayout(pannello, BoxLayout.Y_AXIS));
        pannello.setBackground(Color.WHITE);
        pannello.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titolo = new JLabel("Benvenuto nel Solitario");
        titolo.setFont(new Font("Arial", Font.BOLD, 22));
        titolo.setForeground(VERDE_SCURO);
        titolo.setAlignmentX(Component.CENTER_ALIGNMENT);
        pannello.add(titolo);
        pannello.add(Box.createVerticalStrut(10));

        JLabel sottotitolo = new JLabel("Scegli il livello di difficoltà:");
        sottotitolo.setFont(new Font("Arial", Font.PLAIN, 16));
        sottotitolo.setAlignmentX(Component.CENTER_ALIGNMENT);
        pannello.add(sottotitolo);
        pannello.add(Box.createVerticalStrut(20));

        // Pannello bottoni difficoltà
        JPanel pannelloBotoni = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        pannelloBotoni.setBackground(Color.WHITE);

        JButton bottFacile = creaBottoneStilizzato("Facile", new Color(50, 160, 80), Color.WHITE);
        bottFacile.setFont(new Font("Arial", Font.BOLD, 15));
        bottFacile.setPreferredSize(new Dimension(150, 50));

        JButton bottDifficile = creaBottoneStilizzato("Difficile", new Color(200, 60, 50), Color.WHITE);
        bottDifficile.setFont(new Font("Arial", Font.BOLD, 15));
        bottDifficile.setPreferredSize(new Dimension(150, 50));

        pannelloBotoni.add(bottFacile);
        pannelloBotoni.add(bottDifficile);
        pannello.add(pannelloBotoni);
        pannello.add(Box.createVerticalStrut(15));

        // Risultato della scelta
        final GameModel.Difficulty[] scelta = {GameModel.Difficulty.FACILE};

        JDialog dialogo = new JDialog(this, "Nuova Partita", true);
        dialogo.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialogo.setResizable(false);

        bottFacile.addActionListener(e -> {
            scelta[0] = GameModel.Difficulty.FACILE;
            dialogo.dispose();
        });
        bottDifficile.addActionListener(e -> {
            scelta[0] = GameModel.Difficulty.DIFFICILE;
            dialogo.dispose();
        });

        dialogo.add(pannello);
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);

        return scelta[0];
    }

    // ── Pannello superiore ───────────────────────────────────────────────────
    private JPanel costruisciPannelloSuperiore() {
        JPanel pannelloSuperiore = new JPanel(new BorderLayout());
        pannelloSuperiore.setBackground(VERDE_SCURO);
        pannelloSuperiore.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 3, 0, ORO),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JPanel pannelloStatistiche = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 5));
        pannelloStatistiche.setBackground(VERDE_SCURO);

        JPanel pannelloTimer = creaStatPanel("Tempo:", "00:00");
        etichettaTimer = (JLabel) ((JPanel) pannelloTimer.getComponent(1)).getComponent(0);
        pannelloStatistiche.add(pannelloTimer);

        JPanel pannelloMovimenti = creaStatPanel("Mosse:", "0");
        etichettaMovimenti = (JLabel) ((JPanel) pannelloMovimenti.getComponent(1)).getComponent(0);
        pannelloStatistiche.add(pannelloMovimenti);

        JPanel pannelloDiff = creaStatPanel("Livello:", "Facile");
        etichettaDifficolta = (JLabel) ((JPanel) pannelloDiff.getComponent(1)).getComponent(0);
        pannelloStatistiche.add(pannelloDiff);

        pannelloSuperiore.add(pannelloStatistiche, BorderLayout.CENTER);
        return pannelloSuperiore;
    }

    // ── Pannello inferiore ───────────────────────────────────────────────────
    public JPanel buildBottomPanel() {
        JPanel pannelloInferiore = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 12));
        pannelloInferiore.setBackground(VERDE_SCURO);
        pannelloInferiore.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(3, 0, 0, 0, ORO),
                BorderFactory.createEmptyBorder(5, 0, 5, 0)
        ));

        JButton bottoneNuovaPartita = creaBottoneStilizzato("Nuova Partita", new Color(50, 120, 200), Color.WHITE);
        bottoneNuovaPartita.setName("newGameButton");
        pannelloInferiore.add(bottoneNuovaPartita);

        JButton bottoneMossaPrecedente = creaBottoneStilizzato("Mossa Precedente", new Color(140, 80, 180), Color.WHITE);
        bottoneMossaPrecedente.setName("undoButton");
        pannelloInferiore.add(bottoneMossaPrecedente);

        return pannelloInferiore;
    }

    // ── Aggiornamento etichette ──────────────────────────────────────────────
    public void updateTimerLabel(int secondiTrascorsi) {
        if (etichettaTimer != null) {
            etichettaTimer.setText(String.format("%02d:%02d", secondiTrascorsi / 60, secondiTrascorsi % 60));
        }
    }

    public void updateMovesLabel(int contatoreMovimenti) {
        if (etichettaMovimenti != null) {
            etichettaMovimenti.setText(String.valueOf(contatoreMovimenti));
        }
    }

    public void updateDifficultyLabel(String testo) {
        if (etichettaDifficolta != null) {
            etichettaDifficolta.setText(testo);
        }
    }

    // ── Dialogo vittoria ─────────────────────────────────────────────────────
    public GameModel.Difficulty showVictoryDialog(int secondiTrascorsi, int contatoreMovimenti, GameModel.Difficulty difficolta) {
        JPanel pannello = new JPanel();
        pannello.setLayout(new BoxLayout(pannello, BoxLayout.Y_AXIS));
        pannello.setBackground(Color.WHITE);
        pannello.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JLabel titolo = new JLabel("COMPLIMENTI!");
        titolo.setFont(new Font("Arial", Font.BOLD, 28));
        titolo.setForeground(ORO);
        titolo.setAlignmentX(Component.CENTER_ALIGNMENT);
        pannello.add(titolo);
        pannello.add(Box.createVerticalStrut(10));

        JLabel sottotitolo = new JLabel("Hai completato il Solitario!");
        sottotitolo.setFont(new Font("Arial", Font.PLAIN, 16));
        sottotitolo.setAlignmentX(Component.CENTER_ALIGNMENT);
        pannello.add(sottotitolo);
        pannello.add(Box.createVerticalStrut(20));

        aggiungiStatVittoria(pannello, "Tempo:",
                String.format("%02d:%02d", secondiTrascorsi / 60, secondiTrascorsi % 60));
        aggiungiStatVittoria(pannello, "Mosse:", String.valueOf(contatoreMovimenti));
        aggiungiStatVittoria(pannello, "Difficolta':",
                difficolta == GameModel.Difficulty.FACILE ? "Facile" : "Difficile");

        pannello.add(Box.createVerticalStrut(20));

        JLabel nuovaPartitaLabel = new JLabel("Vuoi giocare ancora? Scegli la difficolta':");
        nuovaPartitaLabel.setFont(new Font("Arial", Font.BOLD, 14));
        nuovaPartitaLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        pannello.add(nuovaPartitaLabel);
        pannello.add(Box.createVerticalStrut(12));

        JPanel pannelloBotoni = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        pannelloBotoni.setBackground(Color.WHITE);

        JButton bottFacile = creaBottoneStilizzato("Facile", new Color(50, 160, 80), Color.WHITE);
        bottFacile.setPreferredSize(new Dimension(130, 45));
        JButton bottDifficile = creaBottoneStilizzato("Difficile", new Color(200, 60, 50), Color.WHITE);
        bottDifficile.setPreferredSize(new Dimension(130, 45));

        pannelloBotoni.add(bottFacile);
        pannelloBotoni.add(bottDifficile);
        pannello.add(pannelloBotoni);

        final GameModel.Difficulty[] scelta = {GameModel.Difficulty.FACILE};

        JDialog dialogo = new JDialog(this, "Vittoria!", true);
        dialogo.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dialogo.setResizable(false);

        bottFacile.addActionListener(e -> {
            scelta[0] = GameModel.Difficulty.FACILE;
            dialogo.dispose();
        });
        bottDifficile.addActionListener(e -> {
            scelta[0] = GameModel.Difficulty.DIFFICILE;
            dialogo.dispose();
        });

        dialogo.add(pannello);
        dialogo.pack();
        dialogo.setLocationRelativeTo(this);
        dialogo.setVisible(true);

        return scelta[0];
    }

    private void aggiungiStatVittoria(JPanel pannello, String etichetta, String valore) {
        JPanel pannelloStat = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        pannelloStat.setBackground(Color.WHITE);
        JLabel l = new JLabel(etichetta);
        l.setFont(new Font("Arial", Font.BOLD, 16));
        JLabel v = new JLabel(valore);
        v.setFont(new Font("Arial", Font.PLAIN, 16));
        v.setForeground(new Color(50, 50, 50));
        pannelloStat.add(l);
        pannelloStat.add(v);
        pannello.add(pannelloStat);
    }

    // ── Utility grafiche ─────────────────────────────────────────────────────
    private JPanel creaStatPanel(String etichetta, String valoreIniziale) {
        JPanel pannello = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pannello.setBackground(VERDE_SCURO);
        JLabel etichettaLabel = new JLabel(etichetta);
        etichettaLabel.setFont(new Font("Arial", Font.BOLD, 14));
        etichettaLabel.setForeground(new Color(200, 230, 200));
        pannello.add(etichettaLabel);
        JPanel pannelloTesto = new JPanel(new GridLayout(0, 1));
        pannelloTesto.setBackground(VERDE_SCURO);
        JLabel etichettaValore = new JLabel(valoreIniziale);
        etichettaValore.setFont(new Font("Arial", Font.BOLD, 22));
        etichettaValore.setForeground(ORO);
        pannelloTesto.add(etichettaValore);
        pannello.add(pannelloTesto);
        return pannello;
    }

    public JButton creaBottoneStilizzato(String testo, Color sfondo, Color primoOpiano) {
        JButton bottone = new JButton(testo);
        bottone.setFont(new Font("Arial", Font.BOLD, 14));
        bottone.setBackground(sfondo);
        bottone.setForeground(primoOpiano);
        bottone.setFocusPainted(false);
        bottone.setBorderPainted(false);
        bottone.setOpaque(true);
        bottone.setCursor(new Cursor(Cursor.HAND_CURSOR));
        bottone.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(primoOpiano, 2),
                BorderFactory.createEmptyBorder(8, 20, 8, 20)));
        bottone.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseEntered(java.awt.event.MouseEvent e) {
                bottone.setBackground(sfondo.brighter());
            }

            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                bottone.setBackground(sfondo);
            }
        });
        return bottone;
    }

    // Alias per compatibilità
    public JButton createStyledButton(String testo, Color sfondo, Color primoOpiano) {
        return creaBottoneStilizzato(testo, sfondo, primoOpiano);
    }

    // ── Caricamento immagini ─────────────────────────────────────────────────
    private void caricaImmaginiCarte() {
        immaginiCarte = new HashMap<>();
        immagineRetro = null;

        Map<String, String> mappatura = new LinkedHashMap<>();
        String[] semi = {"hearts", "diamonds", "clubs", "spades"};
        String[] ranghi = {"ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "jack", "queen", "king"};
        String[] simboliSemi = {"♥", "♦", "♣", "♠"};
        String[] simboliRanghi = {"A", "2", "3", "4", "5", "6", "7", "8", "9", "10", "J", "Q", "K"};

        for (int s = 0; s < semi.length; s++) {
            for (int r = 0; r < ranghi.length; r++) {
                String nomefile = (r < 10)
                        ? ranghi[r] + "_of_" + semi[s] + ".png"
                        : ranghi[r] + "_of_" + semi[s] + "2.png";
                mappatura.put(nomefile, simboliRanghi[r] + simboliSemi[s]);
            }
        }
        mappatura.put("ace_of_spades2.png", "A♠");
        mappatura.put("ace_of_spades.png", "A♠");

        File cartella = new File("cards_images");
        if (cartella.exists() && cartella.isDirectory()) {
            for (Map.Entry<String, String> voce : mappatura.entrySet()) {
                File f = new File(cartella, voce.getKey());
                if (f.exists()) {
                    try {
                        BufferedImage img = ImageIO.read(f);
                        if (img != null) {
                            immaginiCarte.put(voce.getValue(), img);
                        }
                    } catch (IOException e) {
                        System.err.println("❌ " + e.getMessage());
                    }
                }
            }
            File[] retri = cartella.listFiles((d, n) -> {
                String l = n.toLowerCase();
                return l.contains("back") || l.contains("blue") || l.contains("dorso");
            });
            if (retri != null) {
                for (File f : retri) {
                    try {
                        immagineRetro = ImageIO.read(f);
                        break;
                    } catch (IOException e) {
                        System.err.println("❌ retro: " + e.getMessage());
                    }
                }
            }
        }
    }

    public Map<String, BufferedImage> getCardImages() {
        return immaginiCarte;
    }

    public BufferedImage getCardBackImage() {
        return immagineRetro;
    }

    // ── PannelloGioco ─────────────────────────────────────────────────────────
    public class PannelloGioco extends JPanel {

        private List<GameModel.Card> carteTrascinate = new ArrayList<>();
        private Point inizioDrag = null;
        private Point posizioneMouse = null;
        private int colonnaOrigine = -1;
        private int indiceOrigine = -1;
        private GameModel modello;

        // Carta in volo durante l'autocompletamento
        private GameModel.Card cartaInVolo = null;
        private Point posizioneVolo = null;

        public void setCartaInVolo(GameModel.Card carta, Point posizione) {
            this.cartaInVolo = carta;
            this.posizioneVolo = posizione;
        }

        public PannelloGioco() {
            setPreferredSize(new Dimension(900, 720));
            setBackground(VERDE_FELTRO);
        }

        public void setModel(GameModel modello) {
            this.modello = modello;
        }

        public void setDragState(List<GameModel.Card> trascinate, Point inizio,
                Point posizioneMouse, int origine, int indice) {
            this.carteTrascinate = trascinate;
            this.inizioDrag = inizio;
            this.posizioneMouse = posizioneMouse;
            this.colonnaOrigine = origine;
            this.indiceOrigine = indice;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (modello == null) {
                return;
            }

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            List<GameModel.Card> stock = modello.getStockPile();
            List<GameModel.Card> scarto = modello.getWastePile();
            List<List<GameModel.Card>> fonds = modello.getFoundations();
            List<List<GameModel.Card>> tab = modello.getTableau();

            int xStock = SPAZIATURA_CARTE;
            int xScarto = SPAZIATURA_CARTE + LARGHEZZA_CARTA + SPAZIATURA_CARTE;

            // Stock
            if (!stock.isEmpty()) {
                disegnaRetro(g2d, xStock, SPAZIATURA_CARTE);
            } else {
                disegnaSlotVuoto(g2d, xStock, SPAZIATURA_CARTE, "↻");
            }

            // Scarto (waste)
            if (!scarto.isEmpty()) {
                int carteVisibili = modello.getCarteVisibiliWaste();
                int inizioIdx = scarto.size() - carteVisibili;
                for (int i = 0; i < carteVisibili; i++) {
                    GameModel.Card carta = scarto.get(inizioIdx + i);
                    if (!carteTrascinate.contains(carta)) {
                        disegnaCarta(g2d, carta, xScarto + i * 20, SPAZIATURA_CARTE);
                    }
                }
            } else {
                disegnaSlotVuoto(g2d, xScarto, SPAZIATURA_CARTE, "");
            }

            // Fondamenta
            String[] simboliFondamenta = {"♥", "♦", "♣", "♠"};
            for (int i = 0; i < 4; i++) {
                int xF = SPAZIATURA_CARTE + (3 + i) * (LARGHEZZA_CARTA + SPAZIATURA_CARTE);
                List<GameModel.Card> f = fonds.get(i);
                if (f.isEmpty()) {
                    disegnaSlotVuoto(g2d, xF, SPAZIATURA_CARTE, simboliFondamenta[i]);
                } else {
                    GameModel.Card cima = f.get(f.size() - 1);
                    if (!carteTrascinate.contains(cima)) {
                        disegnaCarta(g2d, cima, xF, SPAZIATURA_CARTE);
                    }
                }
            }

            // Tavolo (tableau)
            for (int col = 0; col < 7; col++) {
                int xCol = SPAZIATURA_CARTE + col * (LARGHEZZA_CARTA + SPAZIATURA_CARTE);
                List<GameModel.Card> pila = tab.get(col);
                if (pila.isEmpty()) {
                    disegnaSlotVuoto(g2d, xCol, Y_TAVOLO, "K");
                } else {
                    for (int i = 0; i < pila.size(); i++) {
                        GameModel.Card carta = pila.get(i);
                        if (carteTrascinate.contains(carta)) {
                            continue;
                        }
                        int yC = Y_TAVOLO + i * OFFSET_PILA;
                        if (carta.isFaceUp()) {
                            disegnaCarta(g2d, carta, xCol, yC);
                        } else {
                            disegnaRetro(g2d, xCol, yC);
                        }
                    }
                }
            }

            // Ombra + carte trascinate
            if (!carteTrascinate.isEmpty() && inizioDrag != null && posizioneMouse != null) {
                int offX = posizioneMouse.x - inizioDrag.x;
                int offY = posizioneMouse.y - inizioDrag.y;

                for (int i = 0; i < carteTrascinate.size(); i++) {
                    int x = 0, y = 0;
                    if (colonnaOrigine == -2) {
                        int visibili = modello.getCarteVisibiliWaste();
                        x = xScarto + (visibili - 1) * 20;
                        y = SPAZIATURA_CARTE;
                    } else if (colonnaOrigine < -2) {
                        int fi = -(colonnaOrigine + 3);
                        x = SPAZIATURA_CARTE + (3 + fi) * (LARGHEZZA_CARTA + SPAZIATURA_CARTE);
                        y = SPAZIATURA_CARTE;
                    } else if (colonnaOrigine >= 0) {
                        x = SPAZIATURA_CARTE + colonnaOrigine * (LARGHEZZA_CARTA + SPAZIATURA_CARTE);
                        y = Y_TAVOLO + indiceOrigine * OFFSET_PILA + i * OFFSET_PILA;
                    }
                    g2d.setColor(new Color(0, 0, 0, 100));
                    g2d.fillRoundRect(x + offX + 5, y + offY + 5, LARGHEZZA_CARTA, ALTEZZA_CARTA, 12, 12);
                    g2d.setColor(new Color(0, 0, 0, 50));
                    g2d.fillRoundRect(x + offX + 8, y + offY + 8, LARGHEZZA_CARTA, ALTEZZA_CARTA, 12, 12);
                    disegnaCarta(g2d, carteTrascinate.get(i), x + offX, y + offY);
                }
            }
            // ── Carta in volo (autocompletamento) ───────────────────────────────
            if (cartaInVolo != null && posizioneVolo != null) {
                // Ombra leggera
                g2d.setColor(new Color(0, 0, 0, 80));
                g2d.fillRoundRect(posizioneVolo.x + 4, posizioneVolo.y + 4, LARGHEZZA_CARTA, ALTEZZA_CARTA, 12, 12);
                disegnaCarta(g2d, cartaInVolo, posizioneVolo.x, posizioneVolo.y);
            }
        }
        private void disegnaCarta(Graphics2D g2d, GameModel.Card carta, int x, int y) {
            String chiave = carta.getRank().toString() + carta.getSuit().toString();
            BufferedImage img = immaginiCarte.get(chiave);
            if (img != null) {
                g2d.setColor(new Color(200, 200, 200));
                g2d.fillRoundRect(x - 1, y - 1, LARGHEZZA_CARTA + 2, ALTEZZA_CARTA + 2, 12, 12);
                g2d.drawImage(img, x, y, LARGHEZZA_CARTA, ALTEZZA_CARTA, null);
            } else {
                g2d.setColor(Color.WHITE);
                g2d.fillRoundRect(x, y, LARGHEZZA_CARTA, ALTEZZA_CARTA, 10, 10);
                g2d.setColor(new Color(200, 200, 200));
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRoundRect(x, y, LARGHEZZA_CARTA, ALTEZZA_CARTA, 10, 10);
                boolean rosso = carta.getSuit() == GameModel.Card.Suit.HEARTS
                        || carta.getSuit() == GameModel.Card.Suit.DIAMONDS;
                g2d.setColor(rosso ? new Color(200, 0, 0) : Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 24));
                g2d.drawString(carta.getRank().toString(), x + 10, y + 40);
                g2d.drawString(carta.getSuit().toString(), x + 10, y + 70);
            }
        }

        private void disegnaRetro(Graphics2D g2d, int x, int y) {
            if (immagineRetro != null) {
                g2d.drawImage(immagineRetro, x, y, LARGHEZZA_CARTA, ALTEZZA_CARTA, null);
            } else {
                GradientPaint gradiente = new GradientPaint(
                        x, y, new Color(20, 60, 140),
                        x + LARGHEZZA_CARTA, y + ALTEZZA_CARTA, new Color(40, 90, 180));
                g2d.setPaint(gradiente);
                g2d.fillRoundRect(x, y, LARGHEZZA_CARTA, ALTEZZA_CARTA, 12, 12);
                g2d.setColor(ORO_SCURO);
                g2d.setStroke(new BasicStroke(3));
                g2d.drawRoundRect(x + 3, y + 3, LARGHEZZA_CARTA - 6, ALTEZZA_CARTA - 6, 10, 10);
                g2d.setColor(new Color(255, 255, 255, 40));
                for (int i = 0; i < 4; i++) {
                    for (int j = 0; j < 5; j++) {
                        g2d.fillOval(x + 18 + i * 22, y + 18 + j * 28, 10, 10);
                    }
                }
                g2d.setColor(ORO);
                int cx = x + LARGHEZZA_CARTA / 2, cy = y + ALTEZZA_CARTA / 2;
                g2d.fillPolygon(new int[]{cx, cx + 18, cx, cx - 18}, new int[]{cy - 25, cy, cy + 25, cy}, 4);
            }
        }

        private void disegnaSlotVuoto(Graphics2D g2d, int x, int y, String simbolo) {
            g2d.setColor(VERDE_SCURO);
            g2d.fillRoundRect(x, y, LARGHEZZA_CARTA, ALTEZZA_CARTA, 12, 12);
            g2d.setColor(VERDE_CHIARO);
            g2d.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                    0, new float[]{8, 6}, 0));
            g2d.drawRoundRect(x + 4, y + 4, LARGHEZZA_CARTA - 8, ALTEZZA_CARTA - 8, 10, 10);
            if (!simbolo.isEmpty()) {
                g2d.setColor(new Color(255, 255, 255, 80));
                g2d.setFont(new Font("Arial", Font.BOLD, 48));
                FontMetrics fm = g2d.getFontMetrics();
                g2d.drawString(simbolo,
                        x + (LARGHEZZA_CARTA - fm.stringWidth(simbolo)) / 2,
                        y + ((ALTEZZA_CARTA - fm.getHeight()) / 2) + fm.getAscent());
            }
        }
    }
}