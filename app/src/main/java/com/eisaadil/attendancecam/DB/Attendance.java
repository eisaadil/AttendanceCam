package com.eisaadil.attendancecam.DB;

import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.NonNull;

import static android.arch.persistence.room.ForeignKey.CASCADE;

/**
 * Created by eisaadil on 04/05/18.
 */
@Entity(primaryKeys = {"regNo", "courseId"})
public class Attendance {
    @ForeignKey(entity = Student.class, parentColumns = "regNo", childColumns = "regNo", onDelete = CASCADE)
    @NonNull
    public String regNo;

    @ForeignKey(entity = Course.class, parentColumns = "courseId", childColumns = "courseId", onDelete = CASCADE)
    @NonNull
    public String courseId;

    public int attendanceNumber;

    public Attendance(String regNo, String courseId, int attendanceNumber) {
        this.regNo = regNo;
        this.courseId = courseId;
        this.attendanceNumber = attendanceNumber;
    }
}
