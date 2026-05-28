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

            String title = extractJsonValue(responseBody, "title");
            String slug = extractJsonValue(responseBody, "titleSlug");
            String difficulty = extractJsonValue(responseBody, "difficulty");
            String problemUrl = "https://leetcode.com/problems/" + slug + "/";

            System.out.println("Found problem: " + title + " (" + difficulty + ")");

            // Visual enhancements based on difficulty
            String color = "3066993"; // Easy (Green)
            String diffEmoji = "🟢 Easy";
            if (difficulty.equals("Medium")) {
                color = "16201919"; // Gold/Orange
                diffEmoji = "🟡 Medium";
            } else if (difficulty.equals("Hard")) {
                color = "15548997"; // Red
                diffEmoji = "🔴 Hard";
            }

            // Enhanced AI Prompt for structured insights
            String geminiApiKey = System.getenv("GEMINI_API_KEY");
            String aiHint = "Analyze the constraints carefully and pick the right data structure!";
            String coreTopics = "#DataStructures";
            String targetComplexity = "O(N) Time";

            if (geminiApiKey != null && !geminiApiKey.isEmpty()) {
                System.out.println("Generating elite AI insights via Gemini...");
                String geminiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiApiKey;

                String prompt = "Analyze the LeetCode problem '" + title + "'. "
                        + "Provide a response exactly in this format without any markdown formatting wrappers:\n"
                        + "HINT: [A brilliant 2-sentence conceptual hint without giving away code solutions]\n"
                        + "TOPICS: [2-3 relevant tags like #HashMap, #TwoPointers]\n"
                        + "COMPLEXITY: [Target optimal time/space complexity like O(N) Time / O(1) Space]";

                String geminiPayload = "{\"contents\": [{\"parts\":[{\"text\": \"" + prompt + "\"}]}]}";

                HttpRequest geminiRequest = HttpRequest.newBuilder()
                        .uri(URI.create(geminiUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(geminiPayload))
                        .build();

                HttpResponse<String> geminiResponse = client.send(geminiRequest, HttpResponse.BodyHandlers.ofString());
                String rawAiResponse = extractGeminiText(geminiResponse.body());

                // Parse the structured AI response
                aiHint = parseSection(rawAiResponse, "HINT:", "TOPICS:");
                coreTopics = parseSection(rawAiResponse, "TOPICS:", "COMPLEXITY:");
                targetComplexity = parseSection(rawAiResponse, "COMPLEXITY:", "$");

                if(aiHint.isEmpty()) aiHint = "Focus on optimizing your approach!";
            }

            // Beautiful Grid-Based Discord Embed Payload
            String discordWebhookUrl = System.getenv("DISCORD_WEBHOOK_URL");
            if (discordWebhookUrl != null && !discordWebhookUrl.isEmpty()) {
                String discordPayload = "{\n" +
                        "  \"embeds\": [{\n" +
                        "    \"title\": \"🚀 LEETCODE DAILY CHALLENGE\",\n" +
                        "    \"description\": \"### [" + title + "](" + problemUrl + ")\\n*Time to sharpen those DSA skills!*\",\n" +
                        "    \"color\": " + color + ",\n" +
                        "    \"fields\": [\n" +
                        "      {\n" +
                        "        \"name\": \"📊 Difficulty\",\n" +
                        "        \"value\": \"`" + diffEmoji + "`\",\n" +
                        "        \"inline\": true\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"name\": \"🎯 Target Efficiency\",\n" +
                        "        \"value\": \"`" + targetComplexity + "`\",\n" +
                        "        \"inline\": true\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"name\": \"🏷️ Core Patterns\",\n" +
                        "        \"value\": \"" + coreTopics + "\",\n" +
                        "        \"inline\": false\n" +
                        "      },\n" +
                        "      {\n" +
                        "        \"name\": \"💡 Agent Strategy & Hint\",\n" +
                        "        \"value\": \">>> " + aiHint.replace("\"", "\\\"").replace("\n", " ") + "\",\n" +
                        "        \"inline\": false\n" +
                        "      }\n" +
                        "    ],\n" +
                        "    \"footer\": {\n" +
                        "      \"text\": \"Consistency beats talent. Go submit your solution! 🔥\"\n" +
                        "    }\n" +
                        "  }]\n" +
                        "}";

                HttpRequest discordRequest = HttpRequest.newBuilder()
                        .uri(URI.create(discordWebhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(discordPayload))
                        .build();

                client.send(discordRequest, HttpResponse.BodyHandlers.ofString());
                System.out.println("Upgraded notification sent successfully!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String extractJsonValue(String json, String key) {
        Pattern pattern = Pattern.compile("\"" + key + "\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        return matcher.find() ? matcher.group(1) : "Unknown";
    }

    private static String extractGeminiText(String json) {
        Pattern pattern = Pattern.compile("\"text\":\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            return matcher.group(1).replace("\\n", "\n");
        }
        return "";
    }

    private static String parseSection(String text, String startKeyword, String endKeyword) {
        try {
            int start = text.indexOf(startKeyword);
            if (start == -1) return "";
            start += startKeyword.length();

            int end = endKeyword.equals("$") ? text.length() : text.indexOf(endKeyword, start);
            if (end == -1) end = text.length();

            return text.substring(start, end).trim();
        } catch (Exception e) {
            return "";
        }
    }
}