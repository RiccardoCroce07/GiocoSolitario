package solitairegame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

/**
 * CONTROLLER - Collega Model e View.
 * Gestisce gli eventi utente, aggiorna il Model e ordina alla View di ridisegnarsi.
 */
public class GameController {

    private final GameModel model;
    private final GameView  view;

    // Stato drag (coordinate) — i dati delle carte stanno nel model
    private Point dragStart      = null;
    private Point currentMousePos = null;
    private Point dragOffset     = new Point(0, 0);

    // ── Costruttore ──────────────────────────────────────────────────────────

    public GameController(GameModel model, GameView view) {
        this.model = model;
        this.view  = view;

        // Collega il pannello al model per il rendering
        view.gamePanel.setModel(model);

        // Registra gli event listener
        registerMouseListeners();
        registerButtonListeners();
        startTimer();

        // Prima partita
        model.initGame();
        refreshView();
    }

    // ── Timer ────────────────────────────────────────────────────────────────

    private void startTimer() {
        javax.swing.Timer timer = new javax.swing.Timer(1000, e -> {
            model.tickTimer();
            view.updateTimerLabel(model.getElapsedSeconds());
        });
        timer.start();
    }

    // ── Listener pulsanti bottom panel ───────────────────────────────────────

    private void registerButtonListeners() {
        // Cerca componenti per nome nel bottom panel
        // (il bottom panel è il component SOUTH)
        JPanel bottom = (JPanel) ((BorderLayout) view.getContentPane().getLayout())
            .getLayoutComponent(BorderLayout.SOUTH);

        for (Component c : bottom.getComponents()) {
            if (c instanceof JComboBox) {
                @SuppressWarnings("unchecked")
                JComboBox<String> box = (JComboBox<String>) c;
                box.addActionListener(e -> {
                    int sel = box.getSelectedIndex();
                    model.setDifficulty(sel == 0
                        ? GameModel.Difficulty.FACILE
                        : GameModel.Difficulty.DIFFICILE);
                    view.updateDifficultyLabel(sel == 0 ? "Facile" : "Difficile");
                    model.initGame();
                    refreshView();
                });
            } else if (c instanceof JButton) {
                JButton btn = (JButton) c;
                if ("newGameButton".equals(btn.getName())) {
                    btn.addActionListener(e -> {
                        model.initGame();
                        refreshView();
                    });
                } else if ("winButton".equals(btn.getName())) {
                    btn.addActionListener(e -> showVictory());
                }
            }
        }
    }

    // ── Listener mouse del GamePanel ─────────────────────────────────────────

    private void registerMouseListeners() {
        MouseAdapter ma = new MouseAdapter() {
            @Override public void mousePressed (MouseEvent e) { handleMousePress(e);   }
            @Override public void mouseDragged (MouseEvent e) { handleMouseDrag(e);    }
            @Override public void mouseReleased(MouseEvent e) { handleMouseRelease(e); }
            @Override public void mouseClicked (MouseEvent e) { handleMouseClick(e);   }
        };
        view.gamePanel.addMouseListener(ma);
        view.gamePanel.addMouseMotionListener(ma);
    }

    // ── Click (stock) ─────────────────────────────────────────────────────────

    private void handleMouseClick(MouseEvent e) {
        int x = e.getX(), y = e.getY();
        int stockX = GameView.CARD_SPACING;
        int stockY = GameView.CARD_SPACING;
        if (x >= stockX && x <= stockX + GameView.CARD_WIDTH &&
            y >= stockY && y <= stockY + GameView.CARD_HEIGHT) {
            model.drawFromStock();
            refreshView();
        }
    }

    // ── Press (inizio drag) ───────────────────────────────────────────────────

