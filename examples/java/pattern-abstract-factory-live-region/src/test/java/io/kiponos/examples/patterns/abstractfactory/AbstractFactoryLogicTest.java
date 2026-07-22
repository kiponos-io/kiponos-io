package io.kiponos.examples.patterns.abstractfactory;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class AbstractFactoryLogicTest {
    @Test void euFamily() {
        var f = new AbstractFactoryLiveRegionApp.EuFactory();
        assertEquals("EUR", f.currency().code());
        assertEquals("EU-VAT", f.tax().label());
    }
}
