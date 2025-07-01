/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import connection.koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author ASUS
 */
public class PengelolaanUserController {

    public void showDataEmployee(DefaultTableModel model) {
        try (Connection conn = koneksi.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM tb_user")) {

            model.setRowCount(0);
            while (rs.next()) {
                Object[] row = new Object[]{
                    rs.getString("id_employee"),
                    rs.getString("nama"),
                    rs.getString("alamat"),
                    rs.getString("jenis_kelamin"),
                    rs.getDate("tgl_lahir"),
                    rs.getInt("salary_rate"),
                    rs.getString("no_telp"),
                    rs.getString("email"),
                    rs.getString("username"),
                    rs.getString("password"),
                    rs.getString("role"),
                    rs.getString("status")
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error menampilkan data: " + e.getMessage());
        }
    }

    public void searchData(DefaultTableModel model, String keyword) {
        try (Connection con = koneksi.getConnection()) {
            String query = "SELECT * FROM tb_user WHERE nama LIKE ?";
            try (PreparedStatement stmt = con.prepareStatement(query)) {
                stmt.setString(1, "%" + keyword + "%");
                ResultSet rs = stmt.executeQuery();

                model.setRowCount(0);
                while (rs.next()) {
                    Object[] row = new Object[]{
                        rs.getString("id_employee"),
                        rs.getString("nama"),
                        rs.getString("alamat"),
                        rs.getString("jenis_kelamin"),
                        rs.getDate("tgl_lahir"),
                        rs.getInt("salary_rate"),
                        rs.getString("no_telp"),
                        rs.getString("email"),
                        rs.getString("username"),
                        rs.getString("password"),
                        rs.getString("role"),
                        rs.getString("status")
                    };
                    model.addRow(row);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error pencarian data: " + e.getMessage());
        }
    }

    public boolean isUserIDExists(String idEmp) {
        try (Connection conn = koneksi.getConnection()) {
            String query = "SELECT COUNT(*) FROM tb_user WHERE id_employee = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, idEmp);
                ResultSet rs = stmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error cek ID: " + e.getMessage());
        }
        return false;
    }

    public void addUser(String id, String nama, String alamat, String jenis_kelamin, Date tgl_lahir,
            String salaryStr, String no_telp, String email, String username, String password, String role, String status) {

        if (id.isEmpty() || nama.isEmpty() || alamat.isEmpty() || tgl_lahir == null
                || salaryStr.isEmpty() || no_telp.isEmpty() || email.isEmpty()
                || username.isEmpty() || password.isEmpty()
                || role.equals("Pilih jabatan") || status.equals("Pilih status")) {

            JOptionPane.showMessageDialog(null, "Data tidak lengkap", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validasi salary
        int salary;
        try {
            salary = Integer.parseInt(salaryStr);
            if (salary <= 0) {
                JOptionPane.showMessageDialog(null, "Gaji harus lebih dari 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Gaji harus berupa angka", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Cek ID duplikat
        if (isUserIDExists(id)) {
            JOptionPane.showMessageDialog(null, "ID sudah digunakan", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Simpan ke database
        try (Connection conn = koneksi.getConnection()) {
            String query = "INSERT INTO tb_user (id_employee, nama, alamat, jenis_kelamin, tgl_lahir, salary_rate, no_telp, email, username, password, role, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, id);
                stmt.setString(2, nama);
                stmt.setString(3, alamat);
                stmt.setString(4, jenis_kelamin);
                stmt.setDate(5, new java.sql.Date(tgl_lahir.getTime()));
                stmt.setInt(6, salary);
                stmt.setString(7, no_telp);
                stmt.setString(8, email);
                stmt.setString(9, username);
                stmt.setString(10, password);
                stmt.setString(11, role);
                stmt.setString(12, status);

                if (stmt.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(null, "User berhasil ditambahkan", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Gagal menambah user", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error saat menambah user: " + e.getMessage());
        }
    }

    public void updateUser(String id, String nama, String alamat, String jenis_kelamin, Date tgl_lahir,
            String salaryStr, String no_telp, String email, String username, String password, String role, String status, DefaultTableModel model) {

        if (nama.isEmpty() || alamat.isEmpty() || tgl_lahir == null || salaryStr.isEmpty() || no_telp.isEmpty() || email.isEmpty() || username.isEmpty() || password.isEmpty() || role.equals("Pilih jabatan") || status.equals("Pilih status")) {
            JOptionPane.showMessageDialog(null, "Data tidak lengkap", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Validasi salary
        int salary;
        try {
            salary = Integer.parseInt(salaryStr);
            if (salary <= 0) {
                JOptionPane.showMessageDialog(null, "Gaji harus lebih dari 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Gaji harus berupa angka", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Proses update
        try (Connection conn = koneksi.getConnection()) {
            String query = "UPDATE tb_user SET nama = ?, alamat = ?, jenis_kelamin = ?, tgl_lahir = ?, salary_rate = ?, no_telp = ?, email = ?, username = ?, password = ?, role = ?, status = ? WHERE id_employee = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, nama);
                stmt.setString(2, alamat);
                stmt.setString(3, jenis_kelamin);
                stmt.setDate(4, new java.sql.Date(tgl_lahir.getTime()));
                stmt.setInt(5, salary);
                stmt.setString(6, no_telp);
                stmt.setString(7, email);
                stmt.setString(8, username);
                stmt.setString(9, password);
                stmt.setString(10, role);
                stmt.setString(11, status);
                stmt.setString(12, id); // WHERE id_employee = ?

                if (stmt.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(null, "User berhasil diperbarui", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null, "Gagal memperbarui user", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error saat update: " + e.getMessage());
        }
    }

}
