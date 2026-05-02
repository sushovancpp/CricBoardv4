package com.cricboard.model;

public class BallEvent {
    public enum Type { RUNS, WIDE, NO_BALL, WICKET }

    public Type type;
    public int runs;
    public String dismissal;

    public static BallEvent runs(int r) {
        BallEvent e = new BallEvent();
        e.type = Type.RUNS; e.runs = r; return e;
    }
    public static BallEvent wide() {
        BallEvent e = new BallEvent();
        e.type = Type.WIDE; e.runs = 0; return e;
    }
    public static BallEvent noBall(int r) {
        BallEvent e = new BallEvent();
        e.type = Type.NO_BALL; e.runs = r; return e;
    }
    public static BallEvent wicket(int r, String dismissal) {
        BallEvent e = new BallEvent();
        e.type = Type.WICKET; e.runs = r;
        e.dismissal = dismissal == null ? "Out" : dismissal;
        return e;
    }
}
