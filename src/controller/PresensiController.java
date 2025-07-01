/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import connection.koneksi;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalTime;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import javax.swing.JTable;
import javax.swing.table.TableModel;

/**
 *
 * @author ASUS
 */
public class PresensiController {

    public void generateAttendanceHariIni() {
        String sql = """
            INSERT INTO tb_attendance (id_employee, tanggal)
            SELECT sc.id_employee, CURRENT_DATE
            FROM tb_shift_schedulling sc
            JOIN tb_shifts s ON sc.id_shifts = s.id_shifts
            JOIN tb_hari h ON s.id_hari = h.id_hari
            WHERE h.nama_hari = ?
              AND NOT EXISTS (
                  SELECT 1 FROM tb_attendance a
                  WHERE a.id_employee = sc.id_employee AND DATE(a.tanggal) = CURRENT_DATE
              )
        """;

        try (Connection con = koneksi.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            // Mapping nama hari dari enum ke bahasa Indonesia
            String namaHari = getNamaHariIndonesia(LocalDate.now().getDayOfWeek());

            ps.setString(1, namaHari); // Set parameter nama hari (Senin, Selasa, dst.)
            int inserted = ps.executeUpdate();
            System.out.println("Data absen otomatis dimasukkan: " + inserted);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getNamaHariIndonesia(DayOfWeek dayOfWeek) {
        return switch (dayOfWeek) {
            case MONDAY ->
                "Senin";
            case TUESDAY ->
                "Selasa";
            case WEDNESDAY ->
                "Rabu";
            case THURSDAY ->
                "Kamis";
            case FRIDAY ->
                "Jumat";
            case SATURDAY ->
                "Sabtu";
            case SUNDAY ->
                "Minggu";
        };
    }

    public void loadKaryawanHariIni(DefaultTableModel model) {
        model.setRowCount(0); // Bersihkan tabel sebelum mengisi

        String sql = """
            SELECT u.id_employee, u.nama, s.shift, s.start_time, s.end_time, 
                   a.check_in, a.check_out, a.status
            FROM tb_attendance a
            JOIN tb_user u ON a.id_employee = u.id_employee
            JOIN tb_shift_schedulling sc ON sc.id_employee = u.id_employee
            JOIN tb_shifts s ON sc.id_shifts = s.id_shifts
            JOIN tb_hari h ON s.id_hari = h.id_hari
            WHERE DATE(a.tanggal) = CURRENT_DATE AND h.nama_hari = ?
        """;

        try (Connection con = koneksi.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            String namaHari = getNamaHariIndonesia(LocalDate.now().getDayOfWeek());
            ps.setString(1, namaHari);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Object[] row = {
                    rs.getString("id_employee"),
                    rs.getString("nama"),
                    rs.getString("shift"),
                    rs.getString("start_time"),
                    rs.getString("end_time"),
                    rs.getString("check_in"),
                    rs.getString("check_out"),
                    rs.getString("status")
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void handleCheckIn(String idEmployee, JButton btnCheckIn, JButton btnCheckOut) {
        if (idEmployee == null || idEmployee.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Pilih data karyawan terlebih dahulu.");
            return;
        }

        String namaHari = getNamaHariIndonesia(LocalDate.now().getDayOfWeek());

        String sqlCheck = """
            SELECT a.check_in, s.start_time, s.end_time
            FROM tb_attendance a
            JOIN tb_shift_schedulling sc ON a.id_employee = sc.id_employee
            JOIN tb_shifts s ON sc.id_shifts = s.id_shifts
            JOIN tb_hari h ON s.id_hari = h.id_hari
            WHERE a.id_employee = ? 
              AND DATE(a.tanggal) = CURRENT_DATE
              AND h.nama_hari = ?
        """;

        try (Connection con = koneksi.getConnection(); PreparedStatement ps = con.prepareStatement(sqlCheck)) {
            ps.setString(1, idEmployee);
            ps.setString(2, namaHari);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Time checkInTime = rs.getTime("check_in");

                if (checkInTime != null) {
                    JOptionPane.showMessageDialog(null, "Karyawan sudah melakukan check-in hari ini.");
                    btnCheckIn.setEnabled(false);
                    btnCheckOut.setEnabled(true);
                    return;
                }

                LocalTime now = LocalTime.now();
                LocalTime startTime = rs.getTime("start_time").toLocalTime();
                LocalTime endTime = rs.getTime("end_time").toLocalTime();

                if (now.isAfter(endTime)) {
                    // Terlambat parah -> dianggap "Tanpa Keterangan"
                    String sqlLate = """
                        UPDATE tb_attendance 
                        SET check_in = CURRENT_TIME, check_out = CURRENT_TIME, work_hours = 0, status = 'Tanpa Keterangan'
                        WHERE id_employee = ? AND DATE(tanggal) = CURRENT_DATE
                    """;

                    try (PreparedStatement psLate = con.prepareStatement(sqlLate)) {
                        psLate.setString(1, idEmployee);
                        int updated = psLate.executeUpdate();
                        if (updated > 0) {
                            JOptionPane.showMessageDialog(null, "Check-in terlambat melebihi jam kerja. Status: Tanpa Keterangan.");
                            btnCheckIn.setEnabled(false);
                            btnCheckOut.setEnabled(false);
                        }
                    }
                } else {
                    // Hadir atau Terlambat
                    String status = now.isAfter(startTime.plusMinutes(15)) ? "Terlambat" : "Hadir";

                    String sqlUpdate = """
                        UPDATE tb_attendance 
                        SET check_in = CURRENT_TIME, status = ?
                        WHERE id_employee = ? AND DATE(tanggal) = CURRENT_DATE
                    """;

                    try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
                        psUpdate.setString(1, status);
                        psUpdate.setString(2, idEmployee);
                        int updated = psUpdate.executeUpdate();

                        if (updated > 0) {
                            JOptionPane.showMessageDialog(null, "Check-in berhasil. Status: " + status);
                            btnCheckIn.setEnabled(false);
                            btnCheckOut.setEnabled(true);
                        } else {
                            JOptionPane.showMessageDialog(null, "Data tidak ditemukan untuk check-in.");
                        }
                    }
                }
            } else {
                JOptionPane.showMessageDialog(null, "Data absen hari ini tidak ditemukan.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal melakukan check-in.");
        }
    }

    public void handleCheckOut(String idEmployee, JButton btnCheckIn, JButton btnCheckOut) {
        if (idEmployee == null || idEmployee.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Pilih data karyawan terlebih dahulu.");
            return;
        }

        String namaHari = getNamaHariIndonesia(LocalDate.now().getDayOfWeek());

        String sqlCheck = """
        SELECT a.check_in, a.check_out, s.end_time, s.start_time
        FROM tb_attendance a
        JOIN tb_shift_schedulling sc ON a.id_employee = sc.id_employee
        JOIN tb_shifts s ON sc.id_shifts = s.id_shifts
        JOIN tb_hari h ON s.id_hari = h.id_hari
        WHERE a.id_employee = ? AND DATE(a.tanggal) = CURRENT_DATE
          AND h.nama_hari = ?
    """;

        try (Connection con = koneksi.getConnection(); PreparedStatement ps = con.prepareStatement(sqlCheck)) {
            ps.setString(1, idEmployee);
            ps.setString(2, namaHari);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Time checkInSqlTime = rs.getTime("check_in");
                Time checkOutSqlTime = rs.getTime("check_out");
                Time endSqlTime = rs.getTime("end_time");
                Time startSqlTime = rs.getTime("start_time");

                if (checkInSqlTime == null) {
                    JOptionPane.showMessageDialog(null, "Check-in belum dilakukan!");
                    return;
                }

                if (checkOutSqlTime != null) {
                    JOptionPane.showMessageDialog(null, "Karyawan sudah melakukan check-out hari ini.");
                    btnCheckIn.setEnabled(false);
                    btnCheckOut.setEnabled(false);
                    return;
                }

                LocalTime now = LocalTime.now();
                LocalTime endTime = endSqlTime.toLocalTime();
                LocalTime startTime = startSqlTime.toLocalTime();
                LocalTime checkInTime = checkInSqlTime.toLocalTime();

                // Aturan 1: Tidak boleh check-out > 1 jam setelah jam selesai kerja
                if (now.isAfter(endTime.plusHours(1))) {
                    JOptionPane.showMessageDialog(null, "Check-out tidak dapat dilakukan. Melebihi batas maksimal 1 jam setelah jam kerja berakhir.");
                    return;
                }

                // Hitung durasi kerja
                Duration durasi = Duration.between(checkInTime, now);
                long menit = durasi.toMinutes();
                long durasiShift = Duration.between(startTime, endTime).toMinutes();

                int finalWorkMinutes = (int) Math.min(menit, durasiShift);
                double workHours = finalWorkMinutes / 60.0;

                String sqlUpdate = """
                UPDATE tb_attendance 
                SET check_out = CURRENT_TIME, work_hours = ?
                WHERE id_employee = ? AND DATE(tanggal) = CURRENT_DATE
            """;

                try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
                    psUpdate.setDouble(1, workHours);
                    psUpdate.setString(2, idEmployee);

                    int updated = psUpdate.executeUpdate();
                    if (updated > 0) {
                        JOptionPane.showMessageDialog(null, "Check-out berhasil. Durasi kerja: " + workHours + " jam.");
                        btnCheckIn.setEnabled(false);
                        btnCheckOut.setEnabled(false);
                    } else {
                        JOptionPane.showMessageDialog(null, "Data tidak ditemukan untuk check-out.");
                    }
                }

            } else {
                JOptionPane.showMessageDialog(null, "Data absen hari ini tidak ditemukan.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal melakukan check-out.");
        }
    }

    public void updateTanpaKeterangan() {
        String sql = """
        UPDATE tb_attendance
        SET status = 'Tanpa Keterangan'
        WHERE tanggal BETWEEN CURDATE() - INTERVAL 7 DAY AND CURDATE() - INTERVAL 1 DAY
          AND check_in IS NULL
          AND check_out IS NULL
          AND (status IS NULL OR status = '' OR status = ' ')
    """;

        try (Connection con = koneksi.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            int updated = ps.executeUpdate();
            System.out.println("Status 'Tanpa Keterangan' ditandai untuk " + updated + " karyawan.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    public void loadKaryawanByTanggal(Date tanggal, JTable tbKaryawan) {
        TableModel modelGeneric = tbKaryawan.getModel();
        if (!(modelGeneric instanceof DefaultTableModel model)) {
            JOptionPane.showMessageDialog(null, "Model tabel bukan DefaultTableModel.");
            return;
        }
        model.setRowCount(0); // clear table

        // Format tanggal ke yyyy-MM-dd agar cocok untuk SQL
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String tanggalStr = sdf.format(tanggal);

        // Dapatkan nama hari dari tanggal
        LocalDate localDate = tanggal.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        String namaHari = getNamaHariIndonesia(localDate.getDayOfWeek());

        String sql = """
        SELECT u.id_employee, u.nama, s.shift, s.start_time, s.end_time, a.check_in, a.check_out, a.status
        FROM tb_attendance a
        JOIN tb_user u ON a.id_employee = u.id_employee
        JOIN tb_shift_schedulling sc ON sc.id_employee = u.id_employee
        JOIN tb_shifts s ON sc.id_shifts = s.id_shifts
        JOIN tb_hari h ON s.id_hari = h.id_hari
        WHERE DATE(a.tanggal) = ? AND h.nama_hari = ?
    """;

        try (Connection con = koneksi.getConnection(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tanggalStr);
            ps.setString(2, namaHari);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Object[] row = {
                    rs.getString("id_employee"),
                    rs.getString("nama"),
                    rs.getString("shift"),
                    rs.getString("start_time"),
                    rs.getString("end_time"),
                    rs.getString("check_in"),
                    rs.getString("check_out"),
                    rs.getString("status")
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void tambahKeterangan(String idEmployee, Date selectedDate, String keterangan) {
        if (idEmployee == null || idEmployee.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Pilih data karyawan terlebih dahulu.");
            return;
        }

        if (selectedDate == null) {
            JOptionPane.showMessageDialog(null, "Pilih tanggal terlebih dahulu.");
            return;
        }

        LocalDate tanggalDipilih = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate hariIni = LocalDate.now();
        long selisihHari = ChronoUnit.DAYS.between(tanggalDipilih, hariIni);

        if (selisihHari > 2) {
            JOptionPane.showMessageDialog(null, "Keterangan tidak dapat diubah karena melebihi batas toleransi 2 hari.");
            return;
        }

        String sqlCheck = "SELECT status FROM tb_attendance WHERE id_employee = ? AND DATE(tanggal) = ?";
        String sqlUpdate = "UPDATE tb_attendance SET status = ? WHERE id_employee = ? AND DATE(tanggal) = ?";

        try (Connection con = koneksi.getConnection(); PreparedStatement psCheck = con.prepareStatement(sqlCheck)) {
            psCheck.setString(1, idEmployee);
            psCheck.setDate(2, java.sql.Date.valueOf(tanggalDipilih));
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                String statusLama = rs.getString("status");
                if (statusLama != null && !statusLama.equalsIgnoreCase("Tanpa Keterangan")) {
                    JOptionPane.showMessageDialog(null, "Status sudah diisi sebelumnya: " + statusLama);
                    return;
                }

                try (PreparedStatement psUpdate = con.prepareStatement(sqlUpdate)) {
                    psUpdate.setString(1, keterangan);
                    psUpdate.setString(2, idEmployee);
                    psUpdate.setDate(3, java.sql.Date.valueOf(tanggalDipilih));

                    int updated = psUpdate.executeUpdate();
                    if (updated > 0) {
                        JOptionPane.showMessageDialog(null, "Keterangan berhasil ditambahkan.\nPilih ulang tanggal untuk refresh data");
                    } else {
                        JOptionPane.showMessageDialog(null, "Gagal menambahkan keterangan.");
                    }
                }

            } else {
                JOptionPane.showMessageDialog(null, "Data presensi tidak ditemukan untuk tanggal tersebut.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Terjadi kesalahan saat menambahkan keterangan.");
        }
    }

}
