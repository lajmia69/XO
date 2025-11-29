package com.example.xo;

import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class HomeActivity extends AppCompatActivity {

    private static final String FILENAME = "tournament_results.ser";
    private Spinner partySelector;
    private RadioGroup radioGroupMode;
    private LinearLayout layoutDifficulty;
    private RadioGroup radioGroupDifficulty;
    private RadioGroup radioGroupSymbol;
    
    // Make mediaPlayer public static so GameActivity can access it
    public static MediaPlayer mediaPlayer;
    private boolean isMusicPlaying = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Allow volume keys to control music volume
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Start background music if not already playing
        if (mediaPlayer == null) {
            int musicResId = getResources().getIdentifier("peter", "raw", getPackageName());
            
            if (musicResId != 0) {
                 mediaPlayer = MediaPlayer.create(this, musicResId);
                 if (mediaPlayer != null) {
                     // Force device system volume to max for music
                     AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                     if (audioManager != null) {
                         int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                         audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, 0);
                     }

                     mediaPlayer.setLooping(true);
                     mediaPlayer.setVolume(1.0f, 1.0f); // Set player volume to 100%
                     mediaPlayer.start();
                     isMusicPlaying = true;
                 }
            }
        } else {
            isMusicPlaying = mediaPlayer.isPlaying();
        }

        // Initialisation des vues
        Button btnJouer = findViewById(R.id.btn_jouer);
        Button btnPrincipe = findViewById(R.id.btn_principe);
        Button btnRetrouverScores = findViewById(R.id.btn_retrouver_scores);
        Button btnToggleMusic = findViewById(R.id.btn_toggle_music);
        partySelector = findViewById(R.id.spinner_nb_parties);

        radioGroupMode = findViewById(R.id.radio_group_mode);
        layoutDifficulty = findViewById(R.id.layout_difficulty);
        radioGroupDifficulty = findViewById(R.id.radio_group_difficulty);
        radioGroupSymbol = findViewById(R.id.radio_group_symbol);

        // Update button text based on state
        if (btnToggleMusic != null) {
            btnToggleMusic.setText(isMusicPlaying ? "Musique : ON" : "Musique : OFF");
            
            btnToggleMusic.setOnClickListener(v -> {
                if (mediaPlayer != null) {
                    if (mediaPlayer.isPlaying()) {
                        mediaPlayer.pause();
                        btnToggleMusic.setText("Musique : OFF");
                        isMusicPlaying = false;
                    } else {
                        mediaPlayer.start();
                        btnToggleMusic.setText("Musique : ON");
                        isMusicPlaying = true;
                    }
                }
            });
        }

        // Configuration du sélecteur de parties (5, 10, 15)
        Integer[] nbPartiesOptions = {5, 10, 15};
        ArrayAdapter<Integer> adapter = new ArrayAdapter<>(this,
                R.layout.spinner_item, nbPartiesOptions);
        partySelector.setAdapter(adapter);

        // Gestion de l'affichage de la difficulté
        radioGroupMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_pve) {
                layoutDifficulty.setVisibility(View.VISIBLE);
            } else {
                layoutDifficulty.setVisibility(View.GONE);
            }
        });

        btnJouer.setOnClickListener(v -> startGame());
        btnPrincipe.setOnClickListener(v -> showRules());
        btnRetrouverScores.setOnClickListener(v -> retrieveSavedScores());
    }

    /** Démarre l'activité de jeu avec le nombre de parties choisi. */
    private void startGame() {
        int totalParties = (Integer) partySelector.getSelectedItem();
        
        boolean isPvE = radioGroupMode.getCheckedRadioButtonId() == R.id.radio_pve;
        String userSymbol = ((RadioButton) findViewById(radioGroupSymbol.getCheckedRadioButtonId())).getText().toString();
        
        int difficulty = 0; // 0: Easy, 1: Medium, 2: Hard
        if (isPvE) {
            int diffId = radioGroupDifficulty.getCheckedRadioButtonId();
            if (diffId == R.id.radio_medium) difficulty = 1;
            else if (diffId == R.id.radio_hard) difficulty = 2;
        }

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra(GameActivity.KEY_TOTAL_PARTIES, totalParties);
        intent.putExtra("IS_PVE", isPvE);
        intent.putExtra("DIFFICULTY", difficulty);
        intent.putExtra("USER_SYMBOL", userSymbol);
        startActivity(intent);
    }

    /** Affiche les règles du jeu XO. */
    private void showRules() {
        new AlertDialog.Builder(this)
                .setTitle("Principe du jeu X-O")
                .setMessage("Le jeu X-O se joue sur une grille de 3x3 cases, où deux joueurs s'affrontent: X et O. À tour de rôle, chaque joueur place son symbole.\n\n" +
                        "La partie se termine si un joueur aligne trois symboles ou si toutes les cases sont remplies (partie nulle).")
                .setPositiveButton("Compris", null)
                .show();
    }

    /** Lit le fichier interne pour afficher les scores du dernier tournoi. */
    private void retrieveSavedScores() {
        File file = new File(getFilesDir(), FILENAME);

        if (!file.exists()) {
            Toast.makeText(this, "Aucun tournoi sauvegardé", Toast.LENGTH_LONG).show(); // Affiche si aucun fichier n'existe
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {

            TournamentResult result = (TournamentResult) ois.readObject();

            new AlertDialog.Builder(this)
                    .setTitle("Scores du dernier tournoi")
                    .setMessage(result.toString())
                    .setPositiveButton("OK", null)
                    .show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Erreur lors de la lecture des scores.", Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Note: We typically don't stop the music here if we want it to persist across activities,
        // but properly managing a global MediaPlayer usually involves a Service or a Singleton.
        // For this simple request, we keep it running. 
        // If the app is killed, the system will handle it.
    }
}