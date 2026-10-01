import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

public final class Installer {
    public static void main(String[] args) throws Exception {
        if (args.length != 1) throw new IllegalArgumentException("usage: Installer APK");
        String apk = args[0];

        Class<?> atClass = Class.forName("android.app.ActivityThread");
        Method systemMain = atClass.getDeclaredMethod("systemMain");
        Object at = systemMain.invoke(null);
        Method getSystemContext = atClass.getDeclaredMethod("getSystemContext");
        Context system = (Context) getSystemContext.invoke(at);
        Context ctx = system.createPackageContext("com.termux", Context.CONTEXT_IGNORE_SECURITY);

        PackageInstaller installer = ctx.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName("com.danielealbano.androidremotecontrolmcp");

        int sessionId = installer.createSession(params);
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
                System.out.println("STAGED bytes=" + total + " session=" + sessionId);
            }

            Intent resultIntent = new Intent("com.termux.PACKAGE_INSTALL_RESULT");
            resultIntent.setPackage("com.termux");
            PendingIntent pending = PendingIntent.getBroadcast(
                    ctx, sessionId, resultIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
            session.commit(pending.getIntentSender());
            System.out.println("COMMIT session=" + sessionId);
        } finally {
            session.close();
        }

        Thread.sleep(15000);
        System.out.println("DONE");
    }
}
