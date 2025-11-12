package com.example.h_cas.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * DatabaseExecutor provides a thread pool for executing database operations
 * off the main thread to prevent UI blocking.
 */
public class DatabaseExecutor {
    private static final int THREAD_POOL_SIZE = 3;
    private static DatabaseExecutor instance;
    private final ExecutorService executorService;
    private final Handler mainHandler;

    private DatabaseExecutor() {
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized DatabaseExecutor getInstance() {
        if (instance == null) {
            instance = new DatabaseExecutor();
        }
        return instance;
    }

    /**
     * Execute a database operation on a background thread
     */
    public void execute(Runnable task) {
        executorService.execute(task);
    }

    /**
     * Execute a task on the main thread
     */
    public void executeOnMainThread(Runnable task) {
        mainHandler.post(task);
    }

    /**
     * Shutdown the executor (call this when app is closing)
     */
    public void shutdown() {
        executorService.shutdown();
    }
}



