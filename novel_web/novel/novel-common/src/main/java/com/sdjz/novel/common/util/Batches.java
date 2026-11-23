package com.sdjz.novel.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToLongFunction;

/**
 * 把一个大列表切成若干批，用于把 N 次 INSERT 合并成几条多值 INSERT。
 *
 * <p>切分同时受两个上限约束：条数和「重量」累计（这里用一个权重函数表示，
 * 章节导入时就是正文字节数）。只看条数的话，遇到「章节少但每章很长」的书，
 * 一条 INSERT 可能塞进十几 MB，直接撞上 MySQL 的 max_allowed_packet。
 */
public final class Batches {

    /** 单批默认最多 500 条 */
    public static final int DEFAULT_MAX_SIZE = 500;
    /** 单批默认最多 2 MB 有效载荷，留足余量给 4 MB 的 max_allowed_packet */
    public static final long DEFAULT_MAX_BYTES = 2L * 1024 * 1024;

    private Batches() {
    }

    public static <T> List<List<T>> split(List<T> items, ToLongFunction<T> weigher) {
        return split(items, DEFAULT_MAX_SIZE, DEFAULT_MAX_BYTES, weigher);
    }

    public static <T> List<List<T>> split(List<T> items, int maxSize, long maxBytes,
                                          ToLongFunction<T> weigher) {
        List<List<T>> batches = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            return batches;
        }
        List<T> current = new ArrayList<>(Math.min(maxSize, items.size()));
        long weight = 0;
        for (T item : items) {
            long itemWeight = weigher.applyAsLong(item);
            boolean full = current.size() >= maxSize
                    || (weight + itemWeight > maxBytes && !current.isEmpty());
            if (full) {
                batches.add(current);
                current = new ArrayList<>(Math.min(maxSize, items.size()));
                weight = 0;
            }
            current.add(item);
            weight += itemWeight;
        }
        if (!current.isEmpty()) {
            batches.add(current);
        }
        return batches;
    }
}
