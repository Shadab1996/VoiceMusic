package com.team3amk.voicemusic;

import android.app.Activity;

/**
 * Created by Funkies PC on 06-May-17.
 */

    import android.app.ListActivity;
    import android.content.CursorLoader;
    import android.content.Intent;
    import android.content.IntentFilter;
    import android.database.Cursor;
    import android.media.MediaPlayer;
    import android.net.Uri;
    import android.os.Bundle;
    import android.os.Environment;
    import android.os.Handler;
    import android.provider.MediaStore;
    import android.speech.RecognizerIntent;
    import android.view.View;
    import android.widget.CursorAdapter;
    import android.widget.ListView;
    import android.widget.SeekBar;
    import android.widget.SimpleCursorAdapter;
    import android.widget.TextView;
    import android.widget.Toast;

import java.io.File;
import java.util.HashMap;
    import java.util.List;

    /**
     * Created by Bhupendra on 25-04-2017.
     */

    public class VoiceActivity extends ListActivity implements SMSReciever.OnSmsReceivedListener {
        private static final int SPEECH_REQUEST_CODE = 0;
        MediaPlayer mp = null;
        String path;
        HashMap<String, String> songsMap = null;

        //26-Apr-2017 - To receive SMS
        private SMSReciever smsReceiver;
        //5-May-2017
        SeekBar sb;
        Handler seekHandler = new Handler();
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_voice);
            sb=(SeekBar)findViewById(R.id.seek_bar);
            smsReceiver = new SMSReciever();
            smsReceiver.setOnSmsReceivedListener(this);
            try {
                path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC).getAbsolutePath();
                Uri allcontacts = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

                String[] projection = {
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.ARTIST,
                        MediaStore.Audio.Media.TITLE,
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.DURATION
                };
                //OR
                //Uri allcontacts1=Uri.parse("content://contacts/people");
                Cursor c;
                CursorLoader loader = new CursorLoader(this, allcontacts, projection, selection, null, null);

                c = loader.loadInBackground();

                String colums[] = new String[]{MediaStore.Audio.Media.DISPLAY_NAME, MediaStore.Audio.Media._ID};
                int[] views = new int[]{R.id.displayName, R.id.mediaId};
                SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.activity_song_row_layout, c, colums, views, CursorAdapter.FLAG_REGISTER_CONTENT_OBSERVER);
                this.setListAdapter(adapter);
                //Loading Songs into Map
                c.moveToFirst();
                songsMap = new HashMap<>();
                int displaynameindex = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME);
                String displayname, soundexname;
                //  String msg = "Data Is \n";
                do {
                    displayname = c.getString(displaynameindex);
                    soundexname = Soundex.soundex(displayname.replace(".mp3", ""));
                    songsMap.put(soundexname, displayname);
                    //    msg += soundexname + "-" + displayname + "\n";
                }
                while (c.moveToNext());
                // Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(this, "OnCreate " + e.toString(), Toast.LENGTH_LONG).show();
            }
        }

        @Override
        protected void onListItemClick(ListView l, View v, int position, long id) {
            TextView tv = (TextView) v.findViewById(R.id.displayName);
            String filename = tv.getText().toString();
            if (mp != null && mp.isPlaying())
                mp.stop();
           mp = MediaPlayer.create(getBaseContext(), Uri.parse(path + "/" + filename));
          //  mp = MediaPlayer.create(getBaseContext(), Uri.fromFile(new File(Environment.getExternalStorageDirectory(), filename)));

            sb.setMax(mp.getDuration());
            mp.start();
            seekUpdation();


        }
        //5-May-2017 Runnable interface to update seekbar
        Runnable run = new Runnable() {

            @Override
            public void run() {
                seekUpdation();
            }
        };
        public void seekUpdation() {

            sb.setProgress(mp.getCurrentPosition());
            seekHandler.postDelayed(run, 1000);
        }







        // Create an intent that can start the Speech Recognizer activity
        public void onClick(View v) {
            if (mp != null && mp.isPlaying())
                mp.stop();
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
// Start the activity, the intent will be populated with the speech text
            startActivityForResult(intent, SPEECH_REQUEST_CODE);
        }

        // This callback is invoked when the Speech Recognizer returns.
// This is where you process the intent and extract the speech text from the intent.
        @Override
        protected void onActivityResult(int requestCode, int resultCode,
                                        Intent data) {

            if (requestCode == SPEECH_REQUEST_CODE && resultCode == RESULT_OK) {
                List<String> results = data.getStringArrayListExtra(
                        RecognizerIntent.EXTRA_RESULTS);
                //String spokenText = results.get(0);
                StringBuilder sb = new StringBuilder(1024);
                boolean found = false;
                //  sb.append("Words Detected are\n");
                for (String i : results) {
                    //sb.append(i +"\n");
                    sb.append(i);
                    String key = Soundex.soundex(i);
                    if (songsMap.containsKey(key)) {
                        if (mp != null && mp.isPlaying())
                            mp.stop();

                        mp = MediaPlayer.create(getBaseContext(), Uri.parse(path + "/" + songsMap.get(key)));
                        mp.start();
                        seekUpdation();

                        found = true;
                        break;
                    }


                }
                if (!found) {
                    Toast.makeText(this, "Song Not Found For Given Voice", Toast.LENGTH_SHORT).show();
                    return;
                    //Button btn=(Button)findViewById(R.id.btnVoice);
                    // btn.performClick();
                }
                String str = sb.toString().toLowerCase();
                TextView tv = (TextView) findViewById(R.id.tvVoice);
                tv.setText("String is " + str);
            }
            super.onActivityResult(requestCode, resultCode, data);
        }

        @Override
        protected void onPause() {
            super.onPause();
            unregisterReceiver(smsReceiver);
            if (mp != null && mp.isPlaying())
                mp.pause();
        }

        @Override
        protected void onResume() {
            super.onResume();
            registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
            if (mp != null && !mp.isPlaying())
                mp.start();
            //seekUpdation();
        }

        @Override
        protected void onDestroy() {
            super.onDestroy();
            if (mp != null) {
                mp.release();
            }
        }

        //26-Apr-2017
        @Override
        public void onSmsReceived(String sender, String message) {
            TextView tv = (TextView) findViewById(R.id.tvSMS);
            Toast.makeText(this, "By Voice Activity " + message, Toast.LENGTH_LONG).show();
            tv.setText(sender + ":" + message);

            String key = Soundex.soundex(message);
            if (songsMap.containsKey(key)) {
                if (mp != null && mp.isPlaying())
                    mp.stop();

                mp = MediaPlayer.create(getBaseContext(), Uri.parse(path + "/" + songsMap.get(key)));
                mp.start();
                seekUpdation();
            } else
                Toast.makeText(this, "Song Not Found For Given Voice", Toast.LENGTH_SHORT).show();
        }
    }


