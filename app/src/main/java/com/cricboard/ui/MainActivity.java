package com.cricboard.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.cricboard.Config;
import com.cricboard.R;
import com.cricboard.data.MatchRepository;
import com.cricboard.databinding.ActivityMainBinding;
import com.cricboard.model.Match;
import com.cricboard.ui.live.JoinMatchActivity;
import com.cricboard.ui.matches.MatchAdapter;
import com.cricboard.ui.matches.NewMatchActivity;
import com.cricboard.ui.scoring.ScoringActivity;
import com.cricboard.ui.scorecard.ScorecardActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MatchRepository repo;
    private MatchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        repo = MatchRepository.getInstance(this);

        adapter = new MatchAdapter(match -> {
            if (match.status == Match.Status.COMPLETED) {
                Intent i = new Intent(this, ScorecardActivity.class);
                i.putExtra("matchId", match.id);
                startActivity(i);
            } else {
                // Require admin password to enter scoring mode
                showAdminPasswordDialog(match);
            }
        }, match -> showDeleteDialog(match));

        binding.recyclerMatches.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerMatches.setAdapter(adapter);

        // FAB: New Match (admin only)
        binding.fabNewMatch.setOnClickListener(v -> showAdminGateForNewMatch());

        // Watch Live button
        binding.btnWatchLive.setOnClickListener(v ->
            startActivity(new Intent(this, JoinMatchActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    private void refreshList() {
        List<Match> matches = repo.getAllMatches();
        adapter.setMatches(matches);
        binding.emptyState.setVisibility(matches.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerMatches.setVisibility(matches.isEmpty() ? View.GONE : View.VISIBLE);
    }

    // ─── Admin password gate ──────────────────────────────────────────────────

    private void showAdminGateForNewMatch() {
        showPasswordDialog("Admin Login", () ->
            startActivity(new Intent(this, NewMatchActivity.class)));
    }

    private void showAdminPasswordDialog(Match match) {
        showPasswordDialog("Admin Login", () -> {
            Intent i = new Intent(this, ScoringActivity.class);
            i.putExtra("matchId", match.id);
            i.putExtra("isAdmin", true);
            startActivity(i);
        });
    }

    private void showPasswordDialog(String title, Runnable onSuccess) {
        EditText et = new EditText(this);
        et.setHint("Enter admin password");
        et.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
                        android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        et.setPadding(48, 24, 48, 24);

        new AlertDialog.Builder(this)
            .setTitle("🔒 " + title)
            .setMessage("This action requires admin access.")
            .setView(et)
            .setPositiveButton("Enter", (d, w) -> {
                String entered = et.getText().toString().trim();
                if (Config.ADMIN_PASSWORD.equals(entered)) {
                    onSuccess.run();
                } else {
                    Toast.makeText(this, "❌ Wrong password", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    // ─── Delete dialog ────────────────────────────────────────────────────────

    private void showDeleteDialog(Match match) {
        showPasswordDialog("Confirm Delete", () -> {
            repo.deleteMatch(match.id);
            refreshList();
        });
    }

    // ─── Menu ─────────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_clear_all) {
            showPasswordDialog("Clear All Matches", () -> {
                new AlertDialog.Builder(this)
                    .setTitle("Clear All Matches")
                    .setMessage("Delete ALL match history? This cannot be undone.")
                    .setPositiveButton("Delete All", (d, w) -> {
                        repo.deleteAll();
                        refreshList();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
