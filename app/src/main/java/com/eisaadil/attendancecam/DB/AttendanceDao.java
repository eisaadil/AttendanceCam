package com.eisaadil.attendancecam.DB;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

/**
 * Created by eisaadil on 04/05/18.
 */

@Dao
public interface AttendanceDao {

    @Query("SELECT * FROM Attendance where courseId = :courseId and regNo = :regNo LIMIT 1")
    Attendance getAttendance(String courseId, String regNo);

    @Query("UPDATE Attendance SET attendanceNumber = attendanceNumber + 1 WHERE courseId = :courseId and regNo = :regNo")
    void incrementAttendance(String courseId, String regNo);

    @Query("UPDATE Attendance SET attendanceNumber = attendanceNumber - 1 WHERE courseId = :courseId and regNo = :regNo")
    void decrementAttendance(String courseId, String regNo);

    @Query("UPDATE Attendance SET attendanceNumber = :attendanceNumber WHERE courseId = :courseId and regNo = :regNo")
    void setAttendance(String courseId, String regNo, int attendanceNumber);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertAll(Attendance... attendances);

    @Query("DELETE from Attendance where courseId=:courseId and regNo=:regNo")
    void deleteByStudentId(String courseId, String regNo);

}
