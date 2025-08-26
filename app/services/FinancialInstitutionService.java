package services;


import com.netra.commons.database.EnhancedBeanPropertyRowMapper;
import com.netra.commons.models.FinancialInstitution;
import services.db.JdbcWrapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import static utilities.MapperUtil.projectObjectMapper;

@Singleton
public class FinancialInstitutionService {

    private final JdbcWrapper jdbcClient;

    @Inject
    public FinancialInstitutionService(JdbcWrapper jdbcClient){
        this.jdbcClient = jdbcClient;
    }


    public Boolean findFinancialInstitutionExistenceByUniqueKey(String column, String value){

        String sql = formUniqueColumnSearchSQL(column);

        Long countObject = jdbcClient.sql(sql)
                .param(column)
                .param(value)
                .query(rs -> {
                    try {
                        return rs.next() ? rs.getLong("count_object") : null;
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                }).toCompletableFuture().join();

        return null != countObject && countObject > 0;
    }

    private String formUniqueColumnSearchSQL(String column){

        // Validate column name before using it to prevent SQL injection
        if (!List.of("id", "domain_code", "code").contains(column)) {
            throw new IllegalArgumentException("Invalid search key: " + column);
        }


        String sql = "SELECT * FROM financial_institutions WHERE " + column + " = ?";

        return sql;
    }
    public FinancialInstitution findMinimalFinancialInstitutionByUniqueKey(String column, String value){


        String sql = formUniqueColumnSearchSQL(column);

        FinancialInstitution financialInstitution = jdbcClient.sql(sql)
                .param(value)
                .query(rs -> {
                    try {
                        if (rs.next()) {
                            // Use your EnhancedBeanPropertyRowMapper
                            return new EnhancedBeanPropertyRowMapper<FinancialInstitution>().mapRow(rs, 1);
                        }
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                })
                .toCompletableFuture()
                .join();

        return financialInstitution;

    }

    public FinancialInstitution saveFinancialInstitution(FinancialInstitution financialInstitution){

        //todo: hit db here and re initilize param


        return financialInstitution;
    }

}
