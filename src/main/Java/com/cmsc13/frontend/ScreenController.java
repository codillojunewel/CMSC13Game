package com.cmsc13.frontend;

/**
 * Optional lifecycle hooks for screen controllers. {@link SceneManager} calls
 * {@link #onShow()} after a screen appears and {@link #onHide()} before it is
 * replaced, so screens can start and stop their timers, animations and cameras.
 */
public interface ScreenController {

    default void onShow() {
    }

    default void onHide() {
    }
}
