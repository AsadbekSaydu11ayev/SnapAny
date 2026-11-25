package com.snapany.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.InputType;
import android.view.View;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "snapany_prefs";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String DEFAULT_SERVER_URL = "http://172.20.10.2:8000";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private WebView webView;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private LinearLayout errorLayout;
    private TextView tvErrorDetail;
    private SharedPreferences prefs;
    private String pendingSharedUrl = null;
    private boolean pageHasError = false;

    @SuppressLint({"SetJavaScriptEnabled", "JavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        webView = findViewById(R.id.webView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        progressBar = findViewById(R.id.progressBar);
        errorLayout = findViewById(R.id.errorLayout);
        tvErrorDetail = findViewById(R.id.tvErrorDetail);

        Button btnServerSettings = findViewById(R.id.btnServerSettings);
        Button btnReload = findViewById(R.id.btnReload);
        Button btnChangeServerError = findViewById(R.id.btnChangeServerError);
        Button btnRetry = findViewById(R.id.btnRetry);

        requestNeededPermissions();
        setupWebView();

        swipeRefresh.setColorSchemeColors(0xFF6366F1, 0xFFA855F7, 0xFFEC4899);
        swipeRefresh.setProgressBackgroundColorSchemeColor(0xFF0F172A);
        swipeRefresh.setOnRefreshListener(() -> {
            pageHasError = false;
            webView.reload();
        });

        btnServerSettings.setOnClickListener(v -> showServerUrlDialog());
        btnChangeServerError.setOnClickListener(v -> showServerUrlDialog());
        btnReload.setOnClickListener(v -> loadAppUrl(null));
        btnRetry.setOnClickListener(v -> loadAppUrl(pendingSharedUrl));

        handleIncomingIntent(getIntent());
        loadAppUrl(pendingSharedUrl);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIncomingIntent(intent);
        if (pendingSharedUrl != null) {
            loadAppUrl(pendingSharedUrl);
        }
    }

    private void handleIncomingIntent(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction()) && "text/plain".equals(intent.getType())) {
            String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (sharedText != null) {
                Matcher matcher = Pattern.compile("(https?://\\S+)").matcher(sharedText);
                if (matcher.find()) {
                    pendingSharedUrl = matcher.group(1);
                }
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                pageHasError = false;
                errorLayout.setVisibility(View.GONE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                swipeRefresh.setRefreshing(false);
                if (!pageHasError) {
                    errorLayout.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                }
                pendingSharedUrl = null;
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    pageHasError = true;
                    swipeRefresh.setRefreshing(false);
                    webView.setVisibility(View.GONE);
                    errorLayout.setVisibility(View.VISIBLE);
                    String serverUrl = getServerUrl();
                    tvErrorDetail.setText("Ulanib bo'lmadi: " + serverUrl + "\n\nKompyuteringizda SnapAny serveri ishlayotganini va telefon hamda kompyuter bitta Wi-Fi tarmog'ida ekanini tekshiring.");
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                enqueueDownload(url, contentDisposition, mimeType);
            }
        });
    }

    private String getServerUrl() {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL);
    }

    private void loadAppUrl(String sharedUrl) {
        pageHasError = false;
        errorLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        String baseUrl = getServerUrl().replaceAll("/+$", "");
        if (sharedUrl != null && !sharedUrl.isEmpty()) {
            try {
                String encoded = URLEncoder.encode(sharedUrl, StandardCharsets.UTF_8.toString());
                webView.loadUrl(baseUrl + "/?url=" + encoded);
                return;
            } catch (Exception ignored) {
            }
        }
        webView.loadUrl(baseUrl);
    }

    private void showServerUrlDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Server Manzilini Sozlash");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(getServerUrl());
        input.setHint("http://192.168.1.100:8000");
        input.setPadding(40, 30, 40, 30);

        builder.setView(input);

        builder.setPositiveButton("Saqlash", (dialog, which) -> {
            String newUrl = input.getText().toString().trim();
            if (!newUrl.isEmpty()) {
                if (!newUrl.startsWith("http://") && !newUrl.startsWith("https://")) {
                    newUrl = "http://" + newUrl;
                }
                prefs.edit().putString(KEY_SERVER_URL, newUrl).apply();
                loadAppUrl(pendingSharedUrl);
            }
        });
        builder.setNegativeButton("Bekor qilish", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    private void enqueueDownload(String url, String contentDisposition, String mimeType) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
            if (fileName == null || fileName.equals("downloadfile.bin")) {
                boolean isAudio = url.contains("_mp3") || url.contains("format_id=mp3") || url.contains("best_audio");
                fileName = "SnapAny_" + System.currentTimeMillis() + (isAudio ? ".mp3" : ".mp4");
            }

            request.setTitle(fileName);
            request.setDescription("SnapAny orqali yuklanmoqda...");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
            request.allowScanningByMediaScanner();

            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            if (dm != null) {
                dm.enqueue(request);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Yuklash boshlandi: " + fileName, Toast.LENGTH_LONG).show());
            }
        } catch (Exception e) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Yuklab olishda xatolik: " + e.getMessage(), Toast.LENGTH_LONG).show());
        }
    }

    private void requestNeededPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
            }
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    public class AndroidBridge {
        @JavascriptInterface
        public String getClipboardText() {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip() && clipboard.getPrimaryClip() != null) {
                ClipData.Item item = clipboard.getPrimaryClip().getItemAt(0);
                if (item != null && item.getText() != null) {
                    return item.getText().toString();
                }
            }
            return "";
        }

        @JavascriptInterface
        public void downloadFile(String url) {
            boolean isAudio = url.contains("_mp3") || url.contains("format_id=mp3") || url.contains("best_audio");
            String mimeType = isAudio ? "audio/mpeg" : "video/mp4";
            enqueueDownload(url, null, mimeType);
        }
    }
}
