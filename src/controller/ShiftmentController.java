/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import connection.koneksi;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import connection.koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author ASUS
 */
public class ShiftmentController {

    public void loadFilterJadwalShift(String namaHari, JTable table, String jenisShift) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        String query = """
                       SELECT u.id_employee, u.nama, s.start_time, s.end_time
                       FROM tb_shift_schedulling ss
                       JOIN tb_user u ON ss.id_employee = u.id_employee
                       JOIN tb_shifts s ON ss.id_shifts = s.id_shifts
                       JOIN tb_hari h ON s.id_hari = h.id_hari
                       WHERE h.nama_hari = ? AND s.shift = ?
                       """;

        try (Connection conn = koneksi.getConnection(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, namaHari);
            ps.setString(2, jenisShift);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("id_employee"),
                    rs.getString("nama"),
                    rs.getTime("start_time"),
                    rs.getTime("end_time")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal mengambil jadwal: " + e.getMessage());
        }
    }

    public void loadDataKaryawan(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.getConnection(); PreparedStatement ps = conn.prepareStatement("SELECT id_employee, nama FROM tb_user WHERE status = 'Aktif'")) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("id_employee"),
                    rs.getString("nama")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal mengambil data: " + e.getMessage());
        }
    }

    public void addJadwalShift(String idEmployee, String namaHari, String shift) {
        try {
            //Step 1: Get Id Hari
            String queryHari = "SELECT id_hari FROM tb_hari WHERE nama_hari = ?";
            Connection conn = koneksi.getConnection();
            PreparedStatement psHari = conn.prepareStatement(queryHari);

            psHari.setString(1, namaHari);

            ResultSet rsHari = psHari.executeQuery();

            if (!rsHari.next()) {
                JOptionPane.showMessageDialog(null, "Hari tidak ditemukan!");
                return;
            }

            String idHari = rsHari.getString("id_hari");

            //Step 2: Get Id Shift
            String queryShift = "SELECT  id_shifts FROM tb_shifts WHERE id_hari = ? AND shift = ?";
            PreparedStatement psShift = conn.prepareStatement(queryShift);

            psShift.setString(1, idHari);
            psShift.setString(2, shift);

            ResultSet rsShift = psShift.executeQuery();

            if (!rsShift.next()) {
                JOptionPane.showMessageDialog(null, "Shift tidak ditemukan!");
                return;
            }

            String idShift = rsShift.getString("id_shifts");

            // Step 2.5: Cek apakah user sudah punya shift di hari yang sama
            String queryCekHari = """
    SELECT COUNT(*) AS total
    FROM tb_shift_schedulling sc
    JOIN tb_shifts s ON sc.id_shifts = s.id_shifts
    WHERE sc.id_employee = ? AND s.id_hari = ?
""";
            PreparedStatement psCekHari = conn.prepareStatement(queryCekHari);
            psCekHari.setString(1, idEmployee);
            psCekHari.setString(2, idHari);

            ResultSet rsCekHari = psCekHari.executeQuery();

            if (rsCekHari.next() && rsCekHari.getInt("total") > 0) {
                JOptionPane.showMessageDialog(null, "Karyawan sudah memiliki shift di hari tersebut!");
                return;
            }

            //Step 3: Check Duplication
            String queryCek = "SELECT COUNT(*) AS total FROM tb_shift_schedulling WHERE id_employee = ? AND id_shifts = ?";
            PreparedStatement psCek = conn.prepareStatement(queryCek);

            psCek.setString(1, idEmployee);
            psCek.setString(2, idShift);

            ResultSet rsCek = psCek.executeQuery();

            if (rsCek.next() && rsCek.getInt("total") > 0) {
                JOptionPane.showMessageDialog(null, "Jadwal ini sudah terdaftar.");
                return;
            }

            //Step 4: Insert Data
            String insertSQL = "INSERT INTO tb_shift_schedulling (id_employee, id_shifts) VALUES (?, ?)";
            PreparedStatement psInsert = conn.prepareStatement(insertSQL);

            psInsert.setString(1, idEmployee);
            psInsert.setString(2, idShift);

            psInsert.executeUpdate();

            JOptionPane.showMessageDialog(null, "Jadwal shift berhasil ditambahkan!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal menambahkan jadwal: " + e.getMessage());
        }
    }

    public void updateShift(String idEmployee, String namaHari, String shift) {
        try {
            // Step 1
            String sqlHari = "SELECT id_hari FROM tb_hari WHERE nama_hari = ?";
            Connection conn = koneksi.getConnection();
            PreparedStatement psHari = conn.prepareStatement(sqlHari);

            psHari.setString(1, namaHari);

            ResultSet rsHari = psHari.executeQuery();

            if (!rsHari.next()) {
                JOptionPane.showMessageDialog(null, "Hari tidak ditemukan!");
                return;
            }
            String idHari = rsHari.getString("id_hari");

            // Step 2
            String sqlShift = "SELECT id_shifts FROM tb_shifts WHERE id_hari = ? AND shift = ?";
            PreparedStatement psShift = conn.prepareStatement(sqlShift);

            psShift.setString(1, idHari);
            psShift.setString(2, shift);

            ResultSet rsShift = psShift.executeQuery();

            if (!rsShift.next()) {
                JOptionPane.showMessageDialog(null, "Shift tidak ditemukan!");
                return;
            }
            String idShift = rsShift.getString("id_shifts");

            // Step 3
            String updateSQL = "UPDATE tb_shift_schedulling SET id_shifts = ? WHERE id_employee = ? AND id_shifts != ?";
            PreparedStatement psUpdate = conn.prepareStatement(updateSQL);

            psUpdate.setString(1, idShift);
            psUpdate.setString(2, idEmployee);
            psUpdate.setString(3, idShift);

            int result = psUpdate.executeUpdate();
            if (result > 0) {
                JOptionPane.showMessageDialog(null, "Shift berhasil diperbarui!");
            } else {
                JOptionPane.showMessageDialog(null, "Tidak ada yang diperbarui.");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal update shift: " + e.getMessage());
        }
    }

    public void searchUser(JTable table, String keyword) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tb_user WHERE nama LIKE ?")) {
            stmt.setString(1, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("id_employee"),
                    rs.getString("nama")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal mencari user: " + e.getMessage());
        }
    }

    public void deleteJadwalShift(String idEmployee, String namaHari, String shift) {
        try {
            Connection conn = koneksi.getConnection();

            // Step 1: Ambil id_hari
            String queryHari = "SELECT id_hari FROM tb_hari WHERE nama_hari = ?";
            PreparedStatement psHari = conn.prepareStatement(queryHari);
            psHari.setString(1, namaHari);
            ResultSet rsHari = psHari.executeQuery();

            if (!rsHari.next()) {
                JOptionPane.showMessageDialog(null, "Hari tidak ditemukan!");
                return;
            }

            String idHari = rsHari.getString("id_hari");

            // Step 2: Ambil id_shifts berdasarkan hari dan shift
            String queryShift = "SELECT id_shifts FROM tb_shifts WHERE id_hari = ? AND shift = ?";
            PreparedStatement psShift = conn.prepareStatement(queryShift);
            psShift.setString(1, idHari);
            psShift.setString(2, shift);
            ResultSet rsShift = psShift.executeQuery();

            if (!rsShift.next()) {
                JOptionPane.showMessageDialog(null, "Shift tidak ditemukan!");
                return;
            }

            String idShift = rsShift.getString("id_shifts");

            // Step 3: Hapus dari tb_shift_schedulling
            String deleteSQL = "DELETE FROM tb_shift_schedulling WHERE id_employee = ? AND id_shifts = ?";
            PreparedStatement psDelete = conn.prepareStatement(deleteSQL);
            psDelete.setString(1, idEmployee);
            psDelete.setString(2, idShift);

            int deleted = psDelete.executeUpdate();

            if (deleted > 0) {
                JOptionPane.showMessageDialog(null, "Jadwal shift berhasil dihapus!");
            } else {
                JOptionPane.showMessageDialog(null, "Jadwal tidak ditemukan atau sudah dihapus.");
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal menghapus jadwal: " + e.getMessage());
        }
    }

}
