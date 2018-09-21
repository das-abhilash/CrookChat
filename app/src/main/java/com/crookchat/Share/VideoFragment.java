package com.crookchat.Share;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.crookchat.Profile.AccountSettingsActivity;
import com.crookchat.R;

import static android.app.Activity.RESULT_OK;

/**
 * Created by User on 5/28/2017.
 */

public class VideoFragment extends Fragment {
    private static final String TAG = "PhotoFragment";

    //constant
    private static final int REQUEST_TAKE_GALLERY_VIDEO = 5;

    public static Fragment getInstance(String parent) {
        VideoFragment fragment = new VideoFragment();
        Bundle bundle = new Bundle();
        bundle.putString("parent", parent);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_video, container, false);
        Log.d(TAG, "onCreateView: started.");

        Button btnLaunchCamera = view.findViewById(R.id.btnLaunchCamera);
        btnLaunchCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: launching for video.");
                Intent intent = new Intent();
//                intent.putExtra("android.intent.extra.durationLimit", 30000);
//                intent.putExtra(MediaStore.EXTRA_SIZE_LIMIT, 30000);
                intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 3);
                intent.setType("video/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_TAKE_GALLERY_VIDEO);
            }
        });
        return view;
    }

    /*
        public String getPath(Uri uri) {
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = managedQuery(uri, projection, null, null, null);
            if(cursor!=null) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                cursor.moveToFirst();
                return cursor.getString(column_index);
            }
            else return null;
        }*/
    private boolean isRootTask() {
        return ((ShareActivity) getActivity()).getTask() == 0;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_TAKE_GALLERY_VIDEO) {
//                Uri uri = Uri.fromFile());

                Uri uri = data.getData();
//                Uri selectedMediaUri1 = data.getData().getPath();

//                FirebaseMethods mFirebaseMethods = new FirebaseMethods(getActivity());
//                mFirebaseMethods.uploadNewPhoto("new_video", "", 2, String.valueOf(selectedMediaUri));
//                String path = getPath(selectedMediaUri);
//                File file = new File(String.valueOf(selectedMediaUri));
//                Uri uri = Uri.fromFile(file);

                if (isRootTask() || true) {
                    try {
                        Log.d(TAG, "onActivityResult: received new bitmap from camera: " + uri);
                        Intent intent = new Intent(getActivity(), NextActivity.class);
                        intent.putExtra(getString(R.string.selected_image), uri.toString());
                        intent.putExtra(getString(R.string.upload_media_type), getString(R.string.new_video));
                        startActivity(intent);
                    } catch (NullPointerException e) {
                        Log.d(TAG, "onActivityResult: NullPointerException: " + e.getMessage());
                    }
                } else {
                    try {
                        Log.d(TAG, "onActivityResult: received new bitmap from camera: " + uri);
                        Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
                        intent.putExtra(getString(R.string.selected_image), uri);
                        intent.putExtra(getString(R.string.return_to_fragment), getString(R.string.edit_profile_fragment));
                        startActivity(intent);
                        getActivity().finish();
                    } catch (NullPointerException e) {
                        Log.d(TAG, "onActivityResult: NullPointerException: " + e.getMessage());
                    }
                }
            }
        }

   /*     if(requestCode == CAMERA_REQUEST_CODE){
            Log.d(TAG, "onActivityResult: done taking a photo.");
            Log.d(TAG, "onActivityResult: attempting to navigate to final share screen.");

            Bitmap bitmap;
            bitmap = (Bitmap) data.getExtras().get("data");

            if(isRootTask()){
                try{
                    Log.d(TAG, "onActivityResult: received new bitmap from camera: " + bitmap);
                    Intent intent = new Intent(getActivity(), NextActivity.class);
                    intent.putExtra(getString(R.string.selected_bitmap), bitmap);
                    startActivity(intent);
                }catch (NullPointerException e){
                    Log.d(TAG, "onActivityResult: NullPointerException: " + e.getMessage());
                }
            }else{
               try{
                   Log.d(TAG, "onActivityResult: received new bitmap from camera: " + bitmap);
                   Intent intent = new Intent(getActivity(), AccountSettingsActivity.class);
                   intent.putExtra(getString(R.string.selected_bitmap), bitmap);
                   intent.putExtra(getString(R.string.return_to_fragment), getString(R.string.edit_profile_fragment));
                   startActivity(intent);
                   getActivity().finish();
               }catch (NullPointerException e){
                   Log.d(TAG, "onActivityResult: NullPointerException: " + e.getMessage());
               }
            }

        }*/
    }

    public String getPath(Uri uri) {
        Cursor cursor = null;
        try {
            String[] projection = {MediaStore.Images.Media.DATA};
            cursor = getActivity().getContentResolver().query(uri, projection, null, null, null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(projection[0]);
            String filePath = cursor.getString(columnIndex);
            return cursor.getString(column_index);
        } catch (Exception e){

        } finally {
            if(cursor != null)
            cursor.close();
        }
        return "";
    }
}