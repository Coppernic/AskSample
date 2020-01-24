package fr.coppernic.samples.ask;

import android.content.Context;
import android.os.SystemClock;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.junit.Assert.*;

/**
 * Instrumentation test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class HuntInstrumentedTest {

    @Rule
    public ActivityTestRule<OldActivity> activityRule = new ActivityTestRule<>(OldActivity.class);

    @Test
    public void useAppContext() throws Exception {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getTargetContext();

        assertEquals("fr.coppernic.samples.ask", appContext.getPackageName());
    }

    @Before
    public void before() {
        clickOnPower();
        SystemClock.sleep(2000);
        clickOnOpen();
        SystemClock.sleep(2000);
        clickOnFirmwareVersion();
        SystemClock.sleep(1000);
    }

    @After
    public void after() {
        clickOnPower();
    }

    @Test
    public void simpleHuntTest() {
        clickOnEnterHuntPhase();
        SystemClock.sleep(1000);
        String result = getText(ViewMatchers.withId(R.id.tvAtrValue));

        assertTrue(result.length() > 0);
    }

    @Test
    public void stabilityTest() {
        clickOnEnterHuntPhase();
        SystemClock.sleep(1000);
        String initialValue = getText(ViewMatchers.withId(R.id.tvAtrValue));
        assertTrue(initialValue.length() > 0);

        for (int i = 0; i < 10000; i++) {
            clickOnEnterHuntPhase();
            SystemClock.sleep(500);
            String result = getText(ViewMatchers.withId(R.id.tvAtrValue));
            assertTrue(result.compareTo(initialValue) == 0);
        }
    }

    private void clickOnPower() {
        onView(withId(R.id.swPower)).perform(click());
    }

    private void clickOnOpen() {
        onView(withId(R.id.swOpen)).perform(click());
    }

    private void clickOnFirmwareVersion() {
        onView(withId(R.id.btnFwVersion)).perform(click());
    }

    private void clickOnEnterHuntPhase() {
        onView(withId(R.id.swCardDetection)).perform(click());
    }

    public static String getText(final Matcher<View> matcher) {
        final String[] stringHolder = { null };
        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "getting text from a TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView)view; //Save, because of check in getConstraints()
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }
}
