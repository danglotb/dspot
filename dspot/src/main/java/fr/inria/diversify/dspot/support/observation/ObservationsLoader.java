package fr.inria.diversify.dspot.support.observation;

import fr.inria.diversify.compare.Observation;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * Created by Benjamin DANGLOT
 * benjamin.danglot@inria.fr
 * on 07/12/17
 */
public class ObservationsLoader {

    private static String PATH_SERIAL_OBSERVATIONS = "target/dspot/observations.ser";

    public static Map<String, Observation> loadObservations() {
        Map<String, Observation> observations;
        try (FileInputStream fin = new FileInputStream(PATH_SERIAL_OBSERVATIONS)) {
            try (ObjectInputStream ois = new ObjectInputStream(fin)) {
                observations = (Map) ois.readObject();
                FileUtils.forceDelete(new File(PATH_SERIAL_OBSERVATIONS));
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return observations;
    }

    public static void writeObservations(Map<String, Observation> observations) {
        try (FileOutputStream fout = new FileOutputStream(PATH_SERIAL_OBSERVATIONS)) {
            try (ObjectOutputStream oos = new ObjectOutputStream(fout)) {
                oos.writeObject(observations);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

}
