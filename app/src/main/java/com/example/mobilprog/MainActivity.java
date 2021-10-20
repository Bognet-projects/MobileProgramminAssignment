package com.example.mobilprog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int TAKE_IMAGE_REQUEST = 22;
    private FusedLocationProviderClient fusedLocationClient;
    private LinearLayout gallery;
    private String currentPhotoPath;
    private ProgressBar progressBar;
    FirebaseStorage storage;
    StorageReference storageReference;
    StorageReference listRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        ActivityCompat.requestPermissions(this, new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"}, 0);


        gallery = findViewById(R.id.gallery);
        progressBar = findViewById(R.id.uploadProgress);
        progressBar.setVisibility(ViewGroup.INVISIBLE);

        listRef = storage.getReference().child("Images/");

        loadImages();

    }

    public void onMap(View view) {
        Intent i = new Intent(this, MapsActivity.class);
        startActivity(i);
    }

    public void onImage(View view) {
        Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (i.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(MainActivity.this, "Error occurred while creating the File!", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.android.fileprovider", photoFile);
                i.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(i, TAKE_IMAGE_REQUEST);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == TAKE_IMAGE_REQUEST && resultCode == RESULT_OK) {

            File f = new File(currentPhotoPath);

            StorageReference ref = storageReference.child("Images/" + f.getName());

            ref.putFile(Uri.fromFile(f))
                    .addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                            double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                            progressBar.setVisibility(View.VISIBLE);
                            progressBar.setProgress((int) progress);
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            progressBar.setVisibility(View.INVISIBLE);
                            Toast.makeText(MainActivity.this, "Image Uploaded!", Toast.LENGTH_SHORT).show();
                            loadImages();
                            createLocationMetadata(ref);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(MainActivity.this, "Failed " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    })
            ;
        }
    }

    private void loadImages() {
        gallery.removeAllViews();
        listRef.listAll()
                .addOnSuccessListener(new OnSuccessListener<ListResult>() {
                    @Override
                    public void onSuccess(ListResult listResult) {
                        for (StorageReference item : listResult.getItems()) {
                            ImageView imageView = new ImageView(getApplicationContext());
                            GlideApp.with(getApplicationContext()).load(item).into(imageView);

                            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                            gallery.addView(imageView, ViewGroup.LayoutParams.MATCH_PARENT, 700);

                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "Image load error!", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private File createImageFile() throws IOException {
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, "jpg", storageDir);

        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void createLocationMetadata(StorageReference ref) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        CancellationTokenSource cancellationTokenSource = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, cancellationTokenSource.getToken())
                .addOnSuccessListener(new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            StorageMetadata metadata = new StorageMetadata.Builder()
                                    .setCustomMetadata("Latitude", String.valueOf(location.getLatitude()))
                                    .setCustomMetadata("Longitude", String.valueOf(location.getLongitude()))
                                    .build();

                            ref.updateMetadata(metadata)
                                    .addOnSuccessListener(new OnSuccessListener<StorageMetadata>() {
                                        @Override
                                        public void onSuccess(StorageMetadata storageMetadata) {
                                            Toast.makeText(MainActivity.this, "Image Location is saved!", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            Toast.makeText(MainActivity.this, "Failed to save location for the image!", Toast.LENGTH_LONG).show();
                                        }
                                    })
                            ;
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "The current location is not available!", Toast.LENGTH_SHORT).show();
                    }
                })

        ;
    }

}