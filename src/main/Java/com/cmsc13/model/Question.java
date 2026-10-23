package com.cmsc13.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * One multiple-choice question from the question bank (data/questions.json).
 * <p>
 * Theoretical questions belong to a lecture module (1-7).
 * Programming questions belong to a language (C, Java, Python, JavaScript)
 * and may carry a code snippet and/or an expected output block.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Question {

    public static final String THEORETICAL = "Theoretical";
    public static final String PROGRAMMING = "Programming";

    private int id;
    private String category;
    private String module;
    private Integer moduleNo;
    private String language;
    private String type;
    private String typeCode;
    private String prompt;
    private String code;
    private String expectedOutput;
    private List<String> choices;
    private int correctIndex;
    private String rationale;

    public Question() {
        // required by Jackson
    }

    // ---------------------------------------------------------------- helpers

    public boolean isProgramming() {
        return PROGRAMMING.equalsIgnoreCase(category);
    }

    public boolean hasCode() {
        return code != null && !code.isBlank();
    }

    public boolean hasExpectedOutput() {
        return expectedOutput != null && !expectedOutput.isBlank();
    }

    /** True when the answer choices are code and should be shown in a monospace font. */
    public boolean choicesAreCode() {
        return isProgramming() && !"Output Prediction".equals(type);
    }

    public String getCorrectChoice() {
        return choices.get(correctIndex);
    }

    public static char letter(int index) {
        return (char) ('A' + index);
    }

    /** Short label such as "C · Output Prediction" or "Module 3 · Functional Programming". */
    public String getSourceLabel() {
        if (isProgramming()) {
            return language + " · " + type;
        }
        return "Module " + moduleNo + " · " + module;
    }

    /** The rationale, or a generated explanation when the bank has none. */
    public String getExplanation() {
        if (rationale != null && !rationale.isBlank()) {
            return rationale;
        }
        String answer = letter(correctIndex) + ". " + getCorrectChoice().replace('\n', ' ');
        return switch (type == null ? "" : type) {
            case "Output Prediction" -> "Trace the program line by line: it prints " + getCorrectChoice().replace('\n', ' ') + ".";
            case "Match the Output" -> "Only snippet " + answer + " produces exactly the expected output.";
            case "Complete the Code" -> "Filling the blank with \"" + getCorrectChoice().replace('\n', ' ')
                    + "\" is the only option that compiles and prints the expected output.";
            default -> "The correct answer is " + answer + ".";
        };
    }

    // ------------------------------------------------------- getters/setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getModule() { return module; }
    public void setModule(String module) { this.module = module; }

    public Integer getModuleNo() { return moduleNo; }
    public void setModuleNo(Integer moduleNo) { this.moduleNo = moduleNo; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getExpectedOutput() { return expectedOutput; }
    public void setExpectedOutput(String expectedOutput) { this.expectedOutput = expectedOutput; }

    public List<String> getChoices() { return choices; }
    public void setChoices(List<String> choices) { this.choices = choices; }

    public int getCorrectIndex() { return correctIndex; }
    public void setCorrectIndex(int correctIndex) { this.correctIndex = correctIndex; }

    public String getRationale() { return rationale; }
    public void setRationale(String rationale) { this.rationale = rationale; }
}
