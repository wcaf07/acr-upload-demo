package com.acrcloud.demo;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.acrcloud.utils.ACRCloudRecognizer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TextView mVolume, mResult, tv_time;

    private boolean mProcessing = false;
    private boolean initState = false;

    private String path = "";
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1:
                    String res = (String) msg.obj;
                    mResult.setText(res);
                    break;

                default:
                    break;
            }
        };
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        path = Environment.getExternalStorageDirectory().toString()
                + "/acrcloud/model";

        File file = new File(path);
        if(!file.exists()){
            file.mkdirs();
        }

        mResult = (TextView) findViewById(R.id.result);

        Button recBtn = (Button) findViewById(R.id.rec);
        recBtn.setText(getResources().getString(R.string.rec));

        findViewById(R.id.rec).setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                rec();
            }
        });
    }

    class RecThread extends Thread {

        Uri uri;
        public RecThread(Uri uri) {
            this.uri = uri;
        }

        public void run() {
            Map<String, Object> config = new HashMap<String, Object>();
            config.put("access_key", "XXX");
            config.put("access_secret", "XXX");
            config.put("host", "identify-eu-west-1.acrcloud.com");
            config.put("debug", false);
            config.put("timeout", 5);

            ACRCloudRecognizer re = new ACRCloudRecognizer(config);
            String selectedPath = getPath(uri);
            String result = re.recognizeByFile(selectedPath, 10);
            System.out.println(result);

            try {
                JSONObject object = new JSONObject(result);
                JSONObject metadata = object.getJSONObject("metadata");
                JSONArray musics = metadata.getJSONArray("music");
                JSONObject music = musics.getJSONObject(0);

                JSONArray artists = music.getJSONArray("artists");
                String artistsString = "";
                for (int i = 0; i < artists.length(); i++) {
                    JSONObject artObject = artists.getJSONObject(i);
                    artistsString += artObject.getString("name") + " ";
                }

                String title = music.getString("title");

                JSONObject album = music.getJSONObject("album");
                String albumName = album.getString("name");

                String finalString = "Artistas : "+artistsString+"\nNome: "+title+"\nAlbum: "+albumName;

                Message msg = new Message();
                msg.obj = finalString;

                msg.what = 1;
                mHandler.sendMessage(msg);

            } catch (JSONException e) {
                Message msg = new Message();
                msg.obj = "Erro ao tentar reconhecer música";

                msg.what = 1;
                mHandler.sendMessage(msg);
            }

            //File file = new File(path + "/test.mp3");
            //byte[] buffer = new byte[3 * 1024 * 1024];
            //if (!file.exists()) {
            //    return;
            //}
            //FileInputStream fin = null;
            //int bufferLen = 0;
            //try {
            //    fin = new FileInputStream(file);
            //    bufferLen = fin.read(buffer, 0, buffer.length);
            //} catch (Exception e) {
            //    e.printStackTrace();
            //} finally {
            //    try {
            //        if (fin != null) {
            //            fin.close();
            //        }
            //    } catch (IOException e) {
            //        e.printStackTrace();
            //    }
            //}
            //System.out.println("bufferLen=" + bufferLen);

            //if (bufferLen <= 0)
            //    return;

            //String result = re.recognizeByFileBuffer(buffer, bufferLen, 80);

        }
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public void rec() {
        verifyStoragePermissions(this);
        Intent intent_upload = new Intent();
        intent_upload.setType("audio/*");
        intent_upload.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent_upload,1);
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode,Intent data){

        if(requestCode == 1){

            if(resultCode == RESULT_OK){

                //the selected audio.
                Uri uri = data.getData();

                new RecThread(uri).start();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    public String getPath(Uri uri) {
        String[] projection = { MediaStore.Images.Media.DATA };
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        return cursor.getString(column_index);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
