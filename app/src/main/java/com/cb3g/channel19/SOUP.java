package com.cb3g.channel19;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;

public class SOUP {





    private class Content extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... url) {
            try {
                //Connect to the website
                Document document = Jsoup.connect(url[0]).get();
                //Get the logo source of the website
                Element img = document.select("img").first();
                // Locate the src attribute
                String imgSrc = img.absUrl("src");
                // Download image from URL
                InputStream input = new java.net.URL(imgSrc).openStream();
                // Decode Bitmap
                Bitmap bitmap = BitmapFactory.decodeStream(input);

                //Get the title of the website
                String title = document.title();

            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

        }
    }
}
