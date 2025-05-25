package com.movie.bus;

import com.movie.util.DBConnection;
import com.movie.util.PasswordEncrypter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdminBUS {
    public boolean validateAdmin(String username, String password) {
        String query = "SELECT Password FROM Admin WHERE Username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                String hashedPassword = rs.getString("Password");
                return PasswordEncrypter.checkPassword(password, hashedPassword);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
}