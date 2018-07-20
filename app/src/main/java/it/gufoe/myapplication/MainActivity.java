package it.gufoe.myapplication;

import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import static it.gufoe.myapplication.R.id.navigation;

public class MainActivity extends AppCompatActivity {
    public static String dbfile;
    public static SQLiteDatabase db;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();

            switch (item.getItemId()) {
                case R.id.navigation_home:
                    ft.replace(R.id.content, new HomeFragment()).commit();
                    return true;
                case R.id.navigation_map:
                    ft.replace(R.id.content, new MapFragment()).commit();
                    return true;
                case R.id.navigation_settings:
                    ft.replace(R.id.content, new SettingsFragment()).commit();
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initDatabase(getApplicationContext());
        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.content, new HomeFragment()).commit();

        // start the location service
        startService(getApplicationContext());
    }


    public static void startService(Context c) {
        c.sendBroadcast(new Intent(c, StartLocationServiceReceiver.class));
    }

    public static void initDatabase(Context c) {
        dbfile = c.getDatabasePath("datalogger.sqlite").getAbsolutePath();
        db = c.openOrCreateDatabase(dbfile, 0, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS data (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "time DATETIME NOT NULL," +
                "type TEXT NOT NULL," +
                "lat FLOAT NOT NULL," +
                "lon FLOAT NOT NULL," +
                "signal INTEGER NOT NULL" +
                ")");
    }

}
