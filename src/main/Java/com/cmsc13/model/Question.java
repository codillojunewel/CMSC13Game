package com.cmsc13.model;

import java.util.List;

public class Question {

    private int id;
    private String category;
    private String module;
    private String language;
    private String type;
    private String typeCode;
    private String question;
    private List<String> choices;
    private int correctIndex;
    private String rationale;

    public Question() {

    }

    public int getId() {
        return id;
    }

    public String getCategory() {
        return category;
    }

    public String getModule() {
        return module;
    }

    public String getLanguage() {
        return language;
    }

    public String getType() {
        return type;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public String getQuestion() {
        return question;
    }

    public List<String> getChoices() {
        return choices;
    }

    public int getCorrectIndex() {
        return correctIndex;
    }

    public String getRationale() {
        return rationale;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setTypeCode(String typeCode) {
        this.typeCode = typeCode;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setChoices(List<String> choices) {
        this.choices = choices;
    }

    public void setCorrectIndex(int correctIndex) {
        this.correctIndex = correctIndex;
    }

    public void setRationale(String rationale) {
        this.rationale = rationale;
    }
}