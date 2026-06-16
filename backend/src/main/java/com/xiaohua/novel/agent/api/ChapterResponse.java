package com.xiaohua.novel.agent.api;

import com.xiaohua.novel.agent.domain.ChapterRecord;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "已经通过检查并正式提交的小说章节")
public record ChapterResponse(
        @Schema(description = "章节ID")
        String id,
        @Schema(description = "章节序号", example = "1")
        int chapterNo,
        @Schema(description = "章节标题")
        String title,
        @Schema(description = "正式章节正文")
        String content,
        @Schema(description = "章节摘要")
        String summary,
        @Schema(description = "非空白字符数量", example = "3000")
        int wordCount,
        @Schema(description = "章节版本号", example = "1")
        int versionNo) {

    public static ChapterResponse from(ChapterRecord chapter) {
        return new ChapterResponse(
                chapter.id(),
                chapter.chapterNo(),
                chapter.title(),
                chapter.content(),
                chapter.summary(),
                chapter.wordCount(),
                chapter.versionNo());
    }
}
