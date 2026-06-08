package com.tim14.slagalica.model;

import java.util.List;

public class SpojniceRound {
    private String id;
    private String title;
    private List<String> leftItems;
    private List<String> correctRightItems;
    private List<String> displayedRightItems;

    public SpojniceRound() {
        // Required for Firebase
    }

    public SpojniceRound(String id, String title,
                         List<String> leftItems,
                         List<String> correctRightItems,
                         List<String> displayedRightItems) {
        this.id = id;
        this.title = title;
        this.leftItems = leftItems;
        this.correctRightItems = correctRightItems;
        this.displayedRightItems = displayedRightItems;
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public List<String> getLeftItems() {
        return leftItems;
    }

    public List<String> getCorrectRightItems() {
        return correctRightItems;
    }

    public List<String> getDisplayedRightItems() {
        return displayedRightItems;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setLeftItems(List<String> leftItems) {
        this.leftItems = leftItems;
    }

    public void setCorrectRightItems(List<String> correctRightItems) {
        this.correctRightItems = correctRightItems;
    }

    public void setDisplayedRightItems(List<String> displayedRightItems) {
        this.displayedRightItems = displayedRightItems;
    }
}