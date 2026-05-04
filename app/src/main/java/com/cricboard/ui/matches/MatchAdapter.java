package com.cricboard.ui.matches;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.cricboard.R;
import com.cricboard.model.Innings;
import com.cricboard.model.Match;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.VH> {
    public interface OnMatchClick { void onClick(Match m); }
    public interface OnMatchLongClick { void onLongClick(Match m); }

    private List<Match> matches = new ArrayList<>();
    private final OnMatchClick onClick;
    private final OnMatchLongClick onLongClick;

    public MatchAdapter(OnMatchClick onClick, OnMatchLongClick onLongClick) {
        this.onClick = onClick;
        this.onLongClick = onLongClick;
    }

    public void setMatches(List<Match> matches) {
        this.matches = matches;
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_match, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Match m = matches.get(pos);
        h.tvTitle.setText(m.title);
        if (m.venue != null && !m.venue.isEmpty()) {
            h.tvVenue.setVisibility(View.VISIBLE);
            h.tvVenue.setText("📍 " + m.venue);
        } else {
            h.tvVenue.setVisibility(View.GONE);
        }

        // Status chip
        switch (m.status) {
            case LIVE:
                h.tvStatus.setText("🔴 LIVE");
                h.tvStatus.setTextColor(Color.parseColor("#FF4444"));
                break;
            case INNINGS_BREAK:
                h.tvStatus.setText("⏸ BREAK");
                h.tvStatus.setTextColor(Color.parseColor("#FFB300"));
                break;
            case COMPLETED:
                h.tvStatus.setText("✓ DONE");
                h.tvStatus.setTextColor(Color.parseColor("#4CAF50"));
                break;
            default:
                h.tvStatus.setText("TOSS");
                h.tvStatus.setTextColor(Color.parseColor("#9E9E9E"));
        }

        // Score summary
        StringBuilder sb = new StringBuilder();
        sb.append(m.maxOvers).append(" ov  |  ");
        Innings inn1 = m.innings[0];
        if (inn1 != null) {
            sb.append(inn1.battingTeam).append(": ").append(inn1.runs).append("/").append(inn1.wickets);
            sb.append(" (").append(inn1.overs).append(".").append(inn1.balls).append(")");
        }
        Innings inn2 = m.innings[1];
        if (inn2 != null) {
            sb.append("  •  ").append(inn2.battingTeam).append(": ").append(inn2.runs).append("/").append(inn2.wickets);
            sb.append(" (").append(inn2.overs).append(".").append(inn2.balls).append(")");
        }
        if (m.status == Match.Status.COMPLETED && m.result != null) {
            h.tvScore.setText(m.result);
        } else {
            h.tvScore.setText(sb.toString());
        }

        // Date
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault());
        h.tvDate.setText(sdf.format(new Date(m.createdAt)));

        h.card.setOnClickListener(v -> onClick.onClick(m));
        h.card.setOnLongClickListener(v -> { onLongClick.onLongClick(m); return true; });
    }

    @Override public int getItemCount() { return matches.size(); }

    static class VH extends RecyclerView.ViewHolder {
        CardView card;
        TextView tvTitle, tvStatus, tvScore, tvDate, tvVenue;
        VH(View v) {
            super(v);
            card = v.findViewById(R.id.card);
            tvTitle = v.findViewById(R.id.tvTitle);
            tvStatus = v.findViewById(R.id.tvStatus);
            tvScore = v.findViewById(R.id.tvScore);
            tvDate = v.findViewById(R.id.tvDate);
            tvVenue = v.findViewById(R.id.tvVenue);
        }
    }
}
