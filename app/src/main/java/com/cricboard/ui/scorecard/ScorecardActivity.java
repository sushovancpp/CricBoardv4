package com.cricboard.ui.scorecard;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cricboard.R;
import com.cricboard.data.MatchRepository;
import com.cricboard.databinding.ActivityScorecardBinding;
import com.cricboard.model.BatsmanScore;
import com.cricboard.model.BowlerScore;
import com.cricboard.model.CricketEngine;
import com.cricboard.model.Innings;
import com.cricboard.model.InningsHeaderData;
import com.cricboard.model.Match;
import com.cricboard.model.OverData;
import com.cricboard.model.ScorecardItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ScorecardActivity extends AppCompatActivity {

    public static final String EXTRA_MATCH_ID = "matchId";

    private static final String SECTION_BATTING = "Batting";
    private static final String SECTION_BOWLING = "Bowling";
    private static final String SECTION_BALL_BY_BALL = "Ball by ball";

    private ActivityScorecardBinding binding;
    private Match match;

    // ─── Lifecycle ────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityScorecardBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();

        String matchId = getIntent().getStringExtra(EXTRA_MATCH_ID);
        match = resolveMatch(matchId);
        if (match == null) {
            finish();
            return;
        }

        bindMatchHeader();
        setupRecyclerView();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ─── Setup ────────────────────────────────────────────────────────────────

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.scorecard_title);
        }
    }

    @Nullable
    private Match resolveMatch(@Nullable String matchId) {
        if (TextUtils.isEmpty(matchId)) return null;
        return MatchRepository.getInstance(this).getMatch(matchId);
    }

    private void setupRecyclerView() {
        List<ScorecardItem> items = buildScorecardItems();
        ScorecardAdapter adapter = new ScorecardAdapter(this, items);

        RecyclerView rv = binding.recyclerView;
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);
        rv.setItemAnimator(null); // prevent flicker on updates
        rv.setHasFixedSize(true);
    }

    // ─── Header Binding ───────────────────────────────────────────────────────

    private void bindMatchHeader() {
        binding.tvMatchTitle.setText(match.title);
        binding.tvMatchOvers.setText(buildOverInfoText());

        setTextOrHide(binding.tvResult, match.result);
        setTextOrHide(binding.tvVenue, match.venue);
        bindTossInfo();
    }

    private String buildOverInfoText() {
        return match.maxOvers + " overs  ·  " + formatStatus(match.status);
    }

    private void bindTossInfo() {
        if (!TextUtils.isEmpty(match.tossWinner)) {
            String tossText = match.tossWinner + " won toss, elected to " + match.tossChoice;
            binding.tvToss.setText(tossText);
            binding.tvToss.setVisibility(View.VISIBLE);
        } else {
            binding.tvToss.setVisibility(View.GONE);
        }
    }

    // ─── Scorecard Item Builder ───────────────────────────────────────────────

    @NonNull
    private List<ScorecardItem> buildScorecardItems() {
        List<ScorecardItem> items = new ArrayList<>();
        if (match.innings == null) return items;

        for (int i = 0; i < match.innings.length; i++) {
            Innings inn = match.innings[i];
            if (inn == null) continue;

            appendInningsItems(items, inn, i + 1);

            boolean isLastInnings = (i == match.innings.length - 1);
            if (!isLastInnings) {
                items.add(ScorecardItem.spacer());
            }
        }

        return items;
    }

    private void appendInningsItems(
            @NonNull List<ScorecardItem> items,
            @NonNull Innings inn,
            int inningsNumber
    ) {
        // Header
        items.add(ScorecardItem.inningsHeader(new InningsHeaderData(inn, inningsNumber)));

        // Target bar (2nd innings only)
        if (inn.target != null) {
            items.add(ScorecardItem.target(inn.target));
        }

        // Batting section
        appendBattingSection(items, inn);

        // Bowling section
        appendBowlingSection(items, inn);

        // Ball-by-ball section
        appendBallByBallSection(items, inn);
    }

    private void appendBattingSection(@NonNull List<ScorecardItem> items, @NonNull Innings inn) {
        items.add(ScorecardItem.sectionLabel(SECTION_BATTING));
        items.add(ScorecardItem.batHeader());

        if (inn.batsmen != null) {
            for (BatsmanScore b : inn.batsmen) {
                if (b != null) items.add(ScorecardItem.batsman(b));
            }
        }

        items.add(ScorecardItem.extras(buildExtrasText(inn)));
    }

    private void appendBowlingSection(@NonNull List<ScorecardItem> items, @NonNull Innings inn) {
        items.add(ScorecardItem.sectionLabel(SECTION_BOWLING));
        items.add(ScorecardItem.bowlHeader());

        if (inn.bowlers != null) {
            for (BowlerScore bw : inn.bowlers) {
                if (bw != null) items.add(ScorecardItem.bowler(bw));
            }
        }
    }

    private void appendBallByBallSection(@NonNull List<ScorecardItem> items, @NonNull Innings inn) {
        items.add(ScorecardItem.sectionLabel(SECTION_BALL_BY_BALL));

        List<List<String>> overs = CricketEngine.splitIntoOvers(inn.ballLog);
        for (int o = 0; o < overs.size(); o++) {
            items.add(ScorecardItem.over(new OverData(o + 1, overs.get(o))));
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    @NonNull
    private String buildExtrasText(@NonNull Innings inn) {
        return String.format(Locale.getDefault(),
                "Extras: %d  (wd %d, nb %d)",
                inn.getTotalExtras(), inn.extraWides, inn.extraNoBalls);
    }

    private void setTextOrHide(@NonNull View view, @Nullable String text) {
        if (!TextUtils.isEmpty(text)) {
            ((android.widget.TextView) view).setText(text);
            view.setVisibility(View.VISIBLE);
        } else {
            view.setVisibility(View.GONE);
        }
    }

    @NonNull
    private String formatStatus(@NonNull Match.Status status) {
        switch (status) {
            case LIVE:          return "LIVE";
            case COMPLETED:     return "COMPLETED";
            case INNINGS_BREAK: return "INNINGS BREAK";
            default:            return status.name().replace('_', ' ');
        }
    }
}
