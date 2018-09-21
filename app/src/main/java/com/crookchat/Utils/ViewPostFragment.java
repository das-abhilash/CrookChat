package com.crookchat.Utils;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.ittianyu.bottomnavigationviewex.BottomNavigationViewEx;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.crookchat.R;
import com.crookchat.models.Comment;
import com.crookchat.models.Like;
import com.crookchat.models.Media;
import com.crookchat.models.User;
import com.crookchat.models.UserAccountSettings;

/**
 * Created by User on 8/12/2017.
 */

public class ViewPostFragment extends Fragment {

    private static final String TAG = "ViewPostFragment";


    public interface OnCommentThreadSelectedListener{
        void onCommentThreadSelectedListener(Media media);
    }
    OnCommentThreadSelectedListener mOnCommentThreadSelectedListener;

    public ViewPostFragment(){
        super();
        setArguments(new Bundle());
    }

    //firebase
    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;
    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference myRef;
    private FirebaseMethods mFirebaseMethods;


    //widgets
    private SquareImageView mPostImage;
    private BottomNavigationViewEx bottomNavigationView;
    private TextView mBackLabel, mCaption, mUsername, mTimestamp, mLikes, mComments;
    private ImageView mBackArrow, mEllipses, mHeartRed, mHeartWhite, mProfileImage, mComment;


