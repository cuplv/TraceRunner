package plv.colorado.edu.mediaplayerexample;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, MediaPlayer.OnPreparedListener {
    MediaPlayer m = new MediaPlayer();
    Button b;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        Button bt = new Button(this);
//        bt.setText("A Button");
//        bt.setLayoutParams(new LayoutParams(LayoutParams.FILL_PARENT,
//                LayoutParams.WRAP_CONTENT));
//        linerLayout.addView(bt);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewGroup layout = (ViewGroup)findViewById(R.id.activity_main);
        b = new Button(this);
        b.setText("play");
        b.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        layout.addView(b);
        b.setOnClickListener(this);
        m.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try {
            m.setDataSource("http://csel.cs.colorado.edu/~krsr8608/panjatheme.mp3");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        m.setOnPreparedListener(this);
        m.prepareAsync();
        //b.setEnabled(false);

    }

    @Override
    public void onClick(View view) {
        m.start();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        //b.setEnabled(true);
    }
//    @Override
//    protected void onPause(){
//        super.onPause();
//    }
    public void logEach(String[] strings){
        for(int i=0; i< strings.length; ++i){
            Log.i("...",strings[i]);
        }
    }
//    @Override
//    public void addContentView(View view, ViewGroup.LayoutParams params) {
//        super.addContentView(view, params);
//    }

}
