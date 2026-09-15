package jp.bunkaich.sukashimotion;

import static org.junit.Assert.*;

import java.util.regex.Matcher;
import org.junit.Test;

public class AngleLogTest {
    private static Matcher match(String line){Matcher m=AngleLog.LINE.matcher(line);return m.find()?m:null;}

    @Test public void fold7BracketFormat(){
        Matcher m=match("1789488061.123  1234  5678 I SprWallpaper|FoldInteractive: onCommand: action[jp.bunkaich.sukashimotion.READ_ANGLE], mCurrentAngle[123.5], isVisible[true]");
        assertNotNull(m);assertEquals("1789488061.123",m.group(1));assertEquals("123.5",m.group(2));
    }
    @Test public void fold8EqualsFormat(){
        Matcher m=match("1789488061.123  1234  5678 I SprWallpaper|FoldInteractive: onCommand: action=jp.bunkaich.sukashimotion.READ_ANGLE, mCurrentAngle=87.25, isVisible=true");
        assertNotNull(m);assertEquals("87.25",m.group(2));
    }
    @Test public void fold8SpacedEqualsFormat(){
        assertNotNull(match("1789488061.123  1234  5678 I SprWallpaper|FoldInteractive: onCommand: action = jp.bunkaich.sukashimotion.READ_ANGLE, mCurrentAngle=0.0, isVisible=true"));
    }
    @Test public void hiddenWallpaperIgnored(){
        assertNull(match("1789488061.123  1234  5678 I SprWallpaper|FoldInteractive: onCommand: action=jp.bunkaich.sukashimotion.READ_ANGLE, mCurrentAngle=87.25, isVisible=false"));
        assertNull(match("1789488061.123  1234  5678 I SprWallpaper|FoldInteractive: onCommand: action[jp.bunkaich.sukashimotion.READ_ANGLE], mCurrentAngle[87.25], isVisible[false]"));
    }
    @Test public void otherActionIgnored(){
        assertNull(match("1789488061.123  1234  5678 I SprWallpaper|FoldInteractive: onCommand: action=android.wallpaper.tap, mCurrentAngle=87.25, isVisible=true"));
    }
}
