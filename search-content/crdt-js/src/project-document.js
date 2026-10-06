import { Doc, applyUpdate, encodeStateAsUpdate } from "yjs";
import { fromUint8Array, toUint8Array } from "js-base64";

globalThis.projectDocument = function (inputJson) {
  const input = JSON.parse(inputJson);
  const doc = new Doc();
  try {
    if (input.baseState !== null) {
      applyUpdate(doc, toUint8Array(input.baseState));
    }
    for (const update of input.updates) {
      applyUpdate(doc, toUint8Array(update));
    }

    // Version-specific adapter: keep this check aligned with pinned Yjs 13.6.30.
    if (doc.store.pendingStructs !== null || doc.store.pendingDs !== null) {
      return JSON.stringify({ status: "WAITING_DEPENDENCIES" });
    }
    const text = doc.getText("content").toString();
    if (text.length > input.maxContentCharacters) {
      return JSON.stringify({ status: "OUTPUT_LIMIT" });
    }
    const state = encodeStateAsUpdate(doc);
    if (state.byteLength > input.maxStateBytes) {
      return JSON.stringify({ status: "OUTPUT_LIMIT" });
    }
    return JSON.stringify({
      status: "READY",
      content: text,
      state: fromUint8Array(state),
    });
  } finally {
    doc.destroy();
  }
};
