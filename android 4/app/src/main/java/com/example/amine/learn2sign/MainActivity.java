package com.example.amine.learn2sign;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.stetho.Stetho;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.HashSet;


import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;

import static com.example.amine.learn2sign.LoginActivity.ACTIVITY_TYPE;
import static com.example.amine.learn2sign.LoginActivity.INTENT_EMAIL;
import static com.example.amine.learn2sign.LoginActivity.INTENT_ID;
import static com.example.amine.learn2sign.LoginActivity.INTENT_SERVER_ADDRESS;
import static com.example.amine.learn2sign.LoginActivity.INTENT_TIME_WATCHED;
import static com.example.amine.learn2sign.LoginActivity.INTENT_TIME_WATCHED_VIDEO;
import static com.example.amine.learn2sign.LoginActivity.INTENT_URI;
import static com.example.amine.learn2sign.LoginActivity.INTENT_WORD;
import static com.example.amine.learn2sign.LoginActivity.VIDEO_URI;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_VIDEO_CAPTURE = 1;

    @BindView(R.id.rg_practice_learn)
    RadioGroup rg_practice_learn;

    @BindView(R.id.rb_learn)
    RadioButton rb_learn;

    @BindView(R.id.rb_practice)
    RadioButton rb_practice;

    @BindView(R.id.sp_words)
    Spinner sp_words;

    @BindView(R.id.sp_ip_address)
    Spinner sp_ip_address;

    @BindView(R.id.vv_video_learn)
    VideoView vv_video_learn;

    @BindView(R.id.vv_record)
    VideoView vv_record;

    @BindView(R.id.bt_record)
    Button bt_record;

    @BindView(R.id.bt_send)
    Button bt_send;

    @BindView(R.id.bt_cancel)
    Button bt_cancel;

    @BindView(R.id.ll_after_record)
    LinearLayout ll_after_record;

    @BindView(R.id.tv_filename)
    TextView tv_filename;

    String path;
    String returnedURI;
    String old_text = "";
    String videoUri = "About";
    SharedPreferences sharedPreferences;
    long time_started = 0;
    long time_started_return = 0;
    Activity mainActivity;
    Context context;
    private static final boolean isLearn = true;
    static String[] signNames = new String[]{"About", "And", "Can", "Cat", "Cop","Cost", "Day", "Deaf", "Decide", "Father", "Find", "Go Out", "Gold","Goodnight", "Hearing", "Here", "Hospital", "Hurt", "If", "Large", "Hello", "Help", "Sorry", "After", "Tiger"};
    static HashMap<String, Integer> signMap = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        context = this;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //bind xml to activity
        ButterKnife.bind(this);
        Stetho.initializeWithDefaults(this);

        rb_learn.setChecked(true);
        bt_cancel.setVisibility(View.GONE);
        bt_send.setVisibility(View.GONE);
        ll_after_record.setVisibility(View.VISIBLE);

        final Context context = MainActivity.this;

        if(getIntent().hasExtra(VIDEO_URI)) {
            videoUri = getIntent().getStringExtra(VIDEO_URI);
        }

        rg_practice_learn.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if( checkedId == rb_learn.getId() ) {
                    taskOnLearn();
                } else if ( checkedId == rb_practice.getId()) {
                    boolean check = checkCountForThreeEach();
                    if(check){
                        Intent intent = new Intent(context, PracticeActitvity.class);
                        intent.putExtra(VIDEO_URI, videoUri);
                        startActivity(intent);
                    }
                    else{
                        taskOnLearn();
                        rb_learn.setChecked(true);
                        Toast.makeText(getApplicationContext(),"Learn more to enable practise",
                                Toast.LENGTH_SHORT).show();
                    }
                }
                sp_words.setVisibility(View.VISIBLE);

            }
        });

        sp_words.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String text = sp_words.getSelectedItem().toString();
                videoUri = text;
                selectPlayVideo(text);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        sp_ip_address.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                sharedPreferences.edit().putString(INTENT_SERVER_ADDRESS, sp_ip_address.getSelectedItem().toString()).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        MediaPlayer.OnCompletionListener onCompletionListener = new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                if(mediaPlayer!=null)
                {
                    mediaPlayer.start();

                }

             }
        };
        vv_record.setOnCompletionListener(onCompletionListener);
        vv_video_learn.setOnCompletionListener(onCompletionListener);
        vv_record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                vv_record.start();
            }
        });
        vv_video_learn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!vv_video_learn.isPlaying()) {
                    vv_video_learn.start();
                }
            }
        });
        time_started = System.currentTimeMillis();
        sharedPreferences =  this.getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        Intent intent = getIntent();
        if(intent.hasExtra(INTENT_EMAIL) && intent.hasExtra(INTENT_ID)) {
            Toast.makeText(this,"User : " + intent.getStringExtra(INTENT_EMAIL),Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(this,"Already Logged In",Toast.LENGTH_SHORT).show();

        }
        taskOnLearn();
    }

    public void taskOnLearn() {
        Toast.makeText(getApplicationContext(),"Learn",Toast.LENGTH_SHORT).show();
        vv_video_learn.setVisibility(View.VISIBLE);
        vv_video_learn.start();
        time_started = System.currentTimeMillis();
        rb_practice.setEnabled(true);

        int i = 0;
        for (String name : signNames) {

            if (name.equals(videoUri)) {
                sp_words.setSelection(i);
            }
            i++;
        }

        selectPlayVideo(videoUri);
        bt_record.setVisibility(View.VISIBLE);
        sp_words.setEnabled(true);

    }

    private void selectPlayVideo(String text) {

        if(!old_text.equals(text)) {
            path = "";
            time_started = System.currentTimeMillis();
            play_video(text);
        }
    }

    public boolean checkCountForThreeEach() {

        boolean count = false;

        String server_ip = getSharedPreferences(this.getPackageName(),
                Context.MODE_PRIVATE).getString(INTENT_SERVER_ADDRESS,"10.211.17.171");

        String id = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE)
                .getString(INTENT_ID, "00000000");

        String url ="http://"+server_ip+"/Learn2Sign/uploads/"+id+"/accept/";

        RequestQueue mRequestQueue = Volley.newRequestQueue(this);

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @TargetApi(Build.VERSION_CODES.N)
                    @RequiresApi(api = Build.VERSION_CODES.N)
                    @Override
                    public void onResponse(String response) {

                        String[] resSplitString = response.split("\n");

                        for(String s : resSplitString){
                            for(int i =0; i < 25; i++){
                                if (s.contains(signNames[i])){

                                    signMap.put(signNames[i], signMap.getOrDefault(signNames[i], 0) + 1);
                                }
                            }

                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("html", error.toString());
                    }
                });

        mRequestQueue.add(stringRequest);

        count = true;
        for(int check : signMap.values()){
            if(check < 3){
                count = false;
                break;
            }
        }
        signMap.clear();
        count=true;
        return count;
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
        finish();
        super.onBackPressed();
    }

    @Override
    protected void onResume() {

        vv_video_learn.start();
        time_started = System.currentTimeMillis();
        super.onResume();

    }

    public void play_video(String text) {
        old_text = text;
        if(text.equals("About")) {

             path = "android.resource://" + getPackageName() + "/" + R.raw._about;
        } else if(text.equals("And")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._and;
        } else if (text.equals("Can")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._can;
        }else if (text.equals("Cat")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._cat;
        }else if (text.equals("Cop")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._cop;
        }else if (text.equals("Cost")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._cost;
        }else if (text.equals("Day")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._day;
        }else if (text.equals("Deaf")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._deaf;
        }else if (text.equals("Decide")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._decide;
        }else if (text.equals("Father")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._father;
        }else if (text.equals("Find")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._find;
        }else if (text.equals("Go Out")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._go_out;
        }else if (text.equals("Gold")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._gold;
        }else if (text.equals("Goodnight")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._good_night;
        }else if (text.equals("Hearing")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._hearing;
        }else if (text.equals("Here")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._here;
        }else if (text.equals("Hospital")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._hospital;
        }else if (text.equals("Hurt")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._hurt;
        }else if (text.equals("If")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._if;
        }else if (text.equals("Large")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._large;
        }else if (text.equals("Hello")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._hello;
        }else if (text.equals("Help")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._help;
        }else if (text.equals("Sorry")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._sorry;
        }else if (text.equals("After")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._after;
        }else if (text.equals("Tiger")) {
            path = "android.resource://" + getPackageName() + "/" + R.raw._tiger;
        }
        if(!path.isEmpty()) {
            Uri uri = Uri.parse(path);
            vv_video_learn.setVideoURI(uri);
            vv_video_learn.start();
        }

    }

    @OnClick(R.id.bt_record)
    public void record_video() {

         if( ContextCompat.checkSelfPermission(this,
                Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED ) {

             // Permission is not granted
             // Should we show an explanation?

             if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                     Manifest.permission.CAMERA)) {
                 // Show an explanation to the user *asynchronously* -- don't block
                 // this thread waiting for the user's response! After the user
                 // sees the explanation, try again to request the permission.
             } else {
                 // No explanation needed; request the permission
                 ActivityCompat.requestPermissions(this,
                         new String[]{Manifest.permission.CAMERA},
                         101);

                 // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                 // app-defined int constant. The callback method gets the
                 // result of the request.
             }
         }


         if ( ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED ) {

            // Permission is not granted
            // Should we show an explanation?


            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        100);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }

        } else {
            // Permission has already been granted
             File f = new File(Environment.getExternalStorageDirectory(), "Learn2Sign");

             if (!f.exists()) {
                 f.mkdirs();
             }

             time_started = System.currentTimeMillis() - time_started;

             Intent t = new Intent(this, VideoActivity.class);
             t.putExtra(INTENT_WORD,sp_words.getSelectedItem().toString());
             t.putExtra(INTENT_TIME_WATCHED, time_started);
             t.putExtra(ACTIVITY_TYPE, isLearn);
             startActivityForResult(t,9999);





 /*           File m = new File(Environment.getExternalStorageDirectory().getPath() + "/Learn2Sign");
            if(!m.exists()) {
                if(m.mkdir()) {
                    Toast.makeText(this,"Directory Created",Toast.LENGTH_SHORT).show();
                }
            }

            Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            takeVideoIntent.putExtra(EXTRA_DURATION_LIMIT, 10);

            if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
            }*/
        }
    }

    @OnClick(R.id.bt_send)
    public void sendToServer() {
        Toast.makeText(this,"Send to Server",Toast.LENGTH_SHORT).show();
        Intent t = new Intent(this,UploadActivity.class);
        startActivityForResult(t,2000);
    }

    @OnClick(R.id.bt_cancel)
    public void cancel() {
        vv_record.setVisibility(View.GONE);
        if(rb_learn.isSelected()) {
            vv_video_learn.setVisibility(View.VISIBLE);
        }
        bt_record.setVisibility(View.VISIBLE);
        bt_send.setVisibility(View.GONE);
        bt_cancel.setVisibility(View.GONE);

        sp_words.setEnabled(true);

        rb_learn.setEnabled(false);
        rb_practice.setEnabled(true);
        time_started = System.currentTimeMillis();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {

    Log.e("OnActivityresult",requestCode+" "+resultCode);

        if(requestCode==2000 ) {
            //from video activity
            vv_record.setVisibility(View.GONE);
            rb_learn.setChecked(true);
            bt_cancel.setVisibility(View.GONE);
            bt_send.setVisibility(View.GONE);
            bt_record.setVisibility(View.VISIBLE);
            sp_words.setEnabled(true);
            rb_learn.setEnabled(true);
            //rb_practice.setEnabled(true);
            sp_ip_address.setEnabled(true);


        }
        if(requestCode==9999 && resultCode == 8888) {
            if(intent.hasExtra(INTENT_URI) && intent.hasExtra(INTENT_TIME_WATCHED_VIDEO)) {
                returnedURI = intent.getStringExtra(INTENT_URI);
                time_started_return = intent.getLongExtra(INTENT_TIME_WATCHED_VIDEO,0);

                vv_record.setVisibility(View.VISIBLE);
                bt_record.setVisibility(View.GONE);


                bt_send.setVisibility(View.VISIBLE);
                bt_cancel.setVisibility(View.VISIBLE);
                rb_learn.setChecked(true);
                rb_practice.setChecked(false);

                rb_learn.setEnabled(false);
                rb_practice.setEnabled(false);

                sp_words.setEnabled(false);

                //rb_practice.setEnabled(false);
                vv_record.setVideoURI(Uri.parse(returnedURI));
                int try_number = sharedPreferences.getInt("record_"+sp_words.getSelectedItem().toString(),0);
                try_number++;
                String toAdd  = sp_words.getSelectedItem().toString()+"_"+try_number+"_"+time_started_return + "";
                HashSet<String> set = (HashSet<String>) sharedPreferences.getStringSet("RECORDED",new HashSet<String>());
                set.add(toAdd);
                sharedPreferences.edit().putStringSet("RECORDED", set).apply();
                sharedPreferences.edit().putInt("record_"+sp_words.getSelectedItem().toString(), try_number).apply();

                vv_video_learn.start();
                vv_record.start();
            }

        }

        if(requestCode==9999 && resultCode==7777)
        {
            if(intent != null) {
                //create folder
                if(intent.hasExtra(INTENT_URI) && intent.hasExtra(INTENT_TIME_WATCHED_VIDEO)) {
                    returnedURI = intent.getStringExtra(INTENT_URI);
                    time_started_return = intent.getLongExtra(INTENT_TIME_WATCHED_VIDEO,0);
                    File f = new File(returnedURI);
                    f.delete();

                    rb_learn.setChecked(true);
                    rb_practice.setChecked(false);

                    rb_learn.setEnabled(false);
                    rb_practice.setEnabled(true);



                    time_started = System.currentTimeMillis();
                    vv_video_learn.start();
                }
            }

        }
    }

    //Menu Item for logging out
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }
    public boolean onOptionsItemSelected(MenuItem item) {

        //respond to menu item selection
        switch (item.getItemId()) {
            case R.id.menu_logout:
                mainActivity = this;
                    final AlertDialog alertDialog = new AlertDialog.Builder(this).create();
                    alertDialog.setTitle("ALERT");
                    alertDialog.setMessage("Logging out will delete all the data!");
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    sharedPreferences.edit().clear().apply();
                                    File f = new File(Environment.getExternalStorageDirectory(), "Learn2Sign");
                                    if (f.isDirectory())
                                    {
                                        String[] children = f.list();
                                        for (int i = 0; i < children.length; i++)
                                        {
                                            new File(f, children[i]).delete();
                                        }
                                    }
                                    startActivity(new Intent(mainActivity,LoginActivity.class));
                                    mainActivity.finish();

                                }
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });
                    alertDialog.show();



                    return true;
            case R.id.menu_upload_server:
                sharedPreferences.edit().putInt(getString(R.string.gotoupload), sharedPreferences.getInt(getString(R.string.gotoupload),0)+1).apply();
                Intent t = new Intent(this,UploadActivity.class);
                startActivityForResult(t,2000);

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public class SaveFile extends AsyncTask<String, String, Void> {

        @Override
        protected Void doInBackground(String... strings) {
            FileOutputStream fileOutputStream = null;
            FileInputStream fileInputStream = null;
            try {
                fileOutputStream = new FileOutputStream(strings[0]);
                fileInputStream = (FileInputStream) getContentResolver().openInputStream(Uri.parse(strings[1]));
                Log.d("msg", fileInputStream.available() + " ");
                byte[] buffer = new byte[1024];
                while (fileInputStream.available() > 0) {

                    fileInputStream.read(buffer);
                    fileOutputStream.write(buffer);
                    publishProgress(fileInputStream.available()+"");
                }

                fileInputStream.close();
                fileOutputStream.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;

        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(String... values) {

        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast.makeText(getApplicationContext(),"Video Saved Successfully",Toast.LENGTH_SHORT).show();
        }
    }
}
