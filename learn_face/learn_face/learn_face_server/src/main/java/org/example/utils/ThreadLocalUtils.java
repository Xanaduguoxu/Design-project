package org.example.utils;


public class ThreadLocalUtils {

    private static final ThreadLocal<Long> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(Long value) {
        THREAD_LOCAL.set(value);
    }

    public static Long get() {
        return THREAD_LOCAL.get();
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }
}
