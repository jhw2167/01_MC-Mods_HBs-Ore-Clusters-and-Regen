package com.holybuckets.foundation;

import com.holybuckets.orecluster.config.COreClusters;

import java.util.Arrays;
import java.util.HashSet;

public class ConfigModelBase {

    public static final HashSet<String> ACCEPTED_STRING_BOOLEAN_TRUE = new HashSet<>(Arrays.asList("true", "yes", "1"));


    public Boolean parseBoolean(String value) {
        return ACCEPTED_STRING_BOOLEAN_TRUE.contains(value);
    }

    //Generalize the above error checking to create a template method for validating integers and floats
    public Boolean validateFloat(Float value, ConfigBase.ConfigFloat f, String element) {
        StringBuilder error = new StringBuilder();
        error.append("Error setting ");
        error.append(f.getName());
        error.append(element);
        error.append(" using default value of ");
        error.append(f.getDefault() + " instead");

        if( f.test(value) )
            return true;
        else {
            LoggerBase.logWarning(error.toString());
            return false;
        }
    }

    public Boolean validateInteger(Integer value, ConfigBase.ConfigInt i, String element) {
        StringBuilder error = new StringBuilder();
        error.append("Error setting ");
        error.append(i.getName() + " ");
        error.append(element);
        error.append(" using default value of ");
        error.append(i.getDefault() + " instead");

        if( i.test(value) )
            return true;
        else {
            LoggerBase.logWarning(error.toString());
            return false;
        }
    }
}
