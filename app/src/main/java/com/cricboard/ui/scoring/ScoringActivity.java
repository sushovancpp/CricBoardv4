package com.cricboard.ui.scoring;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.cricboard.R;
import com.cricboard.data.MatchRepository;
import com.cricboard.data.RedisRepository;
import com.cricboard.databinding.ActivityScoringBinding;
import com.cricboard.model.*;
import com.cricboard.ui.scorecard.ScorecardActivity;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScoringActivity extends AppCompatActivity {
    private ActivityScoringBinding binding;
    private MatchRepository repo;
    private RedisRepository redis;
    private Match match;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScoringBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        repo = MatchRepository.getInstance(this);
        redis = RedisRepository.getInstance();

        String matchId = getIntent().getStringExtra("matchId");
        match = repo.getMatch(matchId);

        if (match == null) { finish(); return; }

        setupUI();
        refreshAll();

        pushToRedis();
    }

    // ─── Push to Redis (background thread) ───────────────────────────────────

    private void pushToRedis() {
        Match snapshot = match;
        executor.execute(() -> {
            boolean ok = redis.pushMatch(snapshot);
            if (!ok) {
                runOnUiThread(() ->
                        Toast.makeText(this, "⚠️ Redis sync failed — check credentials", Toast.LENGTH_SHORT).show());
            }
        });
    }

    // ─── Setup ───────────────────────────────────────────────────────────────

    private void setupUI() {
        binding.btn0.setOnClickListener(v -> scoreBall(BallEvent.runs(0)));
        binding.btn1.setOnClickListener(v -> scoreBall(BallEvent.runs(1)));
        binding.btn2.setOnClickListener(v -> scoreBall(BallEvent.runs(2)));
        binding.btn3.setOnClickListener(v -> scoreBall(BallEvent.runs(3)));
        binding.btn4.setOnClickListener(v -> scoreBall(BallEvent.runs(4)));
        binding.btn6.setOnClickListener(v -> scoreBall(BallEvent.runs(6)));
        binding.btnWide.setOnClickListener(v -> scoreBall(BallEvent.wide()));
        binding.btnNoBall.setOnClickListener(v -> showNoBallDialog());
        binding.btnWicket.setOnClickListener(v -> showWicketDialog());

        binding.btnSwapStrike.setOnClickListener(v -> swapStrike());
        binding.btnAddBatsman.setOnClickListener(v -> showAddBatsmanDialog());
        binding.btnSetBowler.setOnClickListener(v -> showSetBowlerDialog());

        binding.btnViewScorecard.setOnClickListener(v -> {
            Intent i = new Intent(this, ScorecardActivity.class);
            i.putExtra("matchId", match.id);
            startActivity(i);
        });

        binding.btnStartSecondInnings.setOnClickListener(v -> startSecondInnings());
        binding.btnEndMatch.setOnClickListener(v -> endMatch());
    }

    private void refreshAll() {
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(String.format(Locale.getDefault(), "%s vs %s", match.team1, match.team2));
            getSupportActionBar().setSubtitle(String.format(Locale.getDefault(), "Match ID: %s", match.id));
        }

        if (match.status == Match.Status.TOSS) {
            showTossScreen();
            return;
        }

        Innings inn = match.getCurrentInnings();

        if (match.status == Match.Status.INNINGS_BREAK) {
            showInningsBreakUI();
            return;
        }
        if (match.status == Match.Status.COMPLETED) {
            showCompletedUI();
            return;
        }

        // Live scoring UI
        binding.scoringLayout.setVisibility(View.VISIBLE);
        binding.tossLayout.setVisibility(View.GONE);
        binding.inningsBreakLayout.setVisibility(View.GONE);
        binding.completedLayout.setVisibility(View.GONE);

        if (inn == null) return;

        binding.tvScore.setText(String.format(Locale.getDefault(), "%d/%d", inn.runs, inn.wickets));
        binding.tvOvers.setText(String.format(Locale.getDefault(), "%d.%d ov", inn.overs, inn.balls));
        binding.tvBattingTeam.setText(String.format(Locale.getDefault(), "%s batting", inn.battingTeam));

        float crr = inn.getRunRate();
        binding.tvCrr.setText(String.format(Locale.getDefault(), "CRR: %.2f", crr));

        if (inn.target != null) {
            binding.targetCard.setVisibility(View.VISIBLE);
            int need = Math.max(0, inn.target - inn.runs);
            int ballsLeft = Math.max(0, match.maxOvers * 6 - inn.getTotalBalls());
            float rrr = inn.getRequiredRunRate(match.maxOvers);
            binding.tvTarget.setText(String.format(Locale.getDefault(),
                    "Target: %d  Need: %d off %d balls  RRR: %.2f",
                    inn.target, need, ballsLeft, rrr));
        } else {
            binding.targetCard.setVisibility(View.GONE);
        }

        updateBallLog(inn);

        BatsmanScore striker = inn.getStriker();
        BatsmanScore nonStriker = inn.getNonStriker();
        if (striker != null) {
            binding.tvStriker.setText(String.format(Locale.getDefault(), "⚡ %s", striker.name));
            binding.tvStrikerStats.setText(String.format(Locale.getDefault(),
                    "%d(%d)  4s:%d  6s:%d",
                    striker.runs, striker.balls, striker.fours, striker.sixes));
        } else {
            binding.tvStriker.setText("⚡ -");
            binding.tvStrikerStats.setText("");
        }

        if (nonStriker != null) {
            binding.tvNonStriker.setText(String.format(Locale.getDefault(), "  %s", nonStriker.name));
            binding.tvNonStrikerStats.setText(String.format(Locale.getDefault(),
                    "%d(%d)",
                    nonStriker.runs, nonStriker.balls));
        } else {
            binding.tvNonStriker.setText("  -");
            binding.tvNonStrikerStats.setText("");
        }

        BowlerScore bowler = inn.getCurrentBowler();
        boolean needsBowler = inn.currentBowlerIndex == -1;
        if (bowler != null) {
            binding.tvBowler.setText(String.format(Locale.getDefault(), "🏏 %s", bowler.name));
            // ✅ FIX: getOversBowled() returns String, so use %s not %d
            binding.tvBowlerStats.setText(String.format(Locale.getDefault(),
                    "%s ov  %dR  %dW",
                    bowler.getOversBowled(), bowler.runs, bowler.wickets));
        } else {
            binding.tvBowler.setText("🏏 Set Bowler");
            binding.tvBowlerStats.setText("");
        }

        boolean canScore = !needsBowler && (striker != null) && (nonStriker != null || inn.wickets == 9);
        binding.scoringButtonsLayout.setVisibility(canScore ? View.VISIBLE : View.GONE);
        binding.btnSetBowler.setVisibility(needsBowler ? View.VISIBLE : View.GONE);
        binding.btnAddBatsman.setVisibility(striker == null ? View.VISIBLE : View.GONE);

        boolean needBatsman = inn.wickets < 10 && inn.getStriker() == null && !inn.isComplete;
        if (needBatsman) {
            binding.btnAddBatsman.setVisibility(View.VISIBLE);
            binding.scoringButtonsLayout.setVisibility(View.GONE);
        }
    }

    private void updateBallLog(Innings inn) {
        List<List<String>> overs = CricketEngine.splitIntoOvers(inn.ballLog);
        binding.ballLogContainer.removeAllViews();

        int displayFrom = Math.max(0, overs.size() - 2);
        for (int oi = displayFrom; oi < overs.size(); oi++) {
            List<String> overBalls = overs.get(oi);
            TextView label = new TextView(this);
            label.setText(String.format(Locale.getDefault(), "O%d: ", (oi + 1)));
            label.setTextColor(Color.parseColor("#9E9E9E"));
            label.setTextSize(12);

            StringBuilder sb = new StringBuilder();
            for (String ball : overBalls) sb.append(formatBall(ball)).append("  ");

            TextView ballsView = new TextView(this);
            ballsView.setText(sb.toString());
            ballsView.setTextSize(14);
            ballsView.setTypeface(android.graphics.Typeface.MONOSPACE);

            android.widget.LinearLayout row = new android.widget.LinearLayout(this);
            row.setOrientation(android.widget.LinearLayout.HORIZONTAL);
            row.addView(label);
            row.addView(ballsView);
            binding.ballLogContainer.addView(row);
        }
    }

    private String formatBall(String ball) {
        switch (ball) {
            case "W":  return "🔴W";
            case "4":  return "🔵4";
            case "6":  return "🟣6";
            case "wd": return "🟡wd";
            case "nb": return "🟠nb";
            case "•":  return "·";
            default:
                if (ball.endsWith("W"))    return "🔴" + ball;
                if (ball.startsWith("nb")) return "🟠" + ball;
                return ball;
        }
    }

    // ─── Toss ─────────────────────────────────────────────────────────────────

    private void showTossScreen() {
        binding.tossLayout.setVisibility(View.VISIBLE);
        binding.scoringLayout.setVisibility(View.GONE);
        binding.inningsBreakLayout.setVisibility(View.GONE);
        binding.completedLayout.setVisibility(View.GONE);

        final String[] tossWinner = {match.team1};
        final String[] tossChoice = {"bat"};

        binding.rgTossWinner.setOnCheckedChangeListener((g, id) ->
                tossWinner[0] = id == R.id.rbTeam1 ? match.team1 : match.team2);

        binding.rgTossChoice.setOnCheckedChangeListener((g, id) ->
                tossChoice[0] = id == R.id.rbBat ? "bat" : "bowl");

        binding.rbTeam1.setText(match.team1);
        binding.rbTeam2.setText(match.team2);
        binding.rbTeam1.setChecked(true);

        binding.btnStartMatch.setOnClickListener(v -> {
            match.tossWinner = tossWinner[0];
            match.tossChoice = tossChoice[0];

            String battingTeam, bowlingTeam;
            if (tossChoice[0].equals("bat")) {
                battingTeam = tossWinner[0];
                bowlingTeam = tossWinner[0].equals(match.team1) ? match.team2 : match.team1;
            } else {
                bowlingTeam = tossWinner[0];
                battingTeam = tossWinner[0].equals(match.team1) ? match.team2 : match.team1;
            }

            match.innings[0] = new Innings(battingTeam, bowlingTeam);
            match.currentInnings = 0;
            match.status = Match.Status.LIVE;
            repo.saveMatch(match);
            pushToRedis();

            showOpenersDialog(match.innings[0]);
        });
    }

    private void showOpenersDialog(Innings inn) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_openers, null);
        android.widget.EditText etOpener1 = dialogView.findViewById(R.id.etOpener1);
        android.widget.EditText etOpener2 = dialogView.findViewById(R.id.etOpener2);
        android.widget.EditText etBowler  = dialogView.findViewById(R.id.etBowler);

        new AlertDialog.Builder(this)
                .setTitle("1st Innings Openers")
                .setView(dialogView)
                .setCancelable(false)
                .setPositiveButton("Start", (d, w) -> {
                    String op1 = etOpener1.getText().toString().trim();
                    String op2 = etOpener2.getText().toString().trim();
                    String bowler = etBowler.getText().toString().trim();
                    if (op1.isEmpty()) op1 = "Batsman 1";
                    if (op2.isEmpty()) op2 = "Batsman 2";
                    if (bowler.isEmpty()) bowler = "Bowler 1";

                    CricketEngine.addBatsman(inn, op1, true);
                    CricketEngine.addBatsman(inn, op2, false);
                    CricketEngine.setBowler(inn, bowler);
                    repo.saveMatch(match);
                    pushToRedis();
                    refreshAll();
                })
                .show();
    }

    // ─── Scoring ──────────────────────────────────────────────────────────────

    private void scoreBall(BallEvent event) {
        Innings inn = match.getCurrentInnings();
        if (inn == null || inn.isComplete) return;

        CricketEngine.applyBallEvent(inn, event, match.maxOvers);
        repo.saveMatch(match);

        if (inn.isComplete) {
            if (match.currentInnings == 0) {
                match.status = Match.Status.INNINGS_BREAK;
            } else {
                match.status = Match.Status.COMPLETED;
                match.result = CricketEngine.computeResult(match);
            }
            repo.saveMatch(match);
        }

        pushToRedis();

        if (inn.getStriker() == null && inn.wickets < 10 && !inn.isComplete) {
            showAddBatsmanDialog();
        }

        refreshAll();
    }

    private void showNoBallDialog() {
        String[] opts = {"No Ball (0)", "No Ball + 1", "No Ball + 2", "No Ball + 4", "No Ball + 6"};
        int[] runs = {0, 1, 2, 4, 6};
        new AlertDialog.Builder(this)
                .setTitle("No Ball")
                .setItems(opts, (d, which) -> scoreBall(BallEvent.noBall(runs[which])))
                .show();
    }

    private void showWicketDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_wicket, null);
        android.widget.EditText etDismissal = v.findViewById(R.id.etDismissal);
        android.widget.EditText etRuns = v.findViewById(R.id.etRunsBeforeWicket);

        new AlertDialog.Builder(this)
                .setTitle("Wicket!")
                .setView(v)
                .setPositiveButton("OUT!", (d, w) -> {
                    String dismissal = etDismissal.getText().toString().trim();
                    if (dismissal.isEmpty()) dismissal = "Out";
                    int runs = 0;
                    try { runs = Integer.parseInt(etRuns.getText().toString().trim()); } catch (Exception ignored) {}
                    scoreBall(BallEvent.wicket(runs, dismissal));
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAddBatsmanDialog() {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Batsman name");
        int batNum = match.getCurrentInnings().batsmen.size() + 1;
        et.setText(String.format(Locale.getDefault(), "Batsman %d", batNum));
        et.selectAll();

        new AlertDialog.Builder(this)
                .setTitle("New Batsman (Striker)")
                .setView(et)
                .setCancelable(false)
                .setPositiveButton("Add", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.isEmpty()) name = String.format(Locale.getDefault(), "Batsman %d", batNum);
                    CricketEngine.addBatsman(match.getCurrentInnings(), name, true);
                    repo.saveMatch(match);
                    pushToRedis();
                    refreshAll();
                })
                .show();
    }

    private void showSetBowlerDialog() {
        android.widget.EditText et = new android.widget.EditText(this);
        et.setHint("Bowler name");

        new AlertDialog.Builder(this)
                .setTitle("Set Bowler for This Over")
                .setView(et)
                .setCancelable(false)
                .setPositiveButton("Set", (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (!name.isEmpty()) {
                        CricketEngine.setBowler(match.getCurrentInnings(), name);
                        repo.saveMatch(match);
                        pushToRedis();
                        refreshAll();
                    }
                })
                .show();
    }

    private void swapStrike() {
        Innings inn = match.getCurrentInnings();
        if (inn == null) return;
        BatsmanScore striker = inn.getStriker();
        BatsmanScore nonStriker = inn.getNonStriker();
        if (striker != null && nonStriker != null) {
            striker.onStrike = false;
            nonStriker.onStrike = true;
            repo.saveMatch(match);
            pushToRedis();
            refreshAll();
        }
    }

    // ─── Innings Break ────────────────────────────────────────────────────────

    private void showInningsBreakUI() {
        binding.inningsBreakLayout.setVisibility(View.VISIBLE);
        binding.scoringLayout.setVisibility(View.GONE);
        binding.tossLayout.setVisibility(View.GONE);
        binding.completedLayout.setVisibility(View.GONE);

        Innings inn1 = match.innings[0];

        binding.tvInnBreakScore.setText(String.format(Locale.getDefault(),
                "%s: %d/%d (%d.%d ov)",
                inn1.battingTeam, inn1.runs, inn1.wickets, inn1.overs, inn1.balls));

        binding.tvInnBreakTarget.setText(String.format(Locale.getDefault(), "Target: %d", (inn1.runs + 1)));
    }

    private void startSecondInnings() {
        Innings inn1 = match.innings[0];
        String battingTeam = inn1.bowlingTeam;
        String bowlingTeam = inn1.battingTeam;
        int target = inn1.runs + 1;

        match.innings[1] = new Innings(battingTeam, bowlingTeam, target);
        match.currentInnings = 1;
        match.status = Match.Status.LIVE;
        repo.saveMatch(match);
        pushToRedis();
        showOpenersDialog(match.innings[1]);
        refreshAll();
    }

    // ─── Completed ────────────────────────────────────────────────────────────

    private void showCompletedUI() {
        binding.completedLayout.setVisibility(View.VISIBLE);
        binding.scoringLayout.setVisibility(View.GONE);
        binding.tossLayout.setVisibility(View.GONE);
        binding.inningsBreakLayout.setVisibility(View.GONE);

        binding.tvResult.setText(match.result);
        Innings inn1 = match.innings[0];
        Innings inn2 = match.innings[1];

        if (inn1 != null) {
            binding.tvFinalScore1.setText(String.format(Locale.getDefault(),
                    "%s: %d/%d", inn1.battingTeam, inn1.runs, inn1.wickets));
        }
        if (inn2 != null) {
            binding.tvFinalScore2.setText(String.format(Locale.getDefault(),
                    "%s: %d/%d", inn2.battingTeam, inn2.runs, inn2.wickets));
        }

        binding.btnViewScorecardCompleted.setOnClickListener(v -> {
            Intent i = new Intent(this, ScorecardActivity.class);
            i.putExtra("matchId", match.id);
            startActivity(i);
        });
    }

    private void endMatch() {
        new AlertDialog.Builder(this)
                .setTitle("End Match")
                .setMessage("Mark this match as completed?")
                .setPositiveButton("End", (d, w) -> {
                    match.status = Match.Status.COMPLETED;
                    match.result = CricketEngine.computeResult(match);
                    if (match.result.isEmpty()) match.result = "Match ended";
                    repo.saveMatch(match);
                    pushToRedis();
                    refreshAll();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }
}