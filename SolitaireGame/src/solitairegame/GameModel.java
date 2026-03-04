package solitairegame;

import java.util.*;

/**
 * MODELLO - Contiene lo stato del gioco e la logica di business. Non ha
 * dipendenze da Swing o dalla Vista.
 */
public class GameModel {

    // ── Carta ────────────────────────────────────────────────────────────────
    public static class Card {

        public enum Suit {
            HEARTS("♥"), DIAMONDS("♦"), CLUBS("♣"), SPADES("♠");
            private final String simbolo;

            Suit(String simbolo) {
                this.simbolo = simbolo;
            }

            @Override
            public String toString() {
                return simbolo;
            }
        }

        public enum Rank {
            ACE("A"), TWO("2"), THREE("3"), FOUR("4"), FIVE("5"), SIX("6"),
            SEVEN("7"), EIGHT("8"), NINE("9"), TEN("10"), JACK("J"), QUEEN("Q"), KING("K");
            private final String simbolo;

            Rank(String simbolo) {
                this.simbolo = simbolo;
            }

            @Override
            public String toString() {
                return simbolo;
            }
        }

        private final Suit seme;
        private final Rank rango;
        boolean facciaInSu;

        public Card(Suit seme, Rank rango) {
            this.seme = seme;
            this.rango = rango;
            this.facciaInSu = false;
        }

        public Suit getSuit() {
            return seme;
        }

        public Rank getRank() {
            return rango;
        }

        public boolean isFaceUp() {
            return facciaInSu;
        }

        public void flip() {
            facciaInSu = !facciaInSu;
        }
    }

    // ── Mazzo ────────────────────────────────────────────────────────────────
    public static class Deck {

        private final List<Card> carte = new ArrayList<>();

        public Deck() {
            for (Card.Suit seme : Card.Suit.values()) {
                for (Card.Rank rango : Card.Rank.values()) {
                    carte.add(new Card(seme, rango));
                }
            }
        }

        public void shuffle() {
            Collections.shuffle(carte);
        }

        public boolean hasCards() {
            return !carte.isEmpty();
        }

        public Card draw() {
            return carte.isEmpty() ? null : carte.remove(carte.size() - 1);
        }
    }

    // ── Livello di difficoltà ─────────────────────────────────────────────────
    public enum Difficulty {
        FACILE, DIFFICILE
    }

    // ── Snapshot per undo ────────────────────────────────────────────────────
    private static class StatoPartita {
        List<Card> pilaStock;
        List<Card> pilaScarto;
        List<List<Card>> fondamenta;
        List<List<Card>> tavolo;
        int contatoreMovimenti;
        // Per ogni carta salviamo lo stato facciaInSu
        // Le carte sono condivise per riferimento, quindi salviamo copie profonde
    }

    // ── Stato del gioco ──────────────────────────────────────────────────────
    private Deck mazzo;
    private List<Card> pilaStock;
    private List<Card> pilaScarto;
    private List<List<Card>> fondamenta;
    private List<List<Card>> tavolo;

    private List<Card> carteTrascinate = new ArrayList<>();
    private int sorgentePila = -1;
    private int sorgentePosizione = -1;

    // Statistiche partita
    private int secondiTrascorsi = 0;
    private int contatoreMovimenti = 0;
    private boolean partitaIniziata = false;

    // Difficoltà
    private Difficulty difficoltaCorrente = Difficulty.FACILE;
    private int carteDaPescareAllaVolta = 1;

    // ── Undo ─────────────────────────────────────────────────────────────────
    // Stack degli stati salvati per la funzione "mossa precedente"
    private final Deque<byte[]> storicoPila = new ArrayDeque<>();

