package skydefense;

public class Avion {

    private static final double ALTITUD_MIN = 1000.0;
    private static final double ALTITUD_MAX = 5000.0;
    private static final int ENERGIA_MAX = 100;
    private static final double DELTA_MOVIMIENTO = 10.0;

    private double posicionX;
    private double altitud;
    private int energia;

    public Avion(double posicionX, double altitud) {
        if (altitud < ALTITUD_MIN || altitud > ALTITUD_MAX) {
            throw new IllegalArgumentException("Altitud fuera de rango permitido (1000-5000).");
        }
        this.posicionX = posicionX;
        this.altitud = altitud;
        this.energia = ENERGIA_MAX;
    }

    public void mover(String direccion) {
        if ("IZQUIERDA".equalsIgnoreCase(direccion)) {
            posicionX -= DELTA_MOVIMIENTO;
        } else if ("DERECHA".equalsIgnoreCase(direccion)) {
            posicionX += DELTA_MOVIMIENTO;
        }
    }

    public void variarAltitud(double nuevaAltitud) {
        if (nuevaAltitud < ALTITUD_MIN || nuevaAltitud > ALTITUD_MAX) {
            throw new IllegalArgumentException("Altitud fuera de rango permitido (1000-5000).");
        }
        this.altitud = nuevaAltitud;
    }

    public void recibirDanio(int porcentaje) {
        this.energia -= porcentaje;
        if (this.energia < 0) {
            this.energia = 0;
        }
    }

    public void reiniciarEnergia() {
        this.energia = ENERGIA_MAX;
    }

    public int getEnergia() { return energia; }
    public double getPosicionX() { return posicionX; }
    public double getAltitud() { return altitud; }

    public static double getAltitudMin() { return ALTITUD_MIN; }
    public static double getAltitudMax() { return ALTITUD_MAX; }
    public static int getEnergiaMax() { return ENERGIA_MAX; }
}
