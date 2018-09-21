package com.crookchat.Home;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.eschao.android.widget.elasticlistview.ElasticListView;
import com.eschao.android.widget.elasticlistview.LoadFooter;
import com.eschao.android.widget.elasticlistview.OnLoadListener;
import com.eschao.android.widget.elasticlistview.OnUpdateListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.crookchat.R;
import com.crookchat.Utils.MainfeedListAdapter;
import com.crookchat.Utils.StoriesRecyclerViewAdapter;
import com.crookchat.models.Comment;
import com.crookchat.models.Media;
import com.crookchat.models.UserAccountSettings;

/**
 * Created by User on 5/28/2017.
 */

public class HomeFragment extends Fragment implements OnUpdateListener, OnLoadListener {

    private static final String TAG = "HomeFragment";

    @Override
    public void onUpdate() {
        Log.d(TAG, "ElasticListView: updating list view...");

        getPhotos();
    }


    @Override
    public void onLoad() {
        Log.d(TAG, "ElasticListView: loading...");

        // Notify load is done
        mListView.notifyLoaded();
    }


    //vars
    private ArrayList<Media> mMedia = new ArrayList<>();;
    private ArrayList<Media> mPaginatedMedia;
    private ArrayList<String> mFollowing;
    private int recursionIterator = 0;
    //    private ListView mListView;
    private ElasticListView mListView;
    private MainfeedListAdapter adapter;
    private int resultsCount = 0;
    private ArrayList<UserAccountSettings> mUserAccountSettings;
    //    private ArrayList<UserStories> mAllUserStories = new ArrayList<>();
    private JSONArray mMasterStoriesArray;

    private RecyclerView mRecyclerView;
    public StoriesRecyclerViewAdapter mStoriesAdapter;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
//        mListView = (ListView) view.findViewById(R.id.listView);
        mListView = (ElasticListView) view.findViewById(R.id.listView);

        initListViewRefresh();
        getPhotos();

