package services.db;

import com.netra.commons.enums.DomainType;
import com.netra.commons.exceptions.AppDataAccessException;
import com.netra.commons.models.EndpointConfig;
import com.netra.commons.models.FinancialInstitution;
import lombok.experimental.UtilityClass;
import utilities.PaginatedResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Unified ResultSet mapper for FinancialInstitution and EndpointConfig
 */
@UtilityClass
public class ResultSetToBeanMapper {

    // ============================================================
    // FinancialInstitution Mapping
    // ============================================================

    /**
     * Map FinancialInstitution (supports column prefix, e.g. "fi_")
     */
    public static FinancialInstitution mapToFinancialInstitution(ResultSet rs, String prefix) {
        try {
            FinancialInstitution fi = new FinancialInstitution();

            fi.setId(getNullableLong(rs, prefix + "id"));
            fi.setCreatedAt(getLocalDateTime(rs, prefix + "created_at"));
            fi.setUpdatedAt(getLocalDateTime(rs, prefix + "updated_at"));

            fi.setName(getNullableString(rs, prefix + "name"));
            fi.setCode(getNullableString(rs, prefix + "code"));
            fi.setDomainCode(getNullableString(rs, prefix + "domain_code"));
            fi.setDisabled(getNullableBoolean(rs, prefix + "disabled", false));
            fi.setLogoKey(getNullableString(rs, prefix + "logo_key"));

            Long endpointConfigId = getNullableLong(rs, prefix + "endpoint_config");
            if (endpointConfigId != null) {
                EndpointConfig endpointRef = new EndpointConfig();
                endpointRef.setId(endpointConfigId);
                fi.setEndpointConfig(endpointRef);
            }

            return fi;
        } catch (SQLException e) {
            throw new AppDataAccessException("Error mapping FinancialInstitution", e);
        }
    }

    public static FinancialInstitution mapToFinancialInstitution(ResultSet rs) {
        return mapToFinancialInstitution(rs, "");
    }

