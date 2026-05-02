package com.cricboard.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure cricket logic engine — no Android dependencies.
 */
public class CricketEngine {

    public static void applyBallEvent(Innings inn, BallEvent event, int maxOvers) {
        BatsmanScore striker = inn.getStriker();
        BatsmanScore nonStriker = inn.getNonStriker();
        BowlerScore bowler = inn.getCurrentBowler();

        if (striker == null || bowler == null) return;

        switch (event.type) {
            case RUNS: {
                int r = event.runs;
                inn.runs += r;
                striker.runs += r;
                striker.balls += 1;
                if (r == 4) striker.fours++;
                if (r == 6) striker.sixes++;
                bowler.runs += r;
                inn.ballLog.add(r == 0 ? "•" : String.valueOf(r));

                // Odd runs → rotate strike
                if (r % 2 != 0 && nonStriker != null) {
                    striker.onStrike = false;
                    nonStriker.onStrike = true;
                }

                inn.balls++;
                bowler.balls++;
                checkEndOver(inn, bowler, maxOvers, false);
                break;
            }
            case WIDE: {
                inn.runs += 1;
                inn.extraWides++;
                bowler.runs += 1;
                bowler.wides++;
                inn.ballLog.add("wd");
                // Wide does NOT count as a ball faced
                break;
            }
            case NO_BALL: {
                int r = event.runs;
                inn.runs += 1 + r;
                inn.extraNoBalls++;
                if (r > 0) {
                    striker.runs += r;
                    if (r == 4) striker.fours++;
                    if (r == 6) striker.sixes++;
                }
                bowler.runs += 1 + r;
                bowler.noBalls++;
                inn.ballLog.add(r > 0 ? "nb+" + r : "nb");
                // Odd off-bat runs rotate strike
                if (r % 2 != 0 && nonStriker != null) {
                    striker.onStrike = false;
                    nonStriker.onStrike = true;
                }
                break;
            }
            case WICKET: {
                int r = event.runs;

                // 1. Apply any runs scored before the wicket
                if (r > 0) {
                    inn.runs += r;
                    striker.runs += r;
                    if (r == 4) striker.fours++;
                    if (r == 6) striker.sixes++;
                    bowler.runs += r;
                }

                // 2. Count the ball
                striker.balls++;
                bowler.balls++;
                inn.balls++;
                inn.wickets++;

                // 3. Dismiss the striker — clear strike BEFORE rotating
                striker.isOut = true;
                striker.onStrike = false;
                striker.dismissal = event.dismissal != null ? event.dismissal : "Out";

                // 4. If odd runs were scored, non-striker crossed — they become the striker
                //    The NEW batsman will come in at the non-striker end (onStrike = false)
                //    If even/zero runs, new batsman comes in at striker end (onStrike = true)
                boolean newBatsmanIsStriker = (r % 2 == 0);

                // Store this decision in innings so ScoringActivity can use it
                inn.pendingNewBatsmanIsStriker = newBatsmanIsStriker;

                // 5. If odd runs, non-striker crossed and is now the striker
                if (!newBatsmanIsStriker && nonStriker != null) {
                    nonStriker.onStrike = true;
                }

                bowler.wickets++;
                inn.ballLog.add(r > 0 ? r + "W" : "W");

                // 6. Check end of over
                checkEndOver(inn, bowler, maxOvers, true);
                break;
            }
        }

        // Check completion
        if (inn.wickets >= 10) {
            inn.isComplete = true;
        } else if (inn.getTotalBalls() >= maxOvers * 6) {
            inn.isComplete = true;
        } else if (inn.target != null && inn.runs >= inn.target) {
            inn.isComplete = true;
        }
    }

    private static void checkEndOver(Innings inn, BowlerScore bowler, int maxOvers, boolean wasWicket) {
        if (inn.balls == 6) {
            inn.overs++;
            inn.balls = 0;
            bowler.overs++;
            bowler.balls = 0;

            if (!wasWicket) {
                // Normal end of over: rotate strike between current batsmen
                BatsmanScore currentStriker = inn.getStriker();
                BatsmanScore currentNonStriker = inn.getNonStriker();
                if (currentStriker != null && currentNonStriker != null) {
                    currentStriker.onStrike = false;
                    currentNonStriker.onStrike = true;
                }
            }
            // If wasWicket: strike is already correctly set in the WICKET case above.
            // Don't touch it here — the new batsman position is handled by pendingNewBatsmanIsStriker.

            inn.currentBowlerIndex = -1; // needs new bowler next over
        }
    }

    public static void addBatsman(Innings inn, String name, boolean onStrike) {
        BatsmanScore b = new BatsmanScore(name);
        b.onStrike = onStrike;
        inn.batsmen.add(b);
    }

    /**
     * Call this after a wicket to add the new batsman with the correct strike end.
     * Uses inn.pendingNewBatsmanIsStriker set during wicket processing.
     */
    public static void addBatsmanAfterWicket(Innings inn, String name) {
        boolean isStriker = inn.pendingNewBatsmanIsStriker;
        addBatsman(inn, name, isStriker);
        inn.pendingNewBatsmanIsStriker = true; // reset to default
    }

    public static void setBowler(Innings inn, String name) {
        // Reuse existing bowler if found
        for (int i = 0; i < inn.bowlers.size(); i++) {
            if (inn.bowlers.get(i).name.equalsIgnoreCase(name)) {
                inn.currentBowlerIndex = i;
                return;
            }
        }
        BowlerScore bowler = new BowlerScore(name);
        inn.bowlers.add(bowler);
        inn.currentBowlerIndex = inn.bowlers.size() - 1;
    }

    public static String computeResult(Match match) {
        Innings inn1 = match.innings[0];
        Innings inn2 = match.innings[1];
        if (inn1 == null || inn2 == null) return "";

        if (inn1.runs == inn2.runs) return "Match Tied";

        if (inn2.runs >= inn1.runs + 1) {
            int wicketsLeft = 10 - inn2.wickets;
            return inn2.battingTeam + " won by " + wicketsLeft + " wicket" + (wicketsLeft != 1 ? "s" : "");
        } else {
            int runDiff = inn1.runs - inn2.runs;
            return inn1.battingTeam + " won by " + runDiff + " run" + (runDiff != 1 ? "s" : "");
        }
    }

    /** Parse ballLog entries into over-grouped lists */
    public static List<List<String>> splitIntoOvers(List<String> ballLog) {
        List<List<String>> overs = new ArrayList<>();
        List<String> current = new ArrayList<>();
        for (String ball : ballLog) {
            current.add(ball);
            boolean isExtra = ball.equals("wd") || ball.startsWith("nb");
            if (!isExtra) {
                long legalBalls = current.stream()
                        .filter(b -> !b.equals("wd") && !b.startsWith("nb"))
                        .count();
                if (legalBalls == 6) {
                    overs.add(new ArrayList<>(current));
                    current.clear();
                }
            }
        }
        if (!current.isEmpty()) overs.add(current);
        return overs;
    }

    public static String ballLogDisplay(String ball) {
        return ball;
    }
}