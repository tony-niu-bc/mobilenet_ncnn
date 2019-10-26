package com.wzhnsc.mobilenet_ncnn;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.provider.MediaStore;

public class PhotoUtil
{
    // get picture in photo
    public static void
    use_photo(Activity activity,
              int      requestCode)
    {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        activity.startActivityForResult(intent, requestCode);
    }

    // get photo from Uri
    public static String
    get_path_from_URI(Context context,
                      Uri     uri)
    {
        String result;
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        if (null == cursor) {
            result = uri.getPath();
        }
        else
        {
            cursor.moveToFirst();
            int idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
            result = cursor.getString(idx);
            cursor.close();
        }

        return result;
    }

    // compress picture
    public static Bitmap
    getScaleBitmap(String filePath)
    {
        BitmapFactory.Options opt = new BitmapFactory.Options();
        opt.inJustDecodeBounds = true;

        BitmapFactory.decodeFile(filePath, opt);

        // compress picture with inSampleSize
        opt.inSampleSize = 1;

        while ((500 < (opt.outWidth  / opt.inSampleSize)) ||
               (500 < (opt.outHeight / opt.inSampleSize)))
        {
            opt.inSampleSize *= 2;
        }

        opt.inJustDecodeBounds = false;

        return BitmapFactory.decodeFile(filePath, opt);
    }
}
