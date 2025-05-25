package com.movie.bus;

import com.movie.dao.CustomerDAO;
import com.movie.model.Customer;
import com.movie.util.DBConnection;
import com.movie.util.PasswordEncrypter;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CustomerBUS {
    private CustomerDAO customerDAO = new CustomerDAO();

    /**
     * Đăng ký khách hàng mới
     * @param customer thông tin khách hàng cần đăng ký
     * @throws SQLException lỗi khi thao tác với cơ sở dữ liệu
     */
    public void registerCustomer(Customer customer) throws SQLException {
        // Kiểm tra thông tin đầu vào
        if (customer.getFullName() == null || customer.getFullName().isEmpty() ||
                customer.getPassword() == null || customer.getPassword().isEmpty() ||
                customer.getEmail() == null || customer.getEmail().isEmpty()) {
            throw new IllegalArgumentException("Thông tin không được để trống");
        }

        // Kiểm tra email đã tồn tại chưa
        if (isEmailExist(customer.getEmail())) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        // Sử dụng fullName làm username nếu username chưa được thiết lập
        if (customer.getUsername() == null || customer.getUsername().isEmpty()) {
            customer.setUsername(customer.getFullName());
        }

        // Kiểm tra username đã tồn tại chưa
        if (isUsernameExist(customer.getUsername())) {
            throw new IllegalArgumentException("Tên đăng nhập đã được sử dụng");
        }

        // Mã hóa mật khẩu trước khi lưu
        String hashedPassword = PasswordEncrypter.hashPassword(customer.getPassword());
        customer.setPassword(hashedPassword);

        // Lưu khách hàng vào cơ sở dữ liệu
        customerDAO.insertCustomer(customer);
    }

    /**
     * Kiểm tra email đã tồn tại trong hệ thống chưa
     * @param email email cần kiểm tra
     * @return true nếu email đã tồn tại
     */
    private boolean isEmailExist(String email) {
        String query = "SELECT COUNT(*) FROM Customer WHERE Email = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Kiểm tra username đã tồn tại trong hệ thống chưa
     * @param username username cần kiểm tra
     * @return true nếu username đã tồn tại
     */
    private boolean isUsernameExist(String username) {
        String query = "SELECT COUNT(*) FROM Customer WHERE Username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Lấy ID khách hàng dựa vào username
     * @param username tên đăng nhập
     * @return ID khách hàng, hoặc -1 nếu không tìm thấy
     */
    public int getCustomerIdByUsername(String username) {
        String query = "SELECT CustomerID FROM Customer WHERE Username = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("CustomerID");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1; // Trả về -1 nếu không tìm thấy
    }

    /**
     * Xác thực đăng nhập người dùng với mật khẩu đã mã hóa
     * @param username tên đăng nhập
     * @param password mật khẩu người dùng nhập vào (chưa mã hóa)
     * @return true nếu thông tin đăng nhập hợp lệ
     */
    public boolean validateUser(String username, String password) {
        try {
            // Lấy thông tin khách hàng từ cơ sở dữ liệu
            Customer customer = customerDAO.getCustomerByUsername(username);

            // Kiểm tra khách hàng có tồn tại không
            if (customer != null) {
                // So sánh mật khẩu người dùng nhập với mật khẩu đã mã hóa trong DB
                return PasswordEncrypter.checkPassword(password, customer.getPassword());
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @deprecated Phương thức này không an toàn và không nên sử dụng.
     * Sử dụng validateUser() thay thế.
     */
    @Deprecated
    public boolean validateUserPlain(String username, String password) {
        // Phương thức này đã không còn phù hợp vì mật khẩu được lưu dưới dạng mã hóa
        // Chuyển hướng sang validateUser
        return validateUser(username, password);
    }

    /**
     * Lấy danh sách tất cả khách hàng
     * @return danh sách khách hàng
     * @throws SQLException lỗi khi thao tác với cơ sở dữ liệu
     */
    public List<Customer> getAllCustomers() throws SQLException {
        List<Customer> customers = new ArrayList<>();
        String query = "SELECT * FROM Customer";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                Customer customer = new Customer();
                customer.setCustomerID(rs.getInt("CustomerID"));
                customer.setUsername(rs.getString("Username"));
                customer.setPassword(rs.getString("Password"));
                customer.setFullName(rs.getString("FullName"));
                customer.setEmail(rs.getString("Email"));
                customers.add(customer);
            }
        }
        return customers;
    }

    /**
     * Lấy thông tin khách hàng theo ID
     * @param customerId ID khách hàng cần tìm
     * @return đối tượng Customer, hoặc null nếu không tìm thấy
     */
    public Customer getCustomerById(int customerId) {
        String query = "SELECT * FROM Customer WHERE CustomerID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, customerId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Customer customer = new Customer();
                customer.setCustomerID(rs.getInt("CustomerID"));
                customer.setUsername(rs.getString("Username"));
                customer.setPassword(rs.getString("Password"));
                customer.setFullName(rs.getString("FullName"));
                customer.setEmail(rs.getString("Email"));
                return customer;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Cập nhật thông tin khách hàng
     * @param customer thông tin khách hàng cần cập nhật
     * @return true nếu cập nhật thành công
     */
    public boolean updateCustomer(Customer customer) {
        String query = "UPDATE Customer SET FullName = ?, Email = ? WHERE CustomerID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, customer.getFullName());
            stmt.setString(2, customer.getEmail());
            stmt.setInt(3, customer.getCustomerID());
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Cập nhật mật khẩu khách hàng
     * @param customerId ID khách hàng
     * @param newPassword mật khẩu mới (chưa mã hóa)
     * @return true nếu cập nhật thành công
     */
    public boolean updatePassword(int customerId, String newPassword) {
        String query = "UPDATE Customer SET Password = ? WHERE CustomerID = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            // Mã hóa mật khẩu mới trước khi lưu
            String hashedPassword = PasswordEncrypter.hashPassword(newPassword);
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, customerId);
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}