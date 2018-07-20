package it.gufoe.myapplication;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.heatmaps.Gradient;
import com.google.maps.android.heatmaps.HeatmapTileProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import static android.database.sqlite.SQLiteDatabase.openDatabase;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MapFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MapFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MapFragment extends Fragment implements OnMapReadyCallback {
    private View mView;


    private OnFragmentInteractionListener mListener;
    private MapView mMapView;
    private GoogleMap mMap;
    private CameraPosition mCamera;
    private HeatmapTileProvider mProvider;
    private Vector<TileOverlay> mOverlays = null;
    private int mType = 0;
    private int mUltimo = -1;

    public MapFragment() {
        super();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_map, container, false);
        Spinner s = (Spinner) mView.findViewById(R.id.type);
        mType = 0;
        s.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mType = position;
                if (mMap != null) {
                    makeHeatMap();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mMapView = (MapView) mView.findViewById(R.id.map);
        mMapView.onCreate(null);
        mMapView.onResume();
        mMapView.getMapAsync(this);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            // Fragment loaded here
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        Location loc = getLocation();
        LatLng l = new LatLng(45.784260, 12.334292);
        if (loc != null) {
            l = new LatLng(loc.getLatitude(), loc.getLongitude());
        }

        mCamera = CameraPosition.builder().target(l).zoom(16).build();


        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(mCamera));

    }

    public Location getLocation() {
        LocationManager locationManager = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getContext(), "Abilitare la posizione", Toast.LENGTH_SHORT).show();
                return null;
            }
            Location lastKnownLocationGPS = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnownLocationGPS != null) {
                return lastKnownLocationGPS;
            } else {
                return locationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            }
        } else {
            return null;
        }
    }

    private void makeHeatMap() {
        Log.e("mappa", "crea heatmap " + mType);
        if (mOverlays != null)  {
            for (TileOverlay o : mOverlays) {
                o.remove();
            }
            mOverlays = null;
        }
        int[] colors = new int[] {
                Color.rgb(255, 0, 0),
                Color.rgb(220, 50, 0),
                Color.rgb(200, 100, 0),
                Color.rgb(150, 150, 0),
                Color.rgb(50, 250, 0),
                Color.rgb(20, 255, 50),
        };

        mOverlays = new Vector();
        for (int i = 0; i < 6; i++) {
            String type = "";
            switch (mType) {
                case 0:
                    type = "lte";
                    break;
                case 1:
                    type = "%cdma";
                    break;
                case 2:
                    type = "wifi";
                    break;
                case 3:
                    type = "free-wifi";
                    break;
            }
            String sql = "select time, type, lat, lon, type, signal from data where signal="+i+" and type like '"+type+"'";
            Cursor res = MainActivity.db.rawQuery(sql, null);

            Log.e("mappa", "crea con type con" + sql +"::::: " + res.getCount());

            List<LatLng> list = new ArrayList<LatLng>();
            while(res.moveToNext()) {
                list.add(new LatLng(res.getFloat(2), res.getFloat(3)));
            }

            if (list.size() == 0) continue;


            int[] cols = {
                    colors[i],
                    colors[i],
            };

            float[] startPoints = {
                0.2f, 1f
            };

            Gradient gradient = new Gradient(cols, startPoints);
            mProvider = new HeatmapTileProvider.Builder()
                    .gradient(gradient)
                    .opacity(0.3)
                    .data(list)
                    .build();

            TileOverlay o = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider));
            mOverlays.add(o);
        }
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
