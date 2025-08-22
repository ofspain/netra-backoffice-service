package utilities.dto;


import javax.swing.*;

public class FileUpload {

    public FileUpload(String binary, FileAction action){
        setBinary(binary);
        setAction(action);
    }

    public FileUpload(){}
    private FileAction action;   // keep | replace | remove | none
    private String binary;   // base64 string if replace

    // --- getters & setters ---
    public FileAction getAction() { return action; }
    public void setAction(FileAction action) { this.action = action; }

    public String getBinary() { return binary; }
    public void setBinary(String binary) { this.binary = binary; }

    // --- helper ---
    public boolean isReplace() {
        return FileAction.REPLACE.equals(action);
    }
    public boolean isRemove() {
        return FileAction.REMOVE.equals(action);
    }
    public boolean isKeep() {
        return FileAction.KEEP.equals(action);
    }




    public enum FileAction{
        KEEP, REMOVE, REPLACE, NONE;


        public static FileAction fixActionTypeFromString(String string){
            for(FileAction action : FileAction.values()){
                if(action.name().equalsIgnoreCase(string))
                    return action;
            }
            return NONE;
        }
    }
}
