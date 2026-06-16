package com.xiaohua.novel.agent.provider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.xiaohua.novel.agent.domain.ChapterDraft;
import com.xiaohua.novel.agent.domain.ChapterPlanDraft;
import com.xiaohua.novel.agent.domain.ChapterPlanRecord;
import com.xiaohua.novel.agent.domain.CreativeProposalDraft;
import com.xiaohua.novel.agent.domain.CreativeProposalRecord;
import com.xiaohua.novel.agent.domain.FoundationDraft;
import com.xiaohua.novel.agent.domain.MarketResearchResult;
import com.xiaohua.novel.agent.domain.ReviewIssue;
import com.xiaohua.novel.agent.domain.ReviewReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.ai",
        name = "provider",
        havingValue = "fake",
        matchIfMissing = true)
public class FakeNovelModelProvider implements NovelModelProvider {

    private static final String REVISION_MARKER = "【待修订：删除测试占位表达】";
    private static final String DECISION_MARKER = "【需要决策：重要人物死亡】";

    @Override
    public String providerName() {
        return "fake";
    }

    @Override
    public String modelName() {
        return "deterministic-novel-provider-v1";
    }

    @Override
    public MarketResearchResult research(String idea, String genre) {
        String normalizedGenre = genre == null || genre.isBlank() ? "综合网文" : genre;
        return new MarketResearchResult(
                "喜欢强目标、快冲突和连续悬念的" + normalizedGenre + "读者",
                List.of(
                        "用明确的主角目标缩短读者进入故事的时间",
                        "前三章连续兑现一次能力、关系或信息差优势",
                        "每章结尾留下能直接推动下一章的问题"),
                List.of(
                        "只追逐热门标签容易造成同质化",
                        "设定过多会挤压人物行动和冲突",
                        "连续解释背景会削弱开篇留存"),
                List.of(
                        "保留用户想法的核心情绪，不照搬具体作品",
                        "用不同冲突机制生成三套真正不同的创意",
                        "先验证前三章，再扩展长篇结构"));
    }

    @Override
    public List<CreativeProposalDraft> generateProposals(
            String idea,
            String genre,
            MarketResearchResult research) {
        String seed = compactIdea(idea);
        String targetAudience = research.targetAudience();
        return List.of(
                new CreativeProposalDraft(
                        1,
                        "《" + seed + "：逆风开局》",
                        "主角在最不利的公开场合失去退路，只能利用一条被所有人忽略的规则完成第一次反击。",
                        targetAudience,
                        "稳健商业方向，开篇目标明确，前三章容易形成连续兑现。",
                        "把常见逆袭结构改造成规则推理与行动选择，不依赖单纯数值碾压。",
                        "如果规则解释过多，可能压低节奏。",
                        Map.of("direction", "稳健商业型", "emotion", "压迫后的连续反击")),
                new CreativeProposalDraft(
                        2,
                        "《" + seed + "：无人知晓的第二身份》",
                        "主角白天维持普通身份，夜晚却必须替一个失踪的人完成危险任务，两条生活线逐渐互相侵入。",
                        targetAudience,
                        "双身份能够稳定制造信息差、关系张力和章节钩子。",
                        "不是简单隐藏实力，而是让两套身份承担互相冲突的责任。",
                        "身份线管理复杂，需要严格维护人物已知信息。",
                        Map.of("direction", "差异创新型", "emotion", "秘密逼近暴露")),
                new CreativeProposalDraft(
                        3,
                        "《" + seed + "：倒计时七日》",
                        "主角发现未来七天会发生一场改变所有人的事件，但每次干预都会让另一个重要结果提前发生。",
                        targetAudience,
                        "强倒计时与选择代价适合形成高密度冲突和讨论。",
                        "每次解决问题都会制造新代价，推动人物关系而不是重复闯关。",
                        "结构风险较高，时间线必须严格校验。",
                        Map.of("direction", "高风险高爆发型", "emotion", "选择与代价")));
    }

