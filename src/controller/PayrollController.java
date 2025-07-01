/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import javax.swing.JTable;
import java.sql.*;
import connection.koneksi;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import model.PayrollModel;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Properties;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;

/**
 *
 * @author ASUS
 */
public class PayrollController {

    private JTable tbPayroll;

    public PayrollController(JTable tbPayroll) {
        this.tbPayroll = tbPayroll;
    }

    public void generateAndShowPayroll() {
        try {
            Connection conn = koneksi.getConnection();
            DefaultTableModel model = (DefaultTableModel) tbPayroll.getModel();
            model.setRowCount(0);

            LocalDate today = LocalDate.now();
            LocalDate monday = today.with(DayOfWeek.MONDAY);
            LocalDate sunday = today.with(DayOfWeek.SUNDAY);

            String sqlUser = "SELECT id_employee, nama, salary_rate FROM tb_user WHERE status = 'Aktif'";
            PreparedStatement psUser = conn.prepareStatement(sqlUser);
            ResultSet rsUser = psUser.executeQuery();

            while (rsUser.next()) {
                String idUser = rsUser.getString("id_employee");
                String nama = rsUser.getString("nama");
                int salaryRate = rsUser.getInt("salary_rate");

                String sqlHours = "SELECT SUM(work_hours) FROM tb_attendance WHERE id_employee = ? AND tanggal BETWEEN ? AND ?";
                PreparedStatement psHours = conn.prepareStatement(sqlHours);
                psHours.setString(1, idUser);
                psHours.setDate(2, java.sql.Date.valueOf(monday));
                psHours.setDate(3, java.sql.Date.valueOf(sunday));
                ResultSet rsHours = psHours.executeQuery();

                double workHours = 0;
                if (rsHours.next()) {
                    workHours = rsHours.getDouble(1);
                }

                int baseSalary = (int) (workHours * salaryRate);
                int bonus = 0;
                int totalSalary = baseSalary + bonus;

                // Fix ONLY_FULL_GROUP_BY: pisahkan query COUNT dan status
                String countSql = "SELECT COUNT(*) FROM tb_payroll WHERE id_employee = ? AND period_start = ? AND period_end = ?";
                PreparedStatement psCount = conn.prepareStatement(countSql);
                psCount.setString(1, idUser);
                psCount.setDate(2, java.sql.Date.valueOf(monday));
                psCount.setDate(3, java.sql.Date.valueOf(sunday));
                ResultSet rsCount = psCount.executeQuery();

                int count = 0;
                String currentStatus = "";
                if (rsCount.next()) {
                    count = rsCount.getInt(1);
                }

                if (count > 0) {
                    String statusSql = "SELECT status FROM tb_payroll WHERE id_employee = ? AND period_start = ? AND period_end = ? LIMIT 1";
                    PreparedStatement psStatus = conn.prepareStatement(statusSql);
                    psStatus.setString(1, idUser);
                    psStatus.setDate(2, java.sql.Date.valueOf(monday));
                    psStatus.setDate(3, java.sql.Date.valueOf(sunday));
                    ResultSet rsStatus = psStatus.executeQuery();
                    if (rsStatus.next()) {
                        currentStatus = rsStatus.getString("status");
                    }
                }

                if (count == 0) {
                    String insert = "INSERT INTO tb_payroll (id_employee, period_start, period_end, total_hours, base_salary, bonus, total_salary, status, generated_at) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, 'Draft', NOW())";
                    PreparedStatement psInsert = conn.prepareStatement(insert);
                    psInsert.setString(1, idUser);
                    psInsert.setDate(2, java.sql.Date.valueOf(monday));
                    psInsert.setDate(3, java.sql.Date.valueOf(sunday));
                    psInsert.setDouble(4, workHours);
                    psInsert.setInt(5, baseSalary);
                    psInsert.setInt(6, bonus);
                    psInsert.setInt(7, totalSalary);
                    psInsert.executeUpdate();
                } else if ("Draft".equalsIgnoreCase(currentStatus)) {
                    String update = "UPDATE tb_payroll SET total_hours = ?, base_salary = ?, total_salary = ? "
                            + "WHERE id_employee = ? AND period_start = ? AND period_end = ?";
                    PreparedStatement psUpdate = conn.prepareStatement(update);
                    psUpdate.setDouble(1, workHours);
                    psUpdate.setInt(2, baseSalary);
                    psUpdate.setInt(3, totalSalary);
                    psUpdate.setString(4, idUser);
                    psUpdate.setDate(5, java.sql.Date.valueOf(monday));
                    psUpdate.setDate(6, java.sql.Date.valueOf(sunday));
                    psUpdate.executeUpdate();
                }
            }

            int currentMonth = today.getMonthValue();
            int currentYear = today.getYear();

            String sqlPayroll = "SELECT p.id_employee, u.nama, p.period_start, p.period_end, p.base_salary, p.bonus, p.total_salary, p.payment_method, p.status, p.paid_at, p.notes "
                    + "FROM tb_payroll p JOIN tb_user u ON p.id_employee = u.id_employee "
                    + "WHERE MONTH(p.period_start) = ? AND YEAR(p.period_start) = ?";

            PreparedStatement psShow = conn.prepareStatement(sqlPayroll);
            psShow.setInt(1, currentMonth);
            psShow.setInt(2, currentYear);
            ResultSet rsPayroll = psShow.executeQuery();

            model.setRowCount(0);

            while (rsPayroll.next()) {
                model.addRow(new Object[]{
                    rsPayroll.getString("id_employee"),
                    rsPayroll.getString("nama"),
                    rsPayroll.getDate("period_start"),
                    rsPayroll.getDate("period_end"),
                    rsPayroll.getInt("base_salary"),
                    rsPayroll.getInt("bonus"),
                    rsPayroll.getInt("total_salary"),
                    rsPayroll.getString("payment_method"),
                    rsPayroll.getString("status"),
                    rsPayroll.getTimestamp("paid_at"),
                    rsPayroll.getString("notes")
                });
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void filterPayrollByMonthYear(int month, int year, JTable tbPayroll) {
        try {
            Connection conn = koneksi.getConnection();
            String sql = "SELECT p.id_employee, u.nama, p.period_start, p.period_end, "
                    + "p.base_salary, p.bonus, (p.base_salary + p.bonus) AS total_salary, "
                    + "p.payment_method, p.status, p.paid_at, p.notes "
                    + "FROM tb_payroll p "
                    + "JOIN tb_user u ON p.id_employee = u.id_employee "
                    + "WHERE MONTH(p.period_start) = ? AND YEAR(p.period_start) = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, month);
            ps.setInt(2, year);
            ResultSet rs = ps.executeQuery();

            DefaultTableModel model = (DefaultTableModel) tbPayroll.getModel();
            model.setRowCount(0); // Bersihkan tabel

            while (rs.next()) {
                Object[] row = new Object[]{
                    rs.getString("id_employee"),
                    rs.getString("nama"),
                    rs.getDate("period_start"),
                    rs.getDate("period_end"),
                    rs.getInt("base_salary"),
                    rs.getInt("bonus"),
                    rs.getInt("total_salary"),
                    rs.getString("payment_method"),
                    rs.getString("status"),
                    rs.getString("paid_at"),
                    rs.getString("notes")
                };
                model.addRow(row);
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error filter payroll: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void updatePayroll(PayrollModel model, JTable tbPayroll, int selectedMonth, int selectedYear) {
        try {
            Connection conn = koneksi.getConnection();

            // Cek apakah payroll dengan period_start dan id_employee sudah berstatus 'Paid'
            String checkSql = "SELECT status FROM tb_payroll WHERE id_employee = ? AND period_start = ?";
            PreparedStatement psCheck = conn.prepareStatement(checkSql);
            psCheck.setString(1, model.getIdEmployee());
            psCheck.setDate(2, new java.sql.Date(model.getPeriodStart().getTime()));
            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                String currentStatus = rs.getString("status");
                if ("Paid".equalsIgnoreCase(currentStatus)) {
                    JOptionPane.showMessageDialog(null, "Data payroll sudah dibayar dan tidak bisa diubah.");
                    return; // Keluar dari method, tidak lanjut update
                }
                
            } else {
                JOptionPane.showMessageDialog(null, "Data payroll tidak ditemukan.");
                return;
            }

            // Lanjutkan update jika belum Paid
            String sql = "UPDATE tb_payroll SET bonus = ?, payment_method = ?, status = ?, notes = ?, "
                    + "total_salary = base_salary + ? "
                    + "WHERE id_employee = ? AND period_start = ?";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, model.getBonus());
            ps.setString(2, model.getPaymentMethod());
            ps.setString(3, model.getStatus());
            ps.setString(4, model.getNotes());
            ps.setInt(5, model.getBonus());
            ps.setString(6, model.getIdEmployee());
            ps.setDate(7, new java.sql.Date(model.getPeriodStart().getTime()));

            int rows = ps.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(null, "Data payroll berhasil diupdate.");
                filterPayrollByMonthYear(selectedMonth, selectedYear, tbPayroll);
            } else {
                JOptionPane.showMessageDialog(null, "Update gagal. Data tidak ditemukan.");
            }

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error update payroll: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public boolean bayarGaji(PayrollModel model) {
        try {
            Connection conn = koneksi.getConnection();
            String sql = "SELECT * FROM tb_payroll "
                    + "WHERE id_employee = ? AND MONTH(period_start) = ? AND YEAR(period_start) = ? "
                    + "AND status != 'Paid'";
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, model.getIdEmployee());
            ps.setInt(2, model.getBulan());
            ps.setInt(3, model.getTahun());
            ResultSet rs = ps.executeQuery();

            int totalSalary = 0;
            int mingguCount = 0;

            while (rs.next()) {
                totalSalary += rs.getInt("total_salary");
                mingguCount++;
            }

            if (mingguCount == 0) {
                JOptionPane.showMessageDialog(null, "Tidak ada data payroll yang bisa dibayar untuk bulan ini.");
                return false;
            }

            // Insert ke tb_paid_payroll
            String insert = "INSERT INTO tb_paid_payroll (id_employee, week, bulan, tahun, total_salary, status, paid_at, notes, created_at) "
                    + "VALUES (?, ?, ?, ?, ?, 'Paid', NOW(), ?, NOW())";
            PreparedStatement psInsert = conn.prepareStatement(insert);
            psInsert.setString(1, model.getIdEmployee());
            psInsert.setInt(2, mingguCount);
            psInsert.setInt(3, model.getBulan());
            psInsert.setInt(4, model.getTahun());
            psInsert.setInt(5, totalSalary);
            psInsert.setString(6, model.getNotes());
            psInsert.executeUpdate();

            // Update semua payroll minggu tersebut menjadi Paid
            String update = "UPDATE tb_payroll SET status = 'Paid', paid_at = NOW() "
                    + "WHERE id_employee = ? AND MONTH(period_start) = ? AND YEAR(period_start) = ?";
            PreparedStatement psUpdate = conn.prepareStatement(update);
            psUpdate.setString(1, model.getIdEmployee());
            psUpdate.setInt(2, model.getBulan());
            psUpdate.setInt(3, model.getTahun());
            psUpdate.executeUpdate();

            JOptionPane.showMessageDialog(null, "Gaji berhasil dibayarkan untuk " + mingguCount + " minggu.");
            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error bayar gaji: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public File generateSlipGaji(PayrollModel model) {
        File file = null;

        try {
            Connection conn = koneksi.getConnection();

            // Ambil nama karyawan
            String sqlUser = "SELECT nama FROM tb_user WHERE id_employee = ?";
            PreparedStatement psUser = conn.prepareStatement(sqlUser);
            psUser.setString(1, model.getIdEmployee());
            ResultSet rsUser = psUser.executeQuery();

            String nama = "";
            if (rsUser.next()) {
                nama = rsUser.getString("nama");
            }

            // Hitung tanggal awal dan akhir bulan
            LocalDate startOfMonth = LocalDate.of(model.getTahun(), model.getBulan(), 1);
            LocalDate endOfMonth = startOfMonth.withDayOfMonth(startOfMonth.lengthOfMonth());

            // Ambil data payroll untuk bulan tersebut
            String sqlPayroll = "SELECT period_start, period_end, total_hours, base_salary, bonus, total_salary "
                    + "FROM tb_payroll WHERE id_employee = ? AND period_start BETWEEN ? AND ?";
            PreparedStatement ps = conn.prepareStatement(sqlPayroll);
            ps.setString(1, model.getIdEmployee());
            ps.setDate(2, java.sql.Date.valueOf(startOfMonth));
            ps.setDate(3, java.sql.Date.valueOf(endOfMonth));
            ResultSet rs = ps.executeQuery();

            if (!rs.isBeforeFirst()) {
                JOptionPane.showMessageDialog(null, "Tidak ada data payroll untuk slip.");
                return null;
            }

            // Siapkan direktori slip/
            File slipDir = new File("slip");
            if (!slipDir.exists()) {
                slipDir.mkdirs();
            }

            String fileName = "slip/SlipGaji_" + model.getIdEmployee() + "_" + model.getBulan() + "_" + model.getTahun() + ".pdf";
            file = new File(fileName);

            // Buat PDF
            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Header
            Font headerFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            Paragraph header = new Paragraph("Slip Gaji Bulanan\nHashirama Ramen", headerFont);
            header.setAlignment(Element.ALIGN_CENTER);
            document.add(header);
            document.add(new Paragraph(" "));

            // Info Karyawan
            document.add(new Paragraph("Nama: " + nama));
            document.add(new Paragraph("ID: " + model.getIdEmployee()));
            document.add(new Paragraph("Periode: " + model.getBulan() + " - " + model.getTahun()));
            document.add(new Paragraph(" "));

            // Tabel Gaji
            PdfPTable table = new PdfPTable(6);
            table.setWidthPercentage(100);
            table.addCell("Start");
            table.addCell("End");
            table.addCell("Jam");
            table.addCell("Base Salary");
            table.addCell("Bonus");
            table.addCell("Total");

            double totalGaji = 0;

            while (rs.next()) {
                table.addCell(rs.getDate("period_start").toString());
                table.addCell(rs.getDate("period_end").toString());
                table.addCell(String.valueOf(rs.getDouble("total_hours")));
                table.addCell("Rp " + rs.getInt("base_salary"));
                table.addCell("Rp " + rs.getInt("bonus"));
                table.addCell("Rp " + rs.getInt("total_salary"));

                totalGaji += rs.getInt("total_salary");
            }

            document.add(table);
            document.add(new Paragraph(" "));

            // Format total gaji agar tidak E notation
            NumberFormat formatter = NumberFormat.getInstance(new Locale("id", "ID"));
            formatter.setMaximumFractionDigits(0);
            String formattedTotal = formatter.format(totalGaji);

            Font bold = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
            Paragraph total = new Paragraph("Total Gaji Diterima: Rp " + formattedTotal, bold);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.close();
            System.out.println("Slip berhasil dibuat: " + file.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "Gagal generate slip: " + e.getMessage());
        }

        return file;
    }

    public void sendSlipToEmail(PayrollModel model) {
        final String fromEmail = "andikayusuf2685@gmail.com";
        final String password = "bcic pyqy rvwz uuzx";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        final File slipFile = model.getSlipFile();
        final String recipientEmail = model.getRecipientEmail();
        final String idEmployee = model.getIdEmployee();
        final int month = model.getBulan();
        final int year = model.getTahun();

        final JDialog loadingDialog = new JDialog();
        JLabel loadingLabel = new JLabel("Mengirim slip gaji, mohon tunggu...");
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingDialog.add(loadingLabel);
        loadingDialog.setSize(300, 100);
        loadingDialog.setLocationRelativeTo(null);
        loadingDialog.setModal(false);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            protected Void doInBackground() {
                try {
                    Session session = Session.getInstance(props, new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(fromEmail, password);
                        }
                    });

                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(fromEmail));
                    message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(recipientEmail));
                    message.setSubject("Slip Gaji_" + idEmployee + "_" + month + "_" + year);

                    // Isi pesan email
                    MimeBodyPart messageBodyPart = new MimeBodyPart();
                    messageBodyPart.setText(
                            "Halo Tim Hashirama Ramen,\n\n"
                            + "Terlampir slip gaji Anda untuk bulan ini.\n"
                            + "Silakan periksa lampiran.\n\n"
                            + "Best regards,\n"
                            + "Owner Hashirama Ramen"
                    );

                    // Lampiran
                    MimeBodyPart attachmentPart = new MimeBodyPart();
                    attachmentPart.attachFile(slipFile);

                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(messageBodyPart);
                    multipart.addBodyPart(attachmentPart);

                    message.setContent(multipart);
                    Transport.send(message);

                } catch (MessagingException | IOException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            protected void done() {
                loadingDialog.dispose();
                try {
                    get();
                    JOptionPane.showMessageDialog(null, "Slip gaji berhasil dikirim ke " + recipientEmail);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Gagal mengirim email.\nPeriksa koneksi & email tujuan!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        loadingDialog.setVisible(true);
        worker.execute();
    }
}
