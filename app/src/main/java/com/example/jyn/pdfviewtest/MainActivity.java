package com.example.jyn.pdfviewtest;


import android.Manifest;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.gun0912.tedpermission.PermissionListener;
import com.gun0912.tedpermission.TedPermission;
import com.shockwave.pdfium.PdfDocument;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends Activity implements OnPageChangeListener, OnLoadCompleteListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int MAX_RETRY = 5;

    PDFView pdfView;
    Uri pdfUri;
    Integer pageNumber = 0;
    String pdfFileName;

    int retryCount = 0;

    PermissionListener permissionListener;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        pdfView = findViewById(R.id.pdfView);

//        showPDF();

        displayFromAsset("sample.pdf");

        // 퍼미션 리스너(테드_ 라이브러리)
        permissionListener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
//                Toast.makeText(a_profile.this, "권한 허가", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
//                Toast.makeText(a_profile.this, "권한 거부", Toast.LENGTH_SHORT).show();
            }
        };
        permission_check();
    }

    public void showPDF() {
        Log.d("MainActivity", "FilePath: " + getIntent().getStringExtra("pdfFile"));

        pdfView.fromFile(new File(getIntent().getStringExtra("pdfFile")))
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .onError(errorListener)
                .scrollHandle(new DefaultScrollHandle(this))
                .load();
    }

    private void displayFromAsset(String assetFileName) {
        pdfFileName = assetFileName;
        Log.d(TAG, "pdfFileName: " + pdfFileName);

        pdfView.fromAsset("sample.pdf")
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .scrollHandle(new DefaultScrollHandle(this))
                .load();
    }

    OnErrorListener errorListener = new OnErrorListener() {
        @Override
        public void onError(Throwable t) {
            t.printStackTrace();

            if (retryCount >= MAX_RETRY) {
                Toast.makeText(MainActivity.this, "PDF error", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                retryCount++;

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "error_occur");
//                        showPDF();
                    }
                }, 1000);
            }
        }
    };

    private void displayFromUri(Uri uri) {
        pdfFileName = getFileName(uri);

        pdfView.fromUri(uri)
                .defaultPage(pageNumber)
                .onPageChange(this)
                .enableAnnotationRendering(true)
                .onLoad(this)
                .onError(errorListener)
                .scrollHandle(new DefaultScrollHandle(this))
                .load();
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));

        Log.d(TAG, String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        Log.e(TAG, "title = " + meta.getTitle());
        Log.e(TAG, "author = " + meta.getAuthor());
        Log.e(TAG, "subject = " + meta.getSubject());
        Log.e(TAG, "keywords = " + meta.getKeywords());
        Log.e(TAG, "creator = " + meta.getCreator());
        Log.e(TAG, "producer = " + meta.getProducer());
        Log.e(TAG, "creationDate = " + meta.getCreationDate());
        Log.e(TAG, "modDate = " + meta.getModDate());

        printBookmarksTree(pdfView.getTableOfContents(), "-");

    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    /**---------------------------------------------------------------------------
     메소드 ==> 퍼미션 체크
     ---------------------------------------------------------------------------*/
    public void permission_check() {
        // 퍼미션 확인(테드_ 라이브러리)
        new TedPermission(this)
                .setPermissionListener(permissionListener)
//                .setRationaleMessage("다음 작업을 허용하시겠습니까? 기기 사진, 미디어, 파일 액세스")
                .setDeniedMessage("[설정] > [권한] 에서 권한을 허용할 수 있습니다")
                .setGotoSettingButton(true)
                .setPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE)
                .check();
    }
}