    /**
     * Salva lo stato corrente nello storico per poterlo ripristinare con undo.
     * Chiamare PRIMA di eseguire ogni mossa.
     */
    private void salvaStatoPerUndo() {
        // Serializzazione leggera: ogni carta è identificata da seme+rango+facciaInSu
        // Usiamo una lista ordinata di interi come snapshot compatto
        // Formato: [stockSize, carte stock..., scartoSize, carte scarto...,
        //           fond0size, carte..., ..., fond3size, carte...,
        //           tab0size, carte..., ..., tab6size, carte..., mosse]
        // Ogni carta: seme(0-3)*13 + rango(0-12), bit 7 = facciaInSu
        List<Integer> dati = new ArrayList<>();

        // Stock
        dati.add(pilaStock.size());
        for (Card c : pilaStock) {
            dati.add(codificaCarta(c));
        }
        // Scarto
        dati.add(pilaScarto.size());
        for (Card c : pilaScarto) {
            dati.add(codificaCarta(c));
        }
        // Fondamenta
        for (List<Card> f : fondamenta) {
            dati.add(f.size());
            for (Card c : f) {
                dati.add(codificaCarta(c));
            }
        }
        // Tavolo
        for (List<Card> col : tavolo) {
            dati.add(col.size());
            for (Card c : col) {
                dati.add(codificaCarta(c));
            }
        }
        // Mosse
        dati.add(contatoreMovimenti);

        // Converti in byte array
        byte[] snapshot = new byte[dati.size()];
        for (int i = 0; i < dati.size(); i++) {
            snapshot[i] = dati.get(i).byteValue();
        }
        storicoPila.push(snapshot);

        // Mantieni max 50 stati
        while (storicoPila.size() > 50) {
            storicoPila.removeLast();
        }
    }

    private int codificaCarta(Card c) {
        int val = c.getSuit().ordinal() * 13 + c.getRank().ordinal();
        if (c.facciaInSu) {
            val |= 0x80;
        }
        return val & 0xFF;
    }

    /**
     * Ripristina lo stato precedente (undo).
     * Ritorna true se l'operazione è riuscita.
     */
    public boolean annullaMossa() {
        if (storicoPila.isEmpty()) {
            return false;
        }
        byte[] snapshot = storicoPila.pop();

        // Ricostruisci tutte le carte del gioco (52 carte totali, stessi oggetti ricostruiti)
        // Prima costruiamo la mappa indice -> Card nuova
        Card[] tutteLeCarteNuove = new Card[52];
        for (Card.Suit seme : Card.Suit.values()) {
            for (Card.Rank rango : Card.Rank.values()) {
                int idx = seme.ordinal() * 13 + rango.ordinal();
                tutteLeCarteNuove[idx] = new Card(seme, rango);
            }
        }

        int pos = 0;

        // Ricostruisci stock
        pilaStock = new ArrayList<>();
        int stockSize = snapshot[pos++] & 0xFF;
        for (int i = 0; i < stockSize; i++) {
            int codice = snapshot[pos++] & 0xFF;
            Card c = tutteLeCarteNuove[codice & 0x7F];
            c.facciaInSu = (codice & 0x80) != 0;
            pilaStock.add(c);
        }

        // Ricostruisci scarto
        pilaScarto = new ArrayList<>();
        int scartoSize = snapshot[pos++] & 0xFF;
        for (int i = 0; i < scartoSize; i++) {
            int codice = snapshot[pos++] & 0xFF;
            Card c = tutteLeCarteNuove[codice & 0x7F];
            c.facciaInSu = (codice & 0x80) != 0;
            pilaScarto.add(c);
        }

        // Ricostruisci fondamenta
        fondamenta = new ArrayList<>();
        for (int f = 0; f < 4; f++) {
            List<Card> fonda = new ArrayList<>();
            int fSize = snapshot[pos++] & 0xFF;
            for (int i = 0; i < fSize; i++) {
                int codice = snapshot[pos++] & 0xFF;
                Card c = tutteLeCarteNuove[codice & 0x7F];
                c.facciaInSu = (codice & 0x80) != 0;
                fonda.add(c);
            }
            fondamenta.add(fonda);
        }

        // Ricostruisci tavolo
        tavolo = new ArrayList<>();
        for (int col = 0; col < 7; col++) {
            List<Card> colonna = new ArrayList<>();
            int colSize = snapshot[pos++] & 0xFF;
            for (int i = 0; i < colSize; i++) {
                int codice = snapshot[pos++] & 0xFF;
                Card c = tutteLeCarteNuove[codice & 0x7F];
                c.facciaInSu = (codice & 0x80) != 0;
                colonna.add(c);
            }
            tavolo.add(colonna);
        }

        contatoreMovimenti = snapshot[pos] & 0xFF;
        carteTrascinate.clear();
        sorgentePila = -1;
        sorgentePosizione = -1;
        return true;
    }

    public boolean hasMossePrecedenti() {
        return !storicoPila.isEmpty();
    }

