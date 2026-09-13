package com.financefreedom.app.mediasave;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

@CapacitorPlugin(
    name = "MediaSave",
    permissions = @Permission(alias = "storage", strings = { Manifest.permission.WRITE_EXTERNAL_STORAGE })
)
public class MediaSavePlugin extends Plugin {

    @PluginMethod
    public void saveFile(PluginCall call) {
        // Android 9 及以下写公共目录需要运行时权限；Android 10+ 走 MediaStore 免权限
        if (Build.VERSION.SDK_INT < 29 && getPermissionState("storage") != PermissionState.GRANTED) {
            requestPermissionForAlias("storage", call, "storagePermCallback");
            return;
        }
        doSave(call);
    }

    @PermissionCallback
    private void storagePermCallback(PluginCall call) {
        if (getPermissionState("storage") == PermissionState.GRANTED) {
            doSave(call);
        } else {
            call.reject("未授予存储权限，无法保存文件");
        }
    }

    private void doSave(PluginCall call) {
        String base64 = call.getString("base64");
        String filename = call.getString("filename", "file");
        String mime = call.getString("mime", "application/octet-stream");
        boolean isImage = "image".equals(call.getString("kind", ""));
        if (base64 == null || base64.isEmpty()) {
            call.reject("缺少文件数据");
            return;
        }
        byte[] bytes;
        try {
            bytes = Base64.decode(base64, Base64.DEFAULT);
        } catch (Exception e) {
            call.reject("数据解码失败: " + e.getMessage());
            return;
        }
        try {
            String where = Build.VERSION.SDK_INT >= 29
                ? saveViaMediaStore(bytes, filename, mime, isImage)
                : saveLegacy(bytes, filename, isImage);
            JSObject ret = new JSObject();
            ret.put("uri", where);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("保存失败: " + e.getMessage());
        }
    }

    // Android 10+：MediaStore 收录，图片立即进图库、文件进系统 Download，无需权限与媒体扫描
    private String saveViaMediaStore(byte[] bytes, String filename, String mime, boolean isImage) throws Exception {
        ContentResolver resolver = getContext().getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, filename);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
        values.put(
            MediaStore.MediaColumns.RELATIVE_PATH,
            isImage ? Environment.DIRECTORY_PICTURES + "/我的财务自由" : Environment.DIRECTORY_DOWNLOADS
        );
        Uri collection = isImage
            ? MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            : MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri item = resolver.insert(collection, values);
        if (item == null) throw new Exception("MediaStore 插入失败");
        OutputStream out = resolver.openOutputStream(item);
        if (out == null) throw new Exception("无法打开输出流");
        out.write(bytes);
        out.flush();
        out.close();
        return item.toString();
    }

    // Android 9 及以下：写公共目录后触发媒体扫描，图片立刻出现在图库
    private String saveLegacy(byte[] bytes, String filename, boolean isImage) throws Exception {
        File dir = Environment.getExternalStoragePublicDirectory(
            isImage ? Environment.DIRECTORY_PICTURES : Environment.DIRECTORY_DOWNLOADS
        );
        if (isImage) dir = new File(dir, "我的财务自由");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("无法创建目录");
        File file = new File(dir, filename);
        FileOutputStream out = new FileOutputStream(file);
        out.write(bytes);
        out.flush();
        out.close();
        MediaScannerConnection.scanFile(getContext(), new String[]{ file.getAbsolutePath() }, null, null);
        return file.getAbsolutePath();
    }
}
