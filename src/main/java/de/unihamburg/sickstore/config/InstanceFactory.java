package de.unihamburg.sickstore.config;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class InstanceFactory {

    public static Object newInstanceFromConfig(Map<String, Object> config) {
        String className = (String) config.get("class");
        if (className == null) {
            throw new RuntimeException("Missing parameter class");
        }

        try {
            Class clazz = null;
            clazz = Class.forName(className);

            if (config.size() == 1) {
                // if no parameters were given (except the class name),
                // do not require a newInstanceFromConfig method and just call the default constructor
                return clazz.newInstance();
            }

            Method method = clazz.getMethod("newInstanceFromConfig", Map.class);

            return method.invoke(null, config);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Class " + className + " has no method newInstanceFromConfig");
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
