/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import javax.swing.JTable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;

import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javax.swing.table.DefaultTableModel;
import javax.swing.JOptionPane;

import jakarta.mail.Authenticator;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import connection.koneksi;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingWorker;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

/**
 *
 * @author ASUS
 */
public class TransaksiController {

    private String tanggalTransaksiGlobal;
    private String kodeTransaksiGlobal;

    public void tampilDataMenu(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
        
        try (Connection conn = koneksi.getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM tb_menu")) {

            while (rs.next()) {
                Object[] row = new Object[]{
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

    public void cariDataMenu(JTable table, String keyword) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        
        String sql = "SELECT * FROM tb_menu WHERE nama_menu LIKE ?";

        try (Connection conn = koneksi.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Keyword dengan wildcard
            String param = "%" + keyword + "%";
            stmt.setString(1, param);

            ResultSet rs = stmt.executeQuery();
            model.setRowCount(0); // reset tabel

            while (rs.next()) {
                Object[] row = new Object[]{
                    rs.getString("id_menu"),
                    rs.getString("nama_menu"),
                    rs.getString("deskripsi_menu"),
                    rs.getInt("harga_menu"),
                    rs.getBoolean("status_menu") ? "Tersedia" : "Tidak tersedia"
                };
                model.addRow(row);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error mencari data: " + e.getMessage());
        }
    }

    public void tampilkanDetailMenu(JTextField txtKodeMenu, JTextField txtNamaMenu, JTextField txtHargaMenu) {
        try (Connection conn = koneksi.getConnection()) {
            String query = "SELECT * FROM tb_menu WHERE id_menu = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, txtKodeMenu.getText());
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        txtNamaMenu.setText(rs.getString("nama_menu"));
                        txtHargaMenu.setText(String.valueOf(rs.getInt("harga_menu")));
                    } else {
                        txtNamaMenu.setText("");
                        txtHargaMenu.setText("");
                        JOptionPane.showMessageDialog(null, "Data menu dengan ID '" + txtKodeMenu + "' tidak ditemukan.", "Data Tidak Ditemukan", JOptionPane.WARNING_MESSAGE);
                    }
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal mengambil detail menu: " + e.getMessage());
        }
    }

    public void tambahItemTransaksi(
            String idMenu,
            String namaMenu,
            String hargaStr,
            String qtyStr,
            JTable table,
            JTextField txtKodeMenu,
            JTextField txtNamaMenu,
            JTextField txtHargaMenu,
            JTextField txtQuantity
    ) {
        if (idMenu.isEmpty() || namaMenu.isEmpty() || hargaStr.isEmpty() || qtyStr.isEmpty()) {
            JOptionPane.showMessageDialog(null, "Data menu atau qty belum lengkap");
            return;
        }

        try {
            int harga = Integer.parseInt(hargaStr);
            int qty = Integer.parseInt(qtyStr);
            
            if(qty <=0) {
                JOptionPane.showMessageDialog(null, "Qty harus lebih dari 0");
                return;
            }
            
                        
            DefaultTableModel model = (DefaultTableModel) table.getModel();
            
            for(int i = 0; i < model.getRowCount(); i++) {
                String existingId = model.getValueAt(i, 0).toString();
                if(existingId.equalsIgnoreCase(idMenu)) {
                    JOptionPane.showMessageDialog(null, "Menu sudah ditambahkan. Silakan update quantity-nya.");
                    return;
                }
            }
            
            int subtotal = harga * qty;
            Object[] rowData = {idMenu, namaMenu, harga, qty, subtotal};
            model.addRow(rowData);

            // Kosongkan field input
            txtKodeMenu.setText("");
            txtNamaMenu.setText("");
            txtHargaMenu.setText("");
            txtQuantity.setText("");

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Harga atau qty tidak valid.");
        }
    }

    public void hitungTotalBayar(JTable table, JTextField txtTotalBayar) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        int total = 0;

        for (int i = 0; i < model.getRowCount(); i++) {
            int subtotal = Integer.parseInt(model.getValueAt(i, 4).toString());
            total += subtotal;
        }

        // Format ke rupiah (Rp#,##0)
        Locale localeID = new Locale("in", "ID");
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(localeID);
        symbols.setCurrencySymbol("Rp");
        symbols.setGroupingSeparator('.');
        symbols.setMonetaryDecimalSeparator(',');

        DecimalFormat rupiahFormat = new DecimalFormat("Rp#,##0", symbols);
        String totalFormatted = rupiahFormat.format(total);

        txtTotalBayar.setText(totalFormatted);
    }

    public void showInvoice(JTable table, JTextArea areaFormStruk) {
        String invoice = generateInvoice(table);
        areaFormStruk.setText(invoice);
    }

    public String generateInvoice(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        StringBuilder sb = new StringBuilder();

        sb.append(centerText("HASHIRAMA RAMEN", 130)).append("\n");
        sb.append(centerText("Jl. Diponegoro No.62-60, Salatiga, Kec. Sidorejo", 120)).append("\n");
        sb.append(centerText("Kota Salatiga, Jawa Tengah 50711", 130)).append("\n");
        sb.append("-----------------------------------------------------------------------------------------------------------------\n");

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        tanggalTransaksiGlobal = now.format(formatter);
        kodeTransaksiGlobal = "TRX-" + UUID.randomUUID().toString().substring(0, 15).toUpperCase();

        sb.append("Tanggal             : ").append(tanggalTransaksiGlobal).append("\n");
        sb.append("Kode Transaksi : ").append(kodeTransaksiGlobal).append("\n");
        sb.append("-----------------------------------------------------------------------------------------------------------------\n");

        sb.append(String.format("%-60s %-20s %20s\n", "Nama Menu", "Qty", "Subtotal"));
        sb.append("-----------------------------------------------------------------------------------------------------------------\n");

        float total = 0;
        for (int i = 0; i < model.getRowCount(); i++) {
            String namaMenu = model.getValueAt(i, 1).toString();
            int qty = Integer.parseInt(model.getValueAt(i, 3).toString());
            float subtotal = Float.parseFloat(model.getValueAt(i, 4).toString());
            total += subtotal;
            sb.append(String.format("%-60s %-22d %20.0f\n", namaMenu, qty, subtotal));
        }

        sb.append("-----------------------------------------------------------------------------------------------------------------\n");

        Locale localeID = new Locale("in", "ID");
        NumberFormat rupiahFormat = NumberFormat.getCurrencyInstance(localeID);
        rupiahFormat.setMaximumFractionDigits(0);
        String totalFormatted = rupiahFormat.format(total);

        sb.append(String.format("Total Bayar: %s\n", totalFormatted));
        sb.append("===============================================================\n");
        sb.append(centerText("Terima kasih telah mengunjungi", 130)).append("\n");
        sb.append(centerText("Hashirama Ramen", 130)).append("\n");
        sb.append("===============================================================\n");

        return sb.toString();
    }

    private String centerText(String text, int width) {
        int padding = (width - text.length()) / 2;
        String pad = " ".repeat(Math.max(padding, 0));
        return pad + text;
    }

    public String getKodeTransaksiGlobal() {
        return kodeTransaksiGlobal;
    }

    public String getTanggalTransaksiGlobal() {
        return tanggalTransaksiGlobal;
    }

    public void sendEmailInvoice(String recipientEmail, String invoiceText, String kodeTransaksi) {
        if (recipientEmail == null || recipientEmail.trim().isEmpty()) {
            return;
        }
        
        final String senderEmail = "andikayusuf2685@gmail.com";
        final String senderPassword = "bcic pyqy rvwz uuzx"; // Password aplikasi, bukan password Gmail biasa

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");

        final JDialog loadingDialog = new JDialog();
        JLabel loadingLabel = new JLabel("Mengirim invoice, mohon tunggu...");
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingDialog.add(loadingLabel);
        loadingDialog.setSize(300, 100);
        loadingDialog.setLocationRelativeTo(null);
        loadingDialog.setModal(false);
        loadingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);

        SwingWorker<Void, Void> worker = new SwingWorker<>() {
            protected Void doInBackground() {
                try {
                    Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(senderEmail, senderPassword);
                        }
                    });

                    Message message = new MimeMessage(session);
                    message.setFrom(new InternetAddress(senderEmail));
                    message.setRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
                    message.setSubject("[" + kodeTransaksi + "] - Invoice Hashirama Ramen");

                    String body = "Hai pelanggan Hashirama Ramen,\n\n"
                            + "Terima kasih telah berkunjung dan menikmati sajian kami.\n"
                            + "Berikut kami lampirkan invoice pesanan Anda sebagai bukti transaksi.\n\n"
                            + "Jika ada pertanyaan terkait pesanan atau pembayaran, silakan hubungi kami.\n\n"
                            + "Salam hangat,\nTim Hashirama Ramen\n\n\n"
                            + invoiceText + "\n";

                    message.setText(body);
                    Transport.send(message);
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
                return null;
            }

            protected void done() {
                loadingDialog.dispose();
                try {
                    get();
                    JOptionPane.showMessageDialog(null, "Invoice berhasil dikirim ke " + recipientEmail);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(null, "Gagal mengirim email.\nPeriksa koneksi & email tujuan!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        };

        loadingDialog.setVisible(true);
        worker.execute();
    }

    public void saveTransaksi(JTable model, String totalBayarText, String metodePembayaran, String emailCustomer, String idUser, String kodeTransaksi, String tanggalTransaksiStr) {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(null, "Tidak ada item untuk disimpan");
            return;
        }

        // Format dan parsing nilai
        totalBayarText = totalBayarText.replace("Rp", "").replace(".", "").replace(",", "").trim();
        int totalBayar = Integer.parseInt(totalBayarText);

        // Konversi waktu dari String ke Timestamp
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        LocalDateTime localDateTime = LocalDateTime.parse(tanggalTransaksiStr, formatter);
        Timestamp tanggalTransaksi = Timestamp.valueOf(localDateTime);

        try (Connection conn = koneksi.getConnection()) {
            // Insert ke tabel transaksi
            String queryTransaksi = "INSERT INTO tb_transaksi (id_transaksi, id_user, total_bayar, tanggal_transaksi, email_customer, metode_pembayaran) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement psTransaksi = conn.prepareStatement(queryTransaksi)) {
                psTransaksi.setString(1, kodeTransaksi);
                psTransaksi.setString(2, idUser);
                psTransaksi.setInt(3, totalBayar);
                psTransaksi.setTimestamp(4, tanggalTransaksi);
                psTransaksi.setString(5, emailCustomer);
                psTransaksi.setString(6, metodePembayaran);
                psTransaksi.executeUpdate();
            }

            // Insert ke tabel detail transaksi
            String queryDtl = "INSERT INTO tb_dtl_transaksi (id_menu, jumlah_beli, subtotal, id_transaksi) VALUES (?, ?, ?, ?)";
            try (PreparedStatement psDtl = conn.prepareStatement(queryDtl)) {
                for (int i = 0; i < model.getRowCount(); i++) {
                    String idMenu = model.getValueAt(i, 0).toString();
                    int qty = Integer.parseInt(model.getValueAt(i, 3).toString());
                    float subtotal = Float.parseFloat(model.getValueAt(i, 4).toString());

                    psDtl.setString(1, idMenu);
                    psDtl.setInt(2, qty);
                    psDtl.setFloat(3, subtotal);
                    psDtl.setString(4, kodeTransaksi);
                    psDtl.addBatch();
                }
                psDtl.executeBatch();
            }

            JOptionPane.showMessageDialog(null, "Transaksi Berhasil Disimpan");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Gagal menyimpan transaksi: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    public void updateQty(DefaultTableModel model, int selectedRow, int newQty) {
        if (newQty <= 0) {
            JOptionPane.showMessageDialog(null, "Qty harus lebih dari 0");
            return;
        }

        int harga = Integer.parseInt(model.getValueAt(selectedRow, 2).toString());
        int newSubtotal = harga * newQty;

        model.setValueAt(newQty, selectedRow, 3);
        model.setValueAt(newSubtotal, selectedRow, 4);
    }
    
    
}
