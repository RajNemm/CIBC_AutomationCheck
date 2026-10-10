package com.example.automation.tests;

import com.example.automation.config.SecupiTestConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.testng.Assert;
import org.testng.annotations.Test;

public class SecupiDatabasePolicyTest {
    private final SecupiTestConfig config = new SecupiTestConfig();

    @Test
    public void regularRoleSeesConfiguredMaskedAndClearValues() throws SQLException {
        verifyRole("regular");
    }

    @Test
    public void privilegedRoleSeesConfiguredClearValues() throws SQLException {
        verifyRole("privileged");
    }

    private void verifyRole(String role) throws SQLException {
        List<String> clearColumns = config.clearColumns(role);
        List<String> maskedColumns = config.maskedColumns(role);
        Assert.assertFalse(clearColumns.isEmpty() && maskedColumns.isEmpty(),
                "Configure at least one clear or masked column for role " + role);
        for (String column : clearColumns) {
            Assert.assertFalse(maskedColumns.contains(column),
                    "Column " + column + " is configured as both clear and masked for role " + role);
        }
        Pattern maskPattern = config.maskedValuePattern();

        try (Connection connection = DriverManager.getConnection(
                config.jdbcUrl(), config.username(role), config.password(role));
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(config.query())) {
            ResultSetMetaData metadata = resultSet.getMetaData();
            Map<String, Integer> columnIndexes = new LinkedHashMap<>();
            for (int index = 1; index <= metadata.getColumnCount(); index++) {
                String column = metadata.getColumnLabel(index).toUpperCase(Locale.ROOT);
                if (columnIndexes.containsKey(column)) {
                    Assert.fail("The configured query returned duplicate column label " + column);
                }
                columnIndexes.put(column, index);
            }

            Map<String, Boolean> configuredColumns = new LinkedHashMap<>();
            for (String column : clearColumns) {
                configuredColumns.put(column, Boolean.TRUE);
            }
            for (String column : maskedColumns) {
                configuredColumns.put(column, Boolean.TRUE);
            }
            Assert.assertEquals(columnIndexes.keySet(), configuredColumns.keySet(),
                    "Returned columns do not match configured expectations for role " + role);

            int rowNumber = 0;
            while (resultSet.next()) {
                rowNumber++;
                for (String column : clearColumns) {
                    String value = resultSet.getString(columnIndexes.get(column));
                    Assert.assertFalse(isMasked(value, maskPattern),
                            "Expected a clear value for role " + role + ", row " + rowNumber
                                    + ", column " + column + " but it matched the mask format");
                }
                for (String column : maskedColumns) {
                    String value = resultSet.getString(columnIndexes.get(column));
                    Assert.assertTrue(isMasked(value, maskPattern),
                            "Expected a masked value for role " + role + ", row " + rowNumber
                                    + ", column " + column + " but it did not match the mask format");
                }
            }
            Assert.assertTrue(rowNumber > 0, "The configured query returned no rows for role " + role);
        }
    }

    private boolean isMasked(String value, Pattern maskPattern) {
        return value != null && maskPattern.matcher(value).matches();
    }
}
