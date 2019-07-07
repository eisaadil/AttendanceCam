package com.eisaadil.attendancecam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eisaadil.attendancecam.DB.AppDatabase;
import com.eisaadil.attendancecam.DB.Course;
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
import com.microsoft.projectoxford.face.contract.LargePersonGroup;
import com.pixplicity.easyprefs.library.Prefs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditCourses extends AppCompatActivity {

    ListView courseListView;
    CoursesListAdapter coursesListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_courses);

        setSupportActionBar((android.support.v7.widget.Toolbar) findViewById(R.id.toolbar));

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);

        courseListView = findViewById(R.id.courseList);

        (findViewById(R.id.addCourseFab)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(EditCourses.this, AddCourse.class));
            }
        });

        Log.v("EditCourses", Prefs.getString("UserID", "A"));
        Log.v("EditCourses", Prefs.getString("UserEmail", "A"));
        Log.v("EditCourses", Prefs.getString("UserDisplayName", "A"));
        Log.v("EditCourses", Prefs.getString("UserCourseIds", "A"));
    }

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_logout:

                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (which){
                            case DialogInterface.BUTTON_POSITIVE:
                                FirebaseAuth.getInstance().signOut();
                                startActivity(new Intent(EditCourses.this, MainActivity.class));
                                finish();
                                break;

                            case DialogInterface.BUTTON_NEGATIVE:
                                dialog.dismiss();
                                break;
                        }
                    }
                };
                AlertDialog.Builder ab = new AlertDialog.Builder(EditCourses.this);
                ab.setMessage("Are you sure you want to Logout?").setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                return true;
            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onStart() {
        super.onStart();
        new GetPersonGroupList().execute();
    }



    public class CoursesListAdapter extends ArrayAdapter<Course> {

        private Context context;

        public CoursesListAdapter(Activity context, int resource, List<Course> courses) {
            super(context, resource, courses);
            this.context = context;
        }


        @Override
        public long getItemId(int i) {
            return 0;
        }


        public View getView(int position, View convertView, ViewGroup parent) {

            final Course course = getItem(position);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_course_row, parent, false);
            }


            TextView courseNameAndYear = convertView.findViewById(R.id.courseNameAndYear);
            TextView courseCode = convertView.findViewById(R.id.courseCode);

            courseNameAndYear.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Prefs.putString("courseId", course.courseId);

                    Intent intent = new Intent(EditCourses.this, Menu.class);
                    startActivity(intent);
                }
            });

            CircleImageView deleteCourseButton = convertView.findViewById(R.id.deleteCourseButton);
            deleteCourseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    new DeletePersonGroupTask().execute(course.courseId);
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    dialog.dismiss();
                                    break;
                            }
                        }
                    };
                    AlertDialog.Builder ab = new AlertDialog.Builder(EditCourses.this);
                    ab.setMessage("Are you sure to delete this course?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();


                }
            });

            assert course != null;
            courseNameAndYear.setText(course.courseName+" - Year "+course.year);
            courseCode.setText(course.courseCode);

            return convertView;

        }
    }

    class GetPersonGroupList extends AsyncTask<Void, Void, LargePersonGroup[]> {

        @Override
        protected LargePersonGroup[] doInBackground(Void... params) {
            Log.v("","Generate list of all person groups");

            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try{
                Log.v("",("Syncing with server to add person group..."));

                return faceServiceClient.listLargePersonGroups();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }




        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(Void... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(LargePersonGroup[] allLpgs) {

            //if lpg course of not user remove

            if (allLpgs.length==0){
                Toast.makeText(EditCourses.this, "No Courses Created Yet", Toast.LENGTH_LONG).show();
                (findViewById(R.id.classListProgress)).setVisibility(View.GONE);
                return;
            }

            if (Prefs.getString("UserCourseIds","").equals("")){
                Toast.makeText(EditCourses.this, "No Courses Created Yet", Toast.LENGTH_LONG).show();
                (findViewById(R.id.classListProgress)).setVisibility(View.GONE);
                return;
            }

            List<String> courseIdsForUser = Arrays.asList(((new Gson()).fromJson(Prefs.getString("UserCourseIds",""), String[].class)));

            List<LargePersonGroup> courseLpgs = new ArrayList<>();

            for (LargePersonGroup lpg: allLpgs){
                if (courseIdsForUser.contains(lpg.largePersonGroupId)) courseLpgs.add(lpg);
            }


            if (courseLpgs.size() == 0){
                Toast.makeText(EditCourses.this, "No Courses Created Yet", Toast.LENGTH_LONG).show();
                (findViewById(R.id.classListProgress)).setVisibility(View.GONE);
                return;
            }

            List<Course> courseList = new ArrayList<>();
            for (LargePersonGroup lpg: courseLpgs){

                AppDatabase db = AppDatabase.getAppDatabase(getApplicationContext());

                CourseData courseData = (new Gson()).fromJson(lpg.userData, CourseData.class);

                Course newCourse = new Course(lpg.largePersonGroupId, lpg.name, courseData.year, Integer.parseInt(courseData.numberOfClasses), courseData.courseCode);
                db.courseDao().insertAll(newCourse);
                courseList.add(newCourse);
            }

            (findViewById(R.id.classListProgress)).setVisibility(View.GONE);

            coursesListAdapter = new CoursesListAdapter(EditCourses.this, R.layout.list_course_row, courseList);
            courseListView.setAdapter(coursesListAdapter);



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
    }

    class DeletePersonGroupTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            Log.v("","Request: Delete person group " + params[0]);

            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try{
                Log.v("",("Deleting person group..."));
                faceServiceClient.deleteLargePersonGroup(params[0]);
                return params[0];
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            }
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(final String deletedCourseId) {
            if(!deletedCourseId.equals("")){
                Toast.makeText(EditCourses.this, "Course successfully deleted", Toast.LENGTH_LONG).show();

                AppDatabase db = AppDatabase.getAppDatabase(getApplicationContext());
                db.courseDao().deleteByCourseId(deletedCourseId);

                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                FirebaseDatabase d = FirebaseDatabase.getInstance();
                final DatabaseReference databaseReference = d.getReference().child("users").child(user.getUid()).child("courseIds");

                databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot ds) {
                        List<String> courseIds = new ArrayList<>();
                        for (DataSnapshot courseData: ds.getChildren()){
                            if (!courseData.getValue().equals(deletedCourseId)){
                                courseIds.add((String)courseData.getValue());
                            }
                        }
                        databaseReference.setValue(courseIds);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });



                onStart();
            }
            else{
                Toast.makeText(EditCourses.this, "Course could not be deleted", Toast.LENGTH_LONG).show();
            }
        }
    }


}
