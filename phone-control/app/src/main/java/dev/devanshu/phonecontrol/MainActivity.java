package dev.devanshu.phonecontrol;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(48, 48, 48, 48);

        TextView title = new TextView(this);
        title.setText("Phone Control");
        title.setTextSize(28);
        root.addView(title);

        TextView status = new TextView(this);
        status.setText("Accessibility controller based on the AccessDroid project approach.\n\nEnable the service once, then commands can be sent from Termux/Desktop Commander.");
        status.setTextSize(16);
        root.addView(status);

        Button settings = new Button(this);
        settings.setText("Open Accessibility Settings");
        settings.setOnClickListener(v ->
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        );
        root.addView(settings);

        setContentView(root);
    }
}
