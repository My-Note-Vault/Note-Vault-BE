package com.example.workspace.document.ui.response;

import com.example.workspace.document.command.domain.DocumentDelta;
import com.example.workspace.document.query.DocumentQueryService;

import java.util.Base64;
import java.util.List;

public record CollaborationBootstrapResponse(
        String state,
        List<Update> updates,
        long cursor
) {

    public static CollaborationBootstrapResponse from(
            final DocumentQueryService.CollaborationHistory history
    ) {
        Base64.Encoder encoder = Base64.getEncoder();
        String state = history.crdtState() == null
                ? null
                : encoder.encodeToString(history.crdtState());
        List<Update> updates = history.deltas().stream()
                .map(delta -> Update.from(delta, encoder))
                .toList();
        return new CollaborationBootstrapResponse(state, updates, history.latestRevision());
    }

    public record Update(
            long revision,
            String update
    ) {

        private static Update from(final DocumentDelta delta, final Base64.Encoder encoder) {
            return new Update(
                    delta.getRevision(),
                    encoder.encodeToString(delta.getCrdtUpdate())
            );
        }
    }
}
