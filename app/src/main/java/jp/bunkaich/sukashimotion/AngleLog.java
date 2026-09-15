package jp.bunkaich.sukashimotion;

import java.util.regex.Pattern;

/** Samsung FoldInteractive wallpaper log line carrying the fine hinge angle, read via "logcat -v epoch". */
final class AngleLog {
    // Fold7 logs "action[x], mCurrentAngle[y], isVisible[true]"; Fold8 SM-F9710 wallpaper strings use "action=x, mCurrentAngle=y" (unverified at runtime).
    // Group 1: epoch seconds. Group 2: angle in degrees.
    static final Pattern LINE=Pattern.compile("^\\s*([0-9.]+)\\s+\\d+\\s+\\d+\\s+I\\s+SprWallpaper\\|FoldInteractive:\\s+onCommand: action(?:\\[| ?= ?)jp\\.bunkaich\\.sukashimotion\\.READ_ANGLE\\]?, mCurrentAngle(?:\\[|=)([0-9.]+)\\]?, isVisible(?:\\[|=)true\\]?");
    private AngleLog(){}
}
