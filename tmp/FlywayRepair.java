import java.sql.*;

public class FlywayRepair {
  public static void main(String[] args) throws Exception {
    Class.forName("org.postgresql.Driver");
    try (Connection c = DriverManager.getConnection(
            "jdbc:postgresql://localhost:5432/ticket_booking", "tbs", "tbs")) {
      // Update checksum of already-applied V3 to match current local file (241926044)
      try (PreparedStatement ps = c.prepareStatement(
              "UPDATE flyway_schema_history SET checksum = ? WHERE version = '3'")) {
        ps.setInt(1, 241926044);
        int n = ps.executeUpdate();
        System.out.println("Updated rows: " + n);
      }
      try (Statement s = c.createStatement();
           ResultSet rs = s.executeQuery(
               "SELECT version, checksum FROM flyway_schema_history WHERE version = '3'")) {
        rs.next();
        System.out.println("v3 checksum now=" + rs.getInt(2));
      }
    }
  }
}
