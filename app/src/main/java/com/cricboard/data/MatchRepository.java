package com.cricboard.data;

import android.content.Context;
import com.cricboard.model.Match;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MatchRepository {
    private static final String FILE_NAME = "matches.json";
    private static MatchRepository instance;
    private final Context context;
    private final Gson gson;
    private List<Match> matches;

    private MatchRepository(Context context) {
        this.context = context.getApplicationContext();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.matches = loadFromDisk();
    }

    public static synchronized MatchRepository getInstance(Context context) {
        if (instance == null) instance = new MatchRepository(context);
        return instance;
    }

    public List<Match> getAllMatches() {
        return new ArrayList<>(matches);
    }

    public Match getMatch(String id) {
        for (Match m : matches) if (m.id.equals(id)) return m;
        return null;
    }

    public void saveMatch(Match match) {
        match.updatedAt = System.currentTimeMillis();
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).id.equals(match.id)) {
                matches.set(i, match);
                saveToDisk();
                return;
            }
        }
        matches.add(0, match);
        saveToDisk();
    }

    public void deleteMatch(String id) {
        matches.removeIf(m -> m.id.equals(id));
        saveToDisk();
    }

    public void deleteAll() {
        matches.clear();
        saveToDisk();
    }

    private void saveToDisk() {
        try (FileWriter writer = new FileWriter(new File(context.getFilesDir(), FILE_NAME))) {
            gson.toJson(matches, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<Match> loadFromDisk() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return new ArrayList<>();
        try (FileReader reader = new FileReader(file)) {
            Type type = new TypeToken<List<Match>>() {}.getType();
            List<Match> loaded = gson.fromJson(reader, type);
            return loaded != null ? loaded : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
