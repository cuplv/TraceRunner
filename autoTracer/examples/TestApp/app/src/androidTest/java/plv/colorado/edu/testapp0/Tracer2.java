package plv.colorado.edu.testapp0;

/**
 * Created by edmund on 11/23/16.
 */


import com.robotium.solo.Solo;

import android.app.Instrumentation;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

@RunWith (AndroidJUnit4.class)
public class Tracer2 {

    @Rule
    public ActivityTestRule<MainActivity> activityTestRule =
            new ActivityTestRule<>(MainActivity.class);

    private Solo solo;

    @Before
    public void setUp() throws Exception {
        //setUp() is run before a test case is started.
        //This is where the solo object is created.
        solo = new Solo(InstrumentationRegistry.getInstrumentation(),
                activityTestRule.getActivity());
    }


    @After
    public void tearDown() throws Exception {
        //tearDown() is run after a test case has finished.
        //finishOpenedActivities() will finish all the activities that have been opened during the test execution.

        solo.finishOpenedActivities();

    }

    @Test
    public void testButton() throws Exception {

        solo.clickOnView( solo.getView( R.id.button ) );

        solo.setActivityOrientation(Solo.LANDSCAPE);

        solo.clickOnView( solo.getView( R.id.button ) );

    }

}
