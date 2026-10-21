package com.gilson.flyff;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.*;
import java.io.*;
import java.net.*;
import java.security.MessageDigest;
import java.util.*;

public class MainActivity extends Activity {
  WebView web;
  File dir;
  static final String[] EXT = {".png",".jpg",".jpeg",".webp",".gif",".mp3",".ogg",".wav",".woff",".woff2",".ttf",".glb",".gltf",".bin",".dds",".ktx2"};

  @Override protected void onCreate(Bundle b) {
    super.onCreate(b);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    dir = new File(getFilesDir(), "gamecache");
    dir.mkdirs();
    web = new WebView(this);
    setContentView(web);
    web.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
      | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    WebSettings s = web.getSettings();
    s.setJavaScriptEnabled(true);
    s.setDomStorageEnabled(true);
    s.setMediaPlaybackRequiresUserGesture(false);
    web.setWebViewClient(new WebViewClient() {
      @Override public WebResourceResponse shouldInterceptRequest(WebView v, WebResourceRequest r) {
        return handle(r);
      }
    });
    web.loadUrl("https://universe.flyff.com/play");
  }

  WebResourceResponse handle(WebResourceRequest r) {
    try {
      if (!"GET".equals(r.getMethod())) return null;
      String url = r.getUrl().toString();
      String path = r.getUrl().getPath();
      if (path == null || !url.startsWith("https://")) return null;
      String low = path.toLowerCase();
      boolean ok = false;
      for (String e : EXT) if (low.endsWith(e)) { ok = true; break; }
      if (!ok) return null;
      String key = md5(url);
      File f = new File(dir, key);
      if (!f.exists()) {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        if (c.getResponseCode() != 200) return null;
        File t = new File(dir, key + ".tmp");
        try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(t)) {
          byte[] buf = new byte[16384]; int n;
          while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
        t.renameTo(f);
      }
      String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(url));
      if (mime == null) mime = "application/octet-stream";
      Map<String,String> h = new HashMap<>();
      h.put("Access-Control-Allow-Origin", "*");
      return new WebResourceResponse(mime, null, 200, "OK", h, new FileInputStream(f));
    } catch (Exception e) { return null; }
  }

  static String md5(String s) throws Exception {
    byte[] d = MessageDigest.getInstance("MD5").digest(s.getBytes("UTF-8"));
    StringBuilder sb = new StringBuilder();
    for (byte x : d) sb.append(String.format("%02x", x));
    return sb.toString();
  }

  @Override public void onBackPressed() {
    if (web.canGoBack()) web.goBack();
  }
}
