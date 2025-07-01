/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package controller;

import model.*;
import view.*;
import connection.koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import utils.LoginSession;

/**
 *
 * @author ASUS
 */
public class LoginController {
    private LoginView view;
    
    public LoginController(LoginView view) {
        this.view = view;
    }
    
    public void loginAction() {
        String username = view.getUsername();
        String password = view.getPassword();
        
        LoginModel user = null;
        
        String query = "SELECT * FROM tb_user WHERE username = ? AND password = ? AND status = 'Aktif'";
        
        try (Connection conn = koneksi.getConnection();
                PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, username);
            stmt.setString(2, password);
            
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                user = new LoginModel(username, password);
                user.setRole(rs.getString("role"));
                user.setId_user(rs.getString("id_employee"));
                
                LoginSession.currentUser = user;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        if (user != null) {
            if("admin".equalsIgnoreCase(user.getRole())) {
                MainAdminView admin = new MainAdminView();
                admin.setVisible(true);
                view.dispose();
            } else if ("kasir".equalsIgnoreCase(user.getRole())) {
                TransaksiView pos = new TransaksiView();
                pos.setVisible(true);
                view.dispose();
            }
            else {
                JOptionPane.showMessageDialog(view, "Role tidak dikenali");
            }
        } else {
            JOptionPane.showMessageDialog(view, "Username atau password salah.", "Login Gagal", JOptionPane.WARNING_MESSAGE);
            view.resetPassword();
        }
    }
}