    // ── Inizializzazione partita ──────────────────────────────────────────────
    public void initGame() {
        mazzo = new Deck();
        mazzo.shuffle();

        pilaStock = new ArrayList<>();
        pilaScarto = new ArrayList<>();
        fondamenta = new ArrayList<>();
        tavolo = new ArrayList<>();
        storicoPila.clear();

        secondiTrascorsi = 0;
        contatoreMovimenti = 0;
        partitaIniziata = false;

        for (int i = 0; i < 4; i++) {
            fondamenta.add(new ArrayList<>());
        }
        for (int i = 0; i < 7; i++) {
            tavolo.add(new ArrayList<>());
        }

        for (int col = 0; col < 7; col++) {
            for (int riga = 0; riga <= col; riga++) {
                Card carta = mazzo.draw();
                if (riga == col) {
                    carta.flip();
                }
                tavolo.get(col).add(carta);
            }
        }

        while (mazzo.hasCards()) {
            pilaStock.add(mazzo.draw());
        }

        carteTrascinate = new ArrayList<>();
    }

    // ── Pesca dallo stock ─────────────────────────────────────────────────────
    /**
     * FACILE: pesca 1 carta alla volta, mostra max 3 carte nello scarto.
     * Quando lo scarto raggiunge 3 carte visibili e si clicca ancora,
     * NON viene pescata una nuova carta ma le 3 vengono "resettate":
     * si fa scorrere la finestra visibile (la più vecchia delle 3 esce dalla
     * vista e la successiva nello stock entra). In pratica le carte visibili
     * avanzano di 1 ogni click, e al 4° click (quando tutte e 3 sono visibili)
     * si continua a pescare normalmente. Se lo stock è vuoto si riciclano le
     * carte dello scarto.
     *
     * DIFFICILE: pesca 3 carte alla volta.
     */
    public void drawFromStock() {
        salvaStatoPerUndo();
        if (!pilaStock.isEmpty()) {
            int daPescare = Math.min(carteDaPescareAllaVolta, pilaStock.size());
            for (int i = 0; i < daPescare; i++) {
                Card carta = pilaStock.remove(pilaStock.size() - 1);
                carta.facciaInSu = true;
                pilaScarto.add(carta);
            }
            incrementaMovimenti();
        } else if (!pilaScarto.isEmpty()) {
            // Reset: rimetti tutte le carte dello scarto nello stock
            Collections.reverse(pilaScarto);
            for (Card carta : pilaScarto) {
                carta.facciaInSu = false;
                pilaStock.add(carta);
            }
            pilaScarto.clear();
        }
    }

    /**
     * Numero di carte visibili nello scarto.
     * FACILE: sempre 1 (solo la carta in cima).
     * DIFFICILE: fino a 3.
     */
    public int getCarteVisibiliWaste() {
        if (pilaScarto.isEmpty()) {
            return 0;
        }
        if (difficoltaCorrente == Difficulty.FACILE) {
            return 1;
        }
        return Math.min(3, pilaScarto.size());
    }

    // ── Regole di posizionamento ──────────────────────────────────────────────
    public boolean canPlaceOnFoundation(Card carta, int indiceFondamenta) {
        List<Card> f = fondamenta.get(indiceFondamenta);
        if (f.isEmpty()) {
            return carta.getRank() == Card.Rank.ACE;
        }
        Card cima = f.get(f.size() - 1);
        return carta.getSuit() == cima.getSuit()
                && carta.getRank().ordinal() == cima.getRank().ordinal() + 1;
    }

    public boolean canPlaceOnTableau(Card carta, int colonna) {
        List<Card> pila = tavolo.get(colonna);
        if (pila.isEmpty()) {
            return carta.getRank() == Card.Rank.KING;
        }
        Card cima = pila.get(pila.size() - 1);
        boolean cartaRossa = carta.getSuit() == Card.Suit.HEARTS || carta.getSuit() == Card.Suit.DIAMONDS;
        boolean cimaRossa = cima.getSuit() == Card.Suit.HEARTS || cima.getSuit() == Card.Suit.DIAMONDS;
        return cartaRossa != cimaRossa
                && carta.getRank().ordinal() == cima.getRank().ordinal() - 1;
    }

