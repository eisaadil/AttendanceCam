package com.eisaadil.attendancecam;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.eisaadil.attendancecam.DB.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.ArrayList;
import java.util.List;

public class AddCourse extends AppCompatActivity {

    String courseName;
    String year;
    String numberOfClassesInCourses;
    String courseCode;

    String newCourseId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_course);

        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);

        getSupportActionBar().setDisplayShowHomeEnabled(true);

        (findViewById(R.id.addCourse)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                courseName = ((EditText)findViewById(R.id.courseName)).getText().toString();
                year = ((EditText)findViewById(R.id.year)).getText().toString();
                numberOfClassesInCourses = ((EditText)findViewById(R.id.numberOfClasses)).getText().toString();
                courseCode = ((EditText)findViewById(R.id.courseCode)).getText().toString();

                if (courseName.equals("")){
                    ((EditText)findViewById(R.id.courseName)).setError("Please enter a Course Name");
                    (findViewById(R.id.courseName)).requestFocus();
                    return;
                }
                else if (year.equals("")){
                    ((EditText)findViewById(R.id.year)).setError("Please enter the Year which the course is in");
                    (findViewById(R.id.year)).requestFocus();
                    return;
                }
                else if (numberOfClassesInCourses.equals("")){
                    ((EditText)findViewById(R.id.numberOfClasses)).setError("Please enter the Number of Classes/Lectures in the course");
                    (findViewById(R.id.numberOfClasses)).requestFocus();
                    return;
                }
                else if (courseCode.equals("")){
                    ((EditText)findViewById(R.id.courseCode)).setError("Please enter the Course Code");
                    (findViewById(R.id.courseCode)).requestFocus();
                    return;
                }

                CourseData newCourseData = new CourseData(courseName, year, numberOfClassesInCourses, courseCode);
                newCourseId = courseCode.replace(' ', '-').toLowerCase() + System.nanoTime();
                new AddPersonGroupTask().execute(newCourseId, courseName, (new Gson()).toJson(newCourseData)); //largeGroupId, name, userInfo

                findViewById(R.id.addCourseProgress).setVisibility(View.VISIBLE);
            }
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(AddCourse.this, EditCourses.class));
    }

    private class CourseData{
        String courseName;
        String year;
        String numberOfClasses;
        String courseCode;

        public CourseData(String courseName, String year, String numberOfClasses, String courseCode) {
            this.courseName = courseName;
            this.year = year;
            this.numberOfClasses = numberOfClasses;
            this.courseCode = courseCode;
        }
    }

    class AddPersonGroupTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            Log.v("","Request: Creating person group " + params[0]);

            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try{
                Log.v("",("Syncing with server to add person group..."));

                // Start creating person group in server.
                faceServiceClient.createLargePersonGroup(
                        params[0],
                        params[1],
                        params[2]);

                return params[0];
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(Void... progress) {
        }

        @Override
        protected void onPostExecute(final String result) {
            findViewById(R.id.addCourseProgress).setVisibility(View.INVISIBLE);
            if (result != null) {
                Log.v("", "Response: Success. Person group " + result + " created");
                //Toast.makeText(getApplicationContext(), "Course for "+result.toUpperCase()+" successfully created", Toast.LENGTH_LONG).show();

                Toast.makeText(getApplicationContext(), "Course successfully created", Toast.LENGTH_LONG).show();

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                FirebaseDatabase d = FirebaseDatabase.getInstance();
                final DatabaseReference databaseReference = d.getReference().child("users").child(user.getUid());
                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        User currentUser = new User();
                        for (DataSnapshot ds: dataSnapshot.getChildren()){
                            switch(ds.getKey()){
                                case "email":
                                    currentUser.setEmail((String)ds.getValue());
                                    break;
                                case "fullName":
                                    currentUser.setFullName((String)ds.getValue());
                                    break;
                                case "courseIds":
                                    currentUser.setCourseIds((ArrayList<String>)ds.getValue());
                                    break;
                            }
                        }

                        List<String> courseIds = new ArrayList<>();

                        if(currentUser.getCourseIds() != null)
                            courseIds.addAll(currentUser.getCourseIds());

                        courseIds.add(newCourseId);

                        currentUser.setCourseIds(courseIds);
                        databaseReference.setValue(currentUser);

                        Prefs.putString("UserCourseIds", new Gson().toJson(currentUser.getCourseIds()));
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                startActivity(new Intent(AddCourse.this, EditCourses.class));
            }
            else{
                Toast.makeText(getApplicationContext(), "The course could not be created at this time", Toast.LENGTH_LONG).show();
            }
        }
    }
}
