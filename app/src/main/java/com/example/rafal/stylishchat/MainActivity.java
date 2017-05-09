package com.example.rafal.stylishchat;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

        private static final String TAG = "MainActivity";

        public static final String ANONYMOUS = "anonymous";
        public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
        public static final int RC_SIGN_IN = 1;
        private static final int RC_PHOTO_PICKER =  2;
        private ListView mMessageListView;
        private MessageAdapter mMessageAdapter;
        private ProgressBar mProgressBar;
        private ImageButton mPhotoPickerButton;
        private EditText mMessageEditText;
        private Button mSendButton;
        private FirebaseDatabase mfirebaseDatabase;
        private DatabaseReference mDatabaseReference;
        private String mUsername;
        private ChildEventListener mChildEventListener;
        private FirebaseAuth mFirebaseAuth;
        private FirebaseAuth.AuthStateListener mAuthStateListener;
        private FirebaseStorage mFirebaseStorage;
        private StorageReference mChatPhotosStorageReference;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            mUsername = ANONYMOUS;

            mfirebaseDatabase = FirebaseDatabase.getInstance();
            mFirebaseAuth = FirebaseAuth.getInstance();
            mFirebaseStorage = FirebaseStorage.getInstance();

            mChatPhotosStorageReference = mFirebaseStorage.getReference().child("chat_photos");
            mDatabaseReference = mfirebaseDatabase.getReference().child("messages");
            // Initialize references to views
            mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
            mMessageListView = (ListView) findViewById(R.id.messageListView);
            mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
            mMessageEditText = (EditText) findViewById(R.id.messageEditText);
            mSendButton = (Button) findViewById(R.id.sendButton);

            // Initialize message ListView and its adapter
            List<Message> messages = new ArrayList<>();
            mMessageAdapter = new MessageAdapter(this, R.layout.message, messages);
            mMessageListView.setAdapter(mMessageAdapter);

            // Initialize progress bar
            mProgressBar.setVisibility(ProgressBar.INVISIBLE);

            // ImagePickerButton shows an image picker to upload a image for a message
            mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/jpeg");
                    intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                    startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
                }
            });

            // Enable Send button when there's text to send
            mMessageEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    if (charSequence.toString().trim().length() > 0) {
                        mSendButton.setEnabled(true);
                    } else {
                        mSendButton.setEnabled(false);
                    }
                }

                @Override
                public void afterTextChanged(Editable editable) {
                }
            });
            mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

            // Send button sends a message and clears the EditText
            mSendButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Send messages on click
                    Message message = new Message(mMessageEditText.getText().toString(), mUsername, null);
                    // Clear input box
                    mDatabaseReference.push().setValue(message);

                    mMessageEditText.setText("");
                }
            });

            mAuthStateListener = new FirebaseAuth.AuthStateListener() {
                @Override
                public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user != null){
                        onSignedIn(user.getDisplayName());
                    } else {
                        onSignedOut();
                        startActivityForResult(
                                AuthUI.getInstance()
                                        .createSignInIntentBuilder()
                                        .setIsSmartLockEnabled(false)
                                        .setProviders(
                                                AuthUI.EMAIL_PROVIDER,
                                                AuthUI.GOOGLE_PROVIDER)
                                        .build(),
                                RC_SIGN_IN);
                    }

                }
            };
        }
        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data){
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == RC_SIGN_IN){
                if (resultCode == RESULT_OK){
                    Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
                }   else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                    finish();
                }
                } else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK){
                    Uri selectedImageUri = data.getData();
                    StorageReference photoRef =
                            mChatPhotosStorageReference.child(selectedImageUri.getLastPathSegment());
                    photoRef.putFile(selectedImageUri).addOnSuccessListener
                            (this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                    Message message = new Message(null, mUsername, downloadUrl.toString());
                                    mDatabaseReference.push().setValue(message);
                                }
                            });
                }
            }
        

        @Override
        public boolean onCreateOptionsMenu(Menu menu) {
            MenuInflater inflater = getMenuInflater();
            inflater.inflate(R.menu.menu, menu);
            return true;
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case R.id.sign_out_menu:
                    AuthUI.getInstance().signOut(this);
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }
        @Override
        protected void onPause(){
            super.onPause();
            if (mAuthStateListener != null) {
                mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
            }
            mMessageAdapter.clear();
            detachDatabaseReadListener();
        }
        @Override
        protected void onResume(){
            super.onResume();
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }
        private void onSignedIn(String username){
            mUsername = username;
            attachDatabaseReadListener();
        }
        private void onSignedOut(){
            mUsername = ANONYMOUS;
            mMessageAdapter.clear();
            detachDatabaseReadListener();
        }
        private void attachDatabaseReadListener(){
            if (mChildEventListener == null) {
                mChildEventListener = new ChildEventListener() {
                    @Override
                    public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                        Message message = dataSnapshot.getValue(Message.class);
                        mMessageAdapter.add(message);
                    }

                    @Override
                    public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onChildRemoved(DataSnapshot dataSnapshot) {
                    }

                    @Override
                    public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                };
                mDatabaseReference.addChildEventListener(mChildEventListener);
            }
        }
        private void detachDatabaseReadListener(){
            if (mChildEventListener != null) {
                mDatabaseReference.removeEventListener(mChildEventListener);
                mChildEventListener = null;
            }
        }
    }