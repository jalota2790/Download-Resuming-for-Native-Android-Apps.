package com.example.stereo.downloadresume;

import android.Manifest;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class Resume extends AppCompatActivity implements NetworkStateReceiver.NetworkStateReceiverListener{

    @BindView(R.id.progressBarFifteen)
    ProgressBar progressBar;

    @BindView(R.id.buttonCancelFifteen)
    Button cancel;

    @BindView(R.id.buttonFifteen)
    Button button;

    private int downloaded=0;
    private BufferedInputStream in;
    private FileOutputStream fos;
    private BufferedOutputStream bout;
    private int i=-1;
    private File file;

    private boolean clicked=false;
    private NetworkStateReceiver networkStateReceiver;

    //Static link to dowload a demo video
    //Replace it with your link
    final String link = "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4" ;

    Random random ;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resume);
        ButterKnife.bind(this);

        random = new Random();

        file=new File(getFilename());

        //initializing the network state reciever class
        networkStateReceiver = new NetworkStateReceiver();

        //adding a listner to start listening to network events
        networkStateReceiver.addListener(this);

        //Registering the network state reciever as
        // a local broadcast reciever to handle download when network state changes
        this.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
    }


    private String getFilename()
    {
        //get system storage path
        String filepath = Environment.getExternalStorageDirectory().getPath();

        //create a new folder to save downloads
        File file = new File(filepath,"New Folder");

        if(!file.exists()){
            file.mkdirs();
        }

        //get file name from the above mentiooned static url
        return (file.getAbsolutePath() + "/" + link.substring(link.lastIndexOf("/") + 1,
                link.length()));
    }

    @OnClick({R.id.buttonCancelFifteen,R.id.buttonFifteen})
    public void onClick(View view){
        switch(view.getId()){
            case R.id.buttonCancelFifteen:
                //add your own code to handle cancelling download

                break;
            case R.id.buttonFifteen:
                clicked=true;
               /* if(file.exists())
                    return;*/
               //start asynctask to dowload file in background
                StartDownload start=new StartDownload();
                start.execute();
                break;
        }
    }

    @Override
    public void networkAvailable() {
        //restart download when network is reconnected
        if(clicked){
            StartDownload startDownload=new StartDownload();
            startDownload.execute();
        }
        if(i==1)
            Toast.makeText(this, "Download resumed", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void networkUnavailable() {
        if(clicked)
            Toast.makeText(this, "No internet connection.Download paused", Toast.LENGTH_SHORT).show();
    }

    //create a background task to donload file without putting load on main thread
    public class StartDownload extends AsyncTask<Void,Void,Void>{

        @Override
        protected Void doInBackground(Void... voids) {
            Log.e("Background task", "doInBackground: ");
            try {
                downloadFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //download();
            return null;
        }
    }

    public void downloadFile() throws IOException {
        URL url = new URL(link);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        //connection.setRequestMethod("GET");
        //default method is GET
            if(file.exists()){
                i=1;
                //get the length of the already downloaded content
                downloaded = (int) file.length();

                //set request byte to last downloaded byte
                connection.setRequestProperty("Range", "bytes="+(file.length())+"-");
                //Toast.makeText(this, "Download resumed", Toast.LENGTH_SHORT).show();
            }else {
                //start dowload from begining if not already downloaded or interrupted
            connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
        }
        connection.setDoInput(true);

        //set progress bar maxlength to the length of file
        progressBar.setMax(connection.getContentLength());
        in = new BufferedInputStream(connection.getInputStream());

        //before writing the downloaded content to memory check if already downloaded
        fos=(downloaded==0)? new FileOutputStream(getFilename()): new FileOutputStream(getFilename(),true);
        bout = new BufferedOutputStream(fos, 1024);
        byte[] data = new byte[1024];
        int x = 0;

        //write the byte data downloaded to memory
        while ((x = in.read(data, 0, 1024)) >= 0) {
            bout.write(data, 0, x);
            downloaded += x;

            //show download progress
            progressBar.setProgress(downloaded);
        }
    }

    @Override
    protected void onResume() {
        //check if permissions are graned to read and write memory
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){

            //ask user for permissions
            ActivityCompat.requestPermissions(Resume.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.READ_EXTERNAL_STORAGE},1234);
        }
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        //remove listener's to make your app memory efficient and avoid null values
        networkStateReceiver.removeListener(this);

        //remove broadcast to save battery
        this.unregisterReceiver(networkStateReceiver);
        super.onDestroy();
    }
}
