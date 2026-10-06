package com.example.search.crdt;

import java.util.List;

public interface YjsDocumentConverter {
    Projection project(byte[] baseState, List<byte[]> updates);

    record Projection(String content, byte[] crdtState) { }
}
