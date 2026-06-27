package com.growthloops.workflow.engine;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName CancellationRegistry
 * @Description TODO
 * @Author samya
 * @Date 26/06/26
 */
@Component
public class CancellationRegistry {

    private final ConcurrentHashMap<String, CancellationToken> tokens = new ConcurrentHashMap<>();

    public CancellationToken register(String executionId){
        CancellationToken token=new CancellationToken();
        tokens.put(executionId, token);
        return token;
    }

    public boolean requestCancellation(String excutionId){
        CancellationToken token = tokens.get(excutionId);
        if(token == null){
            return false;
        }
        token.cancel();
        return true;
    }
    public void deregister(String executionId){
        tokens.remove(executionId);
    }

}
