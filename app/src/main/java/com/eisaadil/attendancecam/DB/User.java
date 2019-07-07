package com.eisaadil.attendancecam.DB;

import java.util.ArrayList;
import java.util.List;

public class User {
    private String email;
    private String fullName;
    private List<String> courseIds;

    public User() {
    }

    public User(String email, String fullName) {
        this.email = email;
        this.fullName = fullName;
        this.courseIds = new ArrayList<>();
    }

    public User(String email, String fullName, List<String> courseIds) {
        this.email = email;
        this.fullName = fullName;
        this.courseIds = courseIds;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public List<String> getCourseIds() {
        return courseIds;
    }

    public void setCourseIds(List<String> courseIds) {
        this.courseIds = courseIds;
    }


    @Override
    public String toString() {
        return "User{" +
                "email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", courseIds=" + courseIds +
                '}';
    }
}
