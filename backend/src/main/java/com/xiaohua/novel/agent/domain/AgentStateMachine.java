package com.xiaohua.novel.agent.domain;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class AgentStateMachine {

    private final Map<AgentRunState, Set<AgentRunState>> transitions =
            new EnumMap<>(AgentRunState.class);

    public AgentStateMachine() {
        allow(AgentRunState.CREATED, AgentRunState.RESEARCHING);
        allow(AgentRunState.RESEARCHING, AgentRunState.GENERATING_PROPOSALS);
        allow(AgentRunState.GENERATING_PROPOSALS, AgentRunState.WAITING_CREATIVE_SELECTION);
        allow(
                AgentRunState.WAITING_CREATIVE_SELECTION,
                AgentRunState.GENERATING_PROPOSALS,
                AgentRunState.INITIALIZING_NOVEL);
        allow(AgentRunState.INITIALIZING_NOVEL, AgentRunState.PLANNING_CHAPTERS);
        allow(
                AgentRunState.PLANNING_CHAPTERS,
                AgentRunState.READY_TO_WRITE,
                AgentRunState.WAITING_USER_DECISION);
        allow(
                AgentRunState.READY_TO_WRITE,
                AgentRunState.GENERATING_CHAPTER,
                AgentRunState.PAUSED,
                AgentRunState.PAUSED_BY_BUDGET);
        allow(AgentRunState.GENERATING_CHAPTER, AgentRunState.VALIDATING_OUTPUT);
        allow(
                AgentRunState.VALIDATING_OUTPUT,
                AgentRunState.REVIEWING_QUALITY,
                AgentRunState.REVISING_CHAPTER);
        allow(
                AgentRunState.REVIEWING_QUALITY,
                AgentRunState.CHECKING_CONTINUITY,
                AgentRunState.REVISING_CHAPTER,
                AgentRunState.WAITING_USER_DECISION);
        allow(
                AgentRunState.CHECKING_CONTINUITY,
                AgentRunState.COMMITTING_CHAPTER,
                AgentRunState.REVISING_CHAPTER,
                AgentRunState.WAITING_USER_DECISION);
        allow(
                AgentRunState.REVISING_CHAPTER,
                AgentRunState.VALIDATING_OUTPUT,
                AgentRunState.WAITING_USER_DECISION);
        allow(
                AgentRunState.COMMITTING_CHAPTER,
                AgentRunState.READY_TO_WRITE,
                AgentRunState.COMPLETED);
        allow(
                AgentRunState.WAITING_USER_DECISION,
                AgentRunState.READY_TO_WRITE,
                AgentRunState.INITIALIZING_NOVEL);
        allow(AgentRunState.PAUSED, AgentRunState.READY_TO_WRITE);
        allow(AgentRunState.PAUSED_BY_BUDGET, AgentRunState.READY_TO_WRITE);

        for (AgentRunState state : AgentRunState.values()) {
            if (state != AgentRunState.COMPLETED && state != AgentRunState.FAILED) {
                transitions.computeIfAbsent(state, ignored -> EnumSet.noneOf(AgentRunState.class))
                        .add(AgentRunState.FAILED);
            }
        }
    }

    public void requireTransition(AgentRunState from, AgentRunState to) {
        if (!transitions.getOrDefault(from, Set.of()).contains(to)) {
            throw new IllegalStateException("非法 NovelAgent 状态转换: " + from + " -> " + to);
        }
    }

    public boolean canTransition(AgentRunState from, AgentRunState to) {
        return transitions.getOrDefault(from, Set.of()).contains(to);
    }

    private void allow(AgentRunState from, AgentRunState... targets) {
        transitions.computeIfAbsent(from, ignored -> EnumSet.noneOf(AgentRunState.class))
                .addAll(Set.of(targets));
    }
}
