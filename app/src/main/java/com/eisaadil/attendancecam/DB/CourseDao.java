package com.eisaadil.attendancecam.DB;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

/**
 * Created by eisaadil on 04/05/18.
 */

@Dao
public interface CourseDao {
    @Query("SELECT * FROM Course")
    List<Course> getAll();

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(Course... courses);

    @Query("SELECT COUNT(*) from Course")
    int countCourses();

    @Query("SELECT numberOfClasses from Course where courseId = :courseId")
    int getNumberOfClasses(String courseId);

    @Query("SELECT courseName from Course where courseId = :courseId")
    String getCourseNameById(String courseId);

    @Query("DELETE from Course where courseId=:courseId")
    void deleteByCourseId(String courseId);
}
