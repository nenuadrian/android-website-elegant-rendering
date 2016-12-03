package com.codingfy.webviewgeneral;


import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class MainActivity extends AppCompatActivity {

    WebView myWebView;
    MainActivity theInstance;

    //Linear Layouts for splash and loading screens
    private LinearLayout loadingScreen;
    private LinearLayout splashScreen;
    private LinearLayout networkError;
    private RelativeLayout webViewRelativeLayout;
    private Button retryConnecting;
    private ImageView goHomeButton;
    private ImageView logo;
    private boolean keepFading = false;
    private String lastLink;


    /*Variables to detect left, right scroll (go back and go forward */
    private int min_distance = 100;
    private float downX, downY, upX, upY;

    /*This is used so that the loading layout doesn't get on top of the splash screen
    when the app starts */
    boolean firstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firstLoad = true;
        theInstance = this;


        networkError = (LinearLayout) findViewById(R.id.networkerror);
        loadingScreen = (LinearLayout) findViewById(R.id.showLoadingScreen);
        splashScreen = (LinearLayout) findViewById(R.id.splashScreen);
        webViewRelativeLayout = (RelativeLayout) findViewById(R.id.webviewRelativeLayout);
        retryConnecting = (Button)findViewById(R.id.retryConnecting);
        goHomeButton = (ImageView) findViewById(R.id.goHomeButton);
        logo = (ImageView) findViewById(R.id.logo);


        goHomeButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                goHome(v);
            }
        });



        myWebView = (WebView) findViewById(R.id.webview);
        myWebView.setVisibility(View.VISIBLE);
        webViewRelativeLayout.setVisibility(View.VISIBLE);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideWebDisplaySplash();
            }
        }, 200);


        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        //To fix layout issues.
        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setUseWideViewPort(true);


        //Left, right navigation enabled/disabled.
        if((getResources().getString(R.string.left_right_nav_enabled).equals("y"))){
            myWebView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN: {
                            downX = event.getX();
                            downY = event.getY();
                            return false;
                        }
                        case MotionEvent.ACTION_UP: {
                            Display display = getWindowManager().getDefaultDisplay();
                            Point size = new Point();
                            display.getSize(size);
                            int width = size.x;

                            if(downX < 200 || downX > width-200){
                                upX = event.getX();
                                upY = event.getY();

                                float deltaX = downX - upX;
                                float deltaY = downY - upY;

                                //HORIZONTAL SCROLL
                                if (Math.abs(deltaX) > Math.abs(deltaY)) {
                                    if (Math.abs(deltaX) > min_distance) {
                                        // left or right
                                        if (deltaX < 0) {
                                            if(myWebView.canGoBack()){
                                                myWebView.goBack();
                                                //Toast.makeText(getApplicationContext(), getResources().getString(R.string.loading_previous), Toast.LENGTH_SHORT).show();
                                            }
                                            return true;
                                        }
                                        if (deltaX > 0) {
                                            if(myWebView.canGoForward()){
                                                myWebView.goForward();
                                                //Toast.makeText(getApplicationContext(), getResources().getString(R.string.loading_next), Toast.LENGTH_SHORT).show();
                                            }
                                            return true;
                                        }
                                    } else {
                                        //not long enough swipe...
                                        return false;
                                    }
                                }
                                return false;
                            }
                        }
                    }
                    return false;
                }
            });
        }


        myWebView.setBackgroundColor(Color.BLACK);

        myWebView.setWebViewClient(new WebViewClient(){
            boolean ignoreDoneOnce = false;

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);

                if(!url.contains("alpha.secretrepublic.net")){
                   goHomeButton.setVisibility(View.VISIBLE);
                } else {
                    goHomeButton.setVisibility(View.GONE);
                    lastLink = url;
                }


                if(firstLoad){
                    webViewRelativeLayout.setVisibility(View.GONE);
                } else if(getResources().getString(R.string.loading_screen_enabled).equals("y")){
                    hideWebDisplayLoad();
                    keepFading = true;
                    fadeOutCall(logo);
                }
                return true;
            }


            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(myWebView, url);

                if(ignoreDoneOnce){
                    ignoreDoneOnce = false;
                } else {
                    view.clearCache(true);

                    if(firstLoad){
                        firstLoad = false;
                        final WebView localView = view;
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                hideSplahDisplayWeb();
                                localView.loadUrl("javascript:loadedFromApp()");

                            }
                        }, 0);
                    } else if(getResources().getString(R.string.loading_screen_enabled).equals("y")) {
                        hideLoadDisplayWeb();
                        keepFading = false;
                        view.loadUrl("javascript:loadedFromApp()");
                    }
                    networkError.setVisibility(View.GONE);
                }
            }


            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                ignoreDoneOnce=true;

                Log.d("failing url", failingUrl);
                hideEverythingAndShowError();
            }
        });


        myWebView.loadUrl(getResources().getString(R.string.url));
        retryConnecting.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(lastLink.length()!=0){
                    myWebView.loadUrl(lastLink);
                } else {
                    myWebView.loadUrl(getResources().getString(R.string.url));
                }
            }
        });
    }




    public void goHome(View v){
        myWebView.loadUrl(lastLink);
        goHomeButton.setVisibility(View.GONE);
    }

    public void hideLoadDisplayWeb(){
        loadingScreen.setVisibility(View.GONE);
        webViewRelativeLayout.setVisibility(View.VISIBLE);
    }

    public void hideSplahDisplayWeb(){
        splashScreen.setVisibility(View.GONE);
        webViewRelativeLayout.setVisibility(View.VISIBLE);
    }

    public void hideBothDisplayWeb(){
        splashScreen.setVisibility(View.GONE);
        loadingScreen.setVisibility(View.GONE);
        webViewRelativeLayout.setVisibility(View.VISIBLE);
    }

    public void hideWebDisplaySplash(){
        webViewRelativeLayout.setVisibility(View.GONE);
        splashScreen.setVisibility(View.VISIBLE);
    }

    public void hideWebDisplayLoad(){
        webViewRelativeLayout.setVisibility(View.GONE);
        loadingScreen.setVisibility(View.VISIBLE);
    }

    public void hideEverythingAndShowError(){
        loadingScreen.setVisibility(View.GONE);
        splashScreen.setVisibility(View.GONE);
        webViewRelativeLayout.setVisibility(View.GONE);
        networkError.setVisibility(View.VISIBLE);
    }



    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && myWebView.canGoBack()) {
            myWebView.goBack();


            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if((myWebView.getUrl().contains("alpha.secretrepublic.net"))){
                        goHomeButton.setVisibility(View.GONE);
                    }
                }
            }, 1000);


            return true;
        }
        // If it wasn't the Back key or there's no web page history, bubble up to the default
        // system behavior (probably exit the activity)
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        return super.onOptionsItemSelected(item);
    }


    //The JS handler
    public class WebAppInterface {
        Context mContext;

        /** Instantiate the interface and set the context */
        WebAppInterface(Context c) {
            mContext = c;
        }

        /** Show a toast from the web page */
        @JavascriptInterface
        public void playSound(String soundName) {
            new BackgroundSound().execute(soundName);
        }
    }


    private void fadeInCall(final ImageView img)
    {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator());
        fadeIn.setDuration(500);

        AnimationSet animation = new AnimationSet(false);
        animation.addAnimation(fadeIn);


        fadeIn.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
               if(keepFading){
                   fadeOutCall(img);
               }
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });

        img.startAnimation(fadeIn);
    }


    private void fadeOutCall(final ImageView img)
    {
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setDuration(500);

        AnimationSet animation = new AnimationSet(false); //change to false
        animation.addAnimation(fadeOut);


        fadeOut.setAnimationListener(new Animation.AnimationListener()
        {
            public void onAnimationEnd(Animation animation)
            {
                fadeInCall(img);
            }
            public void onAnimationRepeat(Animation animation) {}
            public void onAnimationStart(Animation animation) {}
        });

        img.startAnimation(fadeOut);
    }


    public class BackgroundSound extends AsyncTask<String, Void, Void> {
        @Override
        protected Void doInBackground(String... params) {
            MediaPlayer player = MediaPlayer.create(theInstance, getResources().getIdentifier(params[0],
                    "raw", getPackageName()));
            player.setLooping(true); // Set looping
            player.setVolume(100,100);
            player.start();
            player.setLooping(false);
            return null;
        }
    }

}
