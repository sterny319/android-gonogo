package org.ohmage.pulsus;

import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.joda.time.DateTime;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.Random;
import java.util.Timer;

import io.smalldatalab.omhclient.DSUDataPoint;
import io.smalldatalab.omhclient.DSUDataPointBuilder;

public class SquareTask extends AppCompatActivity {

    // Layout
    Button startTaskButton;
    TextView instructionsTextView;
    View squareView;
    View plusSign;
    RelativeLayout mainLayout;
    TextView feedbackLabel;

    // Flags
    Boolean testInProgress = Boolean.FALSE;
    Boolean shouldTap = Boolean.FALSE;
    int currentIndex = 0;

    // Testing Variables
    int TOTAL_LOOPS = 75;
    Chronometer chronometer;
    long[] responseTimes = new long[TOTAL_LOOPS];
    Boolean[] correctnessArray = new Boolean[TOTAL_LOOPS];
    Boolean[] goCuesArray = new Boolean[TOTAL_LOOPS];

    // Durations
    int DURATION_FIXATION_CROSS = 300;
    int DURATION_BLANK_SCREEN = 400;
    int DURATION_TARGET_ON_SCREEN = 600;
    int DURATION_WAIT_LAP = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_square_task);

        findViews();

        // Bold part of instructions
        instructionsTextView.setText(Html.fromHtml("Welcome to the Square Task.<br/><br/><br/>Once you start, you will be presented with a rectangle. When the rectangle turns <b>green, tap anywhere on the screen as quickly as possible</b>. When it turns <b>blue, do not respond at all</b>. <br/><br/>The test will take approximately <b>3 min</b>."));

        // OnClick Listeners
        startTaskButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSquareTask();
            }
        });
    }

    private void findViews() {
        startTaskButton = (Button) findViewById(R.id.start_square_task_button);
        instructionsTextView = (TextView) findViewById(R.id.square_task_instructions);
        squareView = findViewById(R.id.game_view);
        plusSign = findViewById(R.id.plus_image);
        mainLayout = (RelativeLayout) findViewById(R.id.square_task_main_layout);
        feedbackLabel = (TextView) findViewById(R.id.feedback_label);
    }

    private void startSquareTask() {
        // Hide instructions
        instructionsTextView.animate().alpha(0.0f);

        // Hide Start Button
        startTaskButton.animate().alpha(0.0f).withEndAction(new Runnable() {
            @Override
            public void run() {

                startTaskButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tappedSquare();
                    }
                });

                mainLayout.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        tappedSquare();
                    }
                });

                chronometer = new Chronometer(getBaseContext());
                loop();
            }
        });
    }

    private void loop() {
        // Show plus sign for 800 ms
        plusSign.setAlpha(1.0f);

        // Hide plus sign
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                plusSign.setAlpha(0.0f);

                // Show empty square after 500ms
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {

                        final Boolean horizontal = (new Random().nextInt(2) == 0) ? Boolean.TRUE : Boolean.FALSE;
                        squareView.setBackground(ContextCompat.getDrawable(getBaseContext(), R.drawable.no_color_background));
                        squareView.setAlpha(1.0f);

                        // Rotate square to horizontal position
                        if (horizontal)
                            squareView.setRotation(90);

                        // Then show color after 550ms
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                // Randomly select between valid and invalid color
                                int i = new Random().nextInt(100);
                                int drawable;

                                // GO
                                if ((i > 30 && horizontal) || (!horizontal && i < 30)) {
                                    drawable = R.drawable.valid_color_background;

                                    goCuesArray[currentIndex] = Boolean.TRUE;
                                    shouldTap = Boolean.TRUE;

                                    correctnessArray[currentIndex] = Boolean.FALSE; // Assume incorrect (tapped)
                                    responseTimes[currentIndex] = 0;

                                // Should NOT Tap
                                } else {
                                    drawable = R.drawable.invalid_color_background;

                                    goCuesArray[currentIndex] = Boolean.FALSE;
                                    shouldTap = Boolean.FALSE;

                                    correctnessArray[currentIndex] = Boolean.TRUE; // Assume correct (not tapped)
                                    responseTimes[currentIndex] = 0;
                                }
                                squareView.setBackground(ContextCompat.getDrawable(getBaseContext(), drawable));

                                // Start chronometer
                                chronometer.setBase(SystemClock.elapsedRealtime());
                                chronometer.start();

                                // Allow for tapping screen
                                testInProgress = Boolean.TRUE;

                                // Finally, hide after 1000ms
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        squareView.setAlpha(0.0f);

                                        // Set square back to vertical position
                                        if (horizontal)
                                            squareView.setRotation(0);

                                        testInProgress = Boolean.FALSE;

                                        new Handler().postDelayed(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (currentIndex < (TOTAL_LOOPS - 1)) {
                                                    currentIndex++;
                                                    loop();
                                                } else {
                                                    mainLayout.setOnClickListener(null);
                                                    showResults();
                                                }
                                            }
                                        }, DURATION_WAIT_LAP);
                                    }
                                }, DURATION_TARGET_ON_SCREEN);
                            }
                        }, 550);
                    }
                }, DURATION_BLANK_SCREEN);
            }
        }, DURATION_FIXATION_CROSS);
    }

    private void tappedSquare() {

        // No test going on now, ignore taps
        if (!testInProgress) {
            return;
        }

        // Valid
        if (shouldTap) {
            long time = SystemClock.elapsedRealtime() - chronometer.getBase();

            feedbackLabel.setText("Correct! "+time+" ms");
            feedbackLabel.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.validColor));

            // Record event
            correctnessArray[currentIndex] = Boolean.TRUE;
            responseTimes[currentIndex] = time;

        // Invalid
        } else {

            feedbackLabel.setText("Incorrect!");
            feedbackLabel.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.invalidColor));

            correctnessArray[currentIndex] = Boolean.FALSE;
        }

        // Show feedback label
        feedbackLabel.animate().alpha(1.f);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                feedbackLabel.animate().setDuration(100).alpha(0.f);
            }
        }, 600);

        testInProgress = Boolean.FALSE;
        chronometer.stop();
    }

    private void showResults() {

        // Count number of right answers
        int numRightAnswers = 0;
        for (int i=0; i<TOTAL_LOOPS; i++) {
            if (correctnessArray[i] == Boolean.TRUE)
                numRightAnswers++;
        }
        int numWrongAnswers = TOTAL_LOOPS - numRightAnswers;

        // Compute average response time
        double occurrences, total;
        occurrences = total = 0.0;
        for (int i=0; i<TOTAL_LOOPS; i++) {
            if (responseTimes[i] > 0) {
                total += responseTimes[i];
                occurrences++;
            }
        }
        int averageResponseTime = (int) (total / occurrences);

        // Make results string
        String results = "";
        results += "Correct Answers: " + numRightAnswers + "\n\n";
        results += "Incorrect Answers: " + numWrongAnswers + "\n\n";
        if (occurrences > 0) {
            results += "Mean Response Time: " + averageResponseTime + " msec" + "\n\n";
        }
        results += "Number of Commissions (hit when should not): " + numberOfCommissions() + "\n\n";
        results += "Number of Omissions (not hit when should): " + numberOfOmissions();

        // Display results in instructions textview
        instructionsTextView.setText(results);
        instructionsTextView.setTextSize(17);
        instructionsTextView.setGravity(Gravity.CENTER_VERTICAL);
        instructionsTextView.animate().alpha(1.f);

        submitResults(numRightAnswers, numWrongAnswers, averageResponseTime);
    }


    private int numberOfCommissions() {
        int commissions = 0;
        for (int i = 0; i < goCuesArray.length; i++) {
            Boolean cue = goCuesArray[i];
            Boolean correct = correctnessArray[i];
            if (!cue && !correct) {
                commissions++;
            }
        }
        return commissions;
    }

    private int numberOfOmissions() {
        int omissions = 0;
        for (int i = 0; i < goCuesArray.length; i++) {
            Boolean cue = goCuesArray[i];
            Boolean correct = correctnessArray[i];
            if (cue && !correct) {
                omissions++;
            }
        }
        return omissions;
    }

    private void submitResults(int correctResponses, int incorrectResponses, int meanResponseTime) {

        try {
            DateTime dt = new DateTime();

            JSONObject obj = new JSONObject();
            obj.put("variable_label", "Go-no-go");
            obj.put("number_of_trials", TOTAL_LOOPS);
            obj.put("correct_responses", correctResponses);
            obj.put("incorrect_responses", incorrectResponses);
            obj.put("commissions", numberOfCommissions());
            obj.put("omissions", numberOfOmissions());
            obj.put("response_time_mean", meanResponseTime);
            obj.put("correctness_array", correctnessArray);
            obj.put("response_times_array", responseTimes);

            DSUDataPoint dataPoint = new DSUDataPointBuilder()
                    .setSchemaNamespace(getString(R.string.schema_namespace))
                    .setSchemaName(getString(R.string.square_task_schema_name))
                    .setSchemaVersion(getString(R.string.schema_version))
                    .setAcquisitionModality(getString(R.string.acquisition_modality))
                    .setAcquisitionSource(getString(R.string.acquisition_source_name))
                    .setCreationDateTime(dt.toGregorianCalendar())
                    .setBody(obj).createDSUDataPoint();
            dataPoint.save();

        } catch (Exception e) {
            Toast.makeText(SquareTask.this, "Submission failed. Please contact study coordinator", Toast.LENGTH_LONG).show();
        }
    }
}
