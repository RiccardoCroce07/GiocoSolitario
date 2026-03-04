package solitairegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * CONTROLLER - Collega Model e View. Gestisce gli eventi utente, aggiorna il
 * Model e ordina alla View di ridisegnarsi.
 *
 * Gestisce anche l'auto-completamento animato: quando stock e scarto sono
 * vuoti e tutte le carte del tavolo sono scoperte, le carte volano
 * automaticamente verso la fondamenta corretta una alla volta.
 */
public class GameController {

    private final GameModel modello;
    private final GameView vista;

    // ── Stato drag ────────────────────────────────────────────────────────────
    private Point inizioDrag = null;
    private Point posizioneMouse = null;
    private Point offsetDrag = new Point(0, 0);

    // ── Auto-completamento animato ────────────────────────────────────────────
    private boolean autoCompletamentoAttivo = false;
    private GameModel.Card cartaInVolo = null;
    private Point posizioneVoloPartenza = null;
    private Point posizioneVoloArrivo = null;
    private Point posizioneVoloAttuale = null;
    private int frameVoloCorrente = 0;
    private javax.swing.Timer timerVolo = null;

    private static final int INTERVALLO_FRAME_MS = 16;  // ~60fps
    private static final int FRAME_PER_VOLO = 20;       // durata singolo volo
    private static final int PAUSA_TRA_CARTE_MS = 60;   // pausa tra carte

    // ── Costruttore ──────────────────────────────────────────────────────────
    public GameController(GameModel modello, GameView vista) {
        this.modello = modello;
        this.vista = vista;

        vista.gamePanel.setModel(modello);
        registraListenerMouse();
        registraListenerBottoni();
        avviaTimer();

        GameModel.Difficulty difficoltaScelta = vista.mostraDialogoDifficolta();
        modello.setDifficulty(difficoltaScelta);
        vista.updateDifficultyLabel(difficoltaScelta == GameModel.Difficulty.FACILE ? "Facile" : "Difficile");

        modello.initGame();
        aggiornaVista();
    }

