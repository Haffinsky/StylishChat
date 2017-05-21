package com.example.rafal.stylishchat;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

/**
 * Created by Rafal on 5/21/2017.
 */

class MessageViewHolder extends RecyclerView.ViewHolder {
    ImageView photoImageView;
    TextView messageTextView;
    TextView authorTextView;

    public MessageViewHolder(View itemView) {
        super(itemView);
        photoImageView = (ImageView) itemView.findViewById(R.id.photoImageView);
        messageTextView = (TextView) itemView.findViewById(R.id.messageTextView);
        authorTextView = (TextView) itemView.findViewById(R.id.nameTextView);
    }
    public void setAuthorName(String name){
        authorTextView.setText(name);
    }
    public void setPhoto(String photoUrl) {

        messageTextView.setVisibility(View.GONE);
        photoImageView.setVisibility(View.VISIBLE);
        Glide.with(photoImageView.getContext())
                .load(photoUrl)
                .into(photoImageView);
    }
    public void setText(String text){
            messageTextView.setVisibility(View.VISIBLE);
            photoImageView.setVisibility(View.GONE);
            messageTextView.setText(text);
        }
    }
