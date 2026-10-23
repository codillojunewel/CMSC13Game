package com.cmsc13.model.lifeline;

/**
 * What happened when a lifeline was used.
 *
 * @param message      line shown in the proctor's speech bubble
 * @param revealedIndex the choice index the lifeline points at, or -1
 * @param autoCommit   true when the revealed choice must be locked in immediately
 */
public record LifelineResult(String message, int revealedIndex, boolean autoCommit) {
}
