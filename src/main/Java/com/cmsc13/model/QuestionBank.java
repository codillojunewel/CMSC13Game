package com.cmsc13.model;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Singleton that parses the external question file once at runtime and
 * serves randomized questions to the game.
 * <p>
 * Selection is written in a functional style: each ladder rung describes its
 * pool as a {@link Predicate}, and the bank filters the immutable list with streams.
 */
public final class QuestionBank {

    private static final String RESOURCE = "/data/questions.json";
    private static QuestionBank instance;

    private final List<Question> questions;
    private final Random random = new Random();

    private QuestionBank(List<Question> questions) {
        this.questions = Collections.unmodifiableList(questions);
    }

    public static synchronized QuestionBank getInstance() {
        if (instance == null) {
            instance = new QuestionBank(load());
        }
        return instance;
    }

    private static List<Question> load() {
        ObjectMapper mapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (InputStream in = QuestionBank.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(RESOURCE + " was not found on the classpath");
            }
            List<Question> list = mapper.readValue(in, new TypeReference<List<Question>>() {});
            // Validate once so a broken entry can never crash a running audit.
            return list.stream()
                    .filter(q -> q.getChoices() != null && q.getChoices().size() == 4)
                    .filter(q -> q.getCorrectIndex() >= 0 && q.getCorrectIndex() < 4)
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load the question bank: " + e.getMessage(), e);
        }
    }

    public List<Question> all() {
        return questions;
    }

    public int size() {
        return questions.size();
    }

    public long count(Predicate<Question> filter) {
        return questions.stream().filter(filter).count();
    }

    public Optional<Question> findById(int id) {
        return questions.stream().filter(q -> q.getId() == id).findFirst();
    }

    /** Picks a random question matching {@code filter} whose id is not in {@code exclude}. */
    public Question pick(Predicate<Question> filter, Collection<Integer> exclude) {
        Set<Integer> excluded = Set.copyOf(exclude);
        List<Question> pool = questions.stream()
                .filter(filter)
                .filter(q -> !excluded.contains(q.getId()))
                .toList();

        if (pool.isEmpty()) {
            // Every question in this pool was used: allow repeats rather than fail.
            pool = questions.stream().filter(filter).toList();
        }
        if (pool.isEmpty()) {
            throw new IllegalStateException("No questions match this ladder rung");
        }
        return pool.get(random.nextInt(pool.size()));
    }
}
