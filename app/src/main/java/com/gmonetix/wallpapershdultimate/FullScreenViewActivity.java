package com.gmonetix.wallpapershdultimate;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import com.android.volley.Request.Method;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;
import com.android.volley.toolbox.JsonObjectRequest;
import com.github.clans.fab.FloatingActionButton;
import com.gmonetix.wallpapershdultimate.app.AppController;
import com.gmonetix.wallpapershdultimate.picasa.Wallpaper;
import com.gmonetix.wallpapershdultimate.util.Utils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import uk.co.senab.photoview.PhotoViewAttacher;

public class FullScreenViewActivity extends Activity implements OnClickListener {
    private static final String TAG = FullScreenViewActivity.class
            .getSimpleName();
    public static final String TAG_SEL_IMAGE = "selectedImage";
    private Wallpaper selectedPhoto;
    private ImageView fullImageView;
    private FloatingActionButton llSetWallpaper, llDownloadWallpaper;
    private Utils utils;
    private ProgressBar pbLoader;

    // Picasa JSON response node keys
    private static final String TAG_ENTRY = "entry",
            TAG_MEDIA_GROUP = "media$group",
            TAG_MEDIA_CONTENT = "media$content", TAG_IMG_URL = "url",
            TAG_IMG_WIDTH = "width", TAG_IMG_HEIGHT = "height";

