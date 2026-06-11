package Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import skydefense.Avion;

import static org.junit.jupiter.api.Assertions.*;

class AvionTest {

    private Avion avion;

    @BeforeEach
    void setUp() {
        avion = new Avion(500.0, 3000.0);
    }

    @Test
    void testCreacionConValoresValidos() {
        System.out.println("--- Creacion del avion con valores validos ---");
        System.out.println("  PosicionX: " + avion.getPosicionX());
        System.out.println("  Altitud:   " + avion.getAltitud());
        System.out.println("  Energia:   " + avion.getEnergia());

        assertEquals(500.0, avion.getPosicionX(), 0.01);
        assertEquals(3000.0, avion.getAltitud(), 0.01);
        assertEquals(100, avion.getEnergia());
    }

    @Test
    void testAltitudInvalidaMenorAlMinimo() {
        System.out.println("--- Altitud invalida: menor al minimo (1000) ---");

        assertThrows(IllegalArgumentException.class, () -> new Avion(500.0, 999.0));

        System.out.println("  Altitud 999: lanzo excepcion correctamente");
    }

    @Test
    void testAltitudInvalidaMayorAlMaximo() {
        System.out.println("--- Altitud invalida: mayor al maximo (5000) ---");

        assertThrows(IllegalArgumentException.class, () -> new Avion(500.0, 5001.0));

        System.out.println("  Altitud 5001: lanzo excepcion correctamente");
    }

    @Test
    void testMoverDerecha() {
        System.out.println("--- Movimiento hacia la derecha ---");
        System.out.println("  PosicionX inicial: " + avion.getPosicionX());

        avion.mover("DERECHA");

        System.out.println("  PosicionX tras mover DERECHA: " + avion.getPosicionX());
        assertTrue(avion.getPosicionX() > 500.0);
    }

    @Test
    void testMoverIzquierda() {
        System.out.println("--- Movimiento hacia la izquierda ---");
        System.out.println("  PosicionX inicial: " + avion.getPosicionX());

        avion.mover("IZQUIERDA");

        System.out.println("  PosicionX tras mover IZQUIERDA: " + avion.getPosicionX());
        assertTrue(avion.getPosicionX() < 500.0);
    }

    @Test
    void testVariarAltitudValida() {
        System.out.println("--- Cambio de altitud valido ---");
        System.out.println("  Altitud antes: " + avion.getAltitud());

        avion.variarAltitud(4000.0);

        System.out.println("  Altitud despues: " + avion.getAltitud());

        assertEquals(4000.0, avion.getAltitud(), 0.01);
    }

    @Test
    void testVariarAltitudInvalida() {
        System.out.println("--- Altitud invalida en variarAltitud ---");

        assertThrows(IllegalArgumentException.class, () -> avion.variarAltitud(800.0));

        System.out.println("  Altitud 800: lanzo excepcion correctamente");
    }

    @Test
    void testRecibirDanio20Porciento() {
        System.out.println("--- Daño del 20% ---");
        System.out.println("  Energia antes: " + avion.getEnergia());

        avion.recibirDanio(20);

        System.out.println("  Energia despues: " + avion.getEnergia() + " (bajo 20 puntos)");

        assertEquals(80, avion.getEnergia());
    }

    @Test
    void testRecibirDanio40Porciento() {
        System.out.println("--- Daño del 40% ---");
        System.out.println("  Energia antes: " + avion.getEnergia());

        avion.recibirDanio(40);

        System.out.println("  Energia despues: " + avion.getEnergia() + " (bajo 40 puntos)");

        assertEquals(60, avion.getEnergia());
    }

    @Test
    void testEnergiaNoMenorACero() {
        System.out.println("--- Energia no puede bajar de cero ---");
        System.out.println("  Energia antes: " + avion.getEnergia());

        avion.recibirDanio(150);

        System.out.println("  Energia despues de daño de 150: " + avion.getEnergia() + " (se clampeo en 0)");

        assertEquals(0, avion.getEnergia());
    }

    @Test
    void testReiniciarEnergia() {
        System.out.println("--- Reiniciar energia ---");

        avion.recibirDanio(40);
        System.out.println("  Energia tras daño del 40: " + avion.getEnergia());

        avion.reiniciarEnergia();

        System.out.println("  Energia tras reiniciar: " + avion.getEnergia() + " (se reseteo a 100)");

        assertEquals(100, avion.getEnergia());
    }
}
