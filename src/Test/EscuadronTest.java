package Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import skydefense.Dron;
import skydefense.Escuadron;

import static org.junit.jupiter.api.Assertions.*;

class EscuadronTest {

    private Escuadron escuadron;

    @BeforeEach
    void setUp() {
        escuadron = new Escuadron();
    }

    @Test
    void testEscuadronIniciaVacio() {
        System.out.println("--- Escuadron al inicio ---");
        System.out.println("  Drones generados: " + escuadron.getDronesGenerados());
        System.out.println("  Total drones:     " + escuadron.getTotalDrones());

        assertEquals(0, escuadron.getDronesGenerados());
        assertEquals(Escuadron.TOTAL_DRONES_DEFAULT, escuadron.getTotalDrones());
    }

    @Test
    void testPuedeGenerarMasDronesCuandoHayLugar() {
        System.out.println("--- puedeGenerarMasDrones con 0 activos ---");

        boolean resultado = escuadron.puedeGenerarMasDrones(0);

        System.out.println("  Puede generar: " + resultado + " (hay lugar y quedan drones)");
        assertTrue(resultado);
    }

    @Test
    void testNoPuedeGenerarMasDronesSiAlcanzaLimiteActivos() {
        System.out.println("--- Limite de drones activos ---");

        boolean resultado = escuadron.puedeGenerarMasDrones(Escuadron.MAX_DRONES_ACTIVOS);

        System.out.println("  Puede generar con " + Escuadron.MAX_DRONES_ACTIVOS + " activos: " + resultado + " (rechazado)");
        assertFalse(resultado);
    }

    @Test
    void testRegistrarGeneracionDronIncrementaContador() {
        Dron dron = new Dron(Dron.IZQUIERDA_A_DERECHA, 5.0);

        System.out.println("--- Registrar generacion de dron ---");
        System.out.println("  Drones generados antes: " + escuadron.getDronesGenerados());

        escuadron.registrarGeneracionDron(dron);

        System.out.println("  Drones generados despues: " + escuadron.getDronesGenerados());
        assertEquals(1, escuadron.getDronesGenerados());
        assertEquals(1, escuadron.getDrones().size());
    }

    @Test
    void testEscuadronNoTerminadoAlInicio() {
        System.out.println("--- Escuadron terminado al inicio ---");
        System.out.println("  Terminado: " + escuadron.escuadronTerminado() + " (no empezo)");

        assertFalse(escuadron.escuadronTerminado());
    }

    @Test
    void testEscuadronTerminadoCuandoTodosLosDronesCompletaron() {
        Escuadron esc = new Escuadron(3);

        System.out.println("--- Escuadron completa su mision (3 drones) ---");

        for (int i = 0; i < 3; i++) {
            Dron dron = new Dron(Dron.IZQUIERDA_A_DERECHA, 1000.0);
            esc.registrarGeneracionDron(dron);
            dron.mover(); // velocidad 1000 lo lleva al borde y se desactiva
            System.out.println("  Dron " + (i + 1) + "/3 termino su recorrido");
        }

        System.out.println("  Drones generados: " + esc.getDronesGenerados());
        System.out.println("  Escuadron terminado: " + esc.escuadronTerminado());

        assertTrue(esc.escuadronTerminado());
    }

    @Test
    void testEscuadronNoTerminadoConDronesActivos() {
        Dron dron = new Dron(Dron.IZQUIERDA_A_DERECHA, 5.0);
        escuadron.registrarGeneracionDron(dron);

        System.out.println("--- Escuadron con dron activo ---");
        System.out.println("  Drones registrados: " + escuadron.getDronesGenerados());
        System.out.println("  Terminado:          " + escuadron.escuadronTerminado() + " (hay un dron activo)");

        assertFalse(escuadron.escuadronTerminado());
    }

    @Test
    void testNoPuedeGenerarMasDronesSiAgotoTotal() {
        Escuadron esc = new Escuadron(2);
        esc.registrarGeneracionDron(new Dron(Dron.IZQUIERDA_A_DERECHA, 5.0));
        esc.registrarGeneracionDron(new Dron(Dron.IZQUIERDA_A_DERECHA, 5.0));

        System.out.println("--- Escuadron agoto su total de drones ---");
        System.out.println("  Drones generados: " + esc.getDronesGenerados() + " de " + esc.getTotalDrones());

        boolean resultado = esc.puedeGenerarMasDrones(0);

        System.out.println("  Puede generar mas: " + resultado + " (ya agoto el total)");
        assertFalse(resultado);
    }
}
