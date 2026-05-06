package com.cricboard.ui.live;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.cricboard.databinding.ActivityJoinMatchBinding;

public class JoinMatchActivity extends AppCompatActivity {

    private ActivityJoinMatchBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityJoinMatchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("📺 Watch Live Match");

        binding.btnWatch.setOnClickListener(v -> {
            String matchId = "";
            if (binding.etMatchId.getText() != null) {
                matchId = binding.etMatchId.getText().toString().trim().toUpperCase();
            }
            if (matchId.isEmpty()) {
                Toast.makeText(this, "Please enter a Match ID", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, LiveViewActivity.class);
            intent.putExtra("matchId", matchId.toLowerCase());
            startActivity(intent);
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