    // ── Timer secondi ────────────────────────────────────────────────────────
    private void avviaTimer() {
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
            if (!autoCompletamentoAttivo) {
                modello.tickTimer();
                vista.updateTimerLabel(modello.getElapsedSeconds());
            }
        });
        timer.start();
    }

    // ── Listener bottoni ─────────────────────────────────────────────────────
    private void registraListenerBottoni() {
        JPanel pannelloInferiore = (JPanel) ((BorderLayout) vista.getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.SOUTH);

        for (Component c : pannelloInferiore.getComponents()) {
            if (c instanceof JButton bottone) {
                if ("newGameButton".equals(bottone.getName())) {
                    bottone.addActionListener(e -> {
                        fermaAutoCompletamento();
                        GameModel.Difficulty difficoltaScelta = vista.mostraDialogoDifficolta();
                        modello.setDifficulty(difficoltaScelta);
                        vista.updateDifficultyLabel(
                                difficoltaScelta == GameModel.Difficulty.FACILE ? "Facile" : "Difficile");
                        modello.initGame();
                        aggiornaVista();
                    });
                } else if ("undoButton".equals(bottone.getName())) {
                    bottone.addActionListener(e -> {
                        if (autoCompletamentoAttivo) return;
                        if (!modello.annullaMossa()) {
                            JOptionPane.showMessageDialog(vista,
                                    "Nessuna mossa da annullare.",
                                    "Undo", JOptionPane.INFORMATION_MESSAGE);
                        }
                        aggiornaVista();
                    });
                }
            }
        }
    }

    // ── Listener mouse ────────────────────────────────────────────────────────
    private void registraListenerMouse() {
        MouseAdapter adattatore = new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e)  { if (!autoCompletamentoAttivo) gestisciPressione(e); }
            @Override public void mouseDragged(MouseEvent e)  { if (!autoCompletamentoAttivo) gestisciDrag(e); }
            @Override public void mouseReleased(MouseEvent e) { if (!autoCompletamentoAttivo) gestisciRilascio(e); }
            @Override public void mouseClicked(MouseEvent e)  { if (!autoCompletamentoAttivo) gestisciClick(e); }
        };
        vista.gamePanel.addMouseListener(adattatore);
        vista.gamePanel.addMouseMotionListener(adattatore);
    }

    // ── Click stock ───────────────────────────────────────────────────────────
    private void gestisciClick(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        if (x >= GameView.SPAZIATURA_CARTE && x <= GameView.SPAZIATURA_CARTE + GameView.LARGHEZZA_CARTA
                && y >= GameView.SPAZIATURA_CARTE && y <= GameView.SPAZIATURA_CARTE + GameView.ALTEZZA_CARTA) {
            modello.drawFromStock();
            aggiornaVista();
            verificaAutoCompletamento();
        }
    }

    // ── Pressione ────────────────────────────────────────────────────────────
    private void gestisciPressione(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        modello.clearDrag();
        inizioDrag = null;

        int xScarto = GameView.SPAZIATURA_CARTE + GameView.LARGHEZZA_CARTA + GameView.SPAZIATURA_CARTE;

        // Scarto
        if (!modello.getWastePile().isEmpty()) {
            int visibili = modello.getCarteVisibiliWaste();
            int offsetCima = (visibili - 1) * 20;
            if (mx >= xScarto + offsetCima && mx <= xScarto + offsetCima + GameView.LARGHEZZA_CARTA
                    && my >= GameView.SPAZIATURA_CARTE && my <= GameView.SPAZIATURA_CARTE + GameView.ALTEZZA_CARTA) {
                modello.startDragFromWaste();
                inizioDrag = e.getPoint();
                offsetDrag.setLocation(mx - (xScarto + offsetCima), my - GameView.SPAZIATURA_CARTE);
                sincronizzaDragConVista(e.getPoint());
                return;
            }
        }

        // Fondamenta
        for (int i = 0; i < 4; i++) {
            int xF = GameView.SPAZIATURA_CARTE + (3 + i) * (GameView.LARGHEZZA_CARTA + GameView.SPAZIATURA_CARTE);
            if (!modello.getFoundations().get(i).isEmpty()
                    && mx >= xF && mx <= xF + GameView.LARGHEZZA_CARTA
                    && my >= GameView.SPAZIATURA_CARTE && my <= GameView.SPAZIATURA_CARTE + GameView.ALTEZZA_CARTA) {
                modello.startDragFromFoundation(i);
                inizioDrag = e.getPoint();
                offsetDrag.setLocation(mx - xF, my - GameView.SPAZIATURA_CARTE);
                sincronizzaDragConVista(e.getPoint());
                return;
            }
        }

        // Tavolo
        for (int col = 0; col < 7; col++) {
            List<GameModel.Card> pila = modello.getTableau().get(col);
            if (pila.isEmpty()) continue;
            int xCol = GameView.SPAZIATURA_CARTE + col * (GameView.LARGHEZZA_CARTA + GameView.SPAZIATURA_CARTE);
            int yCol = GameView.Y_TAVOLO;
            for (int i = pila.size() - 1; i >= 0; i--) {
                GameModel.Card carta = pila.get(i);
                int yC = yCol + i * GameView.OFFSET_PILA;
                int altezza = (i == pila.size() - 1) ? GameView.ALTEZZA_CARTA : GameView.OFFSET_PILA;
                if (mx >= xCol && mx <= xCol + GameView.LARGHEZZA_CARTA && my >= yC && my <= yC + altezza) {
                    if (carta.isFaceUp()) {
                        modello.startDragFromTableau(col, i);
                        inizioDrag = e.getPoint();
                        offsetDrag.setLocation(mx - xCol, my - yC);
                        sincronizzaDragConVista(e.getPoint());
                        return;
                    }
                }
            }
        }
    }

    // ── Drag ─────────────────────────────────────────────────────────────────
    private void gestisciDrag(MouseEvent e) {
        if (!modello.getDraggedCards().isEmpty() && inizioDrag != null) {
            posizioneMouse = e.getPoint();
            sincronizzaDragConVista(e.getPoint());
        }
    }

    // ── Rilascio ─────────────────────────────────────────────────────────────
    private void gestisciRilascio(MouseEvent e) {
        if (modello.getDraggedCards().isEmpty()) return;

        int mx = e.getX(), my = e.getY();
        boolean posizionata = false;

        if (modello.getDraggedCards().size() == 1) {
            for (int i = 0; i < 4; i++) {
                int xF = GameView.SPAZIATURA_CARTE + (3 + i) * (GameView.LARGHEZZA_CARTA + GameView.SPAZIATURA_CARTE);
                if (mx >= xF && mx <= xF + GameView.LARGHEZZA_CARTA
                        && my >= GameView.SPAZIATURA_CARTE && my <= GameView.SPAZIATURA_CARTE + GameView.ALTEZZA_CARTA) {
                    if (modello.tryPlaceOnFoundation(i)) {
                        posizionata = true;
                        break;
                    }
                }
            }
        }

        if (!posizionata) {
            for (int col = 0; col < 7; col++) {
                int xCol = GameView.SPAZIATURA_CARTE + col * (GameView.LARGHEZZA_CARTA + GameView.SPAZIATURA_CARTE);
                int yCol = GameView.Y_TAVOLO;
                List<GameModel.Card> pila = modello.getTableau().get(col);
                int yTarget = pila.isEmpty() ? yCol : yCol + pila.size() * GameView.OFFSET_PILA;
                if (mx >= xCol && mx <= xCol + GameView.LARGHEZZA_CARTA
                        && my >= yCol && my <= yTarget + GameView.ALTEZZA_CARTA) {
                    if (modello.tryPlaceOnTableau(col)) {
                        posizionata = true;
                        break;
                    }
                }
            }
        }

        modello.clearDrag();
        inizioDrag = null;
        posizioneMouse = null;
        sincronizzaDragConVista(null);
        aggiornaVista();

        if (modello.checkWin()) {
            SwingUtilities.invokeLater(this::mostraVittoria);
        } else {
            verificaAutoCompletamento();
        }
    }

    // ── Verifica autocompletamento ────────────────────────────────────────────
    /**
     * Condizione: stock vuoto, scarto vuoto, nessuna carta coperta nel tavolo.
     */
    private void verificaAutoCompletamento() {
        if (autoCompletamentoAttivo) return;
        if (!modello.getStockPile().isEmpty()) return;
        if (!modello.getWastePile().isEmpty()) return;

        for (List<GameModel.Card> colonna : modello.getTableau()) {
            for (GameModel.Card carta : colonna) {
                if (!carta.isFaceUp()) return;
            }
        }

        // Tutte le condizioni soddisfatte
        avviaAutoCompletamento();
    }

    // ── Avvia autocompletamento ───────────────────────────────────────────────
    private void avviaAutoCompletamento() {
        autoCompletamentoAttivo = true;
        // Piccola pausa prima di iniziare
        javax.swing.Timer pausa = new javax.swing.Timer(400, e -> prossimoPassoAutoCompletamento());
        pausa.setRepeats(false);
        pausa.start();
    }

    /**
     * Trova la prossima carta giocabile (rango minimo tra quelle in cima alle
     * colonne) e avvia l'animazione di volo verso la fondamenta corretta.
     */
    private void prossimoPassoAutoCompletamento() {
        if (modello.checkWin()) {
            fermaAutoCompletamento();
            SwingUtilities.invokeLater(this::mostraVittoria);
            return;
        }

        // Cerca la carta con il rango più basso tra le cime delle colonne
        // che può andare in fondamenta — garantisce ordine A->K senza blocchi
        GameModel.Card cartaDaSpostare = null;
        int colonnaOrigine = -1;
        int fondamentaTarget = -1;
        int rangoMinimo = Integer.MAX_VALUE;

        for (int col = 0; col < 7; col++) {
            List<GameModel.Card> pila = modello.getTableau().get(col);
            if (pila.isEmpty()) continue;
            GameModel.Card cima = pila.get(pila.size() - 1);
            for (int f = 0; f < 4; f++) {
                if (modello.canPlaceOnFoundation(cima, f)) {
                    if (cima.getRank().ordinal() < rangoMinimo) {
                        rangoMinimo = cima.getRank().ordinal();
                        cartaDaSpostare = cima;
                        colonnaOrigine = col;
                        fondamentaTarget = f;
                    }
                }
            }
        }

        if (cartaDaSpostare == null) {
            // Non ci sono mosse: auto-completamento non può procedere
            fermaAutoCompletamento();
            return;
        }

        // Coordinate di partenza (cima della colonna)
        List<GameModel.Card> pilaOrigine = modello.getTableau().get(colonnaOrigine);
        int xPartenza = GameView.SPAZIATURA_CARTE + colonnaOrigine * (GameView.LARGHEZZA_CARTA + GameView.SPAZIATURA_CARTE);
        int yPartenza = GameView.Y_TAVOLO + (pilaOrigine.size() - 1) * GameView.OFFSET_PILA;

        // Coordinate di arrivo (fondamenta target)
        int xArrivo = GameView.SPAZIATURA_CARTE + (3 + fondamentaTarget) * (GameView.LARGHEZZA_CARTA + GameView.SPAZIATURA_CARTE);
        int yArrivo = GameView.SPAZIATURA_CARTE;

        // Rimuovi carta dal modello: ora è "in volo"
        final int fondFin = fondamentaTarget;
        final GameModel.Card cartaFin = cartaDaSpostare;
        pilaOrigine.remove(cartaDaSpostare);

        // Gira eventuale carta sottostante
        if (!pilaOrigine.isEmpty() && !pilaOrigine.get(pilaOrigine.size() - 1).isFaceUp()) {
            pilaOrigine.get(pilaOrigine.size() - 1).flip();
        }

        // Prepara animazione
        posizioneVoloPartenza = new Point(xPartenza, yPartenza);
        posizioneVoloArrivo   = new Point(xArrivo, yArrivo);
        posizioneVoloAttuale  = new Point(xPartenza, yPartenza);
        cartaInVolo = cartaFin;
        frameVoloCorrente = 0;

        vista.gamePanel.setCartaInVolo(cartaInVolo, posizioneVoloAttuale);
        aggiornaVista();

        // Timer animazione frame per frame
        timerVolo = new javax.swing.Timer(INTERVALLO_FRAME_MS, null);
        timerVolo.addActionListener(e -> {
            frameVoloCorrente++;
            float t = Math.min(1f, (float) frameVoloCorrente / FRAME_PER_VOLO);

            if (t >= 1f) {
                // Fine volo: deposita carta nella fondamenta
                timerVolo.stop();
                timerVolo = null;
                cartaInVolo = null;
                vista.gamePanel.setCartaInVolo(null, null);

                modello.getFoundations().get(fondFin).add(cartaFin);
                aggiornaVista();

                // Pausa prima della prossima carta
                javax.swing.Timer prossima = new javax.swing.Timer(PAUSA_TRA_CARTE_MS, ev -> prossimoPassoAutoCompletamento());
                prossima.setRepeats(false);
                prossima.start();

            } else {
                // Easing ease-out cubico per movimento fluido e naturale
                float tEased = 1f - (1f - t) * (1f - t) * (1f - t);
                int x = (int) (posizioneVoloPartenza.x + tEased * (posizioneVoloArrivo.x - posizioneVoloPartenza.x));
                int y = (int) (posizioneVoloPartenza.y + tEased * (posizioneVoloArrivo.y - posizioneVoloPartenza.y));
                posizioneVoloAttuale = new Point(x, y);
                vista.gamePanel.setCartaInVolo(cartaInVolo, posizioneVoloAttuale);
                vista.gamePanel.repaint();
            }
        });
        timerVolo.start();
    }

    // ── Ferma autocompletamento ───────────────────────────────────────────────
    private void fermaAutoCompletamento() {
        autoCompletamentoAttivo = false;
        if (timerVolo != null) {
            timerVolo.stop();
            timerVolo = null;
        }
        cartaInVolo = null;
        vista.gamePanel.setCartaInVolo(null, null);
    }

    // ── Utilities ────────────────────────────────────────────────────────────
    private void sincronizzaDragConVista(Point posMouse) {
        vista.gamePanel.setDragState(
                modello.getDraggedCards(),
                inizioDrag,
                posMouse,
                modello.getSourceTableau(),
                modello.getSourceIndex()
        );
        vista.gamePanel.repaint();
    }

    private void aggiornaVista() {
        vista.updateTimerLabel(modello.getElapsedSeconds());
        vista.updateMovesLabel(modello.getMoveCount());
        vista.gamePanel.repaint();
    }

    private void mostraVittoria() {
        fermaAutoCompletamento();
        GameModel.Difficulty nuovaDiff = vista.showVictoryDialog(
                modello.getElapsedSeconds(),
                modello.getMoveCount(),
                modello.getDifficulty()
        );
        modello.setDifficulty(nuovaDiff);
        vista.updateDifficultyLabel(nuovaDiff == GameModel.Difficulty.FACILE ? "Facile" : "Difficile");
        modello.initGame();
        aggiornaVista();
    }

    // ── Entry point ──────────────────────────────────────────────────────────
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | IllegalAccessException
                     | InstantiationException | UnsupportedLookAndFeelException ex) {
                System.out.println(ex);
            }

            GameModel modello = new GameModel();
            GameView vista = new GameView();
            new GameController(modello, vista);
            vista.pack();
            vista.setLocationRelativeTo(null);
            vista.setVisible(true);
        });
    }
}