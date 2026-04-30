package com.example.workspace.unfolded;

import java.util.List;


public record TaskOverviewResponse(
        Long id,
        String title,
        List<SubTaskSummary> subTaskSummaries
) {
    public record SubTaskSummary(
            Long id,
            String title,
            List<TriviaSummary> triviaSummaries
    ) {

        public record TriviaSummary(
                Long id,
                String title
        ) {
        }
    }
}

