package com.cricboard.model;

public class BowlerScore {
    public String name;
    public int overs;
    public int balls;
    public int runs;
    public int wickets;
    public int wides;
    public int noBalls;
    public int maidens;

    public BowlerScore(String name) {
        this.name = name;
    }

    public float getEconomy() {
        int totalBalls = overs * 6 + balls;
        if (totalBalls == 0) return 0f;
        return (runs * 6f) / totalBalls;
    }

    public String getOversBowled() {
        return overs + "." + balls;
    }
}
