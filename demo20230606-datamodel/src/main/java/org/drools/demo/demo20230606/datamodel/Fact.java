package org.drools.demo.demo20230606.datamodel;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Fact {
    private final String objectType;
    private final String id;
    private final Map<String, Object> fields;

    @JsonCreator
    public Fact(@JsonProperty("objectType") String objectType,
            @JsonProperty("id") String id,
            @JsonProperty("fields") Map<String, Object> fields) {
        this.objectType = objectType;
        this.id = id;
        this.fields = fields;
    }

    // --- inferred from rules?
    public Fact(String objectType, String id) {
        this(objectType, id, new HashMap<>());
    }
    public Object getFieldValue(String key) {
        return fields.get(key);
    }
    public Object setFieldValue(String key, Object value) {
        return fields.put(key, value);
    }
    // --- /inferred from rules.

    public String getId() {
        return id;
    }

    public String getObjectType() {
        return objectType;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        result = prime * result + ((objectType == null) ? 0 : objectType.hashCode());
        result = prime * result + ((fields == null) ? 0 : fields.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Fact other = (Fact) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (objectType == null) {
            if (other.objectType != null)
                return false;
        } else if (!objectType.equals(other.objectType))
            return false;
        if (fields == null) {
            if (other.fields != null)
                return false;
        } else if (!fields.equals(other.fields))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Fact [id=" + id + ", objectType=" + objectType + ", fields=" + fields + "]";
    }
}