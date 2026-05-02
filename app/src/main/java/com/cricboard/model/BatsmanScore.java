package com.cricboard.model;

public class BatsmanScore {
    public String name;
    public int runs;
    public int balls;
    public int fours;
    public int sixes;
    public boolean isOut;
    public String dismissal;
    public boolean onStrike;

    public BatsmanScore(String name) {
        this.name = name;
    }

    public float getStrikeRate() {
        if (balls == 0) return 0f;
        return (runs * 100f) / balls;
    }
}
