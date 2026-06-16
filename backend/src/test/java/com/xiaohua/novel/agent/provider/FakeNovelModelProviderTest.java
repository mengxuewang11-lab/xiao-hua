package com.xiaohua.novel.agent.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.xiaohua.novel.agent.domain.ChapterDraft;
import com.xiaohua.novel.agent.domain.ChapterPlanRecord;
import com.xiaohua.novel.agent.domain.CreativeProposalRecord;
import com.xiaohua.novel.agent.domain.FoundationDraft;
import com.xiaohua.novel.agent.domain.MarketResearchResult;
import com.xiaohua.novel.agent.domain.ReviewReport;
import org.junit.jupiter.api.Test;

class FakeNovelModelProviderTest {

    private final FakeNovelModelProvider provider = new FakeNovelModelProvider();

    @Test
    void createsThreeDifferentProposalsAndTenChapterPlans() {
        MarketResearchResult research = provider.research("旧物能保存记忆", "都市悬疑");
        var proposals = provider.generateProposals("旧物能保存记忆", "都市悬疑", research);

        assertThat(proposals).hasSize(3);
        assertThat(proposals).extracting(proposal -> proposal.title()).doesNotHaveDuplicates();

        CreativeProposalRecord selected = new CreativeProposalRecord(
                "proposal-1",
                "run-1",
                "project-1",
                proposals.get(0).proposalNo(),
                proposals.get(0).title(),
                proposals.get(0).premise(),
                proposals.get(0).targetAudience(),
                proposals.get(0).marketRationale(),
                proposals.get(0).differentiator(),
                proposals.get(0).riskSummary(),
                true);
        FoundationDraft foundation = provider.initializeNovel(
                "旧物能保存记忆",
                "都市悬疑",
                selected,
                null);

        assertThat(foundation.stages()).hasSize(10);
        assertThat(provider.planChapters(foundation, 10)).hasSize(10);
    }

    @Test
    void automaticallyRepairsTheKnownQualityIssue() {
        CreativeProposalRecord selected = proposal();
        FoundationDraft foundation = provider.initializeNovel(
                "旧物能保存记忆",
                "都市悬疑",
                selected,
                null);
        ChapterPlanRecord plan = new ChapterPlanRecord(
                "plan-1",
                "project-1",
                1,
                "第一章",
                "确认异常",
                "公开风险与隐藏代价",
                "证明判断",
                "旧表响起");

        ChapterDraft firstDraft = provider.writeChapter(
                "旧物能保存记忆",
                foundation,
                plan);
        ReviewReport firstReview = provider.reviewQuality(firstDraft);
        ChapterDraft revised = provider.revise(firstDraft, firstReview);

        assertThat(firstReview.passed()).isFalse();
        assertThat(firstReview.issues()).allMatch(issue -> issue.autoFixable());
        assertThat(provider.reviewQuality(revised).passed()).isTrue();
        assertThat(revised.revisionNo()).isEqualTo(1);
    }

    @Test
    void majorCharacterDeathRequiresAUserDecision() {
        CreativeProposalRecord selected = proposal();
        FoundationDraft foundation = provider.initializeNovel(
                "故事中重要人物死亡",
                "都市悬疑",
                selected,
                null);
        ChapterPlanRecord plan = new ChapterPlanRecord(
                "plan-2",
                "project-1",
                2,
                "第二章",
                "确认异常",
                "公开风险与隐藏代价",
                "证明判断",
                "旧表响起");

        ChapterDraft draft = provider.writeChapter(
                "故事中重要人物死亡",
                foundation,
                plan);
        draft = provider.revise(draft, provider.reviewQuality(draft));
        ReviewReport continuity = provider.reviewContinuity(draft);

        assertThat(continuity.passed()).isFalse();
        assertThat(continuity.requiresUserDecision()).isTrue();
        assertThat(continuity.issues()).extracting(issue -> issue.code())
                .containsExactly("MAJOR_CHARACTER_FATE");
    }

    private CreativeProposalRecord proposal() {
        return new CreativeProposalRecord(
                "proposal-1",
                "run-1",
                "project-1",
                1,
                "测试小说",
                "测试设定",
                "目标读者",
                "市场依据",
                "差异化",
                "风险",
                true);
    }
}
