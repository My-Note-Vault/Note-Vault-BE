package com.example.search.retrieval;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WeightedRrf {
    private WeightedRrf() {
    }

    public record Ranking<K>(Map<K, Double> scores, double weight) {
        public Ranking {
            scores = Map.copyOf(scores);
            if (!Double.isFinite(weight) || weight <= 0) {
                throw new IllegalArgumentException("weight must be positive and finite");
            }
            if (scores.values().stream().anyMatch(score -> !Double.isFinite(score))) {
                throw new IllegalArgumentException("scores must be finite");
            }
        }
    }

    public static <K> Map<K, Double> fuse(List<Ranking<K>> rankings, int rankConstant) {
        if (rankConstant < 1) {
            throw new IllegalArgumentException("rankConstant must be positive");
        }
        Map<K, Double> fused = new HashMap<>();
        for (Ranking<K> ranking : rankings) {
            List<Map.Entry<K, Double>> entries = new ArrayList<>(ranking.scores().entrySet());
            entries.sort(Map.Entry.<K, Double>comparingByValue().reversed());
            int rank = 0;
            double previousScore = 0;
            for (int index = 0; index < entries.size(); index++) {
                Map.Entry<K, Double> entry = entries.get(index);
                double score = entry.getValue();
                if (index == 0 || score != previousScore) {
                    rank = index + 1;
                }
                fused.merge(entry.getKey(),
                        ranking.weight() / ((double) rankConstant + rank), Double::sum);
                previousScore = score;
            }
        }
        return Map.copyOf(fused);
    }
}
