package impl.common;


public interface MatchingStrategy {

    /**
     * @param weighted whether the score should be weighted by function size
     * @param symmetric whether to average the score with the inverse direction
     */
    SimilarityResult match(MatchingContext context, boolean weighted, boolean symmetric);
}
