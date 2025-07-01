# Serve Up 🍜
Aplikasi manajemen operasional restoran Hashirama Ramen dengan fitur penjadwalan karyawan, absensi, payroll, dan sistem POS.

## 📖 Deskripsi
**Serve Up** adalah aplikasi desktop berbasis Java yang dirancang untuk mempermudah manajemen karyawan dan operasional harian restoran Hashirama Ramen. Aplikasi ini mendukung berbagai fitur seperti penjadwalan shift, absensi karyawan, perhitungan payroll otomatis, pengelolaan menu makanan dan minuman, hingga sistem Point of Sale (POS) untuk kasir.

## 🎯 Fitur Utama
- 📆 **Penjadwalan Shift:** Buat dan atur jadwal kerja karyawan secara fleksibel.
- 📋 **Absensi:** Pencatatan kehadiran karyawan harian.
- 💵 **Payroll:** Hitung gaji otomatis berdasarkan shift dan absensi.
- 👤 **Manajemen User:** Hak akses untuk admin, kasir, dan staf.
- 🍱 **Manajemen Menu:** Tambah/edit data makanan dan minuman.
- 💳 **Sistem POS:** Transaksi penjualan kasir, invoice otomatis.
- 📊 **Laporan Transaksi:** Rekap penjualan dan data performa.
- ✉️ **Email Otomatis:** Kirim struk transaksi & slip gaji langsung ke email.
- 📄 **PDF Export:** Slip gaji & laporan penjualan disimpan sebagai PDF di penyimpanan lokal.

## 🛠️ Teknologi yang Digunakan
- **Java** (GUI menggunakan Swing)
- **MySQL Database** (dihosting melalui Railway)
- **JDBC** (untuk koneksi database)
- **NetBeans IDE**

## 📦 Dependensi Library
Serve Up menggunakan beberapa library eksternal berikut:

- [`jakarta.mail-2.0.1.jar`](https://eclipse-ee4j.github.io/mail-api/) – Mengirim email struk transaksi & slip gaji.
- [`jakarta.activation-2.0.1.jar`](https://eclipse-ee4j.github.io/jaf/) – Mendukung pengiriman attachment di email.
- [`itextpdf-5.5.13.4.jar`](https://itextpdf.com/en) – Membuat file PDF untuk slip gaji dan laporan penjualan.
- [`jcalendar-1.4.jar`](https://toedter.com/jcalendar/) – Komponen kalender untuk memilih tanggal pada form shift atau absensi.
- [`jfreechart-1.5.6.jar`](https://github.com/jfree/jfreechart) – Visualisasi laporan berbasis chart (jika digunakan).
- [`mysql-connector-j-8.3.0.jar`](https://dev.mysql.com/downloads/connector/j/) – Driver JDBC MySQL (jika dibutuhkan untuk eksperimen; database utama: Oracle).

> 💡 Tambahkan semua file `.jar` ke project melalui menu **Libraries** di NetBeans jika tidak menggunakan Maven/Gradle.

## 🚀 Cara Menjalankan Aplikasi
1. **Clone repository:**
   ```bash
   git clone https://github.com/Meminerose/Project-ServeUp
