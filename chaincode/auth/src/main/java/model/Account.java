package model;

import com.google.gson.Gson;
import org.apache.commons.lang3.SerializationUtils;

import java.io.Serializable;

public class Account implements Serializable {
    private String name;
    private Status status;
    private String ato;

    public Account(String name, Status status, String ato) {
        this.name = name;
        this.status = status;
        this.ato = ato;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getAto() {
        return ato;
    }

    public void setAto(String ato) {
        this.ato = ato;
    }

    public byte[] toBytes() {
        return SerializationUtils.serialize(this);
    }

    public static Account fromBytes(byte[] bytes) {
        return SerializationUtils.deserialize(bytes);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static Account fromJson(String json) {
        return new Gson().fromJson(json, Account.class);
    }
}
