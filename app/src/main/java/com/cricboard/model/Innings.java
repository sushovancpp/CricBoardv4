package com.cricboard.model;

import java.util.ArrayList;
import java.util.List;

public class Innings {
    public String battingTeam;
    public String bowlingTeam;
    public int runs;
    public int wickets;
    public int overs;
    public int balls;
    public int extraWides;
    public int extraNoBalls;
    public int extraByes;
    public int extraLegByes;
    public List<BatsmanScore> batsmen = new ArrayList<>();
    public List<BowlerScore> bowlers = new ArrayList<>();
    public int currentBowlerIndex = -1;
    public List<String> ballLog = new ArrayList<>();
    public boolean isComplete;
    public Integer target;

    /**
     * After a wicket, this flag tells us whether the incoming batsman
     * should be on strike (true = even/zero runs before wicket)
     * or off strike (false = odd runs before wicket, non-striker crossed).
     */
    public boolean pendingNewBatsmanIsStriker = true;

    public Innings(String battingTeam, String bowlingTeam) {
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
    }

    public Innings(String battingTeam, String bowlingTeam, int target) {
        this(battingTeam, bowlingTeam);
        this.target = target;
    }

    public BatsmanScore getStriker() {
        for (BatsmanScore b : batsmen)
            if (b.onStrike && !b.isOut) return b;
        return null;
    }

    public BatsmanScore getNonStriker() {
        for (BatsmanScore b : batsmen)
            if (!b.onStrike && !b.isOut) return b;
        return null;
    }

    public BowlerScore getCurrentBowler() {
        if (currentBowlerIndex >= 0 && currentBowlerIndex < bowlers.size())
            return bowlers.get(currentBowlerIndex);
        return null;
    }

    public int getTotalExtras() {
        return extraWides + extraNoBalls + extraByes + extraLegByes;
    }

    public int getTotalBalls() {
        return overs * 6 + balls;
    }

    public float getRunRate() {
        int total = getTotalBalls();
        if (total == 0) return 0f;
        return (runs * 6f) / total;
    }

    public float getRequiredRunRate(int maxOvers) {
        if (target == null) return 0f;
        int ballsRemaining = maxOvers * 6 - getTotalBalls();
        if (ballsRemaining <= 0) return Float.MAX_VALUE;
        int needed = target - runs;
        if (needed <= 0) return 0f;
        return (needed * 6f) / ballsRemaining;
    }
}