    //vars
    private Media mMedia;
    private int mActivityNumber = 0;
    private String photoUsername = "";
    private String profilePhotoUrl = "";
    private UserAccountSettings mUserAccountSettings;
    private View.OnClickListener mGestureDetector;
    private Heart mHeart;
    private Boolean mLikedByCurrentUser;
    private StringBuilder mUsers;
    private String mLikesString = "";
    private User mCurrentUser;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_view_post, container, false);
        mPostImage = view.findViewById(R.id.post_image);
        bottomNavigationView = view.findViewById(R.id.bottomNavViewBar);
        mBackArrow = view.findViewById(R.id.backArrow);
        mBackLabel = view.findViewById(R.id.tvBackLabel);
        mCaption = view.findViewById(R.id.image_caption);
        mUsername = view.findViewById(R.id.username);
        mTimestamp = view.findViewById(R.id.image_time_posted);
        mEllipses = view.findViewById(R.id.ivEllipses);
        mHeartRed = view.findViewById(R.id.image_heart_red);
        mHeartWhite = view.findViewById(R.id.image_heart);
        mProfileImage = view.findViewById(R.id.profile_photo);
        mLikes = view.findViewById(R.id.image_likes);
        mComment = view.findViewById(R.id.speech_bubble);
        mComments = view.findViewById(R.id.image_comments_link);

        mHeart = new Heart(mHeartWhite, mHeartRed);
        mGestureDetector = new GestureListener();

        setupFirebaseAuth();
        setupBottomNavigationView();


        return view;
    }

    private void init(){
        try{
            //mMedia = getPhotoFromBundle();
            UniversalImageLoader.setImage(getPhotoFromBundle().getImage_path(), mPostImage, null, "");
            mActivityNumber = getActivityNumFromBundle();
            String photo_id = getPhotoFromBundle().getPhoto_id();

            Query query = FirebaseDatabase.getInstance().getReference()
                    .child(getString(R.string.dbname_photos))
                    .orderByChild(getString(R.string.field_photo_id))
                    .equalTo(photo_id);
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for ( DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                        Media newMedia = new Media();
                        Map<String, Object> objectMap = (HashMap<String, Object>) singleSnapshot.getValue();

                        newMedia.setCaption(objectMap.get(getString(R.string.field_caption)).toString());
                        newMedia.setTags(objectMap.get(getString(R.string.field_tags)).toString());
                        newMedia.setPhoto_id(objectMap.get(getString(R.string.field_photo_id)).toString());
                        newMedia.setUser_id(objectMap.get(getString(R.string.field_user_id)).toString());
                        newMedia.setDate_created(objectMap.get(getString(R.string.field_date_created)).toString());
                        newMedia.setImage_path(objectMap.get(getString(R.string.field_image_path)).toString());

                        List<Comment> commentsList = new ArrayList<Comment>();
                        for (DataSnapshot dSnapshot : singleSnapshot
                                .child(getString(R.string.field_comments)).getChildren()){
                            Comment comment = new Comment();
                            comment.setUser_id(dSnapshot.getValue(Comment.class).getUser_id());
                            comment.setComment(dSnapshot.getValue(Comment.class).getComment());
                            comment.setDate_created(dSnapshot.getValue(Comment.class).getDate_created());
                            commentsList.add(comment);
                        }
                        newMedia.setComments(commentsList);

                        mMedia = newMedia;

                        getCurrentUser();
                        getPhotoDetails();
                        //getLikesString();

                    }

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "onCancelled: query cancelled.");
                }
            });

        }catch (NullPointerException e){
            Log.e(TAG, "onCreateView: NullPointerException: " + e.getMessage() );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if(isAdded()){
            init();
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try{
            mOnCommentThreadSelectedListener = (OnCommentThreadSelectedListener) getActivity();
        }catch (ClassCastException e){
            Log.e(TAG, "onAttach: ClassCastException: " + e.getMessage() );
        }
    }

    private void getLikesString(){
        Log.d(TAG, "getLikesString: getting likes string");

        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_photos))
                .child(mMedia.getPhoto_id())
                .child(getString(R.string.field_likes));
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                mUsers = new StringBuilder();
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){

                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                    Query query = reference
                            .child(getString(R.string.dbname_users))
                            .orderByChild(getString(R.string.field_user_id))
                            .equalTo(singleSnapshot.getValue(Like.class).getUser_id());
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                                Log.d(TAG, "onDataChange: found like: " +
                                        singleSnapshot.getValue(User.class).getUsername());

                                mUsers.append(singleSnapshot.getValue(User.class).getUsername());
                                mUsers.append(",");
                            }

                            String[] splitUsers = mUsers.toString().split(",");

                            //mitch, mitchell.tabian
                            mLikedByCurrentUser = mUsers.toString().contains(mCurrentUser.getUsername() + ",");

                            int length = splitUsers.length;
                            if(length == 1){
                                mLikesString = "Liked by " + splitUsers[0];
                            }
                            else if(length == 2){
                                mLikesString = "Liked by " + splitUsers[0]
                                + " and " + splitUsers[1];
                            }
                            else if(length == 3){
                                mLikesString = "Liked by " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                + " and " + splitUsers[2];

                            }
                            else if(length == 4){
                                mLikesString = "Liked by " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + ", " + splitUsers[2]
                                        + " and " + splitUsers[3];
                            }
                            else if(length > 4){
                                mLikesString = "Liked by " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + ", " + splitUsers[2]
                                        + " and " + (splitUsers.length - 3) + " others";
                            }
                            Log.d(TAG, "onDataChange: likes string: " + mLikesString);
                            setupWidgets();
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });
                }
                if(!dataSnapshot.exists()){
                    mLikesString = "";
                    mLikedByCurrentUser = false;
                    setupWidgets();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void getCurrentUser(){
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_users))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for ( DataSnapshot singleSnapshot :  dataSnapshot.getChildren()){
                    mCurrentUser = singleSnapshot.getValue(User.class);
                }
                getLikesString();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: query cancelled.");
            }
        });
    }

    private void setupWidgets(){
        String timestampDiff = getTimestampDifference();
        if(!timestampDiff.equals("0")){
                mTimestamp.setText(timestampDiff + " DAYS AGO");
            }else{
                mTimestamp.setText("TODAY");
        }
        UniversalImageLoader.setImage(mUserAccountSettings.getProfile_photo(), mProfileImage, null, "");
        mUsername.setText(mUserAccountSettings.getUsername());
        if (mLikesString.length() == 0) {
            mLikes.setVisibility(View.GONE);
        } else {
            mLikes.setVisibility(View.VISIBLE);
            mLikes.setText(mLikesString);
        }
        if (TextUtils.isEmpty(mMedia.getCaption()))
            mCaption.setVisibility(View.GONE);
        else {
            mCaption.setText(mMedia.getCaption());
            mCaption.setVisibility(View.VISIBLE);
        }
        if(mMedia.getComments().size() > 0){
            mCaption.setVisibility(View.VISIBLE);
            mComments.setText("View all " + mMedia.getComments().size() + " comments");
        }else{
            mComments.setText("");
            mCaption.setVisibility(View.GONE);
        }

        mComments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating to comments thread");

                mOnCommentThreadSelectedListener.onCommentThreadSelectedListener(mMedia);

            }
        });

        mBackArrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating back");
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        mComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating back");
                mOnCommentThreadSelectedListener.onCommentThreadSelectedListener(mMedia);

            }
        });

        if(mLikedByCurrentUser){
            mHeartWhite.setVisibility(View.GONE);
            mHeartRed.setVisibility(View.VISIBLE);
            mHeartRed.setOnClickListener(mGestureDetector);
        }
        else{
            mHeartWhite.setVisibility(View.VISIBLE);
            mHeartRed.setVisibility(View.GONE);
            mHeartWhite.setOnClickListener(mGestureDetector);
        }


    }

    private void addNewLike() {
        Log.d(TAG, "addNewLike: adding new like");

        String newLikeID = myRef.push().getKey();
        Like like = new Like();
        like.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

        myRef.child(getString(R.string.dbname_photos))
                .child(mMedia.getPhoto_id())
                .child(getString(R.string.field_likes))
                .child(newLikeID)
                .setValue(like);

        myRef.child(getString(R.string.dbname_user_photos))
                .child(mMedia.getUser_id())
                .child(mMedia.getPhoto_id())
                .child(getString(R.string.field_likes))
                .child(newLikeID)
                .setValue(like);

        mHeart.toggleLike();
        getLikesString();
    }

    private void getPhotoDetails() {
        Log.d(TAG, "getPhotoDetails: retrieving media details.");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(getString(R.string.dbname_user_account_settings))
                .orderByChild(getString(R.string.field_user_id))
                .equalTo(mMedia.getUser_id());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    mUserAccountSettings = singleSnapshot.getValue(UserAccountSettings.class);
                }
                //setupWidgets();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "onCancelled: query cancelled.");
            }
        });
    }

    public class GestureListener implements View.OnClickListener {

        public boolean onDoubleTap() {
            Log.d(TAG, "onDoubleTap: double tap detected.");

            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            Query query = reference
                    .child(getString(R.string.dbname_photos))
                    .child(mMedia.getPhoto_id())
                    .child(getString(R.string.field_likes));
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                        String keyID = singleSnapshot.getKey();

                        //case1: Then user already liked the media
                        if (mLikedByCurrentUser &&
                                singleSnapshot.getValue(Like.class).getUser_id()
                                        .equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {

                            myRef.child(getString(R.string.dbname_photos))
                                    .child(mMedia.getPhoto_id())
                                    .child(getString(R.string.field_likes))
                                    .child(keyID)
                                    .removeValue();
///
                            myRef.child(getString(R.string.dbname_user_photos))
                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .child(mMedia.getPhoto_id())
                                    .child(getString(R.string.field_likes))
                                    .child(keyID)
                                    .removeValue();

                            mHeart.toggleLike();
                            getLikesString();
                        }
                        //case2: The user has not liked the media
                        else if (!mLikedByCurrentUser) {
                            //add new like
                            addNewLike();
                            break;
                        }
                    }
                    if (!dataSnapshot.exists()) {
                        //add new like
                        addNewLike();
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

            return true;
        }

        @Override
        public void onClick(View v) {
            onDoubleTap();
        }
    }


    /**
     * Returns a string representing the number of days ago the post was made
     * @return
     */
    private String getTimestampDifference(){
        Log.d(TAG, "getTimestampDifference: getting timestamp difference.");

        String difference = "";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));//google 'android list of timezones'
        Date today = c.getTime();
        sdf.format(today);
        Date timestamp;
        final String photoTimestamp = mMedia.getDate_created();
        try{
            timestamp = sdf.parse(photoTimestamp);
            difference = String.valueOf(Math.round(((today.getTime() - timestamp.getTime()) / 1000 / 60 / 60 / 24 )));
        }catch (ParseException e){
            Log.e(TAG, "getTimestampDifference: ParseException: " + e.getMessage() );
            difference = "0";
        }
        return difference;
    }

    /**
     * retrieve the activity number from the incoming bundle from profileActivity interface
     * @return
     */
    private int getActivityNumFromBundle(){
        Log.d(TAG, "getActivityNumFromBundle: arguments: " + getArguments());

        Bundle bundle = this.getArguments();
        if(bundle != null) {
            return bundle.getInt(getString(R.string.activity_number));
        }else{
            return 0;
        }
    }

    /**
     * retrieve the media from the incoming bundle from profileActivity interface
     * @return
     */
    private Media getPhotoFromBundle(){
        Log.d(TAG, "getPhotoFromBundle: arguments: " + getArguments());

        Bundle bundle = this.getArguments();
        if(bundle != null) {
            return bundle.getParcelable(getString(R.string.photo));
        }else{
            return null;
        }
    }

    /**
     * BottomNavigationView setup
     */
    private void setupBottomNavigationView(){
        Log.d(TAG, "setupBottomNavigationView: setting up BottomNavigationView");
        BottomNavigationViewHelper.setupBottomNavigationView(bottomNavigationView);
        BottomNavigationViewHelper.enableNavigation(getActivity(),getActivity() ,bottomNavigationView);
        Menu menu = bottomNavigationView.getMenu();
        MenuItem menuItem = menu.getItem(mActivityNumber);
        menuItem.setChecked(true);
    }

       /*
    ------------------------------------ Firebase ---------------------------------------------
     */

    /**
     * Setup the firebase auth object
     */
    private void setupFirebaseAuth(){
        Log.d(TAG, "setupFirebaseAuth: setting up firebase auth.");

        mAuth = FirebaseAuth.getInstance();
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        myRef = mFirebaseDatabase.getReference();

        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();


                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };


    }


    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
}





















