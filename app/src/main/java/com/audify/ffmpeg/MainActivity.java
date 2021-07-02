package com.audify.ffmpeg;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;

import com.arthenica.ffmpegkit.ExecuteCallback;
import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegSession;
import com.arthenica.ffmpegkit.ReturnCode;
import com.arthenica.ffmpegkit.Session;
import com.arthenica.ffmpegkit.SessionState;

public class MainActivity extends AppCompatActivity {

    private ImageButton audiomp3;
    private Button cancel;
    private TextView audio_url;
    private ProgressDialog progressDialog;
    private String video_url;
    private VideoView videoView;
    private static final String root= Environment.getExternalStorageDirectory().toString();
    private static final String app_folder=root+"/Audify/";
    private static final MediaPlayer mp = new MediaPlayer();

    private static final String TAG = MainActivity.class.getName();

    //Permision code that will be checked in the method onRequestPermissionsResult
    private int STORAGE_PERMISSION_CODE = 23;

    //We are calling this method to check the permission status
    private boolean isReadStorageAllowed() {
        //Getting the permission status
        int result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE);

        //If permission is granted returning true
        if (result == PackageManager.PERMISSION_GRANTED)
            return true;

        //If permission is not granted returning false
        return false;
    }

    //Requesting permission
    private void requestStoragePermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.INTERNET) ||
                ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.MANAGE_EXTERNAL_STORAGE)){
            //If the user has denied the permission previously your code will come to this block
            //Here you can explain why you need this permission
            //Explain here why you need this permission
        }

        //And finally ask for the permission
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.INTERNET,Manifest.permission.MANAGE_EXTERNAL_STORAGE},STORAGE_PERMISSION_CODE);
    }

    //This method will be called when the user will tap on allow or deny
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,  int[] grantResults) {

        //Checking the request code of our request
        if(requestCode == STORAGE_PERMISSION_CODE){

            //If permission is granted
            if(grantResults.length >0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){

                //Displaying a toast
                Toast.makeText(this,"Permissions granted",Toast.LENGTH_LONG).show();
            }else{
                //Displaying another toast if permission is not granted
                Toast.makeText(this,"Oops you just denied the permission",Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //slow = (ImageButton) findViewById(R.id.slow);
        //reverse = (ImageButton) findViewById(R.id.reverse);
        audio_url = (Button) findViewById(R.id.text2);
        cancel = (Button) findViewById(R.id.cancel_button);
        audiomp3 = (ImageButton) findViewById(R.id.audiomp3);
        videoView=(VideoView) findViewById(R.id.layout_movie_wrapper);

        //creating the progress dialog
        progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setMessage("Please wait..");
        progressDialog.setCancelable(false);
        progressDialog.setCanceledOnTouchOutside(false);
        //set up the onClickListeners
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //create an intent to retrieve the video file from the device storage
                Intent intent = new Intent(
                        Intent.ACTION_PICK,
                        android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
                intent.setType("video/*");
                startActivityForResult(intent, 123);
            }
        });

        audiomp3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               // video_url = "/storage/emulated/0/Movies/1280.mp4";
                if (video_url != null) {
                    try {
                        audiomp3();
                    } catch (Exception e) {
                        //e.printStackTrace();
                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                } else
                    Toast.makeText(MainActivity.this, "Please upload video", Toast.LENGTH_SHORT).show();
            }
        });
        /*
            set up the VideoView.
            We will be using VideoView to view our video.
         */
        requestStoragePermission();
    }

    /**
     * Method for creating fast motion video
     */
    private void audiomp3() throws Exception {

        FFmpegKit.cancel();
        //create a progress dialog and show it until this method executes.
        progressDialog.show();

        //creating a new file in storage
        final String filePath;
        String filePrefix = "audiomp3";
        String fileExtn = ".mp3";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            /*
            With introduction of scoped storage in Android Q the primitive method gives error
            So, it is recommended to use the below method to create a audio file in storage.
             */
            ContentValues valuesvideos = new ContentValues();
            valuesvideos.put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/" + "Folder");
            valuesvideos.put(MediaStore.Audio.Media.TITLE, filePrefix+System.currentTimeMillis());
            valuesvideos.put(MediaStore.Audio.Media.DISPLAY_NAME, filePrefix+System.currentTimeMillis()+fileExtn);
            valuesvideos.put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp3");
            valuesvideos.put(MediaStore.Audio.Media.DATE_ADDED, System.currentTimeMillis() / 1000);
            valuesvideos.put(MediaStore.Audio.Media.DATE_TAKEN, System.currentTimeMillis());
            Uri uri = getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, valuesvideos);

            //get the path of the video file created in the storage.
            File file=FileUtils.getFileFromUri(this,uri);
            filePath=file.getAbsolutePath();

        }else {
            //This else statement will work for devices with Android version lower than 10
            //Here, "app_folder" is the path to your app's root directory in device storage
            File dest = new File(new File(app_folder), filePrefix + fileExtn);
            int fileNo = 0;
            //check if the file name previously exist. Since we don't want to oerwrite the video files
            while (dest.exists()) {
                fileNo++;
                dest = new File(new File(app_folder), filePrefix + fileNo + fileExtn);
            }
            //Get the filePath once the file is successfully created.
            filePath = dest.getAbsolutePath();
        }
        String exe;
        File mp3File = new File(filePath);
        if (!mp3File.getParentFile().exists())
            mp3File.getParentFile().mkdirs();
        //the "exe" string contains the command to process video.The details of command are discussed later in this post.
        // "video_url" is the url of video which you want to edit. You can get this url from intent by selecting any video from gallery.
        exe = "-i " + video_url + " -q:a 0 -map a -y " +filePath;
        //exe="-y -i " +video_url+" -filter_complex [0:v]trim=0:"+startMs/1000+",setpts=PTS-STARTPTS[v1];[0:v]trim="+startMs/1000+":"+endMs/1000+",setpts=0.5*(PTS-STARTPTS)[v2];[0:v]trim="+(endMs/1000)+",setpts=PTS-STARTPTS[v3];[0:a]atrim=0:"+(startMs/1000)+",asetpts=PTS-STARTPTS[a1];[0:a]atrim="+(startMs/1000)+":"+(endMs/1000)+",asetpts=PTS-STARTPTS,atempo=2[a2];[0:a]atrim="+(endMs/1000)+",asetpts=PTS-STARTPTS[a3];[v1][a1][v2][a2][v3][a3]concat=n=3:v=1:a=1 "+"-b:v 2097k -vcodec mpeg4 -crf 0 -preset superfast "+filePath;

        /*
            Here, we have used he Async task to execute our query because if we use the regular method the progress dialog
            won't be visible. This happens because the regular method and progress dialog uses the same thread to execute
            and as a result only one is a allowed to work at a time.
            By using we Async task we create a different thread which resolves the issue.
         */
        FFmpegSession session = FFmpegKit.executeAsync(exe, new ExecuteCallback() {

            @Override
            public void apply(Session session) {
                SessionState state = session.getState();
                ReturnCode returnCode = session.getReturnCode();
                if (ReturnCode.isSuccess(session.getReturnCode())) {
                    progressDialog.dismiss();
                    audio_url.setText("Audio written to: " + filePath);
                } else if (ReturnCode.isCancel(session.getReturnCode())) {
                    Log.i(TAG, "Async command execution cancelled by user.");
                } else {
                    Log.i(TAG, String.format("Async command execution failed with returnCode=%d.", returnCode));
                }
            }
        });
    }

    //Overriding the method onActivityResult() to get the video Uri form intent.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {

            if (requestCode == 123) {

                if (data != null) {
                    //get the video Uri
                    Uri uri = data.getData();
                    try {
                        //get the file from the Uri using getFileFromUri() methid present in FileUils.java
                        File video_file = FileUtils.getFileFromUri(this, uri);
                        //now set the video uri in the VideoView
                        videoView.setVideoURI(uri);
                        //after successful retrieval of the video and properly setting up the retried video uri in
                        //VideoView, Start the VideoView to play that video
                        videoView.start();
                        //get the absolute path of the video file. We will require this as an input argument in
                        //the ffmpeg command.
                        video_url=video_file.getAbsolutePath();
                    } catch (Exception e) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }


                }
            }
        }
    }
}
