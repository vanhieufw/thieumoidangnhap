package com.movie.model;

import java.util.Date;

public class Showtime {
    private int showtimeID;
    private int movieID;
    private int roomID;
    private Date showDate;
    private int staffID;
    private String movieTitle;
    private String roomName;
    private String staffName;
    private String status;
    private int isVisible; // Đổi từ boolean thành int

    // Constructor
    public Showtime() {
        this.isVisible = 0; // Mặc định isVisible = 0
    }

    // Getters and Setters
    public int getShowtimeID() {
        return showtimeID;
    }

    public void setShowtimeID(int showtimeID) {
        this.showtimeID = showtimeID;
    }

    public int getMovieID() {
        return movieID;
    }

    public void setMovieID(int movieID) {
        this.movieID = movieID;
    }

    public int getRoomID() {
        return roomID;
    }

    public void setRoomID(int roomID) {
        this.roomID = roomID;
    }

    public Date getShowDate() {
        return showDate;
    }

    public void setShowDate(Date showDate) {
        this.showDate = showDate;
    }

    public int getStaffID() {
        return staffID;
    }

    public void setStaffID(int staffID) {
        this.staffID = staffID;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getStaffName() {
        return staffName;
    }

    public void setStaffName(String staffName) {
        this.staffName = staffName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getIsVisible() { // Đổi từ isVisible() thành getIsVisible()
        return isVisible;
    }

    public void setIsVisible(int isVisible) { // Đổi từ setVisible(boolean) thành setIsVisible(int)
        this.isVisible = isVisible;
    }

    @Override
    public String toString() {
        return "Showtime{" +
                "showtimeID=" + showtimeID +
                ", movieID=" + movieID +
                ", roomID=" + roomID +
                ", showDate=" + showDate +
                ", staffID=" + staffID +
                ", movieTitle='" + movieTitle + '\'' +
                ", roomName='" + roomName + '\'' +
                ", staffName='" + staffName + '\'' +
                ", status='" + status + '\'' +
                ", isVisible=" + isVisible +
                '}';
    }
}