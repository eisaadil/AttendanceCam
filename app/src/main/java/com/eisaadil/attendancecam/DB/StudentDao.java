package com.eisaadil.attendancecam.DB;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by eisaadil on 04/05/18.
 */

@Dao
public interface StudentDao {
    //@Query("SELECT * FROM Student where courseId = :courseId")
    //List<StudentAndAllFaces> loadStudentsAndAllFaces(int courseId);

    @Query("SELECT * FROM Student where courseId=:courseId")
    List<Student> getAllByCourseId(String courseId);

    @Query("SELECT * FROM Student where studentId = :studentId")
    Student getStudentFromId(String studentId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(Student... students);

    @Query("SELECT COUNT(*) from Student")
    int countStudents();

    @Query("SELECT COUNT(*) from Student where regNo = :regNo")
    int checkRegNoUnique(String regNo);

    @Query("DELETE from Student where courseId=:courseId and studentId=:studentId")
    void deleteByStudentId(String courseId, String studentId);

    @Query("DELETE from Student where courseId=:courseId and regNo=:regNo")
    void deleteByStudentRegNo(String courseId, String regNo);

    @Query("DELETE FROM Student")
    void deleteAllStudents();

}
