package io.kiponos.examples.patterns.adapter;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AdapterLogicTest {
    @Test void stripeReceipt() {
        assertTrue(new AdapterLivePspApp.StripeAdapter().charge(100,"USD").startsWith("stripe_"));
    }
}
