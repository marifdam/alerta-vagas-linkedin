package com.alertalinkedin;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class LinkedInScraper {
    private static final OkHttpClient CLIENT = new OkHttpClient();

    private static final Map<String, String> GEO_IDS = new HashMap<>();
    static {
        GEO_IDS.put("Brasil",          "106057199");
        GEO_IDS.put("Portugal",        "100364837");
        GEO_IDS.put("Estados Unidos",  "103644278");
        GEO_IDS.put("Reino Unido",     "101165590");
        GEO_IDS.put("Canadá",          "101174742");
        GEO_IDS.put("Alemanha",        "101282230");
        GEO_IDS.put("França",          "105015875");
        GEO_IDS.put("Espanha",         "105646813");
        GEO_IDS.put("Países Baixos",   "102890719");
        GEO_IDS.put("Irlanda",         "104738515");
        GEO_IDS.put("Austrália",       "101452733");
        GEO_IDS.put("Argentina",       "100446943");
        GEO_IDS.put("México",          "103323778");
    }

    public List<Job> searchJobs(String keyword, String location, String timeRange) {
        List<Job> jobs = new ArrayList<>();
        try {
            String kw  = URLEncoder.encode(keyword, StandardCharsets.UTF_8.name());
            String loc = URLEncoder.encode(location, StandardCharsets.UTF_8.name());

            StringBuilder url = new StringBuilder(
                "https://www.linkedin.com/jobs-guest/jobs/api/seeMoreJobPostings/search"
                + "?keywords=" + kw + "&location=" + loc + "&start=0&count=25");

            String geoId = GEO_IDS.get(location);
            if (geoId != null) {
                url.append("&geoId=").append(geoId);
            } else if ("Remote".equals(location)) {
                url.append("&f_WT=2");
            }

            if (timeRange != null && !timeRange.isEmpty()) {
                url.append("&f_TPR=").append(timeRange);
            }

            Request request = new Request.Builder()
                .url(url.toString())
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "pt-BR,pt;q=0.9,en;q=0.8")
                .header("Referer", "https://www.linkedin.com/jobs/search/")
                .build();

            try (Response response = CLIENT.newCall(request).execute()) {
                if (!response.isSuccessful()) return jobs;
                ResponseBody body = response.body();
                if (body != null) jobs = parseJobs(body.string(), keyword);
            }
        } catch (IOException ignored) {
        }
        return jobs;
    }

    private List<Job> parseJobs(String html, String keyword) {
        List<Job> jobs = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        Elements items = doc.select("li");

        for (Element li : items) {
            String urn = li.select("[data-entity-urn]").attr("data-entity-urn");
            String[] parts = urn.split(":");
            if (parts.length == 0) continue;
            String id = parts[parts.length - 1];
            if (id.isEmpty() || !isNumeric(id)) continue;

            String title    = li.select(".base-search-card__title").text().trim();
            String company  = li.select(".base-search-card__subtitle").text().trim();
            String location = li.select(".job-search-card__location").text().trim();
            String rawUrl   = li.select("a.base-card__full-link").attr("href");
            String jobUrl   = rawUrl.contains("?") ? rawUrl.substring(0, rawUrl.indexOf('?')) : rawUrl;
            
            String timeAgo = li.select("time").text().trim();
            if (timeAgo.isEmpty()) {
                timeAgo = li.select(".job-search-card__listdate").text().trim();
            }
            if (timeAgo.isEmpty()) {
                timeAgo = li.select(".job-search-card__listdate--new").text().trim();
            }

            if (!title.isEmpty()) {
                jobs.add(new Job(id, title, company, location, jobUrl, keyword, timeAgo));
            }
        }
        return jobs;
    }

    private boolean isNumeric(String s) {
        for (char c : s.toCharArray()) if (!Character.isDigit(c)) return false;
        return true;
    }
}
