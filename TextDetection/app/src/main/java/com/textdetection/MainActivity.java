package com.textdetection;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Calendar;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

public class MainActivity extends AppCompatActivity {

    Uri uriFilePath;
    int CAMERA_REQ = 100;
    ImageView imageView;
    private TextView desc_textView;
    private Button takePicBtn;
    String API_KEY = "YOUR_API_KEY";
    private static String visionEndpoint = "https://vision.googleapis.com/v1/images:annotate?key=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        visionEndpoint += API_KEY;

        takePicBtn = findViewById(R.id.button);
        imageView = (ImageView) findViewById(R.id.imageView);
        desc_textView = findViewById(R.id.desc_textView);

        takePicBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePic();
            }
        });

    }

    private void takePic() {
        if (Build.VERSION.SDK_INT >= 24) {
            try {
                Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                m.invoke(null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        File mainDir = new File(Environment.getExternalStorageDirectory(), "Visionapp/temp");
        if (!mainDir.exists()) {
            mainDir.mkdir();
        }

        Calendar calendar = Calendar.getInstance();
        uriFilePath = Uri.fromFile(new File(mainDir, "IMG_" + calendar.getTimeInMillis()));
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uriFilePath);
        startActivityForResult(intent, CAMERA_REQ);
    }

    private String encodeToBase64(String path) throws Exception {
        Bitmap bm = BitmapFactory.decodeFile(path);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bm.compress(Bitmap.CompressFormat.JPEG, 50, baos); //bm is the bitmap object
        byte[] b = baos.toByteArray();
        return Base64.encodeToString(b, Base64.DEFAULT);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

            if (resultCode == RESULT_OK && requestCode == CAMERA_REQ) {
                String filePath = uriFilePath.getPath(); // Here is path of your captured image, so you can create bitmap from it, etc.
                Glide.with(MainActivity.this).load(uriFilePath).into(imageView);
                try {
                    postToVision(encodeToBase64(filePath));
                } catch (Exception e) {
                    e.printStackTrace();
                }

        }
    }

    private void postToVision(String base64) {
        try {

            JSONObject rootObject = new JSONObject();

            JSONArray requstsArrayObj = new JSONArray();
            JSONObject requestObject = new JSONObject();
            requstsArrayObj.put(requestObject);

            JSONObject imageContentObj = new JSONObject();
            imageContentObj.put("content", base64);

            requestObject.put("image", imageContentObj);

            JSONArray featuresImageArray = new JSONArray();
            JSONObject featuresObj = new JSONObject();
            featuresObj.put("type", "TEXT_DETECTION");
            featuresImageArray.put(featuresObj);
            requestObject.put("features", featuresImageArray);
            rootObject.put("requests", requstsArrayObj);

            System.out.println(rootObject.toString());

            StringEntity postData = new StringEntity(rootObject.toString());

            AsyncHttpClient client = new AsyncHttpClient();
            client.post(this, visionEndpoint, postData, "application/json", new AsyncHttpResponseHandler() {

                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    String response = new String(responseBody);
                    try {
                        JSONObject jsonObject = new JSONObject(response);
                        JSONArray responses = jsonObject.getJSONArray("responses");
                        JSONObject firstObj = responses.getJSONObject(0);
                        if (firstObj.getJSONArray("textAnnotations") != null) {

                            JSONArray textAnnotations = firstObj.getJSONArray("textAnnotations");
                            JSONObject finalObject = textAnnotations.getJSONObject(0);
                            String finalResultText = finalObject.getString("description");

                            desc_textView.setText(finalResultText);
                            System.out.println(response.toString());
                        } else {
                            Toast.makeText(MainActivity.this, "No Desc found", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Log.e("Vision Response", response);
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    String response = new String(responseBody);
                    Log.e("Vision Faliure", response);
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}