package com.xiaohua.novel.agent.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AgentStateMachineTest {

    private final AgentStateMachine stateMachine = new AgentStateMachine();

    @Test
    void supportsTheHappyPathAndRevisionLoop() {
        assertThat(stateMachine.canTransition(
                AgentRunState.CREATED,
                AgentRunState.RESEARCHING)).isTrue();
        assertThat(stateMachine.canTransition(
                AgentRunState.REVIEWING_QUALITY,
                AgentRunState.REVISING_CHAPTER)).isTrue();
        assertThat(stateMachine.canTransition(
                AgentRunState.REVISING_CHAPTER,
                AgentRunState.VALIDATING_OUTPUT)).isTrue();
        assertThat(stateMachine.canTransition(
                AgentRunState.COMMITTING_CHAPTER,
                AgentRunState.COMPLETED)).isTrue();
    }

    @Test
    void rejectsSkippingRequiredChecks() {
        assertThatThrownBy(() -> stateMachine.requireTransition(
                AgentRunState.GENERATING_CHAPTER,
                AgentRunState.COMMITTING_CHAPTER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("非法 NovelAgent 状态转换");
    }
}
