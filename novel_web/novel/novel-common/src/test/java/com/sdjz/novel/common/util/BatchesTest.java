package com.sdjz.novel.common.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BatchesTest {

    @Test
    void splitsByCountWhenPayloadIsSmall() {
        List<Integer> items = IntStream.rangeClosed(1, 1201).boxed().toList();

        List<List<Integer>> batches = Batches.split(items, item -> 0);

        assertEquals(3, batches.size());
        assertEquals(500, batches.get(0).size());
        assertEquals(500, batches.get(1).size());
        assertEquals(201, batches.get(2).size());
        assertEquals(1201, batches.stream().mapToInt(List::size).sum());
    }

    @Test
    void splitsByPayloadWhenChaptersAreHuge() {
        long oneMb = 1024 * 1024L;
        List<Integer> items = IntStream.rangeClosed(1, 5).boxed().toList();

        List<List<Integer>> batches = Batches.split(items, 500, 2 * oneMb, item -> oneMb);

        assertEquals(3, batches.size());
        assertEquals(List.of(1, 2), batches.get(0));
        assertEquals(List.of(3, 4), batches.get(1));
        assertEquals(List.of(5), batches.get(2));
    }

    @Test
    void keepsOversizedSingleItemInItsOwnBatch() {
        List<Integer> items = List.of(1, 2, 3);

        List<List<Integer>> batches = Batches.split(items, 500, 5L, item -> 10L);

        assertEquals(3, batches.size());
        batches.forEach(batch -> assertEquals(1, batch.size()));
    }

    @Test
    void returnsNothingForEmptyInput() {
        assertTrue(Batches.split(List.of(), item -> 0).isEmpty());
        assertTrue(Batches.split((List<Object>) null, item -> 0).isEmpty());
    }
}
