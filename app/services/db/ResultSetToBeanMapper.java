package services.db;

import com.google.common.graph.Network;
import com.netra.commons.enums.DomainType;
import com.netra.commons.exceptions.AppDataAccessException;
import com.netra.commons.models.endpoint.*;
import com.netra.commons.models.FinancialInstitution;
import lombok.experimental.UtilityClass;
import utilities.MapperUtil;
import utilities.PaginatedResult;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
        return mapToFinancialInstitution(rs, "fi_");
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

            // Core identifiers
            config.setId(getNullableLong(rs, prefix + "id"));
            config.setCreatedAt(getLocalDateTime(rs, prefix + "created_at"));
            config.setUpdatedAt(getLocalDateTime(rs, prefix + "updated_at"));
            config.setDomainOwnerId(getNullableLong(rs, prefix + "domain_owner_id"));

            config.setDomainOwnerCode(getNullableString(rs, prefix + "domain_owner_code"));

            // Domain type (enum safe parsing)
            String domainTypeStr = getNullableString(rs, prefix + "domain_owner_type");
            if (domainTypeStr != null) {
                try {
                    config.setDomainOwnerType(DomainType.valueOf(domainTypeStr));
                } catch (IllegalArgumentException ignored) {
                    System.out.println("⚠️ Warning: Unknown domain type: " + domainTypeStr);
                }
            }

            config.setDescription(getNullableString(rs, prefix + "description"));

            // JSONB fields → parse into typed configs
            config.setNetwork(
                    parseJson(getNullableString(rs, prefix + "network_config"), NetworkConfig.class)
            );

            config.setSecurity(
                    parseJson(getNullableString(rs, prefix + "security_config"), SecurityConfig.class)
            );

            config.setEndpoints(
                    parseJson(getNullableString(rs, prefix + "endpoints"), List.class)
            );

            config.setResilience(
                    parseJson(getNullableString(rs, prefix + "resilience_config"), ResilienceConfig.class)
            );

            config.setMetadata(
                    parseJson(getNullableString(rs, prefix + "metadata"), Map.class)
            );

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
                items.add(mapToFinancialInstitution(rs, ""));
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

    private static <T> T parseJson(String json, Class<T> clazz) {
        if (json == null) return null;
        try {
            return MapperUtil.projectObjectMapper().readValue(json, clazz);
        } catch (Exception e) {
            throw new AppDataAccessException("Failed to parse JSON", e);
        }
    }

}
