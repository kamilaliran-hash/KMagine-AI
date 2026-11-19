package com.kmagine.app;

import android.app.Activity;
import android.app.Dialog;
import android.os.Message;
import android.webkit.CookieManager;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.view.WindowInsets;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.webkit.WebResourceResponse;
import androidx.webkit.WebViewAssetLoader;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

public class MainActivity extends Activity {

    private static final int PICK_FILE = 1001;
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    // Serves the app from https://appassets.androidplatform.net/assets/ (a real secure web address)
    private static final String APP_URL = "https://appassets.androidplatform.net/assets/index.html";
    private WebViewAssetLoader assetLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int bg = Color.parseColor("#0a0a0b");
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(bg);
        webView = new WebView(this);
        webView.setBackgroundColor(bg);
        root.addView(webView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // Draw edge-to-edge on every Android version, then pad the content ourselves
        // so it stays clear of the status bar, navigation bar and keyboard.
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
        } else {
            root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int top, bottom, left, right;
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets i = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.ime() | WindowInsets.Type.displayCutout());
                top = i.top; bottom = i.bottom; left = i.left; right = i.right;
            } else {
                top = insets.getSystemWindowInsetTop();
                bottom = insets.getSystemWindowInsetBottom();
                left = insets.getSystemWindowInsetLeft();
                right = insets.getSystemWindowInsetRight();
            }
            v.setPadding(left, top, right, bottom);
            return insets;
        });

        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        // Puter's sign-in uses a pop-up window and cookies.
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                if ("appassets.androidplatform.net".equals(uri.getHost())) return false;
                if ("http".equals(scheme) || "https".equals(scheme)) {
                    // Open web links in the phone's browser instead of inside the app.
                    try { startActivity(new Intent(Intent.ACTION_VIEW, uri)); } catch (Exception ignored) { }
                    return true;
                }
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            // Pop-up windows (Puter sign-in): show them in a full-screen dialog.
            @Override
            public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, Message resultMsg) {
                final Dialog dialog = new Dialog(MainActivity.this, android.R.style.Theme_DeviceDefault_NoActionBar);
                final WebView popup = new WebView(MainActivity.this);
                WebSettings ps = popup.getSettings();
                ps.setJavaScriptEnabled(true);
                ps.setDomStorageEnabled(true);
                ps.setJavaScriptCanOpenWindowsAutomatically(true);
                CookieManager.getInstance().setAcceptThirdPartyCookies(popup, true);
                popup.setWebViewClient(new WebViewClient());
                popup.setWebChromeClient(new WebChromeClient() {
                    @Override
                    public void onCloseWindow(WebView window) {
                        dialog.dismiss();
                    }
                });
                dialog.setContentView(popup);
                dialog.setOnDismissListener(d -> popup.destroy());
                dialog.show();
                WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
                transport.setWebView(popup);
                resultMsg.sendToTarget();
                return true;
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                try {
                    startActivityForResult(Intent.createChooser(intent, "Choose a photo"), PICK_FILE);
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(APP_URL);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{ data.getData() };
            }
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }

    // ---------- helpers ----------

    private void reply(String id, int status, String text) {
        final String js = "window.__nativeCb(" + JSONObject.quote(id) + "," + status + "," + JSONObject.quote(text == null ? "" : text) + ")";
        runOnUiThread(() -> webView.evaluateJavascript(js, null));
    }

    private static byte[] readAll(InputStream in) throws Exception {
        if (in == null) return new byte[0];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
        in.close();
        return out.toByteArray();
    }

    private static byte[] download(String src) throws Exception {
        if (src.startsWith("data:")) {
            int comma = src.indexOf(',');
            return Base64.decode(src.substring(comma + 1), Base64.DEFAULT);
        }
        HttpURLConnection c = (HttpURLConnection) new URL(src).openConnection();
        c.setConnectTimeout(30000);
        c.setReadTimeout(180000);
        c.setInstanceFollowRedirects(true);
        int code = c.getResponseCode();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
        return readAll(c.getInputStream());
    }

    /** Called from the web app as window.AndroidBridge.* */
    private class Bridge {

        @JavascriptInterface
        public void post(final String id, final String url, final String headersJson, final String body) {
            new Thread(() -> {
                try {
                    if (!url.startsWith("https://api.x.ai/")) throw new Exception("Blocked URL");
                    HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
                    c.setRequestMethod("POST");
                    c.setDoOutput(true);
                    c.setConnectTimeout(30000);
                    c.setReadTimeout(300000);
                    JSONObject h = new JSONObject(headersJson);
                    Iterator<String> keys = h.keys();
                    while (keys.hasNext()) {
                        String k = keys.next();
                        c.setRequestProperty(k, h.getString(k));
                    }
                    OutputStream os = c.getOutputStream();
                    os.write(body.getBytes(StandardCharsets.UTF_8));
                    os.close();
                    int code = c.getResponseCode();
                    InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
                    String text = new String(readAll(in), StandardCharsets.UTF_8);
                    reply(id, code, text);
                } catch (Exception e) {
                    reply(id, 0, "Network error: " + e.getMessage());
                }
            }).start();
        }

        @JavascriptInterface
        public void getDataUrl(final String id, final String src) {
            new Thread(() -> {
                try {
                    byte[] bytes = download(src);
                    reply(id, 200, "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP));
                } catch (Exception e) {
                    reply(id, 0, e.getMessage());
                }
            }).start();
        }

        @JavascriptInterface
        public void saveImage(final String id, final String src, final String fileName) {
            new Thread(() -> {
                try {
                    byte[] bytes = download(src);
                    ContentResolver resolver = getContentResolver();
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    Uri uri;
                    if (android.os.Build.VERSION.SDK_INT >= 29) {
                        values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/KMagineAI");
                        values.put(MediaStore.Images.Media.IS_PENDING, 1);
                        uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                        if (uri == null) throw new Exception("Gallery unavailable");
                        OutputStream os = resolver.openOutputStream(uri);
                        os.write(bytes);
                        os.close();
                        ContentValues done = new ContentValues();
                        done.put(MediaStore.Images.Media.IS_PENDING, 0);
                        resolver.update(uri, done, null, null);
                    } else {
                        // Android 8–9: save inside the app's own Pictures folder (no permission needed).
                        java.io.File dir = new java.io.File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "KMagineAI");
                        dir.mkdirs();
                        java.io.File f = new java.io.File(dir, fileName);
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
                        fos.write(bytes);
                        fos.close();
                    }
                    reply(id, 200, "ok");
                } catch (Exception e) {
                    reply(id, 0, e.getMessage());
                }
            }).start();
        }
    }
}
