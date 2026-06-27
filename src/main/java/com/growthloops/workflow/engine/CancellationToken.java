package com.growthloops.workflow.engine;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @ClassName CancellationToken
 * @Description TODO
 * @Author samya
 * @Date 26/06/26
 */
public class CancellationToken {
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    public void cancel(){
        cancelled.set(true);
    }

    public boolean isCancellationRequested(){
        return cancelled.get();
    }
}
