package dev.devanshu.phonecontrol;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityService.ScreenshotResult;
import android.accessibilityservice.AccessibilityService.TakeScreenshotCallback;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Path;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PhoneControlService extends AccessibilityService {
    public static final String TAG = "PhoneControl";

    public static final String ACTION = "dev.devanshu.phonecontrol.COMMAND";
    private static PhoneControlService instance;

    private BroadcastReceiver receiver;
    private int screenWidth;
    private int screenHeight;

    public static boolean isConnected() {
        return instance != null;
    }

    public static PhoneControlService getInstance() {
        return instance;
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;

        screenWidth = getResources().getDisplayMetrics().widthPixels;
        screenHeight = getResources().getDisplayMetrics().heightPixels;

        IntentFilter filter = new IntentFilter(ACTION);
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                handle(intent);
            }
        };

        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }

        Log.i(TAG, "ACCESSIBILITY_READY");
    }

    private void handle(Intent in) {
        String op = in.getStringExtra("op");
        if (op == null) return;

        Log.i(TAG, "COMMAND " + op);

        switch (op) {
            case "tap":
                tap(in.getIntExtra("x", 0), in.getIntExtra("y", 0), 80);
                break;
            case "longpress":
                tap(in.getIntExtra("x", 0), in.getIntExtra("y", 0), in.getLongExtra("duration", 600));
                break;
            case "doubletap":
                doubleTap(in.getIntExtra("x", 0), in.getIntExtra("y", 0));
                break;
            case "swipe":
                swipe(in.getIntExtra("x1", 0), in.getIntExtra("y1", 0),
                        in.getIntExtra("x2", 0), in.getIntExtra("y2", 0),
                        in.getLongExtra("duration", 350));
                break;
            case "back":
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case "home":
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;
            case "recents":
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                break;
            case "notifications":
                performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);
                break;
            case "quick_settings":
                performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
                break;
            case "lock":
                if (Build.VERSION.SDK_INT >= 28) performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
                break;
            case "power":
                if (Build.VERSION.SDK_INT >= 30) performGlobalAction(GLOBAL_ACTION_POWER_DIALOG);
                break;
            case "type":
                setFocusedText(in.getStringExtra("text"));
                break;
            case "clear_text":
                setFocusedText("");
                break;
            case "paste":
                focusedAction(AccessibilityNodeInfo.ACTION_PASTE);
                break;
            case "click_text":
                clickText(in.getStringExtra("text"));
                break;
            case "click_id":
                clickId(in.getStringExtra("id"));
                break;
            case "focus_text":
                focusText(in.getStringExtra("text"));
                break;
            case "scroll_forward":
                focusedOrRootScroll(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
                break;
            case "scroll_backward":
                focusedOrRootScroll(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
                break;
            case "dump_ui":
                dumpUi();
                break;
            case "screenshot":
                screenshot();
                break;
            case "launch":
                launchPackage(in.getStringExtra("package"));
                break;
            case "open_uri":
                openUri(in.getStringExtra("uri"), in.getStringExtra("mime"));
                break;
            case "presskey":
                pressGlobalKey(in.getIntExtra("keyCode", KeyEvent.KEYCODE_BACK));
                break;
            case "status":
                Log.i(TAG, "STATUS connected=true");
                break;
        }
    }

    private void tap(int x, int y, long duration) {
        Path p = new Path();
        p.moveTo(x, y);
        GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, duration))
                .build();
        dispatchGesture(g, null, null);
    }

    private void doubleTap(int x, int y) {
        Path p1 = new Path();
        p1.moveTo(x, y);
        Path p2 = new Path();
        p2.moveTo(x, y);

        GestureDescription.StrokeDescription s1 =
                new GestureDescription.StrokeDescription(p1, 0, 45);
        GestureDescription.StrokeDescription s2 =
                new GestureDescription.StrokeDescription(p2, 120, 45);

        GestureDescription g = new GestureDescription.Builder()
                .addStroke(s1)
                .addStroke(s2)
                .build();
        dispatchGesture(g, null, null);
    }

    private void swipe(int x1, int y1, int x2, int y2, long duration) {
        Path p = new Path();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);

        GestureDescription g = new GestureDescription.Builder()
                .addStroke(new GestureDescription.StrokeDescription(p, 0, duration))
                .build();
        dispatchGesture(g, null, null);
    }

    private AccessibilityNodeInfo root() {
        return getRootInActiveWindow();
    }

    private void setFocusedText(String text) {
        AccessibilityNodeInfo f = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (f == null) {
            Log.w(TAG, "NO_INPUT_FOCUS");
            return;
        }

        Bundle args = new Bundle();
        args.putCharSequence(
                AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                text == null ? "" : text
        );
        boolean ok = f.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
        Log.i(TAG, "SET_TEXT=" + ok);
    }

    private void focusedAction(int action) {
        AccessibilityNodeInfo f = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (f != null) Log.i(TAG, "FOCUSED_ACTION=" + f.performAction(action));
    }

    private AccessibilityNodeInfo findByText(AccessibilityNodeInfo node, String wanted) {
        if (node == null || wanted == null) return null;

        CharSequence t = node.getText();
        CharSequence d = node.getContentDescription();

        if (t != null && wanted.equalsIgnoreCase(t.toString())) return node;
        if (d != null && wanted.equalsIgnoreCase(d.toString())) return node;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findByText(node.getChild(i), wanted);
            if (found != null) return found;
        }
        return null;
    }

    private void clickText(String text) {
        AccessibilityNodeInfo r = root();
        AccessibilityNodeInfo n = findByText(r, text);
        if (n == null) {
            Log.w(TAG, "TEXT_NOT_FOUND " + text);
            return;
        }

        boolean ok = n.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        Log.i(TAG, "CLICK_TEXT=" + ok + " text=" + text);
    }

    private void clickId(String id) {
        AccessibilityNodeInfo r = root();
        if (r == null || id == null) return;

        List<AccessibilityNodeInfo> nodes = r.findAccessibilityNodeInfosByViewId(id);
        if (nodes == null || nodes.isEmpty()) {
            Log.w(TAG, "ID_NOT_FOUND " + id);
            return;
        }

        Log.i(TAG, "CLICK_ID=" + nodes.get(0).performAction(AccessibilityNodeInfo.ACTION_CLICK));
    }

    private void focusText(String text) {
        AccessibilityNodeInfo r = root();
        AccessibilityNodeInfo n = findByText(r, text);
        if (n != null) Log.i(TAG, "FOCUS_TEXT=" + n.performAction(AccessibilityNodeInfo.ACTION_FOCUS));
    }

    private void focusedOrRootScroll(int action) {
        AccessibilityNodeInfo r = root();
        if (r == null) return;

        AccessibilityNodeInfo f = findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (f != null && f.performAction(action)) return;

        if (!r.performAction(action)) {
            swipe(screenWidth / 2, (int) (screenHeight * 0.75),
                    screenWidth / 2, (int) (screenHeight * 0.25), 300);
        }
    }

    private void launchPackage(String pkg) {
        if (pkg == null || pkg.isEmpty()) return;

        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch == null) {
                Log.w(TAG, "NO_LAUNCH_INTENT " + pkg);
                return;
            }
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
            Log.i(TAG, "LAUNCHED " + pkg);
        } catch (Exception e) {
            Log.e(TAG, "LAUNCH_FAILED " + pkg, e);
        }
    }

    private void openUri(String rawUri, String mime) {
        if (rawUri == null || rawUri.isEmpty()) return;

        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(rawUri));
            if (mime != null && !mime.isEmpty()) i.setType(mime);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            Log.e(TAG, "OPEN_URI_FAILED", e);
        }
    }

    private void pressGlobalKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                performGlobalAction(GLOBAL_ACTION_BACK);
                break;
            case KeyEvent.KEYCODE_HOME:
                performGlobalAction(GLOBAL_ACTION_HOME);
                break;
            case KeyEvent.KEYCODE_APP_SWITCH:
                performGlobalAction(GLOBAL_ACTION_RECENTS);
                break;
            default:
                Log.w(TAG, "UNSUPPORTED_GLOBAL_KEY " + keyCode);
        }
    }

    private void dumpUi() {
        AccessibilityNodeInfo r = root();
        if (r == null) {
            Log.w(TAG, "NO_ROOT");
            return;
        }

        StringBuilder out = new StringBuilder(16384);
        appendNode(r, out, 0);
        writeText("ui-tree-" + System.currentTimeMillis() + ".txt", out.toString());
        Log.i(TAG, "UI_TREE_WRITTEN");
    }

    private void appendNode(AccessibilityNodeInfo node, StringBuilder out, int depth) {
        if (node == null || depth > 15) return;

        Rect b = new Rect();
        node.getBoundsInScreen(b);

        out.append(spaces(depth))
                .append("{class=")
                .append(node.getClassName())
                .append(",text=")
                .append(clean(node.getText()))
                .append(",desc=")
                .append(clean(node.getContentDescription()))
                .append(",id=")
                .append(clean(node.getViewIdResourceName()))
                .append(",clickable=")
                .append(node.isClickable())
                .append(",enabled=")
                .append(node.isEnabled())
                .append(",bounds=")
                .append(b)
                .append("}\n");

        for (int i = 0; i < node.getChildCount(); i++) {
            appendNode(node.getChild(i), out, depth + 1);
        }
    }

    private String spaces(int n) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < n; i++) s.append("  ");
        return s.toString();
    }

    private String clean(CharSequence c) {
        if (c == null) return "";
        return c.toString().replace("\n", " ").replace("\r", " ");
    }

    private void screenshot() {
        if (Build.VERSION.SDK_INT < 30) {
            Log.w(TAG, "SCREENSHOT_API_UNAVAILABLE");
            return;
        }

        takeScreenshot(android.view.Display.DEFAULT_DISPLAY, getMainExecutor(),
                new TakeScreenshotCallback() {
                    @Override
                    public void onSuccess(ScreenshotResult result) {
                        Bitmap hw = Bitmap.wrapHardwareBuffer(
                                result.getHardwareBuffer(),
                                result.getColorSpace()
                        );
                        if (hw == null) {
                            Log.w(TAG, "SCREENSHOT_BITMAP_NULL");
                            return;
                        }

                        Bitmap bmp = hw.copy(Bitmap.Config.ARGB_8888, false);
                        hw.recycle();
                        writeImage("screen-" + System.currentTimeMillis() + ".png", bmp);
                        bmp.recycle();
                        Log.i(TAG, "SCREENSHOT_WRITTEN");
                    }

                    @Override
                    public void onFailure(int errorCode) {
                        Log.e(TAG, "SCREENSHOT_FAILED code=" + errorCode);
                    }
                });
    }

    private void writeImage(String name, Bitmap bmp) {
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, name);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/png");
        values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/PhoneControl");

        Uri uri = cr.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            Log.e(TAG, "IMAGE_INSERT_FAILED");
            return;
        }

        try (OutputStream os = cr.openOutputStream(uri)) {
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
        } catch (Exception e) {
            Log.e(TAG, "IMAGE_WRITE_FAILED", e);
        }
    }

    private void writeText(String name, String data) {
        ContentResolver cr = getContentResolver();
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME, name);
        values.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
        values.put(MediaStore.Downloads.RELATIVE_PATH, "Download/PhoneControl");

        Uri uri = cr.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
        if (uri == null) {
            Log.e(TAG, "TEXT_INSERT_FAILED");
            return;
        }

        try (OutputStream os = cr.openOutputStream(uri)) {
            os.write(data.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (Exception e) {
            Log.e(TAG, "TEXT_WRITE_FAILED", e);
        }
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "ACCESSIBILITY_INTERRUPTED");
    }

    @Override
    public void onDestroy() {
        if (receiver != null) {
            try { unregisterReceiver(receiver); } catch (Exception ignored) {}
        }
        instance = null;
        Log.w(TAG, "ACCESSIBILITY_DESTROYED");
        super.onDestroy();
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        return super.onKeyEvent(event);
    }
}
