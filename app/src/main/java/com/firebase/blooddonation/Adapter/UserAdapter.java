package com.firebase.blooddonation.Adapter;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.blooddonation.Email.JavaMailApi;
import com.firebase.blooddonation.Model.User;
import com.firebase.blooddonation.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.ViewHolder> {

    private Context context;
    private List<User> userList;

    public UserAdapter(Context context, List<User> userList) {
        this.context = context;
        this.userList = userList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(context).inflate(R.layout.user_displayed_layout,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
final User user = userList.get(position);

holder.type.setText(user.getType());

//if(user.getType().equals("donor")){
//    holder.emailNow.setVisibility(View.GONE);
//}
holder.userEmail.setText(user.getEmail());
holder.phoneNumber.setText(user.getPhonenumber());
holder.userName.setText(user.getName());
holder.bloodGroup.setText(user.getBloodgroup());

        Glide.with(context).load(user.getProfilepictureurl()).into(holder.userProfileImage);


        final String nameOfTheReciever = user.getName();
        final String idOfTheReciever = user.getId();

        //sending email

        holder.emailNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new AlertDialog.Builder(context)
                        .setTitle("SEND EMAIL")
                        .setMessage("Send an email to " + user.getName() + " ?")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                DatabaseReference reference = FirebaseDatabase.getInstance().getReference()
                                        .child("users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                reference.addValueEventListener(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                      String nameOfSender = snapshot.child("name").getValue().toString();
                                      String email = snapshot.child("email").getValue().toString();
                                      String phone = snapshot.child("phonenumber").getValue().toString();
                                      String blood = snapshot.child("bloodgroup").getValue().toString();


                                      String mEmail = user.getEmail();
                                      String msubject = "BLOOD DoNATION";
                                      String mMessage = "Hello Sir/Ma'am " + nameOfTheReciever+", "+nameOfSender+
                                              "would like some blood donation from you. Here's his/her details:\n"
                                              +"Name: "+nameOfSender+ "\n"+
                                              "Phone Number: "+phone+"\n"+
                                              "Email: " +email+ "\n"+
                                              "Blood Group: "+blood+ " \n"+
                                              "Kindly reach out to him/her. Thank you!\n"
                                              +" Save lives byy donating blood ";

                                        JavaMailApi javaMailApi = new JavaMailApi(context , mEmail , msubject , mMessage);
                                        javaMailApi.execute();

                                        DatabaseReference senderRef = FirebaseDatabase.getInstance().getReference("emails")
                                                .child(FirebaseAuth.getInstance().getCurrentUser().getUid());
                                        senderRef.child(idOfTheReciever).setValue(true).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                if(task.isSuccessful()){
                                                    DatabaseReference receiverRef = FirebaseDatabase.getInstance().getReference("emails")
                                                            .child(idOfTheReciever);
                                                    receiverRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).setValue(true);

                                                    addNotifications(idOfTheReciever, FirebaseAuth.getInstance().getCurrentUser().getUid());
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {

                                    }
                                });
                            }
                        })
                        .setNegativeButton("No" , null)
                        .show();
            }
        });

    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        public CircleImageView userProfileImage;
        public TextView type , userName , userEmail , phoneNumber , bloodGroup;
        public Button emailNow;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            userProfileImage = itemView.findViewById(R.id.userProfileImage);
            type = itemView.findViewById(R.id.type);
            userName = itemView.findViewById(R.id.userName);
            userEmail = itemView.findViewById(R.id.userEmail);
            phoneNumber = itemView.findViewById(R.id.phoneNumber);
            bloodGroup = itemView.findViewById(R.id.bloodGroup);
            emailNow = itemView.findViewById(R.id.emailNow);
        }
    }

    //notification
    private void addNotifications(String receiverId , String senderId){
        DatabaseReference reference = FirebaseDatabase.getInstance()
                .getReference().child("notifications").child(receiverId);
        String date = DateFormat.getDateInstance().format(new Date());
        HashMap<String , Object> hashMap = new HashMap<>();
        hashMap.put("receiverId", receiverId);
        hashMap.put("senderId" , senderId);
        hashMap.put("text" , "Sent you an email , kindly check it out.");
        hashMap.put("date" , date);

        reference.push().setValue(hashMap);
    }

}
