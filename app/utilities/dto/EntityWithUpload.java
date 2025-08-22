package utilities.dto;

import com.netra.commons.models.BaseEntity;

public class EntityWithUpload<T extends BaseEntity> {
    private T modeledEntity;
    private FileUpload fileUpload;

    public void setModeledEntity(T modeledEntity){
        this.modeledEntity = modeledEntity;
    }

    public T getModeledEntity(){
        return this.modeledEntity;
    }

    public FileUpload getFileUpload() {
        return fileUpload;
    }

    public void setFileUpload(FileUpload fileUpload) {
        this.fileUpload = fileUpload;
    }
}
