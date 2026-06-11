package Test;

import org.junit.jupiter.api.Test;
import skydefense.Nivel;

import static org.junit.jupiter.api.Assertions.*;

class NivelTest {

    @Test
    void testNivelUnoTieneValoresBase() {
        Nivel nivel = new Nivel(1);

        System.out.println("--- Valores base del nivel 1 ---");
        System.out.println("  Numero:                " + nivel.getNumero());
        System.out.println("  Velocidad dron:        " + nivel.getVelocidadDron());
        System.out.println("  Velocidad misil:       " + nivel.getVelocidadMisil());
        System.out.println("  Frecuencia disparo:    " + nivel.getFrecuenciaDisparo() + " ticks entre disparos");
        System.out.println("  Multiplicador nivel 1: " + nivel.calcularMultiplicador());

        assertEquals(1, nivel.getNumero());
        assertEquals(5.0, nivel.getVelocidadDron(), 0.01);
        assertEquals(50.0, nivel.getVelocidadMisil(), 0.01);
        assertEquals(60, nivel.getFrecuenciaDisparo());
        assertEquals(1.0, nivel.calcularMultiplicador(), 0.01);
    }

    @Test
    void testNivelInvalidoLanzaExcepcion() {
        System.out.println("--- Niveles invalidos ---");

        assertThrows(IllegalArgumentException.class, () -> new Nivel(0));
        System.out.println("  Nivel 0: lanzo excepcion correctamente");

        assertThrows(IllegalArgumentException.class, () -> new Nivel(-5));
        System.out.println("  Nivel -5: lanzo excepcion correctamente");
    }

    @Test
    void testAvanzarNivelIncrementaNumero() {
        Nivel nivel = new Nivel(1);
        System.out.println("--- Avanzar nivel ---");
        System.out.println("  Numero antes: " + nivel.getNumero());

        nivel.avanzarNivel();

        System.out.println("  Numero despues: " + nivel.getNumero());
        assertEquals(2, nivel.getNumero());
    }

    @Test
    void testNivelDosTieneMayorVelocidadYMenorFrecuencia() {
        Nivel nivel1 = new Nivel(1);
        Nivel nivel2 = new Nivel(2);

        System.out.println("--- Comparacion nivel 1 vs nivel 2 ---");
        System.out.printf("  Velocidad drones:   %.2f -> %.2f (aumento)%n",
                nivel1.getVelocidadDron(), nivel2.getVelocidadDron());
        System.out.printf("  Velocidad misiles:  %.2f -> %.2f (aumento)%n",
                nivel1.getVelocidadMisil(), nivel2.getVelocidadMisil());
        System.out.printf("  Frecuencia disparo: %d -> %d ticks (disminuyo)%n",
                nivel1.getFrecuenciaDisparo(), nivel2.getFrecuenciaDisparo());

        assertTrue(nivel2.getVelocidadDron() > nivel1.getVelocidadDron());
        assertTrue(nivel2.getVelocidadMisil() > nivel1.getVelocidadMisil());
        assertTrue(nivel2.getFrecuenciaDisparo() < nivel1.getFrecuenciaDisparo());
    }

    @Test
    void testIncrementoDelQuincePorCientoPorNivel() {
        Nivel nivel1 = new Nivel(1);
        Nivel nivel2 = new Nivel(2);

        System.out.println("--- Incremento del 15% por nivel ---");
        System.out.printf("  Vel. drones  nivel 1: %.2f  |  nivel 2: %.2f  |  esperado: %.2f%n",
                nivel1.getVelocidadDron(), nivel2.getVelocidadDron(),
                nivel1.getVelocidadDron() * 1.15);

        assertEquals(nivel1.getVelocidadDron() * 1.15, nivel2.getVelocidadDron(), 0.01);
        assertEquals(nivel1.getVelocidadMisil() * 1.15, nivel2.getVelocidadMisil(), 0.01);
    }

    @Test
    void testAvanzarNivelAumentaVelocidad() {
        Nivel nivel = new Nivel(1);
        double velAntes = nivel.getVelocidadDron();

        nivel.avanzarNivel();

        double velDespues = nivel.getVelocidadDron();
        System.out.println("--- Velocidad aumenta al avanzar nivel ---");
        System.out.printf("  Velocidad dron antes: %.2f | despues: %.2f%n", velAntes, velDespues);

        assertTrue(velDespues > velAntes);
    }
}
