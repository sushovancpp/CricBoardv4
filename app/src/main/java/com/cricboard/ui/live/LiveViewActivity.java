package com.cricboard.ui.live;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cricboard.Config;
import com.cricboard.R;
import com.cricboard.data.RedisRepository;
import com.cricboard.databinding.ActivityLiveViewBinding;
import com.cricboard.model.BatsmanScore;
import com.cricboard.model.BowlerScore;
import com.cricboard.model.CricketEngine;
import com.cricboard.model.Innings;
import com.cricboard.model.Match;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LiveViewActivity extends AppCompatActivity {

    private ActivityLiveViewBinding binding;
    private RedisRepository redis;
    private String matchId;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isPolling = false;
    private Match lastMatch = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLiveViewBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("📺 Live Score");

        redis = RedisRepository.getInstance();
        matchId = getIntent().getStringExtra("matchId");

        if (matchId == null || matchId.isEmpty()) {
            Toast.makeText(this, "Invalid match ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.tvMatchId.setText("Match ID: " + matchId);
        binding.tvStatus.setText("Connecting...");
    }

    @Override
    protected void onResume() {
        super.onResume();
        startPolling();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopPolling();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        executor.shutdownNow();
    }

    // ─── Polling ──────────────────────────────────────────────────────────────

    private void startPolling() {
        isPolling = true;
        scheduleNextFetch();
    }

    private void stopPolling() {
        isPolling = false;
        handler.removeCallbacksAndMessages(null);
    }

    private void scheduleNextFetch() {
        if (!isPolling) return;
        executor.execute(() -> {
            Match match = redis.pullMatch(matchId);
            handler.post(() -> {
                if (!isPolling) return;
                if (match == null) {
                    binding.tvStatus.setText("⚠️ Match not found — waiting...");
                    binding.tvStatus.setTextColor(Color.parseColor("#FFB300"));
                } else {
                    updateUI(match);
                }
                // Schedule next poll
                handler.postDelayed(this::scheduleNextFetch, Config.REFRESH_INTERVAL_MS);
            });
        });
    }

    // ─── UI Update ────────────────────────────────────────────────────────────

    private void updateUI(Match match) {
        lastMatch = match;
        getSupportActionBar().setTitle(match.team1 + " vs " + match.team2);

        switch (match.status) {
            case TOSS:
                showTossScreen(match);
                break;
            case LIVE:
                showLiveScreen(match);
                break;
            case INNINGS_BREAK:
                showInningsBreakScreen(match);
                break;
            case COMPLETED:
                showCompletedScreen(match);
                break;
            default:
                binding.tvStatus.setText("Waiting for match to start...");
        }
    }

    private void showTossScreen(Match match) {
        binding.liveLayout.setVisibility(View.GONE);
        binding.completedLayout.setVisibility(View.GONE);

        binding.tvStatus.setText("🪙 Toss in progress...");
        binding.tvStatus.setTextColor(Color.parseColor("#FFD700"));
        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.tvRefreshing.setVisibility(View.VISIBLE);
    }

    private void showLiveScreen(Match match) {
        binding.tvStatus.setVisibility(View.GONE);
        binding.completedLayout.setVisibility(View.GONE);
        binding.liveLayout.setVisibility(View.VISIBLE);
        binding.tvRefreshing.setVisibility(View.VISIBLE);

        Innings inn = match.getCurrentInnings();
        if (inn == null) return;

        // Score
        binding.tvLiveBattingTeam.setText(inn.battingTeam + " batting");
        binding.tvLiveScore.setText(inn.runs + "/" + inn.wickets);
        binding.tvLiveOvers.setText(inn.overs + "." + inn.balls + " ov");

        float crr = inn.getRunRate();
        binding.tvLiveCrr.setText(String.format("CRR: %.2f", crr));

        // Innings label
        String innLabel = (match.currentInnings == 0) ? "1st Innings" : "2nd Innings";
        binding.tvInningsLabel.setText(innLabel);

        // Target
        if (inn.target != null) {
            binding.targetCard.setVisibility(View.VISIBLE);
            int need = Math.max(0, inn.target - inn.runs);
            int ballsLeft = Math.max(0, match.maxOvers * 6 - inn.getTotalBalls());
            float rrr = inn.getRequiredRunRate(match.maxOvers);
            binding.tvLiveTarget.setText(
                "Target: " + inn.target +
                "  Need: " + need + " off " + ballsLeft + " balls" +
                "  RRR: " + String.format("%.2f", rrr));
        } else {
            binding.targetCard.setVisibility(View.GONE);
        }

        // Batsmen
        BatsmanScore striker = inn.getStriker();
        BatsmanScore nonStriker = inn.getNonStriker();
        if (striker != null) {
            binding.tvLiveStriker.setText("⚡ " + striker.name);
            binding.tvLiveStrikerStats.setText(
                striker.runs + "(" + striker.balls + ")  4s:" + striker.fours + "  6s:" + striker.sixes);
        } else {
            binding.tvLiveStriker.setText("⚡ —");
            binding.tvLiveStrikerStats.setText("");
        }
        if (nonStriker != null) {
            binding.tvLiveNonStriker.setText("  " + nonStriker.name);
            binding.tvLiveNonStrikerStats.setText(nonStriker.runs + "(" + nonStriker.balls + ")");
        } else {
            binding.tvLiveNonStriker.setText("  —");
            binding.tvLiveNonStrikerStats.setText("");
        }

        // Bowler
        BowlerScore bowler = inn.getCurrentBowler();
        if (bowler != null) {
            binding.tvLiveBowler.setText("🎳 " + bowler.name);
            binding.tvLiveBowlerStats.setText(
                bowler.getOversBowled() + " ov  " + bowler.runs + "R  " + bowler.wickets + "W");
        } else {
            binding.tvLiveBowler.setText("🎳 —");
            binding.tvLiveBowlerStats.setText("");
        }

        // Ball log
        updateBallLog(inn);
    }

    private void showInningsBreakScreen(Match match) {
        binding.tvStatus.setVisibility(View.VISIBLE);
        binding.liveLayout.setVisibility(View.GONE);
        binding.completedLayout.setVisibility(View.GONE);
        binding.tvRefreshing.setVisibility(View.VISIBLE);

        Innings inn1 = match.innings[0];
        binding.tvStatus.setTextColor(Color.parseColor("#FFB300"));
        binding.tvStatus.setText(
            "⏸ INNINGS BREAK\n\n" +
            inn1.battingTeam + ": " + inn1.runs + "/" + inn1.wickets +
            " (" + inn1.overs + "." + inn1.balls + " ov)\n" +
            "Target: " + (inn1.runs + 1) + "\n\n" +
            "2nd innings starting soon...");
    }

    private void showCompletedScreen(Match match) {
        binding.tvStatus.setVisibility(View.GONE);
        binding.liveLayout.setVisibility(View.GONE);
        binding.tvRefreshing.setVisibility(View.GONE);
        binding.completedLayout.setVisibility(View.VISIBLE);

        binding.tvLiveResult.setText(match.result);

        Innings inn1 = match.innings[0];
        Innings inn2 = match.innings[1];
        if (inn1 != null)
            binding.tvLiveFinalScore1.setText(inn1.battingTeam + ": " + inn1.runs + "/" + inn1.wickets +
                " (" + inn1.overs + "." + inn1.balls + " ov)");
        if (inn2 != null)
            binding.tvLiveFinalScore2.setText(inn2.battingTeam + ": " + inn2.runs + "/" + inn2.wickets +
                " (" + inn2.overs + "." + inn2.balls + " ov)");

        // Stop polling — match is done
        stopPolling();
        binding.tvRefreshing.setText("✅ Final result");
    }

    private void updateBallLog(Innings inn) {
        List<List<String>> overs = CricketEngine.splitIntoOvers(inn.ballLog);
        binding.ballLogContainer.removeAllViews();

        int displayFrom = Math.max(0, overs.size() - 2);
        for (int oi = displayFrom; oi < overs.size(); oi++) {
            List<String> overBalls = overs.get(oi);

            TextView label = new TextView(this);
            label.setText("O" + (oi + 1) + ": ");
            label.setTextColor(Color.parseColor("#9E9E9E"));
            label.setTextSize(12);

            StringBuilder sb = new StringBuilder();
            for (String ball : overBalls) sb.append(formatBall(ball)).append("  ");

            TextView ballsView = new TextView(this);
            ballsView.setText(sb.toString());
            ballsView.setTextSize(14);
            ballsView.setTypeface(android.graphics.Typeface.MONOSPACE);
            ballsView.setTextColor(Color.WHITE);

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
                if (ball.endsWith("W"))   return "🔴" + ball;
                if (ball.startsWith("nb")) return "🟠" + ball;
                return ball;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
