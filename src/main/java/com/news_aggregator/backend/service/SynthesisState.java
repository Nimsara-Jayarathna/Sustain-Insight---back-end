package com.news_aggregator.backend.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class SynthesisState {

    private final AtomicBoolean isSynthesisInProgress = new AtomicBoolean(false);

    public boolean isSynthesisInProgress() {
        return isSynthesisInProgress.get();
    }

    public void setSynthesisInProgress(boolean inProgress) {
        isSynthesisInProgress.set(inProgress);
    }
}
