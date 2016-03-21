package com.cahue.iweco.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;

/**
 * Background Async task to load user profile picture from url
 */
public class LoadImageTask extends AsyncTask<String, Void, Bitmap> {

    final ImageView bmImage;
    private OnLoadImageFinishedListener listener;

    public LoadImageTask(ImageView bmImage) {
        this.bmImage = bmImage;
    }

    @Nullable
    protected Bitmap doInBackground(String... urls) {
        String urldisplay = urls[0];
        if (urldisplay == null || urldisplay.isEmpty())
            return null;
        Bitmap mIcon11 = null;
        try {
            InputStream in = new java.net.URL(urldisplay).openStream();
            mIcon11 = BitmapFactory.decodeStream(in);
        } catch (Exception e) {
            Log.e("Error", e.getMessage());
            e.printStackTrace();
        }
        return mIcon11;
    }

    protected void onPostExecute(Bitmap result) {
        if (listener != null) listener.onLoadImageFinished(result);
        bmImage.setImageBitmap(result);

    }

    public void setListener(OnLoadImageFinishedListener listener) {
        this.listener = listener;
    }

    public interface OnLoadImageFinishedListener {
        void onLoadImageFinished(Bitmap result);
    }
}