/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;
import connection.koneksi;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
/**
 *
 * @author ASUS
 */
public class PengelolaanMenuController {
    
    public void showMenu(DefaultTableModel model) {
        try (Connection conn = koneksi.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM tb_menu")) {
            
            model.setRowCount(0);
            while (rs.next()) {
                Object[] row = new Object[] {
                    rs.getString("id_menu"),
                    rs.getString("nama_menu"),
                    rs.getString("deskripsi_menu"),
                    rs.getInt("harga_menu"),
                    rs.getBoolean("status_menu") ? "Tersedia" : "Tidak tersedia"
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error menampilkan data: " + e.getMessage());
        }
    }
    
    public void searchMenu(DefaultTableModel model, String keyword) {
        try (Connection con = koneksi.getConnection()) {
            String query = "SELECT * FROM tb_menu WHERE nama_menu LIKE ?";
            try (PreparedStatement stmt = con.prepareStatement(query)) {
                stmt.setString(1, "%" + keyword + "%");
                ResultSet rs = stmt.executeQuery();
                
                model.setRowCount(0);
                while (rs.next()) {
                    Object[] row = new Object[] {
                        rs.getString("id_menu"),
                        rs.getString("nama_menu"),
                        rs.getString("deskripsi_menu"),
                        rs.getInt("harga_menu"),
                        rs.getBoolean("status_menu") ? "Tersedia" : "Tidak Tersedia"
                    };
                    model.addRow(row);
                }
            } 
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error pencarian data: " + e.getMessage());
        }
    }
    
    public boolean isKodeExists(String idMenu) {
        try (Connection conn = koneksi.getConnection()) {
            String query = "SELECT COUNT(*) FROM tb_menu WHERE id_menu = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, idMenu);
                ResultSet rs = stmt.executeQuery();
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error cek ID: " + e.getMessage());
        }
        return false;
    }
    
    public void addMenu(String id, String nama, String deskripsi, String hargaStr, String statusStr, DefaultTableModel model) {
        if (id.isEmpty() || nama.isEmpty() || deskripsi.isEmpty() || hargaStr.isEmpty() || statusStr.equals("Pilih status")) {
            JOptionPane.showMessageDialog(null, "Data tidak lengkap", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int harga;
        try {
            harga = Integer.parseInt(hargaStr);
            if (harga <= 0) {
                JOptionPane.showMessageDialog(null, "Harga harus lebih dari 0", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Harga harus berupa angka", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        if (isKodeExists(id)) {
            JOptionPane.showMessageDialog(null, "ID sudah digunakan", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        boolean status = statusStr.equals("Tersedia");
        
        try (Connection conn = koneksi.getConnection()) {
            String query = "INSERT INTO tb_menu (id_menu, nama_menu, deskripsi_menu, harga_menu, status_menu) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, id);
                stmt.setString(2, nama);
                stmt.setString(3, deskripsi);
                stmt.setInt(4, harga);
                stmt.setBoolean(5, status);

                if (stmt.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(null, "Data berhasil ditambahkan", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    showMenu(model);
                } else {
                    JOptionPane.showMessageDialog(null, "Gagal menambah data", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error saat menambah data: " + e.getMessage());
        }
    }
    
    public void updateMenu(String id, String namaMenu, String deskripsiMenu, String hargaStr, String statusStr, DefaultTableModel model) {
        int harga;
        if (id.isEmpty() || namaMenu.isEmpty() || deskripsiMenu.isEmpty() || hargaStr.isEmpty() || statusStr.equals("Pilih status")) {
            JOptionPane.showMessageDialog(null, "Lengkapi data terlebih dahulu", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        try {
            harga = Integer.parseInt(hargaStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Harga harus berupa angka", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        boolean status = statusStr.equals("Tersedia");

        try (Connection conn = koneksi.getConnection()) {
            String query = "UPDATE tb_menu SET nama_menu = ?, deskripsi_menu = ?, harga_menu = ?, status_menu = ? WHERE id_menu = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, namaMenu);
                stmt.setString(2, deskripsiMenu);
                stmt.setInt(3, harga);
                stmt.setBoolean(4, status);
                stmt.setString(5, id);

                if (stmt.executeUpdate() > 0) {
                    JOptionPane.showMessageDialog(null, "Data berhasil diperbarui", "Sukses", JOptionPane.INFORMATION_MESSAGE);
                    showMenu(model);
                } else {
                    JOptionPane.showMessageDialog(null, "Gagal memperbarui status", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error saat update: " + e.getMessage());
        }
    }
    
}