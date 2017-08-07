package fr.inria.diversify.compare;


import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

/**
 * User: Simon
 * Date: 23/10/15
 * Time: 14:31
 */
public class ObjectLog {
    private static ObjectLog singleton;
    private Map<String, Observation> observations;
    private MethodsHandler methodsHandler;
    private Invocator invocator;
    private int maxDeep = 4;
    private Map<String, Object> objects;

    private ObjectLog() {
        this.observations = new HashMap<>();
        this.objects = new HashMap<>();
        this.methodsHandler = new MethodsHandler();
        this.invocator = new Invocator(1);
    }

    private static ObjectLog getSingleton() {
        if (singleton == null) {
            singleton = new ObjectLog();
        }
        return singleton;
    }

    public static void reset() {
        singleton = new ObjectLog();
    }

    public static void log(Object object, String stringObject, String positionId) {
        logObject(object, stringObject, positionId);
        getSingleton().pLog(object, stringObject, positionId, 0);
    }

    public static void logObject(Object object, String stringObject, @Deprecated String positionId) {
        if (object != null) {
            getSingleton().objects.put(stringObject, object);
        }
    }

    public void pLog(Object object, String stringObject, String positionId, int deep) {
        if (deep < maxDeep) {
            if (object == null) {
                addObservation(positionId, stringObject, null);
            } else if (Utils.isPrimitive(object)) {
                addObservation(positionId, stringObject, object);
            } else if (Utils.isPrimitiveArray(object)) {
                addObservation(positionId, stringObject, object);
            } else if (Utils.isPrimitiveCollectionOrMap(object)) {
                addObservation(positionId, stringObject, object);
            } else {
                observeNotNullObject(object, stringObject, positionId, deep);
            }
        }
    }

    private void addObservation(String positionId, String stringObject, Object value) {
        if (!observations.containsKey(positionId)) {
            observations.put(positionId, new Observation());
        }
        observations.get(positionId).add(stringObject, value);
    }

    private void observeNotNullObject(Object o, String stringObject, String positionId, int deep) {
        if (deep < maxDeep) {
            for (Method method : methodsHandler.getAllMethods(o)) {
                Invocation invocation = new Invocation(o, method);
                invocator.invoke(invocation);
                final Object result = invocation.getResult();
                if (!isAClientCode(result) && invocation.getError() == null) {
                    String castType = o.getClass().getCanonicalName();
                    pLog(result, "((" + castType + ")"
                            + stringObject + ")." + method.getName() + "()", positionId, deep + 1);
                }
            }
        }
    }

    //TODO checks this assertion...
    @Deprecated
    private boolean isAClientCode(Object result) {
        return result != null &&
                result.getClass().getProtectionDomain() != null &&
                result.getClass().getProtectionDomain().getCodeSource() != null &&
                result.getClass().getProtectionDomain().getCodeSource().getLocation() != null;
    }

    public static Map<String, Observation> getObservations() {
        return singleton.observations;
    }

}