    // ── Posizionamento carte (drop) ───────────────────────────────────────────
    public boolean tryPlaceOnFoundation(int indiceFondamenta) {
        if (carteTrascinate.size() != 1) {
            return false;
        }
        Card carta = carteTrascinate.get(0);
        if (!canPlaceOnFoundation(carta, indiceFondamenta)) {
            return false;
        }
        salvaStatoPerUndo();
        rimuoviCarteDallaSorgente();
        fondamenta.get(indiceFondamenta).add(carta);
        incrementaMovimenti();
        return true;
    }

    public boolean tryPlaceOnTableau(int colonna) {
        Card primaCarta = carteTrascinate.get(0);
        if (!canPlaceOnTableau(primaCarta, colonna)) {
            return false;
        }
        salvaStatoPerUndo();
        rimuoviCarteDallaSorgente();
        tavolo.get(colonna).addAll(carteTrascinate);
        incrementaMovimenti();
        return true;
    }

    public boolean checkWin() {
        for (List<Card> f : fondamenta) {
            if (f.size() != 13) {
                return false;
            }
        }
        return true;
    }

    public void rimuoviCarteDallaSorgente() {
        if (sorgentePila == -2) {
            if (!pilaScarto.isEmpty()) {
                pilaScarto.remove(pilaScarto.size() - 1);
            }
        } else if (sorgentePila < -2) {
            int idx = -(sorgentePila + 3);
            List<Card> f = fondamenta.get(idx);
            if (!f.isEmpty()) {
                f.remove(f.size() - 1);
            }
        } else if (sorgentePila >= 0) {
            List<Card> pila = tavolo.get(sorgentePila);
            pila.removeAll(carteTrascinate);
            if (!pila.isEmpty() && !pila.get(pila.size() - 1).isFaceUp()) {
                pila.get(pila.size() - 1).flip();
            }
        }
    }

    public void removeCardFromSource() {
        rimuoviCarteDallaSorgente();
    }

    // ── Gestione drag ─────────────────────────────────────────────────────────
    public void startDragFromWaste() {
        if (pilaScarto.isEmpty()) {
            return;
        }
        carteTrascinate.add(pilaScarto.get(pilaScarto.size() - 1));
        sorgentePila = -2;
    }

    public void startDragFromFoundation(int i) {
        List<Card> f = fondamenta.get(i);
        if (f.isEmpty()) {
            return;
        }
        carteTrascinate.add(f.get(f.size() - 1));
        sorgentePila = -(i + 3);
    }

    public void startDragFromTableau(int colonna, int indiceCarta) {
        List<Card> pila = tavolo.get(colonna);
        for (int j = indiceCarta; j < pila.size(); j++) {
            carteTrascinate.add(pila.get(j));
        }
        sorgentePila = colonna;
        sorgentePosizione = indiceCarta;
    }

    public void clearDrag() {
        carteTrascinate.clear();
        sorgentePila = -1;
        sorgentePosizione = -1;
    }

    // ── Timer / Statistiche ──────────────────────────────────────────────────
    public void tickTimer() {
        if (partitaIniziata) {
            secondiTrascorsi++;
        }
    }

    private void incrementaMovimenti() {
        if (!partitaIniziata) {
            partitaIniziata = true;
        }
        contatoreMovimenti++;
    }

    // ── Impostazione difficoltà ───────────────────────────────────────────────
    public void setDifficulty(Difficulty d) {
        difficoltaCorrente = d;
        carteDaPescareAllaVolta = (d == Difficulty.FACILE) ? 1 : 3;
    }

    // ── Getter ───────────────────────────────────────────────────────────────
    public List<Card> getStockPile() {
        return pilaStock;
    }

    public List<Card> getWastePile() {
        return pilaScarto;
    }

    public List<List<Card>> getFoundations() {
        return fondamenta;
    }

    public List<List<Card>> getTableau() {
        return tavolo;
    }

    public List<Card> getDraggedCards() {
        return carteTrascinate;
    }

    public int getSourceTableau() {
        return sorgentePila;
    }

    public int getSourceIndex() {
        return sorgentePosizione;
    }

    public int getElapsedSeconds() {
        return secondiTrascorsi;
    }

    public int getMoveCount() {
        return contatoreMovimenti;
    }

    public boolean isGameStarted() {
        return partitaIniziata;
    }

    public Difficulty getDifficulty() {
        return difficoltaCorrente;
    }

    public int getCardsToDrawFromStock() {
        return carteDaPescareAllaVolta;
    }
}
