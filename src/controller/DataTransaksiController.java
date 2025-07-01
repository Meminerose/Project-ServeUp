/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import java.time.LocalDate;
import javax.swing.JOptionPane;
import connection.koneksi;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.awt.Component;
import java.io.File;
import java.io.FileOutputStream;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.sql.Timestamp;
import java.text.NumberFormat;
import java.util.Locale;

/**
 *
 * @author ASUS
 */
public class DataTransaksiController {

    public void showTransaksi(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM tb_transaksi ORDER BY tanggal_transaksi DESC")) {

            while (rs.next()) {
                Object[] row = {
                    rs.getString("id_transaksi"),
                    rs.getDate("tanggal_transaksi"),
                    rs.getInt("total_bayar"),
                    rs.getString("email_customer") == null || rs.getString("email_customer").trim().isEmpty() ? "-" : rs.getString("email_customer"),
                    rs.getString("metode_pembayaran")
                };
                model.addRow(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void searchTransaksi(JTable table, String keyword) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        try (Connection conn = koneksi.getConnection(); PreparedStatement stmt = conn.prepareStatement("SELECT * FROM tb_transaksi WHERE id_transaksi LIKE ?")) {
            stmt.setString(1, "%" + keyword + "%");
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Object[] row = {
                    rs.getString("id_transaksi"),
                    rs.getDate("tanggal_transaksi"),
                    rs.getInt("total_bayar"),
                    rs.getString("email_customer") == null || rs.getString("email_customer").trim().isEmpty() ? "-" : rs.getString("email_customer"),
                    rs.getString("metode_pembayaran")
                };
                model.addRow(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void filterTransaksi(JTable table, String filter) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);

        String query;
        boolean semua = filter.equalsIgnoreCase("Semua Metode Pembayaran");

        if (semua) {
            query = "SELECT * FROM tb_transaksi";
        } else {
            query = "SELECT * FROM tb_transaksi WHERE metode_pembayaran = ?";
        }

        try (Connection conn = koneksi.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {

            if (!semua) {
                stmt.setString(1, filter);
            }

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Object[] row = {
                    rs.getString("id_transaksi"),
                    rs.getDate("tanggal_transaksi"),
                    rs.getInt("total_bayar"),
                    rs.getString("email_customer") == null || rs.getString("email_customer").trim().isEmpty() ? "-" : rs.getString("email_customer"),
                    rs.getString("metode_pembayaran")
                };
                model.addRow(row);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void exportPDF(Component parent, String periode, LocalDate start, LocalDate end) {
        LocalDate now = LocalDate.now();

        // Tangani periode non-Custom
        if (!periode.equalsIgnoreCase("Custom")) {
            switch (periode) {
                case "Hari ini":
                    start = end = now;
                    break;
                case "Kemarin":
                    start = end = now.minusDays(1);
                    break;
                case "7 Hari terakhir":
                    start = now.minusDays(6);
                    end = now;
                    break;
                case "30 Hari terakhir":
                    start = now.minusDays(29);
                    end = now;
                    break;
                case "Bulan lalu":
                    start = now.minusMonths(1).withDayOfMonth(1);
                    end = start.withDayOfMonth(start.lengthOfMonth());
                    break;
                default:
                    JOptionPane.showMessageDialog(parent, "Periode tidak dikenali.");
                    return;
            }
        }

        // Validasi tanggal
        if (start == null || end == null) {
            JOptionPane.showMessageDialog(parent, "Tanggal awal dan akhir tidak boleh kosong.");
            return;
        }

        // Konversi ke String untuk SQL
        LocalDateTime startDateTime = start.atStartOfDay();
        LocalDateTime endDateTime = end.atTime(LocalTime.MAX);

        Timestamp startTimestamp = Timestamp.valueOf(startDateTime);
        Timestamp endTimestamp = Timestamp.valueOf(endDateTime);

        // Buat string hanya untuk ditampilkan di PDF, bukan untuk query
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String startStr = dtf.format(start);
        String endStr = dtf.format(end);

        Document doc = new Document();
        String query = "SELECT * FROM tb_transaksi WHERE tanggal_transaksi BETWEEN ? AND ?";

        try (Connection conn = koneksi.getConnection(); PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setTimestamp(1, startTimestamp);
            stmt.setTimestamp(2, endTimestamp);

            ResultSet rs = stmt.executeQuery();

            // Siapkan direktori penyimpanan
            String userHome = System.getProperty("user.home");
            String folderPath = userHome + File.separator + "Downloads" + File.separator + "Laporan Penjualan Hashirama Ramen";
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String fileName = folderPath + File.separator + "Laporan Transaksi " + startStr + " sd " + endStr + ".pdf";
            PdfWriter.getInstance(doc, new FileOutputStream(fileName));
            doc.open();

            // Judul Laporan
            Font bold = new Font(Font.FontFamily.HELVETICA, 14, Font.BOLD);
            Paragraph title = new Paragraph("Laporan Penjualan\nHashirama Ramen", bold);
            title.setAlignment(Element.ALIGN_CENTER);
            doc.add(title);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Periode: " + startStr + " s.d " + endStr));
            doc.add(new Paragraph(" "));

            // Tabel
            PdfPTable table = new PdfPTable(new float[]{3, 3, 2, 3, 3});
            table.setWidthPercentage(100);

            Font fontHeader = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
            BaseColor bgHeader = new BaseColor(119, 31, 30);
            String[] headers = {"ID Transaksi", "Tanggal", "Total", "Email", "Metode Pembayaran"};

            for (String h : headers) {
                PdfPCell cell = new PdfPCell(new Phrase(h, fontHeader));
                cell.setBackgroundColor(bgHeader);
                cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                cell.setPadding(6);
                table.addCell(cell);
            }

            Font fontIsi = new Font(Font.FontFamily.HELVETICA, 9);
            int totalSemua = 0;
            boolean adaData = false;
            
            
            NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
            
            while (rs.next()) {
                adaData = true;

                table.addCell(new PdfPCell(new Phrase(rs.getString("id_transaksi"), fontIsi)));
                table.addCell(new PdfPCell(new Phrase(rs.getString("tanggal_transaksi"), fontIsi)));
                int total = rs.getInt("total_bayar");
                table.addCell(new PdfPCell(new Phrase(rupiahFormat.format(total), fontIsi)));
                table.addCell(new PdfPCell(new Phrase(rs.getString("email_customer"), fontIsi)));
                table.addCell(new PdfPCell(new Phrase(rs.getString("metode_pembayaran"), fontIsi)));

                totalSemua += total;
            }

            if (!adaData) {
                JOptionPane.showMessageDialog(parent, "Tidak ada data transaksi untuk periode ini.");
                doc.close();
                return;
            }

            doc.add(table);
            doc.add(new Paragraph(" "));

            // Total keseluruhan
            PdfPTable tbTotal = new PdfPTable(new float[]{10, 4});
            tbTotal.setWidthPercentage(100);
            Font fontTotal = new Font(Font.FontFamily.HELVETICA, 9, Font.BOLD);

            PdfPCell label = new PdfPCell(new Phrase("Total Seluruh Transaksi:", fontHeader));
            label.setBackgroundColor(bgHeader);
            label.setPadding(6);
            tbTotal.addCell(label);

            PdfPCell value = new PdfPCell(new Phrase(rupiahFormat.format(totalSemua), fontTotal));
            value.setHorizontalAlignment(Element.ALIGN_RIGHT);
            //value.setBorder(Rectangle.NO_BORDER);
            value.setPadding(6);
            tbTotal.addCell(value);

            doc.add(tbTotal);
            doc.close();

            JOptionPane.showMessageDialog(parent, "PDF berhasil disimpan: " + fileName);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(parent, "Gagal membuat laporan: " + e.getMessage());
        }
    }
}
