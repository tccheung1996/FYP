package com.example.asus.fyp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;

import ai.api.AIListener;
import ai.api.android.AIConfiguration;
import ai.api.android.AIService;
import ai.api.model.AIError;
import android.hardware.ConsumerIrManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import com.google.gson.JsonElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends Activity implements AIListener{

    private static TextToSpeech textToSpeech;
    private final static String CMD_TV_POWER =
            "0000 006c 0000 0020 000a 0046 000a 001e 000a 001e 000a 001e 000a 001e 000a 001e 000a 0046 000a 0046 000a 001e 000a 0046 000a 001e 000a 001e 000a 001e 000a 0046 000a 001e 000a 06d7 000a 0046 000a 001e 000a 001e 000a 001e 000a 001e 000a 0046 000a 001e 000a 001e 000a 0046 000a 001e 000a 0046 000a 0046 000a 0046 000a 001e 000a 0046 000a 06d7";
    private final static String CMD_TV_ADD =
            "0000 006d 0022 0003 00a9 00a8 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 003f 0015 003f 0015 0702 00a9 00a8 0015 0015 0015 0e6e";
    private final static String CMD_TV_GET =
            "0000 006d 0022 0003 00a9 00a8 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 003f 0015 0015 0015 003f 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 0015 003f 0015 0015 0015 003f 0015 003f 0015 003f 0015 003f 0015 0702 00a9 00a8 0015 0015 0015 0e6e";

    private ConsumerIrManager irManager;



    private Button listenButton;
    private TextView resultTextView;
    private AIService aiService;
    private boolean isTvOpen = false;
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        irManager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);

        listenButton = (Button) findViewById(R.id.listenButton);
        resultTextView = findViewById(R.id.resultTextView);
        final AIConfiguration config = new AIConfiguration("e399e82324ad49139934e931eb67e829",
                AIConfiguration.SupportedLanguages.ChineseHongKong,
                AIConfiguration.RecognitionEngine.System);
        aiService = AIService.getService(this, config);
        aiService.setListener(this);

    }
    @Override
    protected void onStart() {
        super.onStart();
        // The activity is about to become visible.
    }
    public void listenButtonOnClick(final View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            aiService.startListening();
            Log.d("check1", "onResult:listening ");
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.RECORD_AUDIO)) {

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO_PERMISSION);
            Log.d("check", "onResult: ");
        }
    }
    public void testButtonOnClick(final View view) {
        init(this);

        speak("sadasd");
    }






    @Override
    public void onResult(final AIResponse response) {
        Result result = response.getResult();

        // Get parameters
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }

        // Show results in TextView.
        resultTextView.setText("Query:" + result.getResolvedQuery() +
                "\nAction: " + result.getAction() +
                "\nResponse: " + result.getFulfillment().getSpeech()
        +"\nparameter" + result.getParameters());

        if(result.getParameters().get("tv")!=null && result.getParameters().get("action") != null){
            IRCommand cmd = hex2ir(CMD_TV_POWER);

            android.util.Log.d("Remote", "frequency: " + cmd.freq);
            android.util.Log.d("Remote", "pattern: " + Arrays.toString(cmd.pattern));
            irManager.transmit(cmd.freq, cmd.pattern);
        }
    }

    private IRCommand hex2ir(final String irData) {
        List<String> list = new ArrayList<String>(Arrays.asList(irData.split(" ")));
        list.remove(0); // dummy
        int frequency = Integer.parseInt(list.remove(0), 16); // frequency
        list.remove(0); // seq1
        list.remove(0); // seq2

        frequency = (int) (1000000 / (frequency * 0.241246));
        int pulses = 1000000 / frequency;
        int count;

        int[] pattern = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {
            count = Integer.parseInt(list.get(i), 16);
            pattern[i] = count * pulses;
        }

        return new IRCommand(/*frequency*/38400, pattern);
    }

    private class IRCommand {
        private final int freq;
        private final int[] pattern;

        private IRCommand(int freq, int[] pattern) {
            this.freq = freq;
            this.pattern = pattern;
        }
    }

    public static void init(final Context context) {
        if (textToSpeech == null) {

            textToSpeech = new TextToSpeech(context, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int i) {

                }
            });
        }
    }

    public static void speak(final String text) {

        textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null);
    }


    @Override
    public void onError(AIError error) {
        resultTextView.setText(error.toString());
        Log.d("check", "onResult: error");
    }
    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }

}
