package com.arvingin.filezone;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.arialyy.annotations.Upload;
import com.arialyy.aria.core.Aria;
import com.arialyy.aria.core.upload.UploadTask;
import com.chaychan.fileexplorer.utils.OpenFileUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import me.panpf.sketch.SketchImageView;

public class MainActivity extends BaseActivity {

    public static final String KEY_FILE_ZONE = "FileZone";
    public static final String HTTPS_GITHUB_COM_EXCING_FILE_ZONE = "https://github.com/excing/FileZone";

    public static final int ORDER_BY_NAME = 1;
    public static final int ORDER_BY_SIZE = 2;
    public static final int ORDER_BY_TIME = 3;

    private static List<String> FilePaths = new ArrayList<>();
    private static String ExternalStorageDirectory = Environment.getExternalStorageDirectory().getAbsolutePath();

    private Toolbar toolbar;
    private EditText edit_upload_url;
    private SketchImageView image_file;
    private FloatingActionButton fab;

    private RecyclerView list_upload_file;
    private ItemAdapter itemAdapter;

    private String uploadUrl;
    private String currentRoot = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("FineZoneClient");

        fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                upload();
            }
        });
        fab.setBackgroundTintList(getResources().getColorStateList(R.color.selector_fab));

        image_file = new SketchImageView(this);
        int p = (int) getResources().getDimension(R.dimen.fab_margin);
        image_file.setPadding(0, p / 2, 0, p);
        image_file.setZoomEnabled(true);

        edit_upload_url = findViewById(R.id.edit_upload_url);
        list_upload_file = findViewById(R.id.list_upload_file);

        uploadUrl = getUploadUrl();

        setup();
    }

    private void setup() {
        edit_upload_url.setText(uploadUrl);

        list_upload_file.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        itemAdapter = new ItemAdapter(new OnItemClickListener() {
            @Override
            public void onSelect(String filepath) {
                if (new File(filepath).isDirectory()) {
                    loadFolder(filepath);
                } else {
                    if (FilePaths.contains(filepath)) {
                        FilePaths.remove(filepath);
                    } else {
                        FilePaths.add(filepath);
                    }
                    notifyData();
                }
            }

            @Override
            public void onOpen(String filepath) {
                if (new File(filepath).isDirectory()) {
                    loadFolder(filepath);
                } else {
                    openFile(filepath);
                }
            }
        });
        list_upload_file.setAdapter(itemAdapter);

        Aria.upload(this).register();

        requestPermissions(new RequestPermissionResultListener() {
            @Override
            public void onResult(boolean result) {
                if (result) {
                    loadFolder(ExternalStorageDirectory);
                }
            }
        }, Manifest.permission.READ_EXTERNAL_STORAGE);

        notifyData();
    }

    private String getUploadUrl() {
        SharedPreferences sp = getPreferences(MODE_PRIVATE);
        return sp.getString(KEY_FILE_ZONE, "http://192.168.1.3:8090/upload");
    }

    private void saveUploadUrl(String uploadUrl) {
        SharedPreferences.Editor editor = getPreferences(MODE_PRIVATE).edit();
        editor.putString(KEY_FILE_ZONE, uploadUrl);
        editor.apply();
    }

    private void loadFolder(String root) {
        currentRoot = root;

        File rootDir = new File(root);

        String[] files = rootDir.list();

        itemAdapter.clear();
        itemAdapter.setFiles(root, files);

        if (root.equals(ExternalStorageDirectory)) {
            toolbar.setTitle(getResources().getString(R.string.app_name));
        } else {
            toolbar.setTitle(root.substring(ExternalStorageDirectory.length()));
        }

        notifyData();
    }

    private void upload() {
        String uploadUrl = edit_upload_url.getText().toString();
        if (TextUtils.isEmpty(uploadUrl)) {
            uploadUrl = this.uploadUrl;
        } else {
            saveUploadUrl(uploadUrl);
        }
        for (String filepath : FilePaths) {
            Aria.upload(this).load(filepath)
                    .setUploadUrl(uploadUrl).setAttachment("file")
                    .asPost()
                    .start();
        }
    }

    private void openFile(String filepath) {
        if (isImage(filepath)) {
            openImageDialog(filepath);
        } else {
            Intent intent = OpenFileUtil.openFile(filepath);
            MainActivity.this.startActivityForResult(intent, 0);
        }
    }

    private void openImageDialog(String filepath) {
        if (null != image_file.getParent()) {
            ((ViewGroup) image_file.getParent()).removeView(image_file);
        }

        image_file.displayImage("file://" + filepath);

        new AlertDialog.Builder(this)
                .setTitle(new File(filepath).getName())
                .setView(image_file)
                .show();
    }

    private void copyGithubUrl() {
        ClipboardManager clipManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("FileZoneGithubUrl", HTTPS_GITHUB_COM_EXCING_FILE_ZONE);
        clipManager.setPrimaryClip(clip);
    }

    private void release() {
        for (String filepath : FilePaths) {
            Aria.upload(this).load(filepath).cancel();
        }

        FilePaths.clear();

        notifyData();
    }

    private void notifyData() {
        fab.setEnabled(!FilePaths.isEmpty());
        itemAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        release();

        Aria.upload(this).unRegister();

        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (Environment.getExternalStorageDirectory().getAbsolutePath().equals(currentRoot)) {
            release();
            super.onBackPressed();
        } else {
            loadFolder(new File(currentRoot).getParent());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_name_order_by:
                itemAdapter.sort(ORDER_BY_NAME);
                break;
            case R.id.action_size_order_by:
                itemAdapter.sort(ORDER_BY_SIZE);
                break;
            case R.id.action_time_order_by:
                itemAdapter.sort(ORDER_BY_TIME);
                break;
            case R.id.action_github:
                Snackbar.make(toolbar, "GitHub: " + HTTPS_GITHUB_COM_EXCING_FILE_ZONE, Snackbar.LENGTH_LONG)
                        .setAction("Copy", new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                copyGithubUrl();
                                Snackbar.make(toolbar, "Success", Snackbar.LENGTH_LONG).show();
                            }
                        }).show();
                break;
        }
        notifyData();
        return super.onOptionsItemSelected(item);
    }

    @Upload.onTaskStart
    public void taskStart(UploadTask task) {
        Snackbar.make(toolbar, "开始上传：" + task.getEntity().getFilePath(), Snackbar.LENGTH_LONG).show();
    }

    @Upload.onTaskComplete
    public void taskComplete(UploadTask task) {
        String resp = task.getEntity().getResponseStr();
        if ("{\"code\": 0}".equals(resp)) {
            Snackbar.make(toolbar, "上传成功：" + task.getEntity().getFilePath(), Snackbar.LENGTH_LONG).show();
            FilePaths.remove(task.getEntity().getFilePath());
            notifyData();
        } else {
            taskFail(task);
        }
    }

    @Upload.onTaskFail
    public void taskFail(UploadTask task) {
        Snackbar.make(toolbar, "上传失败：" + task.getEntity().getFilePath(), Snackbar.LENGTH_LONG).show();
    }

    static class ItemAdapter extends RecyclerView.Adapter<ItemHolder> {

        private List<String> fileList;
        private boolean orderBy = false;

        private OnItemClickListener l;

        ItemAdapter(OnItemClickListener l) {
            this.l = l;

            fileList = new ArrayList<>();
        }

        void clear() {
            fileList.clear();
        }

        void setFiles(String root, String[] files) {
            for (String file : files) {
                fileList.add(root + File.separator + file);
            }
        }

        void sort(int type) {
            orderBy = !orderBy;
            sortFile(fileList, type, orderBy);
        }

        @NonNull
        @Override
        public ItemHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ItemHolder(LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_file, viewGroup, false), l);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder viewHolder, int i) {
            viewHolder.setItem(fileList.get(i));
        }

        @Override
        public int getItemCount() {
            return null == fileList ? 0 : fileList.size();
        }
    }

    static class ItemHolder extends RecyclerView.ViewHolder {
        private ImageView imageFileIcon;
        private TextView textFileName;
        private CheckBox checkFileSelect;

        private OnItemClickListener l;

        ItemHolder(@NonNull View itemView, OnItemClickListener l) {
            super(itemView);

            this.l = l;

            findViews();
        }

        private void findViews() {
            imageFileIcon = itemView.findViewById(R.id.image_file_icon);
            textFileName = itemView.findViewById(R.id.text_file_name);
            checkFileSelect = itemView.findViewById(R.id.check_file_select);

            checkFileSelect.setClickable(false);
        }

        void setItem(final String filepath) {
            File file = new File(filepath);

            if (file.isDirectory()) {
                imageFileIcon.setImageResource(R.drawable.baseline_folder_black_36);
                checkFileSelect.setVisibility(View.GONE);
            } else {
                if (isImage(filepath)) {
                    imageFileIcon.setImageResource(R.drawable.baseline_insert_photo_black_36);
                } else {
                    imageFileIcon.setImageResource(R.drawable.baseline_insert_drive_file_black_36);
                }

                checkFileSelect.setVisibility(View.VISIBLE);
            }

            textFileName.setText(file.getName());
            checkFileSelect.setChecked(FilePaths.contains(filepath));

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkFileSelect.setChecked(!checkFileSelect.isChecked());

                    l.onSelect(filepath);
                }
            });

            imageFileIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    l.onOpen(filepath);
                }
            });
        }
    }

    interface OnItemClickListener {
        void onSelect(String filepath);

        void onOpen(String filepath);
    }

    public static boolean isImage(String filepath) {
        return filepath.endsWith(".jpg") || filepath.endsWith(".png") || filepath.endsWith(".bmp") || filepath.endsWith(".gif");
    }

    /**
     * 文件排序
     *
     * @param fileList   文件路径列表
     * @param sortMethod 排序方法，0：忽略；1：按名称排序；2：按大小排序；3：按时间排序
     * @param orderBy    true：升序；false：降序
     */
    public static void sortFile(List<String> fileList, final int sortMethod, final boolean orderBy) {
        if (0 == sortMethod) return;

        Collections.sort(fileList, new Comparator<String>() {
            @Override
            public int compare(String object1, String object2) {
                return compareFile(object1, object2, sortMethod, orderBy);
            }
        });

    }

    public static int compareFile(String f1, String f2, int sortMethod, boolean orderBy) {
        File object1 = new File(f1);
        File object2 = new File(f2);

        if (ORDER_BY_NAME == sortMethod) {
            return compareByName(object1, object2, orderBy);
        } else if (ORDER_BY_SIZE == sortMethod) {
            int len = compareBySize(object1.length(), object2.length(), orderBy);
            // the same size ,sort by name
            if (len == 0) {
                return compareByName(object1, object2, orderBy);
            } else {
                return compareBySize(object1.length(), object2.length(), orderBy);
            }
        } else if (ORDER_BY_TIME == sortMethod) {
            int len = compareByDate(object1.lastModified(),
                    object2.lastModified(), orderBy);
            // the same data ,sort by name
            if (len == 0) {
                return compareByName(object1, object2, orderBy);
            } else {
                return compareByDate(object1.lastModified(),
                        object2.lastModified(), orderBy);
            }
        }
        return 0;
    }

    private static int compareByName(File object1, File object2, boolean orderBy) {
        String objectName1 = object1.getName().toLowerCase();
        String objectName2 = object2.getName().toLowerCase();
        int result = objectName1.compareTo(objectName2);
        if (result == 0) {
            return 0;
        } else {
            if (result < 0) {
                return orderBy ? -1 : 1;
            } else {
                return orderBy ? 1 : -1;
            }
        }
    }

    private static int compareBySize(long object1, long object2, boolean orderBy) {
        long diff = object1 - object2;
        if (diff > 0) {
            return orderBy ? 1 : -1;
        } else if (diff == 0) {
            return 0;
        } else {
            return orderBy ? -1 : 1;
        }
    }

    public static int compareByDate(long object1, long object2, boolean orderBy) {
        long diff = object1 - object2;
        if (diff > 0) {
            return orderBy ? 1 : -1;
        } else if (diff == 0) {
            return 0;
        } else {
            return orderBy ? -1 : 1;
        }
    }
}
