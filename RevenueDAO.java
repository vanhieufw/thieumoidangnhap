package com.movie.dao;

import com.movie.util.DBConnection;
import javax.swing.JOptionPane;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

/**
 * Data Access Object for retrieving revenue-related data from the database.
 */
public class RevenueDAO {
    private static final String GET_REVENUE_BY_DATE_AND_MOVIE =
            "SELECT MovieTitle, SUM(Price) as TotalRevenue " +
                    "FROM BookingHistory WHERE CAST(BookingDate AS DATE) = ? GROUP BY MovieTitle";
    private static final String GET_TOTAL_REVENUE_BY_DATE =
            "SELECT SUM(Price) as TotalRevenue " +
                    "FROM BookingHistory WHERE CAST(BookingDate AS DATE) = ?";
    private static final String GET_TOTAL_REVENUE =
            "SELECT SUM(Price) as TotalRevenue FROM BookingHistory";
    private static final String GET_DAILY_REVENUE =
            "SELECT CAST(BookingDate AS DATE) as BookingDay, SUM(Price) as TotalRevenue " +
                    "FROM BookingHistory GROUP BY CAST(BookingDate AS DATE) ORDER BY BookingDay";

    /**
     * Retrieves the revenue for a specific movie on a given date.
     * @param dateStr The date in 'yyyy-MM-dd' format.
     * @param movieTitle The title of the movie.
     * @return The total revenue for the movie on the specified date.
     * @throws SQLException If a database error occurs.
     */
    public double getRevenueByDateAndMovie(String dateStr, String movieTitle) throws SQLException {
        if (dateStr == null || dateStr.isEmpty() || movieTitle == null || movieTitle.isEmpty()) {
            throw new IllegalArgumentException("Ngày và tiêu đề phim không được để trống");
        }

        try {
            // Validate date format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            sdf.parse(dateStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày không hợp lệ: " + dateStr);
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_REVENUE_BY_DATE_AND_MOVIE)) {
            stmt.setDate(1, java.sql.Date.valueOf(dateStr));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    if (rs.getString("MovieTitle").equalsIgnoreCase(movieTitle)) {
                        return rs.getDouble("TotalRevenue");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving revenue for movie " + movieTitle + ": " + e.getMessage());
            throw e;
        }
        return 0.0;
    }

    /**
     * Retrieves the total revenue for a specific date.
     * @param dateStr The date in 'yyyy-MM-dd' format.
     * @return The total revenue for the specified date.
     * @throws SQLException If a database error occurs.
     */
    public double getTotalRevenueByDate(String dateStr) throws SQLException {
        if (dateStr == null || dateStr.isEmpty()) {
            throw new IllegalArgumentException("Ngày không được để trống");
        }

        try {
            // Validate date format
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            sdf.setLenient(false);
            sdf.parse(dateStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("Định dạng ngày không hợp lệ: " + dateStr);
        }

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_TOTAL_REVENUE_BY_DATE)) {
            stmt.setDate(1, java.sql.Date.valueOf(dateStr));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("TotalRevenue");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving total revenue for date " + dateStr + ": " + e.getMessage());
            throw e;
        }
        return 0.0;
    }

    /**
     * Retrieves the total revenue from all bookings.
     * @return The total revenue.
     * @throws SQLException If a database error occurs.
     */
    public double getTotalRevenue() throws SQLException {
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_TOTAL_REVENUE);
             ResultSet rs = stmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("TotalRevenue");
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving total revenue: " + e.getMessage());
            throw e;
        }
        return 0.0;
    }

    /**
     * Retrieves the daily revenue grouped by date.
     * @return A map with the date (yyyy-MM-dd) as the key and the total revenue for that date as the value.
     * @throws SQLException If a database error occurs.
     */
    public Map<String, Double> getDailyRevenue() throws SQLException {
        Map<String, Double> dailyRevenue = new TreeMap<>(); // Sử dụng TreeMap để tự động sắp xếp theo ngày
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(GET_DAILY_REVENUE);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                String bookingDay = rs.getString("BookingDay");
                double revenue = rs.getDouble("TotalRevenue");
                dailyRevenue.put(bookingDay, revenue);
            }
        } catch (SQLException e) {
            System.err.println("Error retrieving daily revenue: " + e.getMessage());
            throw e;
        }
        return dailyRevenue;
    }
}