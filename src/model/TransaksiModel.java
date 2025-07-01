/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package model;

import java.sql.Timestamp;
/**
 *
 * @author ASUS
 */
public class TransaksiModel {
    public String idTransaksi;
    public String idUser;
    public Timestamp tanggalTransaksi;
    public String emailCustomer;
    public String metodePembayaran;
    public int totalBayar;
    
    public TransaksiModel(String idTransaksi, String idUser, Timestamp tanggalTransaksi, String emailCustomer, String metodePembayaran, int totalBayar) {
        this.idTransaksi = idTransaksi;
        this.idUser = idUser;
        this.tanggalTransaksi = tanggalTransaksi;
        this.emailCustomer = emailCustomer;
        this.metodePembayaran = metodePembayaran;
        this.totalBayar = totalBayar;
    }
    
    public String getIdTransaksi() {
        return idTransaksi;
    }

    public String getIdUser() {
        return idUser;
    }

    public Timestamp getTanggalTransaksi() {
        return tanggalTransaksi;
    }

    public String getEmailCustomer() {
        return emailCustomer;
    }

    public String getMetodePembayaran() {
        return metodePembayaran;
    }

    public int getTotalBayar() {
        return totalBayar;
    }
    
    class MenuModel {
        public String id;
        public String nama;
        public String deskripsi;
        public int harga;
        public boolean status;
        
        public MenuModel(String id, String nama, String deskripsi, int harga, boolean status) {
            this.id = id;
            this.nama = nama;
            this.deskripsi = deskripsi;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public String getNama() {
            return nama;
        }

        public String getDeskripsi() {
            return deskripsi;
        }

        public int getHarga() {
            return harga;
        }

        public boolean isStatus() {
            return status;
        }
    }
    
    class ItemTransaksiModel {
        public String idMenu;
        public String namaMenu;
        public int harga;
        public int quantity;
        public int subototal;
        
        public ItemTransaksiModel(String idMenu, String namaMenu, int harga, int quantity, int subtotal) {
            this.idMenu = idMenu;
            this.namaMenu = namaMenu;
            this.harga = harga;
            this.quantity = quantity;
            this.subototal = subtotal;
        }
        
        public String getIdMenu() {
            return idMenu;
        }

        public String getNamaMenu() {
            return namaMenu;
        }

        public int getHarga() {
            return harga;
        }

        public int getQuantity() {
            return quantity;
        }

        public int getSubototal() {
            return subototal;
        }
    }
}
