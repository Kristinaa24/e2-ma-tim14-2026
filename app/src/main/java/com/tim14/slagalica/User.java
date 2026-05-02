package com.tim14.slagalica;

public class User {
    public String username;
    public String email;
    public String region;
    public int tokens;
    public int stars;
    public int league;

    public User(String username, String email, String region, int tokens, int stars, int league) {
        this.username = username;
        this.email = email;
        this.region = region;
        this.tokens = tokens;
        this.stars = stars;
        this.league = league;
    }
}