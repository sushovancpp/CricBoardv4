package com.cricboard.ui.scorecard;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cricboard.R;
import com.cricboard.data.MatchRepository;
import com.cricboard.databinding.ActivityScorecardBinding;
import com.cricboard.model.*;

import java.util.ArrayList;
import java.util.List;

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
        getSupportActionBar().setTitle("Scorecard");

        match = MatchRepository.getInstance(this)
                .getMatch(getIntent().getStringExtra("matchId"));
        if (match == null) { finish(); return; }

        bindMatchHeader();

        List<ScorecardItem> items = buildItems();
        ScorecardAdapter adapter = new ScorecardAdapter(this, items);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        // Disable default item animator to avoid flicker
        binding.recyclerView.setItemAnimator(null);
    }

    private void bindMatchHeader() {
        binding.tvMatchTitle.setText(match.title);

        String overInfo = match.maxOvers + " overs  ·  " + formatStatus(match.status);
        binding.tvMatchOvers.setText(overInfo);

        if (match.result != null && !match.result.isEmpty()) {
            binding.tvResult.setVisibility(View.VISIBLE);
            binding.tvResult.setText(match.result);
        }

        if (match.venue != null && !match.venue.isEmpty()) {
            binding.tvVenue.setVisibility(View.VISIBLE);
            binding.tvVenue.setText(match.venue);
        }

        if (match.tossWinner != null && !match.tossWinner.isEmpty()) {
            binding.tvToss.setVisibility(View.VISIBLE);
            binding.tvToss.setText(match.tossWinner + " won toss, elected to " + match.tossChoice);
        }
    }

    private List<ScorecardItem> buildItems() {
        List<ScorecardItem> items = new ArrayList<>();

        if (match.innings == null) return items;

        for (int i = 0; i < match.innings.length; i++) {
            Innings inn = match.innings[i];
            if (inn == null) continue;

            // Innings score header
            items.add(new ScorecardItem(ScorecardItem.TYPE_INNINGS_HEADER,
                    new InningsHeaderData(inn, i + 1)));

            // Target bar (2nd innings only)
            if (inn.target != null) {
                items.add(new ScorecardItem(ScorecardItem.TYPE_TARGET, inn.target));
            }

            // Batting section label
            items.add(new ScorecardItem(ScorecardItem.TYPE_SECTION_LABEL, "Batting"));

            // Batsmen column headers
            items.add(new ScorecardItem(ScorecardItem.TYPE_BAT_HEADER, null));

            // Batsmen rows
            for (BatsmanScore b : inn.batsmen) {
                items.add(new ScorecardItem(ScorecardItem.TYPE_BATSMAN, b));
            }

            // Extras
            String extras = "Extras: " + inn.getTotalExtras()
                    + "  (wd " + inn.extraWides + ", nb " + inn.extraNoBalls + ")";
            items.add(new ScorecardItem(ScorecardItem.TYPE_EXTRAS, extras));

            // Bowling section label
            items.add(new ScorecardItem(ScorecardItem.TYPE_SECTION_LABEL, "Bowling"));

            // Bowlers column headers
            items.add(new ScorecardItem(ScorecardItem.TYPE_BOWL_HEADER, null));

            // Bowler rows
            for (BowlerScore bw : inn.bowlers) {
                items.add(new ScorecardItem(ScorecardItem.TYPE_BOWLER, bw));
            }

            // Ball by ball section label
            items.add(new ScorecardItem(ScorecardItem.TYPE_SECTION_LABEL, "Ball by ball"));

            // Over rows
            List<List<String>> overs = CricketEngine.splitIntoOvers(inn.ballLog);
            for (int o = 0; o < overs.size(); o++) {
                items.add(new ScorecardItem(ScorecardItem.TYPE_OVER,
                        new OverData(o + 1, overs.get(o))));
            }

            // Spacer between innings
            if (i < match.innings.length - 1) {
                items.add(new ScorecardItem(ScorecardItem.TYPE_SPACER, null));
            }
        }

        return items;
    }

    private String formatStatus(Match.Status s) {
        switch (s) {
            case LIVE:           return "LIVE";
            case COMPLETED:      return "COMPLETED";
            case INNINGS_BREAK:  return "INNINGS BREAK";
            default:             return s.name();
        }
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