    /**
     * Map FinancialInstitution including EndpointConfig (joined result set)
     */
    public static FinancialInstitution mapToFinancialInstitutionWithEndpoint(ResultSet rs) {
        FinancialInstitution fi = mapToFinancialInstitution(rs, "fi_");

        Long endpointConfigId = null;
        try {
            endpointConfigId = getNullableLong(rs, "ec_id");
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        if (endpointConfigId != null) {
            EndpointConfig endpoint = mapResultSetToEndpointConfig(rs, "ec_");
            fi.setEndpointConfig(endpoint);
        }
        return fi;
    }

    // ============================================================
    // EndpointConfig Mapping
    // ============================================================

    public static EndpointConfig mapResultSetToEndpointConfig(ResultSet rs, String prefix) {
        try {
            EndpointConfig config = new EndpointConfig();

            config.setId(getNullableLong(rs, prefix + "id"));
            config.setCreatedAt(getLocalDateTime(rs, prefix + "created_at"));
            config.setUpdatedAt(getLocalDateTime(rs, prefix + "updated_at"));
            config.setDomainCode(getNullableString(rs, prefix + "domain_code"));

            String domainTypeStr = getNullableString(rs, prefix + "domain_type");
            if (domainTypeStr != null) {
                try {
                    config.setDomainType(DomainType.valueOf(domainTypeStr));
                } catch (IllegalArgumentException ignored) {
                    System.out.println("Warning: Unknown domain type: " + domainTypeStr);
                }
            }

            config.setDescription(getNullableString(rs, prefix + "description"));
            config.setBaseUrl(getNullableString(rs, prefix + "base_url"));
            config.setTimeoutMillis(getNullableInteger(rs, prefix + "timeout_millis", 5000));
            config.setUseProxy(getNullableBoolean(rs, prefix + "use_proxy", false));
            config.setRequiresAuth(getNullableBoolean(rs, prefix + "requires_auth", false));

            String authTypeStr = getNullableString(rs, prefix + "auth_type");
            if (authTypeStr != null) {
                try {
                    config.setAuthType(EndpointConfig.AuthType.valueOf(authTypeStr));
                } catch (IllegalArgumentException ignored) {
                    System.out.println("Warning: Unknown auth type: " + authTypeStr);
                }
            }

            config.setRequestBodyTemplate(getNullableString(rs, prefix + "request_body_template"));

            // JSONB fields (implement parseJson if needed)
            // config.setProxy(parseJson(getNullableString(rs, prefix + "proxy_config"), EndpointConfig.ProxyConfig.class));
            // config.setUniqueTransaction(parseJson(getNullableString(rs, prefix + "unique_transaction"), EndpointConfig.EndpointDetail.class));
            // config.setMultipleTransaction(parseJson(getNullableString(rs, prefix + "multiple_transaction"), EndpointConfig.EndpointDetail.class));
            // config.setRetryConfig(parseJson(getNullableString(rs, prefix + "retry_config"), EndpointConfig.RetryConfig.class));
            // config.setFallbackConfig(parseJson(getNullableString(rs, prefix + "fallback_config"), EndpointConfig.FallbackConfig.class));

            return config;
        } catch (SQLException e) {
            throw new AppDataAccessException("Error mapping EndpointConfig", e);
        }
    }

    public static EndpointConfig mapResultSetToEndpointConfig(ResultSet rs) {
        return mapResultSetToEndpointConfig(rs, "");
    }

    // ============================================================
    // Batch Mapping
    // ============================================================

    public static List<FinancialInstitution> mapToFinancialInstitutions(ResultSet rs) {
        return mapToFinancialInstitutions(rs, null);
    }

    public static List<FinancialInstitution> mapToFinancialInstitutions(
            ResultSet rs, Function<FinancialInstitution, FinancialInstitution> transformer) {
        try {
            List<FinancialInstitution> list = new ArrayList<>();
            while (rs.next()) {
                FinancialInstitution fi = mapToFinancialInstitution(rs);
                if (transformer != null) fi = transformer.apply(fi);
                list.add(fi);
            }
            return list;
        } catch (SQLException e) {
            throw new AppDataAccessException("Error mapping FinancialInstitutions", e);
        }
    }

    public static List<FinancialInstitution> mapToFinancialInstitutionsWithEndpoints(ResultSet rs) {
        return mapToFinancialInstitutionsWithEndpoints(rs, null);
    }

    public static List<FinancialInstitution> mapToFinancialInstitutionsWithEndpoints(
            ResultSet rs, Function<FinancialInstitution, FinancialInstitution> transformer) {
        try {
            List<FinancialInstitution> list = new ArrayList<>();
            while (rs.next()) {
                FinancialInstitution fi = mapToFinancialInstitutionWithEndpoint(rs);
                if (transformer != null) fi = transformer.apply(fi);
                list.add(fi);
            }
            return list;
        } catch (SQLException e) {
            throw new AppDataAccessException("Error mapping FinancialInstitutions with endpoints", e);
        }
    }

    // Map paginated financial institutions
    public static PaginatedResult<FinancialInstitution> mapToFinancialInstitutionsPaginated(
            ResultSet rs, int limit, int offset) {
        try {
            List<FinancialInstitution> items = new ArrayList<>();
            long totalCount = 0;

            while (rs.next()) {
                if (totalCount == 0) {
                    totalCount = rs.getLong("total_count");
                }
                items.add(mapToFinancialInstitution(rs));
            }

            return new PaginatedResult<>(items, totalCount, limit, offset);
        } catch (SQLException e) {
            throw new AppDataAccessException("Error mapping paginated results", e);
        }
    }

    // Map paginated financial institutions with endpoints
    public static PaginatedResult<FinancialInstitution> mapToFinancialInstitutionsWithEndpointsPaginated(
            ResultSet rs, int limit, int offset) {
        try {
            List<FinancialInstitution> items = new ArrayList<>();
            long totalCount = 0;

            while (rs.next()) {
                if (totalCount == 0) {
                    totalCount = rs.getLong("total_count");
                }
                items.add(mapToFinancialInstitutionWithEndpoint(rs));
            }

            return new PaginatedResult<>(items, totalCount, limit, offset);
        } catch (SQLException e) {
            throw new AppDataAccessException("Error mapping paginated results with endpoints", e);
        }
    }

    // ============================================================
    // Helper Methods
    // ============================================================

    public static LocalDateTime getLocalDateTime(ResultSet rs, String col) throws SQLException {
        Timestamp ts = rs.getTimestamp(col);
        return ts != null ? ts.toLocalDateTime() : null;
    }

    public static Long getNullableLong(ResultSet rs, String col) throws SQLException {
        long value = rs.getLong(col);
        return rs.wasNull() ? null : value;
    }

    public static String getNullableString(ResultSet rs, String col) throws SQLException {
        String value = rs.getString(col);
        return rs.wasNull() ? null : value;
    }

    public static Boolean getNullableBoolean(ResultSet rs, String col) throws SQLException {
        boolean value = rs.getBoolean(col);
        return rs.wasNull() ? null : value;
    }

    public static boolean getNullableBoolean(ResultSet rs, String col, boolean defaultVal) throws SQLException {
        Boolean value = getNullableBoolean(rs, col);
        return value != null ? value : defaultVal;
    }

    public static Integer getNullableInteger(ResultSet rs, String col) throws SQLException {
        int value = rs.getInt(col);
        return rs.wasNull() ? null : value;
    }

    public static int getNullableInteger(ResultSet rs, String col, int defaultVal) throws SQLException {
        Integer value = getNullableInteger(rs, col);
        return value != null ? value : defaultVal;
    }

    public static Double getNullableDouble(ResultSet rs, String col) throws SQLException {
        double value = rs.getDouble(col);
        return rs.wasNull() ? null : value;
    }

    public static double getNullableDouble(ResultSet rs, String col, double defaultVal) throws SQLException {
        Double value = getNullableDouble(rs, col);
        return value != null ? value : defaultVal;
    }

    // JSON parser hook
    /*
    private static <T> T parseJson(String json, Class<T> clazz) {
        if (json == null) return null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(json, clazz);
        } catch (Exception e) {
            throw new AppDataAccessException("Failed to parse JSON", e);
        }
    }
    */
}
