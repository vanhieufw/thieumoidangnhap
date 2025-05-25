package com.movie.util;

public class GenerateAdminPassword {
    public static void main(String[] args) {
        String password = "123456789";
        String hashedPassword = PasswordEncrypter.hashPassword(password); // Sửa từ encrypt thành hashPassword
        System.out.println("Mật khẩu đã mã hóa cho admin: " + hashedPassword);
    }
}