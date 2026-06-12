package skydefense;

public class Nivel {

    private static final double VELOCIDAD_BASE_DRON_DEFAULT = 5.0;
    private static final double VELOCIDAD_BASE_MISIL_DEFAULT = 50.0;
    private static final double FRECUENCIA_DISPARO_BASE_DEFAULT = 60.0;
    private static final double INCREMENTO_POR_NIVEL = 0.04;

    private int numero;
    private double velocidadBaseDron;
    private double velocidadBaseMisil;
    private double frecuenciaDisparoBase;

    public Nivel(int numero) {
        if (numero < 1) throw new IllegalArgumentException("El número de nivel debe ser mayor a 0.");
        this.numero = numero;
        this.velocidadBaseDron = VELOCIDAD_BASE_DRON_DEFAULT;
        this.velocidadBaseMisil = VELOCIDAD_BASE_MISIL_DEFAULT;
        this.frecuenciaDisparoBase = FRECUENCIA_DISPARO_BASE_DEFAULT;
    }

    public void avanzarNivel() {
        numero++;
    }

    public double calcularMultiplicador() {
        return Math.pow(1 + INCREMENTO_POR_NIVEL, numero - 1);
    }

    public double getVelocidadDron() {
        return velocidadBaseDron * calcularMultiplicador();
    }

    public double getVelocidadMisil() {
        return velocidadBaseMisil * calcularMultiplicador();
    }

    public int getFrecuenciaDisparo() {
        int frecuencia = (int) (frecuenciaDisparoBase / calcularMultiplicador());
        return Math.max(1, frecuencia);
    }

    // +4 drones por cada grupo de 5 niveles (ej: niv 6 → 14, niv 11 → 18 ...)
    public int getTotalDronesNivel() {
        return 10 + 4 * ((numero - 1) / 5);
    }

    // Nivel jefe: cada múltiplo de 5 a partir de 20
    public boolean esBossLevel() {
        return numero >= 20 && numero % 5 == 0;
    }

    // +50 % de daño a partir del nivel 16
    public double getMultiplicadorDanio() {
        return numero >= 16 ? 1.50 : 1.0;
    }

    // +25 % en el radio de explosión a partir del nivel 16
    public double getMultiplicadorRadio() {
        return numero >= 16 ? 1.25 : 1.0;
    }

    public int getNumero() { return numero; }
    public double getVelocidadBaseDron() { return velocidadBaseDron; }
    public double getVelocidadBaseMisil() { return velocidadBaseMisil; }
    public double getFrecuenciaDisparoBase() { return frecuenciaDisparoBase; }
}
