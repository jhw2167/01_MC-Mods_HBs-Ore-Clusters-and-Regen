package com.holybuckets.foundation.modelInterface;

public interface IStringSerializable {

    String serialize();

    void deserialize(String jsonString);
}
