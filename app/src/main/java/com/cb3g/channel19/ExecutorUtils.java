package com.cb3g.channel19;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ExecutorUtils {

    public static void shutdown(ExecutorService executorService){
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    public static ExecutorService newSingleThreadExecutor(){
        return Executors.newSingleThreadExecutor();
    }

    public static String getFutureString(ExecutorService executorService, Callable<String> callable) {
        Future<String> future = executorService.submit(callable);
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return null;
        }
    }

    public static Boolean getFutureBoolean(ExecutorService executorService, Callable<Boolean> callable) {
        Future<Boolean> future = executorService.submit(callable);
        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return false;
        }
    }
}
