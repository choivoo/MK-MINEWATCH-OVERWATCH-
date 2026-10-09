package com.minewatch.net;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InputPayloadTest {
    @Test void dropsUnknownButtonBits() {
        InputPayload p = new InputPayload(0xFFFF, 0, 0).sanitized();
        assertEquals(InputPayload.FIRE | InputPayload.RELOAD | InputPayload.ABILITY1 | InputPayload.ABILITY2
                | InputPayload.ULT | InputPayload.MELEE, p.buttons());
    }

    @Test void clampsMovementAndRejectsNaN() {
        InputPayload p = new InputPayload(0, 99f, -99f).sanitized();
        assertEquals(1f, p.forward());
        assertEquals(-1f, p.sideways());
        InputPayload n = new InputPayload(0, Float.NaN, Float.POSITIVE_INFINITY).sanitized();
        assertEquals(0f, n.forward());
        assertEquals(1f, n.sideways());
    }

    @Test void validInputPassesThrough() {
        InputPayload p = new InputPayload(InputPayload.FIRE, 0.5f, -0.25f).sanitized();
        assertEquals(InputPayload.FIRE, p.buttons());
        assertEquals(0.5f, p.forward());
        assertEquals(-0.25f, p.sideways());
    }
}
