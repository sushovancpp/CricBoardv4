package com.cricboard.ui.matches;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.cricboard.data.MatchRepository;
import com.cricboard.databinding.ActivityNewMatchBinding;
import com.cricboard.model.Match;
import com.cricboard.ui.scoring.ScoringActivity;

public class NewMatchActivity extends AppCompatActivity {
    private ActivityNewMatchBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityNewMatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("New Match");

        // Preset over options
        int[] overOptions = {5, 6, 10, 15, 20, 25, 50};
        binding.chipGroupOvers.removeAllViews();
        for (int o : overOptions) {
            com.google.android.material.chip.Chip chip = new com.google.android.material.chip.Chip(this);
            chip.setText(o + " ov");
            chip.setCheckable(true);
            chip.setTag(o);
            if (o == 20) chip.setChecked(true);
            binding.chipGroupOvers.addView(chip);
        }

        binding.btnCreate.setOnClickListener(v -> createMatch());
    }

    private void createMatch() {
        String team1 = binding.etTeam1.getText().toString().trim();
        String team2 = binding.etTeam2.getText().toString().trim();
        if (team1.isEmpty()) team1 = "Team A";
        if (team2.isEmpty()) team2 = "Team B";
        if (team1.equals(team2)) {
            Toast.makeText(this, "Team names must be different", Toast.LENGTH_SHORT).show();
            return;
        }

        int overs = 20;
        for (int i = 0; i < binding.chipGroupOvers.getChildCount(); i++) {
            com.google.android.material.chip.Chip chip =
                (com.google.android.material.chip.Chip) binding.chipGroupOvers.getChildAt(i);
            if (chip.isChecked()) { overs = (int) chip.getTag(); break; }
        }

        // Custom overs
        String customOvers = binding.etCustomOvers.getText().toString().trim();
        if (!customOvers.isEmpty()) {
            try {
                int co = Integer.parseInt(customOvers);
                if (co > 0 && co <= 50) overs = co;
            } catch (NumberFormatException ignored) {}
        }

        String venue = binding.etVenue.getText().toString().trim();

        Match match = new Match(team1, team2, overs);
        match.venue = venue;
        MatchRepository.getInstance(this).saveMatch(match);

        Intent i = new Intent(this, ScoringActivity.class);
        i.putExtra("matchId", match.id);
        startActivity(i);
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() { finish(); return true; }
}
