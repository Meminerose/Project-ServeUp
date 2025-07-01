/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;

/**
 *
 * @author ASUS
 */
public class koneksi {
    private static final String url = "jdbc:mysql://switchback.proxy.rlwy.net:58428/railway?useSSL=false&allowPublicKeyRetrieval=true";
    private static final String username = "root";
    private static final String password = "mBcXdMqboczMTSMMSGByhrpOpsRUrWJE";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }
}
