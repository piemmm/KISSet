package org.prowl.kisset.services.remote.netrom.circuit;

import org.prowl.kisset.services.remote.netrom.NetROMClientHandler;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CircuitManager {

    /**
     * Connection tracking.
     */
    private static final Map<Integer, Circuit> circuits = Collections.synchronizedMap(new HashMap<>());

    /**
     * Create a circuit
     *
     * @param localCircuitId
     * @param localCircuitIndex
     * @return
     */
    public static Circuit getCircuit(int localCircuitIndex, int localCircuitId) {

        int key = localCircuitId << 8 | localCircuitIndex;
        Circuit circuit = circuits.get(key);

        return circuit;
    }

    /**
     * Register a circuit and five it a circuit index and circuit id.
     *
     * @return A new circuit
     * @throws CircuitException if the circuit already exists.
     */
    public static Circuit registerCircuit(Circuit circuit, NetROMClientHandler ownerClientHandler) {

        int key = findFreeCircuit();
        if (key == -1) {
            circuit.setValid(false);
        }

        //Circuit circuit = new Circuit(key >> 8, key & 0xFF);
        circuit.setMyCircuitId(key >> 8);
        circuit.setMyCircuitIndex(key & 0xFF);
        circuit.setOwnerClientHandler(ownerClientHandler);
        circuit.setValid(true);
        circuits.put(key, circuit);
        return circuit;
    }

    /**
     * Removes a circuit from our table
     *
     * @return true if a circuit was located and removed, false otherwise.
     */
    public static boolean deleteCircuit(int localCircuitIndex, int localCircuitId) {

        int key = localCircuitId << 8 | localCircuitIndex;
        Circuit circuit = circuits.get(key);
        if (circuit == null) {
            return false;
        }

        // Possibly report the state of the circuit in the logs if not disconnected?
        circuits.remove(key);
        return true;

    }

    /**
     * Find a free circuit that has not been allocated.
     * @return
     */
    private static int findFreeCircuit() {
        for (int i = 257; i < 65535; i++) {
            if (circuits.get(i) == null) {
                return i;
            }
        }
        return -1;
    }

}
