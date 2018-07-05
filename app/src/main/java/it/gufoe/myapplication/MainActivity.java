package it.gufoe.myapplication;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {
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
        initDatabase();
        setContentView(R.layout.activity_main);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.content, new HomeFragment()).commit();

        // start the location service
        sendBroadcast(new Intent(getApplicationContext(), StartLocationServiceReceiver.class));
    }

    private void initDatabase() {
        db = openOrCreateDatabase(getDatabasePath("datalogger.sqlite").getPath(), 0, null);
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
