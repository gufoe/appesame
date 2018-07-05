package it.gufoe.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

public class Utils {
    public static SharedPreferences settings(Context ctx) {
        return ctx.getSharedPreferences("my_app", Context.MODE_PRIVATE);
    }
    public static SharedPreferences.Editor settingsEditor(Context ctx) {
        return settings(ctx).edit();
    }
}
