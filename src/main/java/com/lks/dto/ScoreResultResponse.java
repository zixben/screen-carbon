package com.lks.dto;

import com.lks.bean.Score;

public record ScoreResultResponse(
        Integer id,
        Integer vId,
        String videoType,
        String score
) {
    public static ScoreResultResponse from(Score score) {
        return new ScoreResultResponse(
                score.getId(),
                score.getvId(),
                score.getVideoType(),
                score.getScore()
        );
    }
}
