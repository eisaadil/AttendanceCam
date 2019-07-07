package com.eisaadil.attendancecam;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.eisaadil.attendancecam.DB.AppDatabase;
import com.eisaadil.attendancecam.DB.Attendance;
import com.eisaadil.attendancecam.DB.Student;
import com.google.gson.Gson;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Person;
import com.opencsv.CSVReader;
import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvToBeanBuilder;
import com.pixplicity.easyprefs.library.Prefs;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class EditStudents extends AppCompatActivity {

    ListView studentListView;
    String courseId;

    List<Student> students = null;
    StudentListAdapter studentListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_students);

        AppDatabase db = AppDatabase.getAppDatabase(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        TextView toolbarSubtitle = toolbar.findViewById(R.id.toolbar_subtitle);
        toolbarSubtitle.setText(db.courseDao().getCourseNameById(Prefs.getString("courseId", "")));

        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
        getSupportActionBar().setElevation(100);
        //ab.setSubtitle("Add or Delete students");



        (findViewById(R.id.addStudent)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(EditStudents.this, AddStudent.class));
            }
        });

    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(EditStudents.this, Menu.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        students = new ArrayList<>();

        studentListView = findViewById(R.id.studentListView);

        studentListView.setEmptyView(findViewById(R.id.editStudentsListProgress));

        studentListAdapter = new StudentListAdapter(this, R.layout.list_edit_student_row, students);
        studentListView.setAdapter(studentListAdapter);

        courseId = Prefs.getString("courseId", "");

        studentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                //see all faces of the student
            }
        });
        new GetAllStudentsTask().execute();
    }

    public class StudentListAdapter extends ArrayAdapter<Student> {

        private Context context;

        public StudentListAdapter(Activity context, int resource, List<Student> students) {
            super(context, resource, students);
            this.context = context;
        }


        @Override
        public long getItemId(int i) {
            return 0;
        }


        public View getView(int position, View convertView, ViewGroup parent) {

            final Student student = getItem(position);

            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.list_edit_student_row, parent, false);
            }

            TextView studentName = convertView.findViewById(R.id.studentName);
            ImageView studentFaceImage = convertView.findViewById(R.id.studentFaceImage);
            TextView studentRegNo = convertView.findViewById(R.id.studentRegNo);

            CircleImageView deleteStudentButton = convertView.findViewById(R.id.deleteStudentButton);
            deleteStudentButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case DialogInterface.BUTTON_POSITIVE:
                                    new DeletePersonTask().execute(student.courseId, student.studentId, student.regNo);
                                    break;

                                case DialogInterface.BUTTON_NEGATIVE:
                                    dialog.dismiss();
                                    break;
                            }
                        }
                    };
                    AlertDialog.Builder ab = new AlertDialog.Builder(EditStudents.this);
                    ab.setMessage("Are you sure to delete this person?").setPositiveButton("Yes", dialogClickListener)
                            .setNegativeButton("No", dialogClickListener).show();

                }
            });

            assert student != null;
            studentName.setText(student.studentName);
            studentRegNo.setText(student.regNo);


            String[] faceIDs = (new Gson()).fromJson(student.faceArrayJson, String[].class);



            if (faceIDs.length != 0) {
                String photoPath = Environment.getExternalStorageDirectory() + "/Faces/" + faceIDs[0] + ".jpg"; //take first faceId image /storage/emulated/0/7a677caf-1ece-47af-a771-857f979cd241.jpg

                if (!(new File(photoPath).exists())){
                    studentFaceImage.setImageResource(R.drawable.person_icon);
                }
                else {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 8;
                    final Bitmap bitmap = BitmapFactory.decodeFile(photoPath, options);
                    studentFaceImage.setImageBitmap(bitmap);
                }
            }

            return convertView;

        }
    }

    class GetAllStudentsTask extends AsyncTask<Void, Void, List<Student>>{
        @Override
        protected List<Student> doInBackground(Void... params) {
            Log.v("", "Generate all students task");

            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));

            try{
                Log.v("",("Syncing with server to get all students..."));

                Person[] personArray = faceServiceClient.listPersonsInLargePersonGroup(courseId);

                AppDatabase db = AppDatabase.getAppDatabase(getApplicationContext());

                List<Student> allStudents = new ArrayList<>();

                if (personArray == null || personArray.length == 0){
                    return null;
                }

                for (Person person: personArray){
                    Student student = new Student(person.personId.toString(), courseId, person.name, person.userData, (new Gson()).toJson(person.persistedFaceIds));
                    allStudents.add(student);
                    db.studentDao().insertAll(student);
                    studentListAdapter.notifyDataSetChanged();


                    db.attendanceDao().insertAll(new Attendance(student.regNo, student.courseId, 0));
                }


                Log.v("",("Added all students to database.."));

                return allStudents;

            } catch (Exception e) {
                e.printStackTrace();
                (findViewById(R.id.editStudentsListProgress)).setVisibility(View.INVISIBLE);
                Toast.makeText(EditStudents.this, "No Students Found!", Toast.LENGTH_SHORT).show();
                Toast.makeText(EditStudents.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                (findViewById(R.id.editStudentsListProgress)).setVisibility(View.INVISIBLE);

                return null;
            }
        }

        @Override
        protected void onPostExecute(List<Student> students) {
            if (students == null){
                Toast.makeText(EditStudents.this, "No Students Found!", Toast.LENGTH_SHORT).show();
                (findViewById(R.id.editStudentsListProgress)).setVisibility(View.INVISIBLE);

                super.onPostExecute(null);
                return;
            }
            (findViewById(R.id.editStudentsListProgress)).setVisibility(View.INVISIBLE);

            Collections.sort(students, new Comparator<Student>() {
                @Override
                public int compare(Student t1, Student t2) {
                    return (t1.studentName).compareTo(t2.studentName);
                }
            });

            studentListAdapter = new StudentListAdapter(EditStudents.this, R.layout.list_edit_student_row, students);

            studentListView.setAdapter(studentListAdapter);

            super.onPostExecute(students);
        }
    }

    class DeletePersonTask extends AsyncTask<String, Void, String[]> {

        @Override
        protected String[] doInBackground(String... params) {
            Log.v("","Request: Delete person courseId " + params[0]+" "+"personId "+params[1]);

            // Get an instance of face service client.
            FaceServiceClient faceServiceClient = new FaceServiceRestClient(getString(R.string.subscription_key));
            try{
                Log.v("",("Deleting person..."));
                faceServiceClient.deletePersonInLargePersonGroup(params[0], UUID.fromString(params[1]));
                return params;
            } catch (Exception e) {
                e.printStackTrace();
                return new String[0];
            }
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onPostExecute(String deletedStudentParams[]) { //courseId, studentId, regNo
            if(!deletedStudentParams.equals("")){
                Toast.makeText(EditStudents.this, "Person successfully deleted.", Toast.LENGTH_LONG).show();

                AppDatabase db = AppDatabase.getAppDatabase(getApplicationContext());

                Student deletedStudent = db.studentDao().getStudentFromId(deletedStudentParams[1]);
                String[] facesArray = (new Gson()).fromJson(deletedStudent.faceArrayJson, String[].class);

                db.studentDao().deleteByStudentId(deletedStudentParams[0], deletedStudentParams[1]);

                db.studentDao().deleteByStudentRegNo(deletedStudentParams[0], deletedStudentParams[2]);

                db.attendanceDao().deleteByStudentId(deletedStudentParams[0], deletedStudentParams[2]);

                for (String face: facesArray){
                    String photoPath = Environment.getExternalStorageDirectory() + "/Faces/" + face + ".jpg";

                    File file = new File(photoPath);
                    if(file.exists()){
                        boolean result = file.delete();
                    }
                }

                onStart();


            }
            else{
                Toast.makeText(EditStudents.this, "Person could not be deleted.", Toast.LENGTH_LONG).show();
            }
        }
    }


}
