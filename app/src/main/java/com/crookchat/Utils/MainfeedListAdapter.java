package com.crookchat.Utils;

import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.FileProvider;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import de.hdodenhof.circleimageview.CircleImageView;
import com.crookchat.Home.HomeActivity;
import com.crookchat.Profile.ProfileActivity;
import com.crookchat.R;
import com.crookchat.models.Comment;
import com.crookchat.models.Like;
import com.crookchat.models.Media;
import com.crookchat.models.User;
import com.crookchat.models.UserAccountSettings;
import com.crookchat.videoPlayer.VideoPlayActivity;

/**
 * Created by User on 9/22/2017.
 */

public class MainfeedListAdapter extends ArrayAdapter<Media> {

    public interface OnLoadMoreItemsListener{
        void onLoadMoreItems();
    }
    OnLoadMoreItemsListener mOnLoadMoreItemsListener;
    private final ImageLoader imageLoader = ImageLoader.getInstance();
    private static final String TAG = "MainFeedListAdapter";

    private LayoutInflater mInflater;
    private int mLayoutResource;
    private Context mContext;
    private DatabaseReference mReference;
    private String currentUsername = "";

    public MainfeedListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Media> objects) {
        super(context, resource, objects);
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayoutResource = resource;
        this.mContext = context;
        mReference = FirebaseDatabase.getInstance().getReference();

//        for(Media media: objects){
//            Log.d(TAG, "MainFeedListAdapter: media id: " + media.getPhoto_id());
//        }
    }

    private String fileName(String currentUsername) {
        return (System.currentTimeMillis() + "U" + currentUsername + ".png");
    }

    public static Uri getFileContentProviderUri(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, context.getApplicationContext().getPackageName() + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    private boolean reachedEndOfList(int position) {
        return position == getCount() - 1;
    }

    private Bitmap getBitmap(View view) {
        //Define a bitmap with the same size as the view
        Bitmap returnedBitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        //Bind a canvas to it
        Canvas canvas = new Canvas(returnedBitmap);
        //Get the view's background
        Drawable bgDrawable = view.getBackground();
        if (bgDrawable != null) {
            //has background drawable, then draw it on the canvas
            bgDrawable.draw(canvas);
        } else {
            //does not have background drawable, then draw white background on the canvas
            canvas.drawColor(Color.WHITE);
        }
        // draw the view on the canvas
        view.draw(canvas);
        //return the bitmap
        return returnedBitmap;
    }

    public static Bitmap retriveVideoFrameFromVideo(String videoPath) throws Throwable {
        Bitmap bitmap = null;
        MediaMetadataRetriever mediaMetadataRetriever = null;
        try {
            mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(videoPath, new HashMap<String, String>());
            //   mediaMetadataRetriever.setDataSource(videoPath);
            bitmap = mediaMetadataRetriever.getFrameAtTime();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Throwable("Exception in retriveVideoFrameFromVideo(String videoPath)" + e.getMessage());

        } finally {
            if (mediaMetadataRetriever != null) {
                mediaMetadataRetriever.release();
            }
        }
        return bitmap;
    }

    private void loadMoreData(){

        try{
            mOnLoadMoreItemsListener = (OnLoadMoreItemsListener) getContext();
        }catch (ClassCastException e){
            Log.e(TAG, "loadMoreData: ClassCastException: " +e.getMessage() );
        }

        try{
            mOnLoadMoreItemsListener.onLoadMoreItems();
        }catch (NullPointerException e){
            Log.e(TAG, "loadMoreData: ClassCastException: " +e.getMessage() );
        }
    }

    private void setupLikesString(final ViewHolder holder, String likesString) {
        Log.d(TAG, "setupLikesString: likes string:" + holder.likesString);

        Log.d(TAG, "setupLikesString: media id: " + holder.media.getPhoto_id());
        if (holder.likeByCurrentUser) {
            Log.d(TAG, "setupLikesString: media is liked by current user");
            holder.heartWhite.setVisibility(View.GONE);
            holder.heartRed.setVisibility(View.VISIBLE);
            holder.heartRed.setOnClickListener(holder.detector);
        } else {
            Log.d(TAG, "setupLikesString: media is not liked by current user");
            holder.heartWhite.setVisibility(View.VISIBLE);
            holder.heartRed.setVisibility(View.GONE);
            holder.heartWhite.setOnClickListener(holder.detector);
        }
        holder.likes.setText(likesString);
    }


    private void addNewLike(final ViewHolder holder){
        Log.d(TAG, "addNewLike: adding new like");

        String newLikeID = mReference.push().getKey();
        Like like = new Like();
        like.setUser_id(FirebaseAuth.getInstance().getCurrentUser().getUid());

        mReference.child(mContext.getString(R.string.dbname_photos))
                .child(holder.media.getPhoto_id())
                .child(mContext.getString(R.string.field_likes))
                .child(newLikeID)
                .setValue(like);

        mReference.child(mContext.getString(R.string.dbname_user_photos))
                .child(holder.media.getUser_id())
                .child(holder.media.getPhoto_id())
                .child(mContext.getString(R.string.field_likes))
                .child(newLikeID)
                .setValue(like);

        holder.heart.toggleLike();
        getLikesString(holder);
    }

    private void downloadManager(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("download");
        request.setTitle("" + System.currentTimeMillis());
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "" + System.currentTimeMillis() + ".mp4");

// get download service and enqueue file
        DownloadManager manager = (DownloadManager) mContext.getSystemService(Context.DOWNLOAD_SERVICE);
        manager.enqueue(request);
    }
    private void getCurrentUsername(){
        Log.d(TAG, "getCurrentUsername: retrieving user account settings");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_users))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                    currentUsername = singleSnapshot.getValue(UserAccountSettings.class).getUsername();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    private void getLikesString(final ViewHolder holder){
        Log.d(TAG, "getLikesString: getting likes string");

        Log.d(TAG, "getLikesString: media id: " + holder.media.getPhoto_id());
        try{
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_photos))
                .child(holder.media.getPhoto_id())
                .child(mContext.getString(R.string.field_likes));
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                holder.users = new StringBuilder();
                for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){

                    DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                    Query query = reference
                            .child(mContext.getString(R.string.dbname_users))
                            .orderByChild(mContext.getString(R.string.field_user_id))
                            .equalTo(singleSnapshot.getValue(Like.class).getUser_id());
                    query.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            for(DataSnapshot singleSnapshot : dataSnapshot.getChildren()){
                                Log.d(TAG, "onDataChange: found like: " +
                                        singleSnapshot.getValue(User.class).getUsername());

                                holder.users.append(singleSnapshot.getValue(User.class).getUsername());
                                holder.users.append(",");
                            }

                            String[] splitUsers = holder.users.toString().split(",");

                            //mitch, mitchell.tabian
                            holder.likeByCurrentUser = holder.users.toString().contains(currentUsername + ",");

                            int length = splitUsers.length;
                            if(length == 0){
                                holder.likes.setVisibility(View.GONE);
                                return;
                            }
                            holder.likes.setVisibility(View.GONE);
                            if(length == 1){
                                holder.likesString = "Liked by " + splitUsers[0];
                            }
                            else if(length == 2){
                                holder.likesString = "Liked by " + splitUsers[0]
                                        + " and " + splitUsers[1];
                            }
                            else if(length == 3){
                                holder.likesString = "Liked by " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + " and " + splitUsers[2];

                            }
                            else if(length == 4){
                                holder.likesString = "Liked by " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + ", " + splitUsers[2]
                                        + " and " + splitUsers[3];
                            }
                            else if(length > 4){
                                holder.likesString = "Liked by " + splitUsers[0]
                                        + ", " + splitUsers[1]
                                        + ", " + splitUsers[2]
                                        + " and " + (splitUsers.length - 3) + " others";
                            }
                            Log.d(TAG, "onDataChange: likes string: " + holder.likesString);
                            //setup likes string
                            setupLikesString(holder, holder.likesString);
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            Toast.makeText(mContext, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                if(!dataSnapshot.exists()){
                    holder.likesString = "";
                    holder.likeByCurrentUser = false;
                    //setup likes string
                    setupLikesString(holder, holder.likesString);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        }catch (NullPointerException e){
            Log.e(TAG, "getLikesString: NullPointerException: " + e.getMessage() );
            holder.likesString = "";
            holder.likeByCurrentUser = false;
            //setup likes string
            setupLikesString(holder, holder.likesString);
        }
    }

    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        final ViewHolder holder;

        if (convertView == null) {
            convertView = mInflater.inflate(mLayoutResource, parent, false);
            holder = new ViewHolder();

            holder.username = convertView.findViewById(R.id.username);
            holder.image = convertView.findViewById(R.id.post_image);
            holder.heartRed = convertView.findViewById(R.id.image_heart_red);
            holder.heartWhite = convertView.findViewById(R.id.image_heart);
            holder.comment = convertView.findViewById(R.id.speech_bubble);
            holder.share = convertView.findViewById(R.id.image_share);
            holder.video = convertView.findViewById(R.id.post_video);
            holder.download = convertView.findViewById(R.id.image_download);
            holder.likes = convertView.findViewById(R.id.image_likes);
            holder.comments = convertView.findViewById(R.id.image_comments_link);
            holder.caption = convertView.findViewById(R.id.image_caption);
            holder.timeDetla = convertView.findViewById(R.id.image_time_posted);
            holder.mprofileImage = convertView.findViewById(R.id.profile_photo);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.media = getItem(position);
        holder.detector = new LikeUnLikeListener(holder);
        holder.users = new StringBuilder();
        holder.heart = new Heart(holder.heartWhite, holder.heartRed);

        //get the current users username (need for checking likes string)
        getCurrentUsername();

        //get likes string
        getLikesString(holder);

        //set the caption
        if (TextUtils.isEmpty(getItem(position).getCaption())) {
            holder.caption.setVisibility(View.GONE);
        } else {
            holder.caption.setText(getItem(position).getCaption());
            holder.caption.setVisibility(View.VISIBLE);
        }
        //set the comment
        List<Comment> comments = getItem(position).getComments();
        if (comments.isEmpty()) {
            holder.comments.setVisibility(View.GONE);
        } else {
            holder.comments.setText("View all " + comments.size() + " comments");
            holder.comments.setVisibility(View.VISIBLE);
        }

        holder.comments.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: loading comment thread for " + getItem(position).getPhoto_id());
                ((HomeActivity) mContext).onCommentThreadSelected(getItem(position),
                        mContext.getString(R.string.home_activity));
                //going to need to do something else?
                ((HomeActivity) mContext).hideLayout();
            }
        });

        //set the time it was posted
        String timestampDifference = getTimestampDifference(getItem(position));
        if (!timestampDifference.equals("0")) {
            holder.timeDetla.setText(timestampDifference + " DAYS AGO");
        } else {
            holder.timeDetla.setText("TODAY");
        }

        if (holder.media.getType().equals(mContext.getString(R.string.new_video))) {
            holder.image.setVisibility(View.INVISIBLE);
            holder.share.setVisibility(View.GONE);
            holder.download.setVisibility(View.VISIBLE);
            /*try {
                holder.video.setImageBitmap(retriveVideoFrameFromVideo(holder.media.getImage_path()));
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }*/
            holder.video.setVisibility(View.VISIBLE);
        } else if (holder.media.getType().equals(mContext.getString(R.string.new_photo))) {
            //set the profile image
            imageLoader.displayImage(holder.media.getImage_path(), holder.image);
            holder.image.setVisibility(View.VISIBLE);
            holder.share.setVisibility(View.VISIBLE);
            holder.download.setVisibility(View.GONE);
            holder.video.setVisibility(View.GONE);
        }

        // currentUsername = singleSnapshot.getValue(UserAccountSettings.class).getUsername();

        holder.username.setText(holder.media.getUsername());
        holder.username.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating to profile of: " +
                        holder.user.getUsername());

                Intent intent = new Intent(mContext, ProfileActivity.class);
                intent.putExtra(mContext.getString(R.string.calling_activity),
                        mContext.getString(R.string.home_activity));
                intent.putExtra(mContext.getString(R.string.intent_user), holder.user);
                mContext.startActivity(intent);
            }
        });

        imageLoader.displayImage(holder.media.getProfile_photo(),
                holder.mprofileImage);
        holder.mprofileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: navigating to profile of: " +
                        holder.user.getUsername());

                Intent intent = new Intent(mContext, ProfileActivity.class);
                intent.putExtra(mContext.getString(R.string.calling_activity),
                        mContext.getString(R.string.home_activity));
                intent.putExtra(mContext.getString(R.string.intent_user), holder.user);
                mContext.startActivity(intent);
            }
        });


        holder.comment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((HomeActivity) mContext).onCommentThreadSelected(getItem(position),
                        mContext.getString(R.string.home_activity));

                //another thing?
                ((HomeActivity) mContext).hideLayout();
            }
        });

        holder.video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VideoPlayActivity.startActivity(v.getContext(), holder.media.getImage_path());
            }
        });
        holder.download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadManager(holder.media.getImage_path());
                Toast.makeText(v.getContext(), "Downloading...", Toast.LENGTH_LONG).show();
            }
        });

        holder.share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Intent share = new Intent(Intent.ACTION_SEND);
                share.putExtra(Intent.EXTRA_TEXT, mContext.getString(R.string.check_out_post));
                share.setPackage("com.whatsapp");
                share.setType("image/jpeg");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                Bitmap bitmap = getBitmap(holder.image);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, bytes);
                File tempFile = null;
                try {
                    if (bytes.size() != 0) {
                        tempFile = new File(mContext.getExternalFilesDir(null), fileName(holder.user.getUsername()));
                        tempFile.createNewFile();
                        FileOutputStream fo = new FileOutputStream(tempFile);
                        fo.write(bytes.toByteArray());
                        fo.close();
                    }
                } catch (Exception e) {

                }
                if (tempFile != null) {
                    share.putExtra(Intent.EXTRA_STREAM, getFileContentProviderUri(mContext, tempFile));
                }
                mContext.startActivity(share);
            }
        });

        //get the user object
        Query userQuery = mReference
                .child(mContext.getString(R.string.dbname_users))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(getItem(position).getUser_id());
        userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "onDataChange: found user: " +
                            singleSnapshot.getValue(User.class).getUsername());

                    holder.user = singleSnapshot.getValue(User.class);
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        if (reachedEndOfList(position)) {
            loadMoreData();
        }

        return convertView;
    }

    /**
     * Returns a string representing the number of days ago the post was made
     *
     * @return
     */
    private String getTimestampDifference(Media media) {
        Log.d(TAG, "getTimestampDifference: getting timestamp difference.");

        String difference = "";
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.CANADA);
        sdf.setTimeZone(TimeZone.getTimeZone("Canada/Pacific"));//google 'android list of timezones'
        Date today = c.getTime();
        sdf.format(today);
        Date timestamp;
        final String photoTimestamp = media.getDate_created();
        try {
            timestamp = sdf.parse(photoTimestamp);
            difference = String.valueOf(Math.round(((today.getTime() - timestamp.getTime()) / 1000 / 60 / 60 / 24)));
        } catch (ParseException e) {
            Log.e(TAG, "getTimestampDifference: ParseException: " + e.getMessage());
            difference = "0";
        }
        return difference;
    }

    static class ViewHolder {
        CircleImageView mprofileImage;
        String likesString;
        TextView username, timeDetla, caption, likes, comments;
        SquareImageView image;
        ImageView heartRed, heartWhite, comment, share, video, download;

        User user = new User();
        StringBuilder users;
        String mLikesString;
        boolean likeByCurrentUser;
        Heart heart;
        View.OnClickListener detector;
        Media media;
    }

    public class LikeUnLikeListener implements View.OnClickListener {

        ViewHolder mHolder;

        public LikeUnLikeListener(ViewHolder holder) {
            mHolder = holder;
        }

        @Override
        public void onClick(View v) {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            Query query = reference
                    .child(mContext.getString(R.string.dbname_photos))
                    .child(mHolder.media.getPhoto_id())
                    .child(mContext.getString(R.string.field_likes));
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                        String keyID = singleSnapshot.getKey();

                        //case1: Then user already liked the media
                        if (mHolder.likeByCurrentUser
//                                && singleSnapshot.getValue(Like.class).getUser_id()
//                                        .equals(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                ) {

                            mReference.child(mContext.getString(R.string.dbname_photos))
                                    .child(mHolder.media.getPhoto_id())
                                    .child(mContext.getString(R.string.field_likes))
                                    .child(keyID)
                                    .removeValue();
///
                            mReference.child(mContext.getString(R.string.dbname_user_photos))
//                                    .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                    .child(mHolder.media.getUser_id())
                                    .child(mHolder.media.getPhoto_id())
                                    .child(mContext.getString(R.string.field_likes))
                                    .child(keyID)
                                    .removeValue();

                            mHolder.heart.toggleLike();
                            getLikesString(mHolder);
                        }
                        //case2: The user has not liked the media
                        else if (!mHolder.likeByCurrentUser) {
                            //add new like
                            addNewLike(mHolder);
                            break;
                        }
                    }
                    if (!dataSnapshot.exists()) {
                        //add new like
                        addNewLike(mHolder);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Toast.makeText(mContext, databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    /*public static boolean isPackageInstalled(String packageName) {
        try {
            List<ApplicationInfo> installedApplications =
                    getPackageManager().getInstalledApplications(0);
            for (ApplicationInfo applicationInfo : installedApplications) {
                if (applicationInfo.packageName.equals(packageName)) {
                    return true;
                }
            }
        } catch (Exception e) {
            CrittericismUtils.logHandledException(e);
        }
        return false;
    }*/
}