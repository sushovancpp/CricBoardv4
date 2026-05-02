package com.cricboard.model;

import java.util.UUID;

public class Match {
    public enum Status { UPCOMING, TOSS, LIVE, INNINGS_BREAK, COMPLETED }

    public String id;
    public String title;
    public String team1;
    public String team2;
    public int maxOvers;
    public Status status;
    public String tossWinner;
    public String tossChoice; // "bat" or "bowl"
    public Innings[] innings = new Innings[2];
    public int currentInnings;
    public String result;
    public long createdAt;
    public long updatedAt;
    public String venue;

    public Match(String team1, String team2, int maxOvers) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.team1 = team1;
        this.team2 = team2;
        this.maxOvers = maxOvers;
        this.title = team1 + " vs " + team2;
        this.status = Status.TOSS;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Innings getCurrentInnings() {
        return innings[currentInnings];
    }
}
