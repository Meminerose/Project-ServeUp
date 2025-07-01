/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import connection.koneksi;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.JLabel;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.DefaultCategoryDataset;
import model.MainAdminModel;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;

/**
 *
 * @author ASUS
 */
public class MainAdminController {
    private JLabel lblTotalMenu;
    private JLabel lblTotalOrder;
    private JLabel lblTotalRevenue;
    
    public MainAdminController(JLabel lblMenu, JLabel lblOrder, JLabel lblRevenue) {
        this.lblTotalMenu = lblMenu;
        this.lblTotalOrder = lblOrder;
        this.lblTotalRevenue = lblRevenue;
    }
    
    public void loadDashboardData() {
        hitungMenu();
        hitungOrder();
        hitungRevenue();
    }
    
    private void hitungMenu() {
        try (Connection conn = koneksi.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM tb_menu");
            
            if (rs.next()) {
                int totalMenu = rs.getInt("total");
                lblTotalMenu.setText(String.valueOf(totalMenu));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void hitungOrder() {
        try (Connection conn = koneksi.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM tb_transaksi");
            
            if (rs.next()) {
                int totalOrder = rs.getInt("total");
                lblTotalOrder.setText(String.valueOf(totalOrder));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void hitungRevenue() {
        try (Connection conn = koneksi.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT SUM(total_bayar) AS total FROM tb_transaksi");
            
            if (rs.next()) {
                int totalRevenue = rs.getInt("total");
                
                Locale localeID = new Locale("in", "ID");
                DecimalFormatSymbols symbols = new DecimalFormatSymbols(localeID);
                symbols.setCurrencySymbol("Rp");
                symbols.setGroupingSeparator('.');
                symbols.setMonetaryDecimalSeparator(',');
                
                DecimalFormat rupiahFormat = new DecimalFormat("Rp#,##0", symbols);
                String formattedRevenue = rupiahFormat.format(totalRevenue);
                
                lblTotalRevenue.setText(formattedRevenue);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void showLateTransaksi(JTable tbTransaksiTerbaru) {
        try (Connection conn = koneksi.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT id_transaksi, tanggal_transaksi, total_bayar, metode_pembayaran " +
                       "FROM tb_transaksi ORDER BY tanggal_transaksi DESC LIMIT 12");

            DefaultTableModel model = (DefaultTableModel) tbTransaksiTerbaru.getModel();
            model.setRowCount(0);

            while (rs.next()) {
                MainAdminModel transaksi = new MainAdminModel(
                        rs.getString("id_transaksi"),
                        rs.getDate("tanggal_transaksi"),
                        rs.getInt("total_bayar"),
                        rs.getString("metode_pembayaran")
                );
                

                Object[] rowData = {
                    transaksi.getIdTransaksi(),
                    transaksi.getTanggalTransaksi(),
                    transaksi.getTotalBayar(),
                    transaksi.getMetodePembayaran()
                };

                model.addRow(rowData);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public void tampilkanGrafik(JPanel panelGrafikPenjualan) {
        try (Connection conn = koneksi.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT DATE(tanggal_transaksi) AS tanggal, COUNT(*) AS jumlah_transaksi " +
                       "FROM tb_transaksi " +
                       "WHERE tanggal_transaksi >= CURDATE() - INTERVAL 6 DAY " +
                       "GROUP BY DATE(tanggal_transaksi) ORDER BY DATE(tanggal_transaksi)");

            DefaultCategoryDataset dataset = new DefaultCategoryDataset();

            while (rs.next()) {
                Date tanggal = rs.getDate("tanggal");
                String label = new SimpleDateFormat("dd MMM", new Locale("id", "ID")).format(tanggal);
                int jumlah = rs.getInt("jumlah_transaksi");

                dataset.addValue(jumlah, "Transaksi", label);
            }

            JFreeChart lineChart = ChartFactory.createLineChart(
                    "Grafik Penjualan 7 Hari Terakhir",
                    "Tanggal", "Jumlah Transaksi", dataset
            );
            
            CategoryPlot plot = lineChart.getCategoryPlot();
            plot.setBackgroundPaint(new Color(245, 245, 245)); // warna bg
            plot.setRangeGridlinePaint(Color.red); // warna garis hz
            
            // grafik
            LineAndShapeRenderer renderer = new LineAndShapeRenderer();
            renderer.setSeriesPaint(0, new Color(118,119,30));
            renderer.setSeriesStroke(0, new BasicStroke(2.5f)); // ketebalan garis
            renderer.setSeriesShapesVisible(0, true); // titik ditampilkan
            renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-3.0, -3.0, 6.0, 6.0));
            plot.setRenderer(renderer);
            
            // Font
            lineChart.getTitle().setFont(new Font("Segoe UI", Font.BOLD, 16));
            lineChart.getTitle().setPaint(new Color(33, 33, 33));
            lineChart.getCategoryPlot().getDomainAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 12));
            lineChart.getCategoryPlot().getRangeAxis().setTickLabelFont(new Font("Segoe UI", Font.PLAIN, 12));

            ChartPanel chartPanel = new ChartPanel(lineChart);
            chartPanel.setPreferredSize(new Dimension(560, 265));
            chartPanel.setBackground(Color.WHITE); // background chart panel
            
            panelGrafikPenjualan.removeAll();
            panelGrafikPenjualan.setLayout(new BorderLayout());
            panelGrafikPenjualan.add(chartPanel, BorderLayout.CENTER);
            panelGrafikPenjualan.validate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
}