    private Uri bitmapUri;
    private final static int PIC_CROP = 99;
    private  Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fullscreen_image);

        fullImageView = (ImageView) findViewById(R.id.imgFullscreen);
        llSetWallpaper = (FloatingActionButton) findViewById(R.id.fab_set_as_wallpaper);
        llDownloadWallpaper = (FloatingActionButton) findViewById(R.id.fab_download);
        pbLoader = (ProgressBar) findViewById(R.id.pbLoader);

        utils = new Utils(getApplicationContext());

        // layout click listeners
        llSetWallpaper.setOnClickListener(this);
        llDownloadWallpaper.setOnClickListener(this);

        // setting layout buttons alpha/opacity
        llSetWallpaper.getBackground().setAlpha(70);
        llDownloadWallpaper.getBackground().setAlpha(70);

        Intent i = getIntent();
        selectedPhoto = (Wallpaper) i.getSerializableExtra(TAG_SEL_IMAGE);

        // check for selected photo null
        if (selectedPhoto != null) {

            // fetch photo full resolution image by making another json request
            fetchFullResolutionImage();

        } else {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.msg_unknown_error), Toast.LENGTH_SHORT)
                    .show();
        }
    }

    /**
     * Fetching image fullresolution json
     * */
    private void fetchFullResolutionImage() {
        String url = selectedPhoto.getPhotoJson();

        // show loader before making request
        pbLoader.setVisibility(View.VISIBLE);
        llSetWallpaper.setVisibility(View.GONE);
        llDownloadWallpaper.setVisibility(View.GONE);

        // volley's json obj request
        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Method.GET, url,
                (String) null, new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG,
                        "Image full resolution json: "
                                + response.toString());
                try {
                    // Parsing the json response
                    JSONObject entry = response
                            .getJSONObject(TAG_ENTRY);

                    JSONArray mediacontentArry = entry.getJSONObject(
                            TAG_MEDIA_GROUP).getJSONArray(
                            TAG_MEDIA_CONTENT);

                    JSONObject mediaObj = (JSONObject) mediacontentArry
                            .get(0);

                    String fullResolutionUrl = mediaObj
                            .getString(TAG_IMG_URL);

                    // image full resolution widht and height
                    final int width = mediaObj.getInt(TAG_IMG_WIDTH);
                    final int height = mediaObj.getInt(TAG_IMG_HEIGHT);

                    Log.d(TAG, "Full resolution image. url: "
                            + fullResolutionUrl + ", w: " + width
                            + ", h: " + height);

                    ImageLoader imageLoader = AppController
                            .getInstance().getImageLoader();

                    // We download image into ImageView instead of
                    // NetworkImageView to have callback methods
                    // Currently NetworkImageView doesn't have callback
                    // methods

                    imageLoader.get(fullResolutionUrl,
                            new ImageListener() {

                                @Override
                                public void onErrorResponse(
                                        VolleyError arg0) {
                                    Toast.makeText(
                                            getApplicationContext(),
                                            getString(R.string.msg_wall_fetch_error),
                                            Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void onResponse(
                                        ImageContainer response,
                                        boolean arg1) {
                                    if (response.getBitmap() != null) {
                                        // load bitmap into imageview
                                        fullImageView
                                                .setImageBitmap(response
                                                        .getBitmap());
                                        PhotoViewAttacher photoViewAttacher = new PhotoViewAttacher(fullImageView);
                                        photoViewAttacher.update();
                                       // adjustImageAspect(width, height);

                                        // hide loader and show set &
                                        // download buttons
                                        pbLoader.setVisibility(View.GONE);
                                        llSetWallpaper.setVisibility(View.VISIBLE);
                                        llDownloadWallpaper.setVisibility(View.VISIBLE);
                                    }
                                }
                            });

                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.msg_unknown_error),
                            Toast.LENGTH_LONG).show();
                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG, "Error: " + error.getMessage());
                // unable to fetch wallpapers
                // either google username is wrong or
                // devices doesn't have internet connection
                Toast.makeText(getApplicationContext(),
                        getString(R.string.msg_wall_fetch_error),
                        Toast.LENGTH_LONG).show();

            }
        });

        // Remove the url from cache
        AppController.getInstance().getRequestQueue().getCache().remove(url);

        // Disable the cache for this url, so that it always fetches updated
        // json
        jsonObjReq.setShouldCache(false);

        // Adding request to request queue
        AppController.getInstance().addToRequestQueue(jsonObjReq);
    }

    /**
     * Adjusting the image aspect ratio to scroll horizontally, Image height
     * will be screen height, width will be calculated respected to height
     * */
    /*@SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void adjustImageAspect(int bWidth, int bHeight) {
        LayoutParams params = new LayoutParams(
                LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);

        if (bWidth == 0 || bHeight == 0)
            return;

        int sHeight = 0;

        if (android.os.Build.VERSION.SDK_INT >= 13) {
            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            sHeight = size.y;
        } else {
            Display display = getWindowManager().getDefaultDisplay();
            sHeight = display.getHeight();
        }

        int new_width = (int) Math.floor((double) bWidth * (double) sHeight
                / (double) bHeight);
        params.width = new_width;
        params.height = sHeight;

        Log.d(TAG, "Fullscreen image new dimensions: w = " + new_width
                + ", h = " + sHeight);

        fullImageView.setLayoutParams(params);
    }*/

    @Override
    public void onClick(View v) {
        bitmap = ((BitmapDrawable) fullImageView.getDrawable())
                .getBitmap();
        bitmapUri = getImageUri(this,bitmap);

        switch (v.getId()) {
            case R.id.fab_download:
                utils.saveImageToSDCard(bitmap);
                break;
            case R.id.fab_set_as_wallpaper:
                performCrop(bitmapUri);
                break;
            default:
                break;
        }
    }

    private void performCrop(Uri picUri){
//        int width = getWindowManager().getDefaultDisplay().getWidth();
//        int height = getWindowManager().getDefaultDisplay().getHeight();
        try {
            //call the standard crop action intent (the user device may not support it)
            Intent cropIntent = new Intent("com.android.camera.action.CROP");
            //indicate image type and Uri
            cropIntent.setDataAndType(picUri, "image/*");
            //set crop properties
            cropIntent.putExtra("crop", "true");
            //indicate aspect of desired crop
            /*cropIntent.putExtra("aspectX", width);
            cropIntent.putExtra("aspectY", height);*/
            //indicate output X and Y
            cropIntent.putExtra("outputX", 256);
            cropIntent.putExtra("outputY", 256);
            //retrieve data on return
            cropIntent.putExtra("return-data", true);
            //start the activity - we handle returning in onActivityResult
            startActivityForResult(cropIntent, PIC_CROP);
        }
        catch(ActivityNotFoundException anfe){
            //display an error message
            String errorMessage = "Whoops - your device doesn't support the crop action!";
            Toast toast = Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT);
            toast.show();
            utils.setAsWallpaper(bitmap);
        }
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PIC_CROP && data != null) {
            Bundle extras = data.getExtras();
            Bitmap thePic = extras.getParcelable("data");
            utils.setAsWallpaper(thePic);
        }
    }

}