    @Override
    public FoundationDraft initializeNovel(
            String idea,
            String genre,
            CreativeProposalRecord selectedProposal,
            String userAdjustments) {
        Map<String, Object> world = new LinkedHashMap<>();
        world.put("era", "近未来城市");
        world.put("coreRule", "每个公开选择都会改变人物关系与可获得的信息");
        world.put("forbiddenRule", "任何人不能无代价获知尚未发生的完整结果");
        world.put("tone", "紧张、克制、具有现实触感");

        List<Map<String, Object>> characters = List.of(
                mapOf(
                        "key", "protagonist",
                        "name", "林川",
                        "role", "主角",
                        "goal", "在失控局面中保护家人并找出真相",
                        "flaw", "习惯独自承担，不愿解释",
                        "currentLocation", "临江市"),
                mapOf(
                        "key", "partner",
                        "name", "顾遥",
                        "role", "关键搭档",
                        "goal", "确认主角隐瞒的信息是否会危及所有人",
                        "flaw", "过度相信证据，难以接受直觉",
                        "currentLocation", "临江市"));

        List<Map<String, Object>> relationships = List.of(
                mapOf(
                        "from", "protagonist",
                        "to", "partner",
                        "type", "互不完全信任的合作",
                        "stage", "建立共同目标"));

        Map<String, Object> mainPlot = mapOf(
                "promise", selectedProposal.premise(),
                "goal", "查清事件来源并在代价失控前完成关键选择",
                "endingDirection", "主角学会与可信任的人共同承担选择",
                "adjustments", userAdjustments == null ? "" : userAdjustments);

        List<Map<String, Object>> stages = new ArrayList<>();
        for (int stage = 1; stage <= 10; stage++) {
            stages.add(mapOf(
                    "stageNo", stage,
                    "name", "阶段" + stage,
                    "goal", "推进核心谜团并改变一次关键关系",
                    "turningPoint", "获得新证据，同时付出新的选择代价"));
        }

        List<Map<String, Object>> foreshadowing = List.of(
                mapOf(
                        "key", "broken_watch",
                        "description", "停在十一点四十七分的旧表",
                        "plantChapter", 1,
                        "plannedRevealStage", 3,
                        "status", "PLANNED"),
                mapOf(
                        "key", "missing_message",
                        "description", "被删除但仍留下发送记录的消息",
                        "plantChapter", 2,
                        "plannedRevealStage", 2,
                        "status", "PLANNED"));

        List<Map<String, Object>> facts = List.of(
                mapOf(
                        "factType", "WORLD_RULE",
                        "subject", "choice",
                        "predicate", "has_cost",
                        "value", true),
                mapOf(
                        "factType", "CHARACTER_STATE",
                        "subject", "protagonist",
                        "predicate", "location",
                        "value", "临江市"));

        return new FoundationDraft(
                selectedProposal.title(),
                world,
                characters,
                relationships,
                mainPlot,
                stages,
                foreshadowing,
                facts);
    }

    @Override
    public List<ChapterPlanDraft> planChapters(FoundationDraft foundation, int count) {
        List<ChapterPlanDraft> plans = new ArrayList<>();
        for (int chapter = 1; chapter <= count; chapter++) {
            plans.add(new ChapterPlanDraft(
                    chapter,
                    "第" + chapter + "章 选择留下的痕迹",
                    "让主角完成第" + chapter + "个可验证行动，并得到推动下一章的新信息。",
                    "主角必须在公开风险与隐藏代价之间做出选择。",
                    List.of(
                            "确认一个异常细节",
                            "与关键人物发生目标冲突",
                            "用行动换取一条新线索"),
                    List.of(mapOf(
                            "character", "protagonist",
                            "change", "对合作与隐瞒的判断发生细微变化")),
                    List.of(mapOf(
                            "key", chapter == 1 ? "broken_watch" : "missing_message",
                            "action", chapter <= 2 ? "PLANT" : "ADVANCE")),
                    "主角在看似失败时证明自己的判断有一部分正确。",
                    "新线索表明刚才的选择并不是偶然。",
                    List.of(
                            "不得无代价解决核心谜团",
                            "不得让重要人物无铺垫死亡")));
        }
        return plans;
    }

