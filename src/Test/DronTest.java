package Test;

import org.junit.jupiter.api.Test;
import skydefense.Dron;
import skydefense.Misil;

import static org.junit.jupiter.api.Assertions.*;

class DronTest {

    @Test
    void testCreacionIzquierdaADerechaEmpiezaEnCero() {
        Dron dron = new Dron(Dron.IZQUIERDA_A_DERECHA, 5.0);

        System.out.println("--- Creacion dron izquierda a derecha ---");
        System.out.println("  Posicion inicial: " + dron.getPosicionX() + " (borde izquierdo)");
        System.out.println("  Activo:           " + dron.estaActivo());

        assertEquals(0.0, dron.getPosicionX(), 0.01);
        assertTrue(dron.estaActivo());
    }

    @Test
    void testCreacionDerechaAIzquierdaEmpiezaEnMil() {
        Dron dron = new Dron(Dron.DERECHA_A_IZQUIERDA, 5.0);

        System.out.println("--- Creacion dron derecha a izquierda ---");
        System.out.println("  Posicion inicial: " + dron.getPosicionX() + " (borde derecho)");
        System.out.println("  Activo:           " + dron.estaActivo());

        assertEquals(1000.0, dron.getPosicionX(), 0.01);
        assertTrue(dron.estaActivo());
    }

    @Test
    void testMoverAvanzaEnDireccionCorrecta() {
        Dron dron = new Dron(Dron.IZQUIERDA_A_DERECHA, 10.0);

        System.out.println("--- Movimiento del dron ---");
        System.out.println("  Posicion antes: " + dron.getPosicionX());

        dron.mover();

        System.out.println("  Posicion despues: " + dron.getPosicionX() + " (avanzo 10 hacia la derecha)");

        assertEquals(10.0, dron.getPosicionX(), 0.01);
    }

    @Test
    void testSeDesactivaAlLlegarAlBordeDerecho() {
        Dron dron = new Dron(Dron.IZQUIERDA_A_DERECHA, 1000.0);

        System.out.println("--- Dron llega al borde derecho ---");
        System.out.println("  Activo antes: " + dron.estaActivo());

        dron.mover();

        System.out.println("  Posicion:       " + dron.getPosicionX() + " (llego al borde)");
        System.out.println("  Activo despues: " + dron.estaActivo() + " (se desactivo)");

        assertFalse(dron.estaActivo());
    }

    @Test
    void testSeDesactivaAlLlegarAlBordeIzquierdo() {
        Dron dron = new Dron(Dron.DERECHA_A_IZQUIERDA, 1000.0);

        System.out.println("--- Dron llega al borde izquierdo ---");
        System.out.println("  Activo antes: " + dron.estaActivo());

        dron.mover();

        System.out.println("  Posicion:       " + dron.getPosicionX() + " (llego al borde)");
        System.out.println("  Activo despues: " + dron.estaActivo() + " (se desactivo)");

        assertFalse(dron.estaActivo());
    }

    @Test
    void testNoSeMueveSiEstaInactivo() {
        Dron dron = new Dron(Dron.IZQUIERDA_A_DERECHA, 1000.0);
        dron.mover();
        double posicionFinal = dron.getPosicionX();

        System.out.println("--- Dron inactivo no se mueve ---");
        System.out.println("  Posicion al desactivarse: " + posicionFinal);

        dron.mover();

        System.out.println("  Posicion tras segundo mover(): " + dron.getPosicionX() + " (no cambio)");

        assertEquals(posicionFinal, dron.getPosicionX(), 0.01);
    }

    @Test
    void testLanzarMisilCreaProyectilEnPosicionDelDron() {
        Dron dron = new Dron(Dron.IZQUIERDA_A_DERECHA, 5.0);
        dron.mover();

        System.out.println("--- Dron lanza misil ---");
        System.out.println("  Posicion del dron: " + dron.getPosicionX());

        Misil misil = dron.lanzarMisil(100.0);

        System.out.println("  Posicion del misil: " + misil.getPosicionX() + " (hereda la x del dron)");

        assertNotNull(misil);
        assertEquals(5.0, misil.getPosicionX(), 0.01);
    }

    @Test
    void testMisilLanzadoSeAgregaALaLista() {
        Dron dron = new Dron(Dron.IZQUIERDA_A_DERECHA, 5.0);

        System.out.println("--- Lista de misiles del dron ---");
        System.out.println("  Misiles antes de disparar: " + dron.getMisiles().size());

        dron.lanzarMisil(100.0);

        System.out.println("  Misiles despues de disparar: " + dron.getMisiles().size());

        assertEquals(1, dron.getMisiles().size());
    }

    @Test
    void testDronTieneAltitudFija() {
        Dron dron = new Dron(Dron.IZQUIERDA_A_DERECHA, 5.0);

        System.out.println("--- Altitud del dron ---");
        System.out.println("  PosicionY: " + dron.getPosicionY());

        assertEquals(Dron.getAltitudDron(), dron.getPosicionY(), 0.01);
    }
}
