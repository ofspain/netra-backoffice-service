package utilities.dto;

import com.netra.commons.models.BaseEntity;

public class EntityWithUpload<T extends BaseEntity> {
    private T modeledEntity;
    private String logo;

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public void setModeledEntity(T modeledEntity){
        this.modeledEntity = modeledEntity;
    }

    public T getModeledEntity(){
        return this.modeledEntity;
    }

}
