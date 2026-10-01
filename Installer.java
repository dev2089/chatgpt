import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.OutputStream;
import java.lang.reflect.Method;

public final class Installer {
    static final String LOG = "/storage/8096-3BC5/PhoneControl/installer-log.txt";

    static void log(String s) {
        try (FileWriter w = new FileWriter(LOG, true)) {
            w.write(s + "\n");
        } catch (Throwable ignored) {}
        System.out.println(s);
    }

    public static void main(String[] args) {
        try {
            log("START");
            if (args.length != 1) throw new IllegalArgumentException("usage: Installer APK");
            String apk = args[0];
            log("APK=" + apk + " exists=" + new File(apk).isFile() + " len=" + new File(apk).length());

            Class<?> atClass = Class.forName("android.app.ActivityThread");
            log("ActivityThread loaded");
            Method systemMain = atClass.getDeclaredMethod("systemMain");
            Object at = systemMain.invoke(null);
            log("systemMain ok");
            Method getSystemContext = atClass.getDeclaredMethod("getSystemContext");
            Context system = (Context) getSystemContext.invoke(at);
            log("system context ok package=" + system.getPackageName());

            Context ctx = system.createPackageContext("com.termux", Context.CONTEXT_IGNORE_SECURITY);
            log("termux context ok package=" + ctx.getPackageName());

            PackageInstaller installer = ctx.getPackageManager().getPackageInstaller();
            log("PackageInstaller ok");

            PackageInstaller.SessionParams params =
                    new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            params.setAppPackageName("com.danielealbano.androidremotecontrolmcp");
            int sessionId = installer.createSession(params);
            log("SESSION " + sessionId);

            PackageInstaller.Session session = installer.openSession(sessionId);
            try {
                try (FileInputStream in = new FileInputStream(apk);
                     OutputStream out = session.openWrite("base.apk", 0, -1)) {
                    byte[] buf = new byte[1024 * 1024];
                    int n;
                    long total = 0;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                        total += n;
                    }
                    out.flush();
                    session.fsync(out);
                    log("STAGED " + total);
                }

                Intent resultIntent = new Intent("com.termux.PACKAGE_INSTALL_RESULT");
                resultIntent.setPackage("com.termux");
                PendingIntent pending = PendingIntent.getBroadcast(
                        ctx, sessionId, resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
                log("PENDINGINTENT ok");

                session.commit(pending.getIntentSender());
                log("COMMIT CALLED");
            } finally {
                session.close();
            }

            log("CLOSED");
            Thread.sleep(20000);
            log("DONE");
        } catch (Throwable t) {
            log("ERROR " + t);
            try (PrintWriter p = new PrintWriter(new FileWriter(LOG, true))) {
                t.printStackTrace(p);
            } catch (Throwable ignored) {}
        }
    }
}
