package com.example.xo;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameActivity extends AppCompatActivity {

    public static final String KEY_TOTAL_PARTIES = "TotalParties";

    private int totalParties;
    private int partieActuelle = 1;
    private int scoreX = 0;
    private int scoreO = 0;
    private int partiesNulles = 0;
    private boolean isPlayerXTurn = true;
    private boolean partieTerminee = false;
    private String[][] board = new String[3][3];
    private Button[][] buttons = new Button[3][3];

    // PvE variables
    private boolean isPvE;
    private int difficulty; // 0: Easy, 1: Medium, 2: Hard
    private String userSymbol;
    private String botSymbol;

    private TextView textPartieNumero;
    private TextView textScoreX;
    private TextView textScoreO;
    private TextView textPartiesNulles;
    private TextView textStatus;
    private Button btnToggleMusicGame;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Allow volume control
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Récupérer les données de l'intent
        Intent intent = getIntent();
        totalParties = intent.getIntExtra(KEY_TOTAL_PARTIES, 5);
        isPvE = intent.getBooleanExtra("IS_PVE", false);
        difficulty = intent.getIntExtra("DIFFICULTY", 0);
        userSymbol = intent.getStringExtra("USER_SYMBOL");
        if (userSymbol == null) userSymbol = "X";
        botSymbol = userSymbol.equals("X") ? "O" : "X";

        // Initialisation des vues
        textPartieNumero = findViewById(R.id.text_partie_numero);
        textScoreX = findViewById(R.id.text_score_x);
        textScoreO = findViewById(R.id.text_score_o);
        textPartiesNulles = findViewById(R.id.text_parties_nulles);
        textStatus = findViewById(R.id.text_status);
        btnToggleMusicGame = findViewById(R.id.btn_toggle_music_game);
        progressBar = findViewById(R.id.progress_bar);

        initializeButtons();
        updateUI();
        resetBoard();

        // Music Toggle Logic for Game Screen
        if (btnToggleMusicGame != null) {
            // Initial state check
            boolean isPlaying = false;
            if (HomeActivity.mediaPlayer != null) {
                isPlaying = HomeActivity.mediaPlayer.isPlaying();
            }
            btnToggleMusicGame.setText(isPlaying ? "🎵 Musique : ON" : "🎵 Musique : OFF");

            btnToggleMusicGame.setOnClickListener(v -> {
                if (HomeActivity.mediaPlayer != null) {
                    if (HomeActivity.mediaPlayer.isPlaying()) {
                        HomeActivity.mediaPlayer.pause();
                        btnToggleMusicGame.setText("🎵 Musique : OFF");
                    } else {
                        HomeActivity.mediaPlayer.start();
                        btnToggleMusicGame.setText("🎵 Musique : ON");
                    }
                }
            });
        }
    }

    private void initializeButtons() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                String buttonID = "button_" + i + j;
                int resID = getResources().getIdentifier(buttonID, "id", getPackageName());
                buttons[i][j] = findViewById(resID);
            }
        }
    }

    public void onCellClick(View v) {
        if (partieTerminee) {
            Toast.makeText(this, "La partie est terminée.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Si c'est le tour du bot, on ignore le clic (sécurité)
        if (isPvE) {
            boolean isBotTurn = (isPlayerXTurn && botSymbol.equals("X")) || (!isPlayerXTurn && botSymbol.equals("O"));
            if (isBotTurn) return;
        }

        Button b = (Button) v;
        if (!b.getText().toString().isEmpty()) return; // Case déjà jouée

        String tag = b.getTag().toString();
        int row = Character.getNumericValue(tag.charAt(0));
        int col = Character.getNumericValue(tag.charAt(1));

        makeMove(row, col);
    }

    private void makeMove(int row, int col) {
        String symbol = isPlayerXTurn ? "X" : "O";

        // Set text and color
        buttons[row][col].setText(symbol);
        if (symbol.equals("X")) {
            buttons[row][col].setTextColor(getResources().getColor(R.color.neon_pink));
        } else {
            buttons[row][col].setTextColor(getResources().getColor(R.color.neon_blue));
        }

        board[row][col] = symbol;

        if (checkForWin()) {
            handleEndPartie(symbol, true);
        } else if (isBoardFull()) {
            handleEndPartie("NUL", false);
        } else {
            isPlayerXTurn = !isPlayerXTurn; // Passer le tour
            updateStatus();

            // Trigger bot move if applicable
            if (isPvE && !partieTerminee) {
                boolean isBotTurn = (isPlayerXTurn && botSymbol.equals("X")) || (!isPlayerXTurn && botSymbol.equals("O"));
                if (isBotTurn) {
                    new Handler(Looper.getMainLooper()).postDelayed(this::playBotMove, 500);
                }
            }
        }
    }

    private void playBotMove() {
        if (partieTerminee) return;
        int[] move = getBestMove();
        if (move != null) {
            makeMove(move[0], move[1]);
        }
    }

    private int[] getBestMove() {
        // Facile: Aléatoire
        if (difficulty == 0) {
            return getRandomMove();
        }
        // Moyen: Bloquer ou Gagner ou Aléatoire
        else if (difficulty == 1) {
            // Tenter de gagner
            int[] winningMove = findWinningMove(botSymbol);
            if (winningMove != null) return winningMove;

            // Bloquer l'adversaire
            int[] blockingMove = findWinningMove(userSymbol);
            if (blockingMove != null) return blockingMove;

            return getRandomMove();
        }
        // Difficile: Minimax
        else {
            // Optimisation pour le premier coup (le centre ou aléatoire) pour éviter un calcul inutile sur grille vide
            if (isEmptyBoard()) return getRandomMove();
            return minimaxRoot();
        }
    }

    private int[] getRandomMove() {
        List<int[]> availableMoves = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].isEmpty()) {
                    availableMoves.add(new int[]{i, j});
                }
            }
        }
        if (availableMoves.isEmpty()) return null;
        return availableMoves.get(new Random().nextInt(availableMoves.size()));
    }

    private int[] findWinningMove(String symbol) {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].isEmpty()) {
                    board[i][j] = symbol;
                    if (checkForWin(symbol)) {
                        board[i][j] = ""; // Backtrack
                        return new int[]{i, j};
                    }
                    board[i][j] = ""; // Backtrack
                }
            }
        }
        return null;
    }

    private boolean isEmptyBoard() {
        for(int i=0; i<3; i++)
            for(int j=0; j<3; j++)
                if(!board[i][j].isEmpty()) return false;
        return true;
    }

    /** Vérifie si le joueur spécifié a gagné */
    private boolean checkForWin(String player) {
        // Lignes
        for (int i = 0; i < 3; i++) {
            if (board[i][0].equals(player) && board[i][1].equals(player) && board[i][2].equals(player)) return true;
        }
        // Colonnes
        for (int i = 0; i < 3; i++) {
            if (board[0][i].equals(player) && board[1][i].equals(player) && board[2][i].equals(player)) return true;
        }
        // Diagonales
        if (board[0][0].equals(player) && board[1][1].equals(player) && board[2][2].equals(player)) return true;
        if (board[0][2].equals(player) && board[1][1].equals(player) && board[2][0].equals(player)) return true;
        return false;
    }

    /** Vérifie s'il y a un gagnant (pour la logique de jeu principale) */
    private boolean checkForWin() {
        String s;
        for (int i = 0; i < 3; i++) {
            // Lignes
            s = board[i][0];
            if (s != null && !s.isEmpty() && s.equals(board[i][1]) && s.equals(board[i][2])) return true;
            // Colonnes
            s = board[0][i];
            if (s != null && !s.isEmpty() && s.equals(board[1][i]) && s.equals(board[2][i])) return true;
        }
        // Diagonales
        s = board[0][0];
        if (s != null && !s.isEmpty() && s.equals(board[1][1]) && s.equals(board[2][2])) return true;
        s = board[0][2];
        if (s != null && !s.isEmpty() && s.equals(board[1][1]) && s.equals(board[2][0])) return true;

        return false;
    }

    // --- Minimax Algorithm ---
    private int[] minimaxRoot() {
        int bestScore = Integer.MIN_VALUE;
        int[] bestMove = null;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].isEmpty()) {
                    board[i][j] = botSymbol;
                    int score = minimax(false, 0);
                    board[i][j] = "";
                    if (score > bestScore) {
                        bestScore = score;
                        bestMove = new int[]{i, j};
                    }
                }
            }
        }
        return bestMove != null ? bestMove : getRandomMove();
    }

    private int minimax(boolean isMaximizing, int depth) {
        if (checkForWin(botSymbol)) return 10 - depth;
        if (checkForWin(userSymbol)) return depth - 10;
        if (isBoardFull()) return 0;

        if (isMaximizing) {
            int bestScore = Integer.MIN_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j].isEmpty()) {
                        board[i][j] = botSymbol;
                        int score = minimax(false, depth + 1);
                        board[i][j] = "";
                        bestScore = Math.max(score, bestScore);
                    }
                }
            }
            return bestScore;
        } else {
            int bestScore = Integer.MAX_VALUE;
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    if (board[i][j].isEmpty()) {
                        board[i][j] = userSymbol;
                        int score = minimax(true, depth + 1);
                        board[i][j] = "";
                        bestScore = Math.min(score, bestScore);
                    }
                }
            }
            return bestScore;
        }
    }

    private boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j].isEmpty()) return false;
            }
        }
        return true;
    }

    private void handleEndPartie(String result, boolean isWin) {
        partieTerminee = true;

        if (isWin) {
            textStatus.setText("🏆 Victoire de " + result + "! 🏆");
            if (result.equals("X")) scoreX++; else scoreO++;
        } else {
            textStatus.setText("🤝 Match nul 🤝");
            partiesNulles++;
        }
        updateUI();

        if (partieActuelle < totalParties) {
            new Handler(Looper.getMainLooper()).postDelayed(this::nextPartie, 2000);
        } else {
            new Handler(Looper.getMainLooper()).postDelayed(this::endTournament, 2000);
        }
    }

    private void nextPartie() {
        partieActuelle++;
        resetBoard();
        updateUI();
        partieTerminee = false;
    }

    private void resetBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = "";
                buttons[i][j].setText("");
            }
        }
        isPlayerXTurn = true; // X commence toujours
        updateStatus();

        // Si c'est au tour du bot (PvE et Bot est X)
        if (isPvE && botSymbol.equals("X")) {
            new Handler(Looper.getMainLooper()).postDelayed(this::playBotMove, 500);
        }
    }

    private void updateUI() {
        textPartieNumero.setText("PARTIE " + partieActuelle + "/" + totalParties);
        textScoreX.setText(String.valueOf(scoreX));
        textScoreO.setText(String.valueOf(scoreO));
        textPartiesNulles.setText(String.valueOf(partiesNulles));

        // Update progress bar
        if (progressBar != null) {
            int progress = (int) ((partieActuelle / (float) totalParties) * 100);
            progressBar.setProgress(progress);
        }
    }

    private void updateStatus() {
        String currentPlayer = isPlayerXTurn ? "X" : "O";
        textStatus.setText("Tour de : " + currentPlayer);
    }

    private void endTournament() {
        String winnerMessage;
        String vainqueur;

        if (scoreX > scoreO) {
            winnerMessage = "🏆 Victoire du joueur X! 🏆";
            vainqueur = "X";
        } else if (scoreO > scoreX) {
            winnerMessage = "🏆 Victoire du joueur O! 🏆";
            vainqueur = "O";
        } else {
            winnerMessage = "🤝 Égalité parfaite! 🤝";
            vainqueur = "Égalité";
        }

        new AlertDialog.Builder(this)
                .setTitle("🎉 Résultat du Tournoi 🎉")
                .setMessage(winnerMessage + "\n\nScore X: " + scoreX + "\nScore O: " + scoreO + "\nNulles: " + partiesNulles)
                .setPositiveButton("💾 Sauvegarder", (dialog, which) -> saveTournamentResults(vainqueur))
                .setNegativeButton("🏠 Accueil", (dialog, which) -> goToHome())
                .setCancelable(false)
                .show();
    }

    private void saveTournamentResults(String finalWinner) {
        TournamentResult result = new TournamentResult(scoreX, scoreO, partiesNulles, totalParties, finalWinner);

        try {
            File file = new File(getFilesDir(), "tournament_results.ser");
            FileOutputStream fos = new FileOutputStream(file);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(result);
            oos.close();
            fos.close();
            Toast.makeText(this, "✅ Scores sauvegardés.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ Erreur sauvegarde.", Toast.LENGTH_LONG).show();
        }
        goToHome();
    }

    private void goToHome() {
        Intent intent = new Intent(this, HomeActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}