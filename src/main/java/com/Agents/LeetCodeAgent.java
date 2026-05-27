package com.Agents;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LeetCodeAgent {

    public static void main(String[] args) {
        try {
            // 1. Fetch LeetCode Daily Question
            System.out.println("Fetching LeetCode Daily Question...");
            String leetCodeGraphqlUrl = "https://leetcode.com/graphql";
            String query = "{\"query\": \"query questionOfToday { activeDailyCodingChallengeQuestion { date link question { questionId questionFrontendId title titleSlug difficulty content } } }\"}";

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest lcRequest = HttpRequest.newBuilder()
                    .uri(URI.create(leetCodeGraphqlUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(query))
                    .build();

            HttpResponse<String> lcResponse = client.send(lcRequest, HttpResponse.BodyHandlers.ofString());
            String responseBody = lcResponse.body();

            // Simple regex parsing to keep it third-party library free
            String title = extractJsonValue(responseBody, "title");
            String slug = extractJsonValue(responseBody, "titleSlug");
            String difficulty = extractJsonValue(responseBody, "difficulty");
            String problemUrl = "https://leetcode.com/problems/" + slug + "/";

            System.out.println("Found problem: " + title + " (" + difficulty + ")");

            // 2. Generate AI Hint via Gemini Free Tier
            String geminiApiKey = System.getenv("GEMINI_API_KEY");
            String aiHint = "Good luck solving it!"; // Fallback

            if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
                System.out.println("Generating AI insights via Gemini...");
                String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

                String prompt = "Give a 2-sentence quick strategic hint or pattern recommendation (e.g., Two Pointers, Sliding Window, DP) for the LeetCode problem: " + title + ". Do not give away code solutions.";
                String geminiPayload = "{\"contents\": [{\"parts\":[{\"text\": \"" + prompt + "\"}]}]}";

                HttpRequest geminiRequest = HttpRequest.newBuilder()
                        .uri(URI.create(geminiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(geminiPayload))
                        .build();

                HttpResponse<String> geminiResponse = client.send(geminiRequest, HttpResponse.BodyHandlers.ofString());
                // Extracting text from Gemini's nested response structure
                aiHint = extractGeminiText(geminiResponse.body());
            }

            // 3. Format and Send Message to Discord
            String discordWebhookUrl = System.getenv("DISCORD_WEBHOOK_URL");
            if (discordWebhookUrl != null && !discordWebhookUrl.isEmpty()) {
                String discordPayload = "{\n" +
                        "  \"embeds\": [{\n" +
                        "    \"title\": \"☀️ LeetCode Daily Challenge!\",\n" +
                        "    \"description\": \"**" + title + "** (" + difficulty + ")\\n\\n" +
                        "**AI Agent Hint:** " + aiHint.replace("\"", "\\\"").replace("\n", " ") + "\",\n" +
                        "    \"url\": \"" + problemUrl + "\",\n" +
                        "    \"color\": " + (difficulty.equals("Easy") ? "3066993" : difficulty.equals("Medium") ? "16201919" : "15548997") + "\n" +
                        "  }]\n" +
                        "}";

                HttpRequest discordRequest = HttpRequest.newBuilder()
                        .uri(URI.create(discordWebhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(discordPayload))
                        .build();

                client.send(discordRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println("Notification sent successfully!");
            } else {
                System.out.println("Discord Webhook URL environment variable missing.");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.compile("\"" + key + "\":\\s*\"([^\"]+)\"").matcher(json);
        return matcher.find() ? matcher.group(1) : "Unknown";
    }

    private static String extractGeminiText(String json) {
        Pattern pattern = Pattern.compile("\"text\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "Analyze the constraints carefully and pick the right data structure!";
    }
}