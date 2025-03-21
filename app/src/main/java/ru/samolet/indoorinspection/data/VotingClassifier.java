package ru.samolet.indoorinspection.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingClassifier {
    private final int n;

    public VotingClassifier(int n) {
        if (n >= 3) {
            this.n = n;
        } else {
            throw new IllegalArgumentException("Input list must contain at least 3 maps");
        }
    }

    public String summarizeFloats(List<HashMap<String, Float>> hash) {
        if (hash == null || hash.size() < n) {
            throw new IllegalArgumentException("Invalid input list");
        }

        HashMap<String, Float> newMap = new HashMap<>();
        String maxKey = "";
        float maxValue = Float.MIN_VALUE;

        for (HashMap<String, Float> map : hash) {
            for (Map.Entry<String, Float> entry : map.entrySet()) {
                newMap.merge(entry.getKey(), entry.getValue(), Float::sum);
            }
        }

        // Find the key with the maximum value
        for (Map.Entry<String, Float> entry : newMap.entrySet()) {
            if (entry.getValue() > maxValue) {
                maxKey = entry.getKey();
                maxValue = entry.getValue();
            }
        }

        return maxKey;
    }
}