package com.example.demo.docgen.service;

import java.lang.reflect.Field;

public final class TestUtils {
    private TestUtils() {}

    public static void setField(Object target, String fieldName, Object value) {
        try {
            Field f = target.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
