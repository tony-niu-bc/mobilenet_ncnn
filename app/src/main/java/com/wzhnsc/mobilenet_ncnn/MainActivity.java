package com.wzhnsc.mobilenet_ncnn;

import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.net.Uri;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity
{
    public native boolean Init(byte[] param, byte[] bin);
    public native float[] Detect(Bitmap bitmap);

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    private static final String TAG       = MainActivity.class.getName();
    private static final int    USE_PHOTO = 1001;

    private boolean      load_result = false;
    private int[]        ddims       = {1, 3, 224, 224};
    private List<String> resultLabel = new ArrayList<>();

    private ImageView show_image;
    private TextView  result_text;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        try {
            initSqueezeNcnn();
        } catch (IOException e) {
            Log.e("MainActivity", "initSqueezeNcnn error");
        }

        init_view();
        readCacheLabelFromLocalFile();
    }

    private void initSqueezeNcnn() throws IOException
    {
        byte[] param;
        byte[] bin;

        InputStream paramInputStream = getAssets().open("mobilenet_v2.param.bin");
        int byteParam = paramInputStream.available();

        InputStream binInputStream = getAssets().open("mobilenet_v2.bin");
        int byteBin = binInputStream.available();

        if ((0 < byteParam) &&
            (0 < byteBin))
        {
            param = new byte[byteParam];
            paramInputStream.close();

            bin = new byte[byteBin];
            binInputStream.close();

            if ((byteParam == paramInputStream.read(param) &&
                (byteBin   == binInputStream.read(bin))))
            {
                load_result = Init(param, bin);
            }
        }

        Log.d("load model", "result:" + load_result);
    }

    // initialize view
    private void init_view()
    {
        request_permissions();

        show_image  = findViewById(R.id.show_image);

        result_text = findViewById(R.id.result_text);
        result_text.setMovementMethod(ScrollingMovementMethod.getInstance());

        Button use_photo = findViewById(R.id.use_photo);
        // use photo click
        use_photo.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (!load_result)
                {
                    Toast.makeText(MainActivity.this,
                                   "never load model",
                                   Toast.LENGTH_SHORT).show();
                    return;
                }

                PhotoUtil.use_photo(MainActivity.this,
                                    USE_PHOTO);
            }
        });
    }

    // load label's name
    private void readCacheLabelFromLocalFile()
    {
        try
        {
            AssetManager assetManager = getApplicationContext().getAssets();
            BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open("synset.txt")));
            String readLine;

            while (null != (readLine = reader.readLine())) {
                resultLabel.add(readLine);
            }

            reader.close();
        }
        catch (Exception e) {
            Log.e("labelCache", "error " + e);
        }
    }

    @Override
    protected void
    onActivityResult(int    requestCode,
                     int    resultCode,
                     Intent data)
    {
        if (resultCode == Activity.RESULT_OK)
        {
            if (USE_PHOTO == requestCode)
            {
                if (null == data) {
                    Log.w(TAG, "user photo data is null");
                    return;
                }

                Uri image_uri = data.getData();

                RequestOptions options = new RequestOptions().skipMemoryCache(true)
                                                             .diskCacheStrategy(DiskCacheStrategy.NONE);
                Glide.with(MainActivity.this)
                     .load(image_uri)
                     .apply(options)
                     .into(show_image);

                // get image path from uri
                String image_path = PhotoUtil.get_path_from_URI(MainActivity.this,
                                                                image_uri);

                // predict image
                predict_image(image_path);
            }
        }
    }

    //  predict image
    private void predict_image(String image_path)
    {
        // picture to float array
        Bitmap bmp  = PhotoUtil.getScaleBitmap(image_path);
        Bitmap rgba = bmp.copy(Bitmap.Config.ARGB_8888,
                               true);

        // resize to 227x227
        Bitmap input_bmp = Bitmap.createScaledBitmap(rgba,
                                                     ddims[2],
                                                     ddims[3],
                                                     false);

        try
        {
            // Data format conversion takes too long
            // Log.d("inputData", Arrays.toString(inputData));
            long start = System.currentTimeMillis();
            // get predict result
            float[] result = Detect(input_bmp);
            long end = System.currentTimeMillis();
            Log.d(TAG, "origin predict result:" + Arrays.toString(result));
            long time = end - start;
            Log.d("result length", String.valueOf(result.length));
            // show predict result and time
            int r = get_max_result(result);
            String show_text = "result：" + r + "\nname：" + resultLabel.get(r) + "\nprobability：" + result[r] + "\ntime：" + time + "ms";
            result_text.setText(show_text);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // get max probability label
    private int get_max_result(float[] result)
    {
        float probability = result[0];
        int r = 0;

        for (int i = 0; i < result.length; i++)
        {
            if (probability < result[i])
            {
                probability = result[i];
                r = i;
            }
        }

        return r;
    }

    // request permissions
    private void request_permissions()
    {
        if (Build.VERSION.SDK_INT >= 23) {
            List<String> permissionList = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.CAMERA);
            }

            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }

            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.READ_EXTERNAL_STORAGE);
            }

            // if list is not empty will request permissions
            if (!permissionList.isEmpty()) {
                requestPermissions(permissionList.toArray(new String[0]), 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults)
    {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (1 == requestCode)
        {
            if (grantResults.length > 0)
            {
                for (int i = 0; i < grantResults.length; i++)
                {
                    int grantResult = grantResults[i];

                    if (grantResult == PackageManager.PERMISSION_DENIED)
                    {
                        String s = permissions[i];
                        Toast.makeText(this,
                                       s + " permission was denied",
                                       Toast.LENGTH_SHORT)
                             .show();
                    }
                }
            }
        }
    }
}
