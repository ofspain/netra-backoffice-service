package services;


import com.netra.commons.models.FinancialInstitution;
import services.db.JdbcWrapper;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.Types;

import static utilities.MapperUtil.projectObjectMapper;

@Singleton
public class FinancialInstitutionService {

    private final JdbcWrapper jdbcClient;

    @Inject
    public FinancialInstitutionService(JdbcWrapper jdbcClient){
        this.jdbcClient = jdbcClient;
    }


    public FinancialInstitution findFinancialInstitutionByUniqueKey(String column, String value){
        String json = jdbcClient.call("find_financial_institution_json")
                .param(column)
                .param(value)
                .outParam(1, Types.OTHER)
                .executeForOutput(1, String.class)
                .toCompletableFuture()
                .join();


        return  (json != null) ? projectObjectMapper().convertValue(json, FinancialInstitution.class) : null;
    }

    public FinancialInstitution saveFinancialInstitution(FinancialInstitution financialInstitution){

        //todo: hit db here and re initilize param
        return financialInstitution;
    }

}