    @Override
    public ChapterDraft writeChapter(
            String originalIdea,
            FoundationDraft foundation,
            ChapterPlanRecord chapterPlan) {
        StringBuilder content = new StringBuilder();
        content.append(REVISION_MARKER).append("\n\n");
        content.append("雨从临江市旧城区的高架边缘压下来，灯光被切成一段一段。")
                .append("林川站在便利店门口，没有立刻推门。第")
                .append(chapterPlan.chapterNo())
                .append("次响起的提示音来自他已经关机的手机。\n\n");

        String[] actions = {
            "他先确认玻璃上的倒影，再观察街对面的车，没有急着追逐那个一闪而过的人影。",
            "顾遥把证据放在桌面中央，却没有替他得出结论。两个人都知道，先开口的人会暴露更多。",
            "旧表的秒针没有移动，表面却多了一道新鲜水痕，像是有人刚刚把它从雨里捞出来。",
            "林川把最危险的猜测压在心里，只说出能够被验证的那一半，并要求对方给他十分钟。",
            "门外传来脚步声时，他没有躲，而是故意让来人看见自己手中的纸条。",
            "线索并没有直接给出答案，它只证明此前被忽略的时间差真实存在。",
            "顾遥第一次没有追问原因。她把出口让开，却提醒他，这次选择会留下无法撤回的记录。",
            "林川意识到真正的危险不是被发现，而是有人正在计算他每一次犹豫需要付出的代价。"
        };
        for (int index = 0; index < 58; index++) {
            content.append(actions[index % actions.length])
                    .append("这是第")
                    .append(index + 1)
                    .append("个被确认的细节。");
            if (index % 2 == 1) {
                content.append("\n\n");
            }
        }
        content.append("他推开门前回头看了一眼。街对面的电子屏正好跳到十一点四十七分，")
                .append("而那块停摆多年的旧表，在口袋里轻轻响了一声。\n");

        if (originalIdea.contains("重要人物死亡") && chapterPlan.chapterNo() == 2) {
            content.append("\n").append(DECISION_MARKER);
        }

        Map<String, Object> metadata = mapOf(
                "events", List.of("发现异常提示", "与顾遥交换有限信息", "确认时间差线索"),
                "characterChanges", List.of(mapOf(
                        "character", "protagonist",
                        "change", "开始接受有限合作")),
                "foreshadowing", List.of(mapOf(
                        "key", chapterPlan.chapterNo() == 1 ? "broken_watch" : "missing_message",
                        "action", chapterPlan.chapterNo() <= 2 ? "PLANT" : "ADVANCE")),
                "timeline", mapOf("day", 1, "time", "night"),
                "location", "临江市旧城区");

        return new ChapterDraft(
                chapterPlan.chapterNo(),
                chapterPlan.title(),
                content.toString(),
                "林川通过一次有代价的行动确认异常线索，并与顾遥建立有限合作。",
                metadata,
                0);
    }

    @Override
    public ReviewReport reviewQuality(ChapterDraft draft) {
        if (draft.content().contains(REVISION_MARKER)) {
            return new ReviewReport(
                    false,
                    List.of(new ReviewIssue(
                            "PLACEHOLDER_EXPRESSION",
                            "MEDIUM",
                            "正文包含测试占位表达，需要自动删除并重新检查。",
                            true)));
        }
        return ReviewReport.success();
    }

    @Override
    public ReviewReport reviewContinuity(ChapterDraft draft) {
        if (draft.content().contains(DECISION_MARKER)) {
            return new ReviewReport(
                    false,
                    List.of(new ReviewIssue(
                            "MAJOR_CHARACTER_FATE",
                            "BLOCKER",
                            "草稿试图让重要人物死亡，超出自动决策边界。",
                            false)));
        }
        return ReviewReport.success();
    }

    @Override
    public ChapterDraft revise(ChapterDraft draft, ReviewReport report) {
        String revised = draft.content().replace(REVISION_MARKER + "\n\n", "");
        return draft.revised(revised, draft.revisionNo() + 1);
    }

    private static String compactIdea(String idea) {
        String compact = idea.replaceAll("\\s+", "");
        return compact.substring(0, Math.min(compact.length(), 8));
    }

    private static Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
