package jp.bunkaich.sukashimotion;

import android.os.Build;
import java.util.Set;

/** Models allowed to use Samsung display control. SM-F9710 (Fold8, China, One UI 9) is unverified. */
final class DeviceSupport {
    static final Set<String> MODELS=Set.of("SM-F966Z","SM-F9710");
    private DeviceSupport(){}
    static boolean supported(){return MODELS.contains(Build.MODEL);}
    /** Fold8 runs a different launcher activity on each display, so HOME tasks cannot be reparented between them. */
    static boolean separateHomes(){return "SM-F9710".equals(Build.MODEL);}
}
