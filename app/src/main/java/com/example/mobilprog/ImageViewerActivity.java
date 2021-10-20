package com.example.mobilprog;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

public class ImageViewerActivity extends AppCompatActivity {

    ImageView imageView;
    FirebaseStorage storage;
    StorageReference image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_viewer);
        imageView = findViewById(R.id.imageView);


        storage = FirebaseStorage.getInstance();

        String name = getIntent().getStringExtra("image");
        image = storage.getReference().child("Images/" + name);

        GlideApp.with(getApplicationContext()).load(image).into(imageView);
    }

    public void onDelete(View view) {
        image.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(ImageViewerActivity.this, "Deleted!", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(ImageViewerActivity.this, MainActivity.class);
                        startActivity(i);

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ImageViewerActivity.this, "Delete failed!", Toast.LENGTH_SHORT).show();
                    }
                })
        ;
    }
}