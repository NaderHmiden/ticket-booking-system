import java.sql.*;

public class FlywayFix {
  public static void main(String[] args) throws Exception {
    Class.forName("org.postgresql.Driver");
    String url = "jdbc:postgresql://localhost:5432/ticket_booking";
    try (Connection c = DriverManager.getConnection(url, "tbs", "tbs")) {
      System.out.println("Connected");
      try (Statement s = c.createStatement();
           ResultSet rs = s.executeQuery("SELECT version, checksum, description FROM flyway_schema_history ORDER BY installed_rank")) {
        while (rs.next()) {
          System.out.println("v" + rs.getString(1) + " checksum=" + rs.getInt(2) + " " + rs.getString(3));
        }
      }
      try (Statement s = c.createStatement();
           ResultSet rs = s.executeQuery("SELECT column_name, data_type, is_nullable FROM information_schema.columns WHERE table_name='booking_hold' ORDER BY ordinal_position")) {
        System.out.println("--- booking_hold columns ---");
        while (rs.next()) {
          System.out.println(rs.getString(1) + " " + rs.getString(2) + " nullable=" + rs.getString(3));
        }
      }
      try (Statement s = c.createStatement();
           ResultSet rs = s.executeQuery("SELECT to_regclass('public.processed_payments')::text, to_regclass('public.app_user')::text")) {
        rs.next();
        System.out.println("processed_payments=" + rs.getString(1) + " app_user=" + rs.getString(2));
      }
    }
  }
}
