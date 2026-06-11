package skydefense;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Dron extends ObjetoVolador {

    public static final int IZQUIERDA_A_DERECHA = 1;
    public static final int DERECHA_A_IZQUIERDA = -1;

    private static final double PANTALLA_MIN = 0.0;
    private static final double PANTALLA_MAX = 1000.0;
    private static final double ALTITUD_DRON = 6000.0;

    private int direccion;
    private boolean activo;
    private List<Misil> misiles;
    private Random random;

    public Dron(int direccion, double velocidad) {
        super(direccion == IZQUIERDA_A_DERECHA ? PANTALLA_MIN : PANTALLA_MAX, ALTITUD_DRON, velocidad);
        this.direccion = direccion;
        this.activo = true;
        this.misiles = new ArrayList<>();
        this.random = new Random();
    }

    @Override
    public void mover() {
        if (!activo) return;
        posicionX += velocidad * direccion;
        if (posicionX >= PANTALLA_MAX) {
            posicionX = PANTALLA_MAX;
            activo = false;
        } else if (posicionX <= PANTALLA_MIN && direccion == DERECHA_A_IZQUIERDA) {
            posicionX = PANTALLA_MIN;
            activo = false;
        }
    }

    public Misil lanzarMisil(double velocidadMisil) {
        int altitudDetonacion = Misil.getAltitudDetonacionMin()
                + random.nextInt(Misil.getAltitudDetonacionMax() - Misil.getAltitudDetonacionMin() + 1);
        Misil misil = new Misil(posicionX, ALTITUD_DRON, altitudDetonacion, velocidadMisil);
        misiles.add(misil);
        return misil;
    }

    public boolean estaActivo() { return activo; }
    public int getDireccion() { return direccion; }
    public List<Misil> getMisiles() { return misiles; }
    public static double getAltitudDron() { return ALTITUD_DRON; }
}
