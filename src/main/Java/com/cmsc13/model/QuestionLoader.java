
package com.cmsc13.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;

public class QuestionLoader {

    public static List<Question> loadQuestions()
            throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        InputStream input = QuestionLoader.class
                .getResourceAsStream("/data/questions.json");

        if (input == null) {
            throw new IllegalStateException(
                    "questions.json was not found!");
        }

        try (input) {
            return mapper.readValue(
                    input,
                    new TypeReference<List<Question>>() {}
            );
        }
    }
}
