package com.cricboard.data;

import com.cricboard.Config;
import com.cricboard.model.Match;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Handles all Upstash Redis REST API communication.
 *
 * Redis key format:  match:{matchId}
 * All methods are blocking — call from a background thread.
 *
 * Upstash REST API:
 *   SET  → POST /set/{key}  body: {"value":"..."}
 *   GET  → GET  /get/{key}
 *   DEL  → GET  /del/{key}
 */
public class RedisRepository {

    private static final Gson GSON = new GsonBuilder().create();

    // ─── Singleton ────────────────────────────────────────────────────────────

    private static RedisRepository instance;

    public static synchronized RedisRepository getInstance() {
        if (instance == null) instance = new RedisRepository();
        return instance;
    }

    private RedisRepository() {}

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Push full match JSON to Redis.
     * Key: match:{matchId}
     */
    public boolean pushMatch(Match match) {
        try {
            String json = GSON.toJson(match);
            // URL-encode the match JSON as the value via pipeline command
            String key = "match:" + match.id;
            return redisSet(key, json);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Pull match JSON from Redis by matchId.
     * Returns null if not found or on error.
     */
    public Match pullMatch(String matchId) {
        try {
            String key = "match:" + matchId;
            String json = redisGet(key);
            if (json == null || json.isEmpty()) return null;
            return GSON.fromJson(json, Match.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Delete a match from Redis.
     */
    public void deleteMatch(String matchId) {
        try {
            String key = "match:" + matchId;
            redisDel(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ─── Upstash REST helpers ─────────────────────────────────────────────────

    /**
     * SET key value  (Upstash REST: POST /pipeline with SET command)
     */
    private boolean redisSet(String key, String value) throws Exception {
        // Use the pipeline endpoint for reliable SET with large values
        String endpoint = Config.UPSTASH_URL + "/pipeline";
        String body = "[[\"SET\",\"" + escapeJson(key) + "\",\"" + escapeJson(value) + "\"]]";

        HttpURLConnection conn = openConnection(endpoint, "POST");
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int code = conn.getResponseCode();
        conn.disconnect();
        return code == 200;
    }

    /**
     * GET key  → returns the string value or null
     */
    private String redisGet(String key) throws Exception {
        String endpoint = Config.UPSTASH_URL + "/get/" + urlEncode(key);
        HttpURLConnection conn = openConnection(endpoint, "GET");

        int code = conn.getResponseCode();
        if (code != 200) {
            conn.disconnect();
            return null;
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
        }
        conn.disconnect();

        // Upstash GET response: {"result":"<value>"} or {"result":null}
        String response = sb.toString();
        return parseResult(response);
    }

    /**
     * DEL key
     */
    private void redisDel(String key) throws Exception {
        String endpoint = Config.UPSTASH_URL + "/del/" + urlEncode(key);
        HttpURLConnection conn = openConnection(endpoint, "GET");
        conn.getResponseCode();
        conn.disconnect();
    }

    private HttpURLConnection openConnection(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Bearer " + Config.UPSTASH_TOKEN);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setConnectTimeout(8000);
        conn.setReadTimeout(8000);
        if (method.equals("POST")) conn.setDoOutput(true);
        return conn;
    }

    /**
     * Parse {"result":"<value>"} or {"result":null}
     */
    private String parseResult(String json) {
        if (json == null || json.contains("\"result\":null")) return null;
        int start = json.indexOf("\"result\":\"");
        if (start == -1) return null;
        start += 10; // skip past "result":"
        // Find closing quote, accounting for escaped quotes
        int end = start;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }
        return unescapeJson(json.substring(start, end));
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        return s.replace("\\\"", "\"")
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\\", "\\");
    }

    private String urlEncode(String s) {
        return s.replace(":", "%3A");
    }
}
