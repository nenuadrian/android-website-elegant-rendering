package com.codingfy.webviewgeneral;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.AbstractMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    WebView myWebView;
    MainActivity theInstance;

    //Linear Layouts for splash and loading screens
    private LinearLayout loadingScreen;
    private LinearLayout splashScreen;
    private LinearLayout networkError;

    /* for GCM, not used
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";
    private BroadcastReceiver mRegistrationBroadcastReceiver;
    */

    /*Variables to detect left, right scroll (go back and go forward */
    private int min_distance = 100;
    private float downX, downY, upX, upY;

    /*This is used so that the loading layout doesn't get on top of the splash screen
    when the app starts */
    boolean firstLoad = true;
    //int loadingCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firstLoad = true;
        theInstance = this;


        networkError = (LinearLayout) findViewById(R.id.networkerror);
        loadingScreen = (LinearLayout) findViewById(R.id.showLoadingScreen);
        splashScreen = (LinearLayout) findViewById(R.id.splashScreen);

        splashScreen.setVisibility(View.VISIBLE);


        /*
            linkToGoTo can be used to either:
                - get an intent with a link (e.g you click on a link
                    from an email and it'll tell the webview to go to that link
                - save the link in shouldOverrideUrlLoading(), so that when the user
                    tries to reload, it reloads the same link instead of the homepage
             CURRENTLY NOT USED
         */
        String linkToGoTo;
        Bundle extras = getIntent().getExtras();
        if(extras == null) {
            linkToGoTo= "noLink";
        } else {
            linkToGoTo= extras.getString("goTo");
        }



        myWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = myWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //Registering JS callback here.
        myWebView.addJavascriptInterface(new WebAppInterface(this), "Android");

        //To fix layout issues.
        myWebView.getSettings().setLoadWithOverviewMode(true);
        myWebView.getSettings().setUseWideViewPort(true);


        //Left, right navigation enabled/disabled.
        if((getResources().getString(R.string.left_right_nav_enabled).equals("y"))){
            myWebView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch(event.getAction()) { // Check vertical and horizontal touches
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
                                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.loading_previous), Toast.LENGTH_SHORT).show();
                                            }
                                            return true;
                                        }
                                        if (deltaX > 0) {
                                            if(myWebView.canGoForward()){
                                                myWebView.goForward();
                                                Toast.makeText(getApplicationContext(), getResources().getString(R.string.loading_next), Toast.LENGTH_SHORT).show();
                                            }
                                            return true;
                                        }
                                    } else {
                                        //not long enough swipe...
                                        return false;
                                    }
                                }
                                //VERTICAL SCROLL
                            /*
                            else {
                                if (Math.abs(deltaY) > min_distance) {
                                    // top or down
                                    if (deltaY < 0) {
                                        Log.d("going", "top to bottom");
                                        return false;
                                    }
                                    if (deltaY > 0) {
                                        Log.d("going", "bottom to top");

                                        return true;
                                    }
                                } else {
                                    //not long enough swipe...
                                    return false;
                                }
                            }
                            */
                                return false;
                            }
                        }
                    }
                    return false;
                }
            });
        }

        myWebView.setWebViewClient(new WebViewClient(){
            boolean ignoreDoneOnce = false;

            //This will be called each time a new link is tapped on.
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url + "?inapp=true");

                if(firstLoad){
                    myWebView.setVisibility(View.GONE);
                }
                else if(getResources().getString(R.string.loading_screen_enabled).equals("y")){
                    myWebView.setVisibility(View.GONE);
                    loadingScreen.setVisibility(View.VISIBLE);
                }
                return true;
            }


            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(myWebView, url);

                if(ignoreDoneOnce){
                    ignoreDoneOnce = false;
                }
                else {
                    view.clearCache(true);

                    if(firstLoad){
                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                firstLoad = false;
                                splashScreen.setVisibility(View.GONE);
                                myWebView.setVisibility(View.VISIBLE);
                            }
                        }, 300);
                    } else if(getResources().getString(R.string.loading_screen_enabled).equals("y")) {
                        myWebView.setVisibility(View.VISIBLE);
                        loadingScreen.setVisibility(View.GONE);

                    }
                    networkError.setVisibility(View.GONE);
                }

            }


            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                ignoreDoneOnce=true;

                Log.d("errorcode", String.valueOf(errorCode));
                Log.d("ERROR", description);
                Log.d("failing url", failingUrl);

                myWebView.setVisibility(View.GONE);
                splashScreen.setVisibility(View.GONE);
                networkError.setVisibility(View.VISIBLE);

                System.out.println("RECEIVED ERROR");

            }
        });

        if (linkToGoTo != null) {
            if(!linkToGoTo.equals("noLink")){
                //myWebView.loadUrl(linkToGoTo);
                linkToGoTo = "noLink";
            }
            else {
                //myWebView.loadUrl("http://secretrepublic.net/");
            }
        }
        else {
            //
            // myWebView.loadUrl("http://secretrepublic.net/");
        }

        myWebView.loadUrl(getResources().getString(R.string.url));

        Button retryConnecting = (Button)findViewById(R.id.retryConnecting);

        retryConnecting.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub

                myWebView.loadUrl(getResources().getString(R.string.url));
            }


        });


        /*
        //GCM, not usd
        mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //mRegistrationProgressBar.setVisibility(ProgressBar.GONE);
                SharedPreferences sharedPreferences =
                        PreferenceManager.getDefaultSharedPreferences(context);
                boolean sentToken = sharedPreferences
                        .getBoolean(QuickstartPreferences.SENT_TOKEN_TO_SERVER, false);
                if (sentToken) {
                    //mInformationTextView.setText(getString(R.string.gcm_send_message));
                    //hasToken = true;
                    //selectItem(0);

                } else {
                    Log.d("FAILURE", "failed to set token");
                    //mInformationTextView.setText(getString(R.string.token_error_message));
                }
            }
        };

        if (checkPlayServices()) {
            // Start IntentService to register this application with GCM.
            Intent intent = new Intent(this, RegistrationIntentService.class);
            startService(intent);
            Log.d("action", "starting intent to register token");
        }
        //GCM end


        String theToken = PreferenceManager.getDefaultSharedPreferences(MainActivity.theInstance).getString("theToken", "defaultStringIfNothingFound");

        if(!theToken.equals("defaultStringIfNothingFound")){
            boolean subscribedEverything = PreferenceManager.getDefaultSharedPreferences(theInstance).getBoolean("subscribedEverything", false);
            if(!subscribedEverything){
                //new subscribeToEverything().execute("");
            }
        }
       */

    }




    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    /* For GCM, not used now.
    private boolean checkPlayServices() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        Log.d(TAG, "Device supported");
        return true;
    }
    */


    public void goHome(View v){
        myWebView.loadUrl(getResources().getString(R.string.url));

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Check if the key event was the Back button and if there's history
        if ((keyCode == KeyEvent.KEYCODE_BACK) && myWebView.canGoBack()) {
            myWebView.goBack();
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


    /*Use this to construct a http request, if communicating to an API for example*/
    public String getQuery(List<AbstractMap.SimpleEntry> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (AbstractMap.SimpleEntry pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");

            //Log.d("sending key", pair.getKey().toString());
            //Log.d("sending value", pair.getValue().toString());
            result.append(URLEncoder.encode(pair.getKey().toString(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue().toString(), "UTF-8"));
        }

        return result.toString();
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
        public void showToast(String toast) {
            Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
        }
    }

}
