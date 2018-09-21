package com.crookchat.Utils;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;
import com.crookchat.R;
import com.crookchat.models.Like;
import com.crookchat.models.Media;
import com.crookchat.models.User;
import com.crookchat.models.UserAccountSettings;
import com.crookchat.videoPlayer.VideoPlayActivity;

/**
 * Created by User on 9/22/2017.
 */

public class FeedListAdapter extends RecyclerView.Adapter<FeedListAdapter.FeedViewHolder> {

    private static final String TAG = "MainFeedListAdapter";
    private final ImageLoader imageLoader = ImageLoader.getInstance();
    OnLoadMoreItemsListener mOnLoadMoreItemsListener;
    private LayoutInflater mInflater;
    private int mLayoutResource;
    private Context mContext;
    private DatabaseReference mReference;
    private String currentUsername = "";
    private List<Media> objects = null;


    public FeedListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<Media> objects) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mLayoutResource = resource;
        this.mContext = context;
        mReference = FirebaseDatabase.getInstance().getReference();
        this.objects = objects;
    }

    @Override
    public FeedViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FeedViewHolder(LayoutInflater.from(mContext), parent);
    }

    @Override
    public void onBindViewHolder(FeedViewHolder holder, int position) {
        Media media = objects.get(position);

    }

    @Override
    public int getItemCount() {
        return objects == null ? 0 : objects.size();
    }

    private boolean reachedEndOfList(int position) {
        return position == getItemCount() - 1;
    }

    private void loadMoreData() {

        try {
            mOnLoadMoreItemsListener = (OnLoadMoreItemsListener) mContext;
        } catch (ClassCastException e) {
        }

        try {
            mOnLoadMoreItemsListener.onLoadMoreItems();
        } catch (NullPointerException e) {
        }
    }

    private void getCurrentUsername() {
        Log.d(TAG, "getCurrentUsername: retrieving user account settings");
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
        Query query = reference
                .child(mContext.getString(R.string.dbname_users))
                .orderByChild(mContext.getString(R.string.field_user_id))
                .equalTo(FirebaseAuth.getInstance().getCurrentUser().getUid());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                    currentUsername = singleSnapshot.getValue(UserAccountSettings.class).getUsername();
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getLikesString(final FeedViewHolder holder) {
        Log.d(TAG, "getLikesString: getting likes string");

        Log.d(TAG, "getLikesString: media id: " + holder.media.getPhoto_id());
        try {
            DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
            Query query = reference
                    .child(mContext.getString(R.string.dbname_photos))
                    .child(holder.media.getPhoto_id())
                    .child(mContext.getString(R.string.field_likes));
            query.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    holder.users = new StringBuilder();
                    for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {

                        DatabaseReference reference = FirebaseDatabase.getInstance().getReference();
                        Query query = reference
                                .child(mContext.getString(R.string.dbname_users))
                                .orderByChild(mContext.getString(R.string.field_user_id))
                                .equalTo(singleSnapshot.getValue(Like.class).getUser_id());
                        query.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                for (DataSnapshot singleSnapshot : dataSnapshot.getChildren()) {
                                    Log.d(TAG, "onDataChange: found like: " +
                                            singleSnapshot.getValue(User.class).getUsername());

                                    holder.users.append(singleSnapshot.getValue(User.class).getUsername());
                                    holder.users.append(",");
                                }

                                String[] splitUsers = holder.users.toString().split(",");

                                //mitch, mitchell.tabian
                                holder.likeByCurrentUser = holder.users.toString().contains(currentUsername + ",");

                                int length = splitUsers.length;
                                if (length == 0) {
                                    holder.likes.setVisibility(View.GONE);
                                    return;
                                }
                                holder.likes.setVisibility(View.GONE);
                                if (length == 1) {
                                    holder.likesString = "Liked by " + splitUsers[0];
                                } else if (length == 2) {
                                    holder.likesString = "Liked by " + splitUsers[0]
                                            + " and " + splitUsers[1];
                                } else if (length == 3) {
                                    holder.likesString = "Liked by " + splitUsers[0]
                                            + ", " + splitUsers[1]
                                            + " and " + splitUsers[2];

                                } else if (length == 4) {
                                    holder.likesString = "Liked by " + splitUsers[0]
                                            + ", " + splitUsers[1]
                                            + ", " + splitUsers[2]
                                            + " and " + splitUsers[3];
                                } else if (length > 4) {
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
                    if (!dataSnapshot.exists()) {
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
        } catch (NullPointerException e) {
            Log.e(TAG, "getLikesString: NullPointerException: " + e.getMessage());
            holder.likesString = "";
            holder.likeByCurrentUser = false;
            //setup likes string
            setupLikesString(holder, holder.likesString);
        }
    }

    private void setupLikesString(final FeedViewHolder holder, String likesString) {
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

    private void addNewLike(final FeedViewHolder holder) {
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

    public interface OnLoadMoreItemsListener {
        void onLoadMoreItems();
    }

    class FeedViewHolder extends RecyclerView.ViewHolder {
        CircleImageView mprofileImage;
        String likesString;
        TextView username, timeDetla, caption, likes, comments;
        SquareImageView image;
        ImageView heartRed, heartWhite, comment, share, video;

        UserAccountSettings settings = new UserAccountSettings();
        User user = new User();
        StringBuilder users;
        String mLikesString;
        boolean likeByCurrentUser;
        Heart heart;
        View.OnClickListener detector;
        Media media;

        FeedViewHolder(LayoutInflater inflater, ViewGroup parent) {
            super(inflater.inflate(R.layout.layout_mainfeed_listitem, parent, false));
            username = itemView.findViewById(R.id.username);
            image = itemView.findViewById(R.id.post_image);
            heartRed = itemView.findViewById(R.id.image_heart_red);
            heartWhite = itemView.findViewById(R.id.image_heart);
            comment = itemView.findViewById(R.id.speech_bubble);
            share = itemView.findViewById(R.id.image_share);
            video = itemView.findViewById(R.id.post_video);
            likes = itemView.findViewById(R.id.image_likes);
            comments = itemView.findViewById(R.id.image_comments_link);
            caption = itemView.findViewById(R.id.image_caption);
            timeDetla = itemView.findViewById(R.id.image_time_posted);
            mprofileImage = itemView.findViewById(R.id.profile_photo);
            heartRed.setOnClickListener(new LikeUnLikeListener(this));
            heartWhite.setOnClickListener(new LikeUnLikeListener(this));
            video.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String url = (String) v.getTag();
                    if (!TextUtils.isEmpty(url)) {
                        VideoPlayActivity.startActivity(v.getContext(), url);
                    }
                }
            });
        }


    }

    public class LikeUnLikeListener implements View.OnClickListener {

        FeedViewHolder mHolder;

        public LikeUnLikeListener(FeedViewHolder holder) {
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

}