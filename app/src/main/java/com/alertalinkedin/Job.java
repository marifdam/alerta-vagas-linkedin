package com.alertalinkedin;

public class Job {
    private final String id;
    private final String title;
    private final String company;
    private final String location;
    private final String url;
    private final String keyword;
    private final String timeAgo;

    public Job(String id, String title, String company, String location, String url, String keyword, String timeAgo) {
        this.id = id;
        this.title = title;
        this.company = company;
        this.location = location;
        this.url = url;
        this.keyword = keyword;
        this.timeAgo = timeAgo;
    }

    public String getId()       { return id; }
    public String getTitle()    { return title; }
    public String getCompany()  { return company; }
    public String getLocation() { return location; }
    public String getUrl()      { return url; }
    public String getKeyword()  { return keyword; }
    public String getTimeAgo()  { return timeAgo; }
}