    private void handleMousePress(MouseEvent e) {
        int mx = e.getX(), my = e.getY();
        model.clearDrag();
        dragStart = null;

        int wasteX = GameView.CARD_SPACING + GameView.CARD_WIDTH + GameView.CARD_SPACING;

        // Waste
        if (!model.getWastePile().isEmpty()) {
            int show   = Math.min(3, model.getWastePile().size());
            int topOff = (show - 1) * 20;
            if (mx >= wasteX + topOff && mx <= wasteX + topOff + GameView.CARD_WIDTH &&
                my >= GameView.CARD_SPACING && my <= GameView.CARD_SPACING + GameView.CARD_HEIGHT) {
                model.startDragFromWaste();
                dragStart = e.getPoint();
                dragOffset.setLocation(mx - (wasteX + topOff), my - GameView.CARD_SPACING);
                syncDragToView(e.getPoint());
                return;
            }
        }

        // Foundations
        for (int i = 0; i < 4; i++) {
            int fx = GameView.CARD_SPACING + (3 + i) * (GameView.CARD_WIDTH + GameView.CARD_SPACING);
            if (!model.getFoundations().get(i).isEmpty() &&
                mx >= fx && mx <= fx + GameView.CARD_WIDTH &&
                my >= GameView.CARD_SPACING && my <= GameView.CARD_SPACING + GameView.CARD_HEIGHT) {
                model.startDragFromFoundation(i);
                dragStart = e.getPoint();
                dragOffset.setLocation(mx - fx, my - GameView.CARD_SPACING);
                syncDragToView(e.getPoint());
                return;
            }
        }

        // Tableau
        for (int col = 0; col < 7; col++) {
            List<GameModel.Card> pile = model.getTableau().get(col);
            if (pile.isEmpty()) continue;
            int tx = GameView.CARD_SPACING + col * (GameView.CARD_WIDTH + GameView.CARD_SPACING);
            int ty = GameView.TABLEAU_Y;

            for (int i = pile.size() - 1; i >= 0; i--) {
                GameModel.Card card = pile.get(i);
                int cardY   = ty + i * GameView.PILE_OFFSET;
                boolean last = (i == pile.size() - 1);
                int cardH   = last ? GameView.CARD_HEIGHT : GameView.PILE_OFFSET;

                if (mx >= tx && mx <= tx + GameView.CARD_WIDTH &&
                    my >= cardY && my <= cardY + cardH) {
                    if (card.isFaceUp()) {
                        model.startDragFromTableau(col, i);
                        dragStart = e.getPoint();
                        dragOffset.setLocation(mx - tx, my - cardY);
                        syncDragToView(e.getPoint());
                        return;
                    }
                }
            }
        }
    }

    // ── Drag ─────────────────────────────────────────────────────────────────

    private void handleMouseDrag(MouseEvent e) {
        if (!model.getDraggedCards().isEmpty() && dragStart != null) {
            currentMousePos = e.getPoint();
            syncDragToView(e.getPoint());
        }
    }

    // ── Release (drop) ────────────────────────────────────────────────────────

    private void handleMouseRelease(MouseEvent e) {
        if (model.getDraggedCards().isEmpty()) return;

        int mx = e.getX(), my = e.getY();
        boolean placed = false;

        // Prova foundation
        if (model.getDraggedCards().size() == 1) {
            for (int i = 0; i < 4; i++) {
                int fx = GameView.CARD_SPACING + (3 + i) * (GameView.CARD_WIDTH + GameView.CARD_SPACING);
                if (mx >= fx && mx <= fx + GameView.CARD_WIDTH &&
                    my >= GameView.CARD_SPACING && my <= GameView.CARD_SPACING + GameView.CARD_HEIGHT) {
                    if (model.tryPlaceOnFoundation(i)) {
                        placed = true;
                        if (model.checkWin())
                            SwingUtilities.invokeLater(this::showVictory);
                        break;
                    }
                }
            }
        }

        // Prova tableau
        if (!placed) {
            for (int col = 0; col < 7; col++) {
                int tx  = GameView.CARD_SPACING + col * (GameView.CARD_WIDTH + GameView.CARD_SPACING);
                int ty  = GameView.TABLEAU_Y;
                List<GameModel.Card> pile = model.getTableau().get(col);
                int targetY = pile.isEmpty() ? ty : ty + pile.size() * GameView.PILE_OFFSET;

                if (mx >= tx && mx <= tx + GameView.CARD_WIDTH &&
                    my >= ty && my <= targetY + GameView.CARD_HEIGHT) {
                    if (model.tryPlaceOnTableau(col)) {
                        placed = true;
                        break;
                    }
                }
            }
        }

        model.clearDrag();
        dragStart       = null;
        currentMousePos = null;
        syncDragToView(null);
        refreshView();
    }

    // ── Sincronizza stato drag con la View ───────────────────────────────────

    private void syncDragToView(Point mousePos) {
        view.gamePanel.setDragState(
            model.getDraggedCards(),
            dragStart,
            mousePos,
            model.getSourceTableau(),
            model.getSourceIndex()
        );
        view.gamePanel.repaint();
    }

    // ── Aggiorna etichette e ridisegna ───────────────────────────────────────

    private void refreshView() {
        view.updateTimerLabel(model.getElapsedSeconds());
        view.updateMovesLabel(model.getMoveCount());
        view.gamePanel.repaint();
    }

    private void showVictory() {
        view.showVictoryDialog(
            model.getElapsedSeconds(),
            model.getMoveCount(),
            model.getDifficulty()
        );
    }

    // ── Entry point ──────────────────────────────────────────────────────────

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) { ex.printStackTrace(); }

            GameModel model = new GameModel();
            GameView  view  = new GameView();

            new GameController(model, view);

            view.pack();
            view.setLocationRelativeTo(null);
            view.setVisible(true);
        });
    }
}
