package com.healthcare.cas.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Debouncer - Prevents rapid-fire updates from overwhelming the system
 * Useful for Firebase listeners that trigger frequently
 */
public class Debouncer {
    private static final long DEFAULT_DELAY_MS = 300; // 300ms debounce delay
    private final Handler handler;
    private final ConcurrentHashMap<String, Runnable> pendingTasks = new ConcurrentHashMap<>();
    
    private static Debouncer instance;
    
    private Debouncer() {
        handler = new Handler(Looper.getMainLooper());
    }
    
    public static synchronized Debouncer getInstance() {
        if (instance == null) {
            instance = new Debouncer();
        }
        return instance;
    }
    
    /**
     * Debounce a task - if called multiple times within delay, only last one executes
     * 
     * @param key Unique key for this task (e.g., "sync_patients", "update_ui")
     * @param task Task to execute
     * @param delayMs Delay in milliseconds
     */
    public void debounce(String key, Runnable task, long delayMs) {
        // Remove any pending task with same key
        Runnable existing = pendingTasks.remove(key);
        if (existing != null) {
            handler.removeCallbacks(existing);
        }
        
        // Schedule new task
        Runnable wrappedTask = () -> {
            pendingTasks.remove(key);
            task.run();
        };
        
        pendingTasks.put(key, wrappedTask);
        handler.postDelayed(wrappedTask, delayMs);
    }
    
    /**
     * Debounce with default delay (300ms)
     */
    public void debounce(String key, Runnable task) {
        debounce(key, task, DEFAULT_DELAY_MS);
    }
    
    /**
     * Cancel a pending debounced task
     */
    public void cancel(String key) {
        Runnable existing = pendingTasks.remove(key);
        if (existing != null) {
            handler.removeCallbacks(existing);
        }
    }
    
    /**
     * Cancel all pending tasks
     */
    public void cancelAll() {
        for (Runnable task : pendingTasks.values()) {
            handler.removeCallbacks(task);
        }
        pendingTasks.clear();
    }
}







