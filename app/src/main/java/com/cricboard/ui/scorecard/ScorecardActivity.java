package com.cricboard.ui.scorecard;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.cricboard.R;
import com.cricboard.data.MatchRepository;
import com.cricboard.databinding.ActivityScorecardBinding;
import com.cricboard.model.*;

public class ScorecardActivity extends AppCompatActivity {
    private ActivityScorecardBinding binding;
    private Match match;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScorecardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        match = MatchRepository.getInstance(this).getMatch(getIntent().getStringExtra("matchId"));
        if (match == null) { finish(); return; }

        getSupportActionBar().setTitle("Scorecard");
        binding.tvMatchTitle.setText(match.title);
        binding.tvOvers.setText(match.maxOvers + " overs  |  " + formatStatus(match.status));

        if (match.result != null) {
            binding.tvResult.setVisibility(View.VISIBLE);
            binding.tvResult.setText("🏆 " + match.result);
        }

        if (match.venue != null && !match.venue.isEmpty()) {
            binding.tvVenue.setVisibility(View.VISIBLE);
            binding.tvVenue.setText("📍 " + match.venue);
        }

        if (match.tossWinner != null) {
            binding.tvToss.setText("Toss: " + match.tossWinner + " elected to " + match.tossChoice);
            binding.tvToss.setVisibility(View.VISIBLE);
        }

        // Render innings
        Innings inn1 = match.innings[0];
        Innings inn2 = match.innings[1];

        if (inn1 != null) renderInnings(inn1, binding.innings1Container, 1);
        if (inn2 != null) renderInnings(inn2, binding.innings2Container, 2);
    }

    private void renderInnings(Innings inn, android.widget.LinearLayout container, int innNum) {
        container.setVisibility(View.VISIBLE);

        // Header
        TextView tvHeader = container.findViewWithTag("header");
        if (tvHeader == null) {
            tvHeader = new TextView(this);
            tvHeader.setTag("header");
            container.addView(tvHeader, 0);
        }
        tvHeader.setText("Innings " + innNum + " — " + inn.battingTeam);
        tvHeader.setTextSize(16);
        tvHeader.setTextColor(Color.WHITE);
        tvHeader.setPadding(0, 24, 0, 12);

        // Score line
        addRow(container, inn.battingTeam + ": " + inn.runs + "/" + inn.wickets +
            "  (" + inn.overs + "." + inn.balls + " ov)  CRR: " +
            String.format("%.2f", inn.getRunRate()), true, Color.parseColor("#4CAF50"));

        if (inn.target != null) {
            addRow(container, "Target: " + inn.target, false, Color.parseColor("#FFB300"));
        }

        // Extras
        addRow(container, "Extras: " + inn.getTotalExtras() +
            "  (wd " + inn.extraWides + ", nb " + inn.extraNoBalls + ")", false, Color.parseColor("#9E9E9E"));

        // Batsmen table header
        addRow(container, "BATSMAN               R    B   4s  6s   SR", false, Color.parseColor("#757575"));

        for (BatsmanScore b : inn.batsmen) {
            String dismissal = b.isOut ? b.dismissal : (b.onStrike ? "batting*" : "not out");
            String line = padRight(b.name, 20) + " " +
                padLeft(String.valueOf(b.runs), 4) + " " +
                padLeft(String.valueOf(b.balls), 4) + " " +
                padLeft(String.valueOf(b.fours), 4) + " " +
                padLeft(String.valueOf(b.sixes), 3) + " " +
                padLeft(String.format("%.1f", b.getStrikeRate()), 6);
            addRow(container, line + "\n  " + dismissal, false,
                b.isOut ? Color.parseColor("#BDBDBD") : Color.WHITE);
        }

        // Bowlers table header
        addSpacer(container);
        addRow(container, "BOWLER          O    R    W   Econ", false, Color.parseColor("#757575"));

        for (BowlerScore bw : inn.bowlers) {
            String line = padRight(bw.name, 16) + " " +
                padLeft(bw.getOversBowled(), 4) + " " +
                padLeft(String.valueOf(bw.runs), 4) + " " +
                padLeft(String.valueOf(bw.wickets), 4) + " " +
                padLeft(String.format("%.2f", bw.getEconomy()), 6);
            addRow(container, line, false, Color.WHITE);
        }

        // Ball by ball
        addSpacer(container);
        java.util.List<java.util.List<String>> overs = CricketEngine.splitIntoOvers(inn.ballLog);
        addRow(container, "BALL BY BALL", false, Color.parseColor("#757575"));
        for (int i = 0; i < overs.size(); i++) {
            StringBuilder sb = new StringBuilder("O" + (i + 1) + ":  ");
            for (String ball : overs.get(i)) sb.append(ball).append("  ");
            addRow(container, sb.toString(), false, Color.parseColor("#BDBDBD"));
        }
    }

    private void addRow(android.widget.LinearLayout container, String text, boolean bold, int color) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(color);
        tv.setTextSize(12);
        tv.setTypeface(android.graphics.Typeface.MONOSPACE);
        tv.setPadding(0, 4, 0, 4);
        if (bold) tv.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD));
        container.addView(tv);
    }

    private void addSpacer(android.widget.LinearLayout container) {
        View v = new View(this);
        android.widget.LinearLayout.LayoutParams p = new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1);
        p.topMargin = 12; p.bottomMargin = 12;
        v.setLayoutParams(p);
        v.setBackgroundColor(Color.parseColor("#333333"));
        container.addView(v);
    }

    private String padRight(String s, int n) {
        if (s.length() >= n) return s.substring(0, n);
        return s + " ".repeat(n - s.length());
    }
    private String padLeft(String s, int n) {
        if (s.length() >= n) return s;
        return " ".repeat(n - s.length()) + s;
    }

    private String formatStatus(Match.Status s) {
        switch (s) {
            case LIVE: return "LIVE";
            case COMPLETED: return "COMPLETED";
            case INNINGS_BREAK: return "INNINGS BREAK";
            default: return s.name();
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
