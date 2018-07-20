package it.gufoe.myapplication;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.database.sqlite.SQLiteDatabase.openDatabase;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link HomeFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link HomeFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HomeFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;
    private View mView;

    public HomeFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment HomeFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static HomeFragment newInstance(String param1, String param2) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mView = inflater.inflate(R.layout.fragment_home, container, false);


        return mView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Cursor res = MainActivity.db.rawQuery("select" +
                " count(case type when 'lte' then 1 else null end)" +
                ",count(case when type like '%cdma' then 1 else null end)" +
                ",count(case type when 'wifi' then 1 else null end)" +
                " from data", null);
//        res.moveToNext();
//        int count = res.getInt(0);
//
//
//        String sql = "select time, type, lat, lon from data order by time desc limit 100";
//        res = MainActivity.db.rawQuery(sql, null);

        while (res.moveToNext()) {
            View v = mView;
            ((TextView) v.findViewById(R.id.oss_lte2)).setText(res.getString(0));
            ((TextView) v.findViewById(R.id.oss_umts2)).setText(res.getString(1));
            ((TextView) v.findViewById(R.id.oss_wifi2)).setText(res.getString(2));
            ((TextView) v.findViewById(R.id.oss_tot2)).setText(
                ""+(res.getInt(0)+res.getInt(1)+res.getInt(2))
            );
        }

        res.close();

        Button export = mView.findViewById(R.id.btn_export);
        export.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File f = new File(MainActivity.dbfile);
                File f2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)+"/export.sqlite");
                try {
                    copy(f, f2);

                    Intent intentShareFile = new Intent(Intent.ACTION_SEND);


                    intentShareFile.setType("application/x-sqlite3");
                    intentShareFile.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+f2.getAbsolutePath()));

                    intentShareFile.putExtra(Intent.EXTRA_SUBJECT,
                            "Condivisione...");

                    startActivity(Intent.createChooser(intentShareFile, "Share File"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        Button btn_import = mView.findViewById(R.id.btn_import);
        btn_import.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ChooserDialog show = new ChooserDialog().with(getContext())
                        .withFilter(false, false, "sqlite")
                        .withStartFile(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath())
                        .withChosenListener(new ChooserDialog.Result() {
                            @Override
                            public void onChoosePath(String path, File pathFile) {
                                getContext().stopService(new Intent(getContext(), LocationService.class));
                                MainActivity.db.close();
                                try {
                                    Thread.sleep(500);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                try {
                                    copy(pathFile, new File(MainActivity.dbfile));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                MainActivity.initDatabase(getContext());
                                MainActivity.startService(getContext());

                                Toast.makeText(getContext(), "FILE: " + path, Toast.LENGTH_SHORT).show();
                            }
                        })
                        .build()
                        .show();

            }
        });
    }

    public static void copy(File src, File dst) throws IOException {
        try (FileInputStream in = new FileInputStream(src)) {
            try (FileOutputStream out = new FileOutputStream(dst)) {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        }
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
