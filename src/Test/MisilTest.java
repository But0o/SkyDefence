package Test;

import org.junit.jupiter.api.Test;
import skydefense.Avion;
import skydefense.Misil;

import static org.junit.jupiter.api.Assertions.*;

class MisilTest {

    @Test
    void testCreacionValida() {
        Misil misil = new Misil(500.0, 6000.0, 3000.0, 100.0);

        System.out.println("--- Creacion del misil con valores validos ---");
        System.out.println("  PosicionX:          " + misil.getPosicionX());
        System.out.println("  PosicionY (altitud actual): " + misil.getPosicionY());
        System.out.println("  Altitud detonacion: " + misil.getAltitudDetonacion());
        System.out.println("  Explotado:          " + misil.haExplotado());

        assertEquals(500.0, misil.getPosicionX(), 0.01);
        assertEquals(6000.0, misil.getPosicionY(), 0.01);
        assertEquals(3000.0, misil.getAltitudDetonacion(), 0.01);
        assertFalse(misil.haExplotado());
    }

    @Test
    void testAltitudDetonacionInvalida() {
        System.out.println("--- Altitudes de detonacion invalidas ---");

        assertThrows(IllegalArgumentException.class, () -> new Misil(500.0, 6000.0, 1000.0, 100.0));
        System.out.println("  Altitud 1000: lanzo excepcion (menor al minimo 1200)");

        assertThrows(IllegalArgumentException.class, () -> new Misil(500.0, 6000.0, 5000.0, 100.0));
        System.out.println("  Altitud 5000: lanzo excepcion (mayor al maximo 4500)");
    }

    @Test
    void testMoverDescendiendo() {
        Misil misil = new Misil(500.0, 6000.0, 2000.0, 100.0);

        System.out.println("--- Misil descendiendo ---");
        System.out.println("  PosicionY antes: " + misil.getPosicionY());

        misil.mover();

        System.out.println("  PosicionY despues: " + misil.getPosicionY() + " (bajo 100)");
        System.out.println("  Explotado:         " + misil.haExplotado());

        assertEquals(5900.0, misil.getPosicionY(), 0.01);
        assertFalse(misil.haExplotado());
    }

    @Test
    void testExplosionAlLlegarAltitudDetonacion() {
        Misil misil = new Misil(500.0, 6000.0, 2000.0, 4000.0);

        System.out.println("--- Misil explota al llegar a su altitud ---");
        System.out.println("  PosicionY inicial:  " + misil.getPosicionY());
        System.out.println("  Altitud detonacion: " + misil.getAltitudDetonacion());

        misil.mover();

        System.out.println("  PosicionY despues: " + misil.getPosicionY());
        System.out.println("  Explotado:         " + misil.haExplotado() + " (llego a la altitud objetivo)");

        assertTrue(misil.haExplotado());
        assertEquals(2000.0, misil.getPosicionY(), 0.01);
    }

    @Test
    void testNoMueveDespuesDeExplotar() {
        Misil misil = new Misil(500.0, 6000.0, 2000.0, 4000.0);
        misil.mover();

        System.out.println("--- Misil ya explotado no cambia estado ---");
        System.out.println("  PosicionY al explotar: " + misil.getPosicionY());

        misil.mover();

        System.out.println("  PosicionY tras segundo mover(): " + misil.getPosicionY() + " (no cambio)");
        System.out.println("  Explotado: " + misil.haExplotado());

        assertEquals(2000.0, misil.getPosicionY(), 0.01);
        assertTrue(misil.haExplotado());
    }

    @Test
    void testCalcularDistanciaAvionMismoLugar() {
        Avion avion = new Avion(500.0, 2000.0);
        Misil misil = new Misil(500.0, 6000.0, 2000.0, 100.0);

        System.out.println("--- Distancia: misil y avion en el mismo punto ---");
        System.out.println("  Avion  -> x: " + avion.getPosicionX() + " | altitud: " + avion.getAltitud());
        System.out.println("  Misil  -> x: " + misil.getPosicionX() + " | alt. detonacion: " + misil.getAltitudDetonacion());

        double distancia = misil.calcularDistanciaA(avion);

        System.out.println("  Distancia calculada: " + distancia);

        assertEquals(0.0, distancia, 0.01);
    }

    @Test
    void testCalcularDistanciaAvionMuyLejos() {
        Avion avion = new Avion(500.0, 5000.0);
        Misil misil = new Misil(500.0, 6000.0, 1200.0, 100.0);

        System.out.println("--- Distancia: misil y avion separados verticalmente ---");
        System.out.println("  Avion  -> x: " + avion.getPosicionX() + " | altitud: " + avion.getAltitud());
        System.out.println("  Misil  -> x: " + misil.getPosicionX() + " | alt. detonacion: " + misil.getAltitudDetonacion());

        double distancia = misil.calcularDistanciaA(avion);

        System.out.println("  Distancia calculada: " + distancia + " (diferencia vertical de 3800)");

        assertEquals(3800.0, distancia, 0.01);
    }
}
