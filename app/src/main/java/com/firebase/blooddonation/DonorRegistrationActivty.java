package com.firebase.blooddonation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class DonorRegistrationActivty extends AppCompatActivity {

    private TextView backButton;
    private CircleImageView profileImage;
    private TextInputEditText registerFullName , registerIdNumber , registerPhoneNumber , registerEmail , registerPassword ;
    private Spinner bloodGroupsSpinner ;
    private Button registerButton;
    private Uri resultUri ;
    private ProgressDialog loader;

    private FirebaseAuth mAuth;
    private DatabaseReference userDatabaseRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donor_registration_activty);

        backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(DonorRegistrationActivty.this , LoginActivity.class);
                startActivity(intent);
            }
        });


        profileImage = findViewById(R.id.profile_image);
        registerIdNumber = findViewById(R.id.registerIdNumber);
        registerFullName = findViewById(R.id.registerFullName);
        registerPhoneNumber = findViewById(R.id.registerPhoneNumber);
        registerEmail = findViewById(R.id.registerEmail);
        registerPassword = findViewById(R.id.registerPassword);
        bloodGroupsSpinner = findViewById(R.id.bloodGroupSpinner);
        registerButton = findViewById(R.id.registerButton);
        loader  = new ProgressDialog(this);

        mAuth = FirebaseAuth.getInstance();

        profileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               Intent intent = new  Intent(Intent.ACTION_PICK);
               intent.setType("image/*");
               startActivityForResult(intent ,1 );
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final String email = registerEmail.getText().toString().trim();
                final String password = registerPassword.getText().toString().trim();
                final String fullName = registerFullName.getText().toString().trim();
                final String idNumber = registerIdNumber.getText().toString().trim();
                final String phoneNumber = registerPhoneNumber.getText().toString().trim();
                final String bloodGroup = bloodGroupsSpinner.getSelectedItem().toString();

                if(TextUtils.isEmpty(email)){
                    registerEmail.setError("Email is required");
                    return;
                }
                if(TextUtils.isEmpty(password)){
                    registerPassword.setError("password is required");
                    return;
                }
                if(TextUtils.isEmpty(fullName)){
                    registerFullName.setError("Name is required");
                    return;
                }
                if(TextUtils.isEmpty(idNumber)){
                    registerIdNumber.setError("Id is required");
                    return;
                }
                if(TextUtils.isEmpty(phoneNumber)){
                    registerPhoneNumber.setError("Phone Number is required");
                    return;
                }
                if(bloodGroup.equals("Select your blood group")){
                    Toast.makeText(DonorRegistrationActivty.this, "Select a Blood Group", Toast.LENGTH_SHORT).show();
                    return;
                }
                else{
loader.setMessage("Registering You...");
loader.setCanceledOnTouchOutside(false);
loader.show();

mAuth.createUserWithEmailAndPassword(email , password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
    @Override
    public void onComplete(@NonNull Task<AuthResult> task) {

        if(!task.isSuccessful()){
            String error = task.getException().toString();
            Toast.makeText(DonorRegistrationActivty.this, "Error" + error, Toast.LENGTH_SHORT).show();
        }
        else{
            String currentUerId = mAuth.getCurrentUser().getUid();
            userDatabaseRef = FirebaseDatabase.getInstance().getReference()
                    .child("users").child(currentUerId);
            HashMap userInfo = new HashMap();
            userInfo.put("id" , currentUerId);
            userInfo.put("name" , fullName);
            userInfo.put("email" , email);
            userInfo.put("idnumber" , idNumber);
            userInfo.put("phonenumber" , phoneNumber);
            userInfo.put("bloodgroup" , bloodGroup);
            userInfo.put("type" , "donor");
            userInfo.put("search" , "donor" + bloodGroup);

            userDatabaseRef.updateChildren(userInfo).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if(task.isSuccessful()){
                        Toast.makeText(DonorRegistrationActivty.this, "Data set successful", Toast.LENGTH_SHORT).show();
                    }else{
                        Toast.makeText(DonorRegistrationActivty.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                    }

                    finish();
                  //  loader.dismiss();
                }
            });

            if(resultUri != null){
                final StorageReference filePath = FirebaseStorage.getInstance().getReference()
                        .child("profile images").child(currentUerId);
                Bitmap bitMap = null ;

                try {
                    bitMap = MediaStore.Images.Media.getBitmap(getApplication().getContentResolver(), resultUri);
                }catch(Exception e){
                    e.printStackTrace();
                }
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                bitMap.compress(Bitmap.CompressFormat.JPEG,20,byteArrayOutputStream);
                byte[] data = byteArrayOutputStream.toByteArray();
                UploadTask uploadTask = filePath.putBytes(data);

                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(DonorRegistrationActivty.this, "Image Upload Fail", Toast.LENGTH_SHORT).show();
                    }
                });

                uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                        if(taskSnapshot.getMetadata() != null && taskSnapshot.getMetadata().getReference() != null){
                            Task<Uri> result = taskSnapshot.getStorage().getDownloadUrl();
                            result.addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    String imageUrl = uri.toString();
                                    Map newImageMap = new HashMap();
                                    newImageMap.put("profilepictureurl" , imageUrl);

                                    userDatabaseRef.updateChildren(newImageMap).addOnCompleteListener(new OnCompleteListener() {
                                        @Override
                                        public void onComplete(@NonNull Task task) {
                                            if(task.isSuccessful()){
                                                Toast.makeText(DonorRegistrationActivty.this, "Image url added to database", Toast.LENGTH_SHORT).show();
                                            }else{
                                                Toast.makeText(DonorRegistrationActivty.this, task.getException().toString(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });
                                    finish();
                                }
                            });
                        }
                    }
                });

                Intent intent = new Intent(DonorRegistrationActivty.this , MainActivity.class);
                startActivity(intent);
                finish();
                loader.dismiss();
            }

        }
    }
});

                }
            }
        });


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1 && resultCode == RESULT_OK && data!= null ){
            resultUri = data.getData();
            profileImage.setImageURI(resultUri);
        }
    }
}