        return view;
    }

    private void initListViewRefresh(){
        mListView.setHorizontalFadingEdgeEnabled(true);
        mListView.setAdapter(adapter);
        mListView.enableLoadFooter(true)
                .getLoadFooter().setLoadAction(LoadFooter.LoadAction.RELEASE_TO_LOAD);
        mListView.setOnUpdateListener(this)
                .setOnLoadListener(this);
//        mListView.requestUpdate();
    }


    private void getFriendsAccountSettings(){
        Log.d(TAG, "getFriendsAccountSettings: getting friends account settings.");

        for(int i = 0; i < mFollowing.size(); i++) {
            Log.d(TAG, "getFriendsAccountSettings: user: " + mFollowing.get(i));
            final int count = i;
            Query query = FirebaseDatabase.getInstance().getReference()
                    .child(getString(R.string.dbname_user_account_settings))
                    .orderByKey()
                    .equalTo(mFollowing.get(i));

            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {


                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        Log.d(TAG, "getFriendsAccountSettings: got a user: " + snapshot.getValue(UserAccountSettings.class).getDisplay_name());
                        mUserAccountSettings.add(snapshot.getValue(UserAccountSettings.class));

                        if(count == 0){
                            JSONObject userObject = new JSONObject();
                            try {
                                userObject.put(getString(R.string.field_display_name), mUserAccountSettings.get(count).getDisplay_name());
                                userObject.put(getString(R.string.field_username), mUserAccountSettings.get(count).getUsername());
                                userObject.put(getString(R.string.field_profile_photo), mUserAccountSettings.get(count).getProfile_photo());
                                userObject.put(getString(R.string.field_user_id), mUserAccountSettings.get(count).getUser_id());
                                JSONObject userSettingsStoryObject = new JSONObject();
                                userSettingsStoryObject.put(getString(R.string.user_account_settings), userObject);
                                mMasterStoriesArray.put(0, userSettingsStoryObject);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }

                    }
                    if (count == mFollowing.size() - 1) {
//                        getFriendsStories();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    int i = 0;
                    Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void clearAll(){
        if(mFollowing != null){
            mFollowing.clear();
        }
        if(mMedia != null){
            mMedia.clear();
            if(adapter != null){
                adapter.clear();
                adapter.notifyDataSetChanged();
            }
        }
        if(mUserAccountSettings != null){
            mUserAccountSettings.clear();
        }
        if(mPaginatedMedia != null){
            mPaginatedMedia.clear();
        }
        mMasterStoriesArray = new JSONArray(new ArrayList<String>());
        if(mStoriesAdapter != null){
            mStoriesAdapter.notifyDataSetChanged();
        }
        if(mRecyclerView != null){
            mRecyclerView.setAdapter(null);
        }
        mFollowing = new ArrayList<>();
        mMedia = new ArrayList<>();
        mPaginatedMedia = new ArrayList<>();
        mUserAccountSettings = new ArrayList<>();
    }

    /**
     //     * Retrieve all user id's that current user is following
     //     */
    private void getFollowing() {
        Log.d(TAG, "getFollowing: searching for following");

        clearAll();
        //also add your own id to the list
        mFollowing.add(FirebaseAuth.getInstance().getCurrentUser().getUid());

        Query query = FirebaseDatabase.getInstance().getReference()
//                .child(new FilePaths().FIREBASE_IMAGE_STORAGE);
                .child("photos").child("users")
                .child(getActivity().getString(R.string.dbname_following))
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "getFollowing: found user: " + singleSnapshot
                            .child(getString(R.string.field_user_id)).getValue());

                    mFollowing.add(singleSnapshot
                            .child(getString(R.string.field_user_id)).getValue().toString());
                }

                getPhotos();
//                getMyUserAccountSettings();
                getFriendsAccountSettings();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                int i = 0;
                Toast.makeText(getActivity(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }

        });

    }

    private void getPhotos(){
        Log.d(TAG, "getPhotos: getting list of photos");

            Query query = FirebaseDatabase.getInstance().getReference()
                    .child(getActivity().getString(R.string.dbname_photos));
//                    .child(mFollowing.get(i))
//                    .orderByChild(getString(R.string.field_timestamp))
//                    .equalTo(mFollowing.get(i));
//                    .child(new FilePaths().FIREBASE_IMAGE_STORAGE);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    List<Media> list = new ArrayList<>();
                    for ( DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){

                        Media newMedia = new Media();
                        Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                        newMedia.setCaption(objectMap.get(getString(R.string.field_caption)).toString());
                        newMedia.setTags(objectMap.get(getString(R.string.field_tags)).toString());
                        newMedia.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
                        newMedia.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                        newMedia.setDate_created(objectMap.get(getString(R.string.field_date_created)).toString());
                        newMedia.setImage_path(objectMap.get(getString(R.string.field_image_path)).toString());
                        newMedia.setType(objectMap.get(getString(R.string.field_photo_type)).toString());

                        Log.d(TAG, "getPhotos: photo: " + newMedia.getPhoto_id());
                        List<Comment> commentsList = new ArrayList<Comment>();
                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(getString(R.string.field_comments)).getChildren()){
                            Map<String, Object> object_map = (HashMap<String, Object>) dSnapshot.getValue();
                            Comment comment = new Comment();
                            comment.setUser_id(object_map.get(getString(R.string.field_user_id)).toString());
                            comment.setComment(object_map.get(getString(R.string.field_comment)).toString());
                            comment.setDate_created(object_map.get(getString(R.string.field_date_created)).toString());
                            commentsList.add(comment);
                        }
                        newMedia.setComments(commentsList);
                        list.add(newMedia);
                    }
                    mMedia.clear();
                    mMedia.addAll(list);
                    displayPhotos();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: query cancelled.");
                }
            });

    }

    private void displayPhotos(){
        mPaginatedMedia = new ArrayList<>();
        if(mMedia != null){

            try{

                //sort for newest to oldest
                Collections.sort(mMedia, new Comparator<Media>() {
                    public int compare(Media o1, Media o2) {
                        return o2.getDate_created().compareTo(o1.getDate_created());
                    }
                });

                //we want to load 10 at a time. So if there is more than 10, just load 10 to start
                int iterations = mMedia.size();
                if(iterations > 10){
                    iterations = 10;
                }
//
                resultsCount = 0;
                for(int i = 0; i < iterations; i++){
                    mPaginatedMedia.add(mMedia.get(i));
                    resultsCount++;
                    Log.d(TAG, "displayPhotos: adding a photo to paginated list: " + mMedia.get(i).getPhoto_id());
                }

                adapter = new MainfeedListAdapter(getActivity(), R.layout.layout_mainfeed_listitem, mPaginatedMedia);
                mListView.setAdapter(adapter);

                // Notify update is done
                mListView.notifyUpdated();

            }catch (IndexOutOfBoundsException e){
                Log.e(TAG, "displayPhotos: IndexOutOfBoundsException:" + e.getMessage() );
            }catch (NullPointerException e){
                Log.e(TAG, "displayPhotos: NullPointerException:" + e.getMessage() );
            }
        }
    }

    public void displayMorePhotos(){
        Log.d(TAG, "displayMorePhotos: displaying more photos");

        try{

            if(mMedia.size() > resultsCount && mMedia.size() > 0){

                int iterations;
                if(mMedia.size() > (resultsCount + 10)){
                    Log.d(TAG, "displayMorePhotos: there are greater than 10 more photos");
                    iterations = 10;
                }else{
                    Log.d(TAG, "displayMorePhotos: there is less than 10 more photos");
                    iterations = mMedia.size() - resultsCount;
                }

                //add the new photos to the paginated list
                for(int i = resultsCount; i < resultsCount + iterations; i++){
                    mPaginatedMedia.add(mMedia.get(i));
                }

                resultsCount = resultsCount + iterations;
                adapter.notifyDataSetChanged();
            }
        }catch (IndexOutOfBoundsException e){
            Log.e(TAG, "displayPhotos: IndexOutOfBoundsException:" + e.getMessage() );
        }catch (NullPointerException e){
            Log.e(TAG, "displayPhotos: NullPointerException:" + e.getMessage() );
        }
    }


}





















