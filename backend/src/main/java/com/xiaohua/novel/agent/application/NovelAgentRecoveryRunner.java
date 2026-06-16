package com.xiaohua.novel.agent.application;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class NovelAgentRecoveryRunner implements ApplicationRunner {

    private final NovelAgentService novelAgentService;

    public NovelAgentRecoveryRunner(NovelAgentService novelAgentService) {
        this.novelAgentService = novelAgentService;
    }

    @Override
    public void run(ApplicationArguments args) {
        novelAgentService.recoverInterruptedChapterRuns();
    }
}
