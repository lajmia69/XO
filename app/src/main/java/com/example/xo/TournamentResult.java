package com.example.xo;

import java.io.Serializable;

/**
 * Classe modèle pour sauvegarder les résultats du tournoi par sérialisation[cite: 48].
 */
public class TournamentResult implements Serializable {

    // Constante requise pour la sérialisation
    private static final long serialVersionUID = 1L;

    private int scoreX;
    private int scoreO;
    private int partiesNulles;
    private int totalParties;
    private String vainqueur;

    public TournamentResult(int scoreX, int scoreO, int partiesNulles, int totalParties, String vainqueur) {
        this.scoreX = scoreX;
        this.scoreO = scoreO;
        this.partiesNulles = partiesNulles;
        this.totalParties = totalParties;
        this.vainqueur = vainqueur;
    }

    // Getters pour récupérer les informations sauvegardées [cite: 19]
    public int getScoreX() {
        return scoreX;
    }

    public int getScoreO() {
        return scoreO;
    }

    public int getPartiesNulles() {
        return partiesNulles;
    }

    public int getTotalParties() {
        return totalParties;
    }

    public String getVainqueur() {
        return vainqueur;
    }

    @Override
    public String toString() {
        return "Score X: " + scoreX + "\n" +
                "Score O: " + scoreO + "\n" +
                "Parties nulles: " + partiesNulles + "\n" +
                "Total des parties jouées: " + totalParties + "\n" +
                "Vainqueur du tournoi: " + vainqueur;
    }
}