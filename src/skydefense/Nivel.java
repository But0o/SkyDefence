package skydefense;

public class Nivel {

    private static final double VELOCIDAD_BASE_DRON_DEFAULT = 5.0;
    private static final double VELOCIDAD_BASE_MISIL_DEFAULT = 50.0;
    private static final double FRECUENCIA_DISPARO_BASE_DEFAULT = 60.0;
    private static final double INCREMENTO_POR_NIVEL = 0.15;

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

    public int getNumero() { return numero; }
    public double getVelocidadBaseDron() { return velocidadBaseDron; }
    public double getVelocidadBaseMisil() { return velocidadBaseMisil; }
    public double getFrecuenciaDisparoBase() { return frecuenciaDisparoBase; }
}
