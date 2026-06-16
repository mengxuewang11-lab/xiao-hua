package com.xiaohua.novel.agent.provider;

import java.util.List;

import com.xiaohua.novel.agent.domain.ChapterDraft;
import com.xiaohua.novel.agent.domain.ChapterPlanDraft;
import com.xiaohua.novel.agent.domain.ChapterPlanRecord;
import com.xiaohua.novel.agent.domain.CreativeProposalDraft;
import com.xiaohua.novel.agent.domain.CreativeProposalRecord;
import com.xiaohua.novel.agent.domain.FoundationDraft;
import com.xiaohua.novel.agent.domain.MarketResearchResult;
import com.xiaohua.novel.agent.domain.ReviewReport;

public interface NovelModelProvider {

    String providerName();

    String modelName();

    MarketResearchResult research(String idea, String genre);

    List<CreativeProposalDraft> generateProposals(
            String idea,
            String genre,
            MarketResearchResult research);

    FoundationDraft initializeNovel(
            String idea,
            String genre,
            CreativeProposalRecord selectedProposal,
            String userAdjustments);

    List<ChapterPlanDraft> planChapters(FoundationDraft foundation, int count);

    ChapterDraft writeChapter(
            String originalIdea,
            FoundationDraft foundation,
            ChapterPlanRecord chapterPlan);

    ReviewReport reviewQuality(ChapterDraft draft);

    ReviewReport reviewContinuity(ChapterDraft draft);

    ChapterDraft revise(ChapterDraft draft, ReviewReport report);
}
