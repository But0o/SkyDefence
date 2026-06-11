package skydefense;

import java.util.ArrayList;
import java.util.List;

public class Escuadron {

    public static final int TOTAL_DRONES_DEFAULT = 10;
    public static final int MAX_DRONES_ACTIVOS = 4;

    private int totalDrones;
    private int dronesGenerados;
    private List<Dron> drones;

    public Escuadron() {
        this(TOTAL_DRONES_DEFAULT);
    }

    public Escuadron(int totalDrones) {
        this.totalDrones = totalDrones;
        this.dronesGenerados = 0;
        this.drones = new ArrayList<>();
    }

    public boolean puedeGenerarMasDrones(int activos) {
        return dronesGenerados < totalDrones && activos < MAX_DRONES_ACTIVOS;
    }

    public boolean escuadronTerminado() {
        long activos = drones.stream().filter(Dron::estaActivo).count();
        return dronesGenerados >= totalDrones && activos == 0;
    }

    public void registrarGeneracionDron(Dron dron) {
        drones.add(dron);
        dronesGenerados++;
    }

    public List<Dron> getDrones() { return drones; }
    public int getTotalDrones() { return totalDrones; }
    public int getDronesGenerados() { return dronesGenerados; }
}
