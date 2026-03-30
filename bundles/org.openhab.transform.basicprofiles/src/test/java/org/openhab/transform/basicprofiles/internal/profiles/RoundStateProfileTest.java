/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.transform.basicprofiles.internal.profiles;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;

import javax.measure.Unit;
import javax.measure.quantity.Temperature;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DateTimeType;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.unit.ImperialUnits;
import org.openhab.core.library.unit.SIUnits;
import org.openhab.core.thing.profiles.ProfileCallback;
import org.openhab.core.thing.profiles.ProfileContext;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.TimeSeries;
import org.openhab.core.types.TimeSeries.Entry;
import org.openhab.core.types.TimeSeries.Policy;

/**
 * Basic unit tests for {@link RoundStateProfile}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@NonNullByDefault
public class RoundStateProfileTest {

    private @Nullable Locale savedLocale;

    @BeforeEach
    public void setup() {
        savedLocale = Locale.getDefault();
        // initialize parser with ImperialUnits, otherwise units like °F are unknown
        @SuppressWarnings("unused")
        Unit<Temperature> fahrenheit = ImperialUnits.FAHRENHEIT;
    }

    @AfterEach
    public void tearDown() {
        Locale saved = savedLocale;
        if (saved != null) {
            Locale.setDefault(saved);
        }
    }

    @Test
    public void testParsingParameters() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, 4, 2, "NOT_SUPPORTED");

        assertThat(roundProfile.precision, is(4));
        assertThat(roundProfile.scale, is(2));
        assertThat(roundProfile.roundingMode, is(RoundingMode.HALF_UP));
    }

    @Test
    public void testDecimalTypeOnCommandFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, 2);

        Command cmd = new DecimalType(23.333);
        roundProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(23.33));
    }

    @Test
    public void testDecimalTypeOnCommandFromItemForPrecision() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, 1, null);

        Command cmd = new DecimalType(24.444);
        roundProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(20.0));
    }

    @Test
    public void testDecimalTypeOnCommandFromItemWithNegativeScale() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, -2);

        Command cmd = new DecimalType(1234.333);
        roundProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(1200.0));
    }

    @Test
    public void testDecimalTypeOnCommandFromItemWithCeiling() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, 0, RoundingMode.CEILING.name());

        Command cmd = new DecimalType(23.3);
        roundProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(24.0));
    }

    @Test
    public void testDecimalTypeOnCommandFromItemWithFloor() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, 0, RoundingMode.FLOOR.name());

        Command cmd = new DecimalType(23.6);
        roundProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(23.0));
    }

    @Test
    public void testQuantityTypeOnCommandFromItem() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, 1);

        Command cmd = new QuantityType<Temperature>("23.333 °C");
        roundProfile.onCommandFromItem(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).handleCommand(capture.capture());

        Command result = capture.getValue();
        @SuppressWarnings("unchecked")
        QuantityType<Temperature> qtResult = (QuantityType<Temperature>) result;
        assertThat(qtResult.doubleValue(), is(23.3));
        assertThat(qtResult.getUnit(), is(SIUnits.CELSIUS));
    }

    @Test
    public void testDecimalTypeOnStateUpdateFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, 1);

        State state = new DecimalType(23.333);
        roundProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        State result = capture.getValue();
        DecimalType dtResult = (DecimalType) result;
        assertThat(dtResult.doubleValue(), is(23.3));
    }

    @Test
    public void testQuantityTypeOnCommandFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile offsetProfile = createProfile(callback, null, 1);

        Command cmd = new QuantityType<>("23.333 °C");
        offsetProfile.onCommandFromHandler(cmd);

        ArgumentCaptor<Command> capture = ArgumentCaptor.forClass(Command.class);
        verify(callback, times(1)).sendCommand(capture.capture());

        Command result = capture.getValue();
        QuantityType<?> qtResult = (QuantityType<?>) result;
        assertThat(qtResult.doubleValue(), is(23.3));
        assertThat(qtResult.getUnit(), is(SIUnits.CELSIUS));
    }

    @Test
    public void testTimeSeriesFromHandler() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, 1);

        TimeSeries ts = new TimeSeries(Policy.ADD);
        Instant now = Instant.now();
        ts.add(now, new DecimalType(23.333));

        roundProfile.onTimeSeriesFromHandler(ts);

        ArgumentCaptor<TimeSeries> capture = ArgumentCaptor.forClass(TimeSeries.class);
        verify(callback, times(1)).sendTimeSeries(capture.capture());

        TimeSeries result = capture.getValue();
        assertEquals(ts.getStates().count(), result.getStates().count());
        Entry entry = result.getStates().findFirst().get();
        assertNotNull(entry);
        assertEquals(now, entry.timestamp());
        DecimalType dtResult = (DecimalType) entry.state();
        assertThat(dtResult.doubleValue(), is(23.3));
    }

    @Test
    public void testDateTimeTypeWeekFloor_MondayLocale() {
        // Wednesday 2024-01-17 in a Monday-start locale → Monday 2024-01-15
        Locale.setDefault(Locale.GERMANY);
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, -1);

        ZonedDateTime wednesday = ZonedDateTime.of(2024, 1, 17, 10, 30, 0, 0, ZoneId.systemDefault());
        State state = new DateTimeType(wednesday);
        roundProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        DateTimeType result = (DateTimeType) capture.getValue();
        ZonedDateTime resultZdt = result.getZonedDateTime(ZoneId.systemDefault());
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 15, 0, 0, 0, 0, ZoneId.systemDefault());
        assertThat(resultZdt, is(expected));
    }

    @Test
    public void testDateTimeTypeWeekFloor_SundayLocale() {
        // Wednesday 2024-01-17 in a Sunday-start locale → Sunday 2024-01-14
        Locale.setDefault(Locale.US);
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, -1);

        ZonedDateTime wednesday = ZonedDateTime.of(2024, 1, 17, 10, 30, 0, 0, ZoneId.systemDefault());
        State state = new DateTimeType(wednesday);
        roundProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        DateTimeType result = (DateTimeType) capture.getValue();
        ZonedDateTime resultZdt = result.getZonedDateTime(ZoneId.systemDefault());
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 14, 0, 0, 0, 0, ZoneId.systemDefault());
        assertThat(resultZdt, is(expected));
    }

    @Test
    public void testDateTimeTypeWeekCeiling_MondayLocale() {
        // Wednesday 2024-01-17 with CEILING in a Monday-start locale → Monday 2024-01-22
        Locale.setDefault(Locale.GERMANY);
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, -1, RoundingMode.CEILING.name());

        ZonedDateTime wednesday = ZonedDateTime.of(2024, 1, 17, 10, 30, 0, 0, ZoneId.systemDefault());
        State state = new DateTimeType(wednesday);
        roundProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        DateTimeType result = (DateTimeType) capture.getValue();
        ZonedDateTime resultZdt = result.getZonedDateTime(ZoneId.systemDefault());
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 22, 0, 0, 0, 0, ZoneId.systemDefault());
        assertThat(resultZdt, is(expected));
    }

    @Test
    public void testDateTimeTypeMonthFloor() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, -2);

        ZonedDateTime midMonth = ZonedDateTime.of(2024, 3, 15, 14, 30, 0, 0, ZoneId.systemDefault());
        State state = new DateTimeType(midMonth);
        roundProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        DateTimeType result = (DateTimeType) capture.getValue();
        ZonedDateTime resultZdt = result.getZonedDateTime(ZoneId.systemDefault());
        ZonedDateTime expected = ZonedDateTime.of(2024, 3, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        assertThat(resultZdt, is(expected));
    }

    @Test
    public void testDateTimeTypeMonthCeiling() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, -2, RoundingMode.CEILING.name());

        ZonedDateTime midMonth = ZonedDateTime.of(2024, 3, 15, 14, 30, 0, 0, ZoneId.systemDefault());
        State state = new DateTimeType(midMonth);
        roundProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        DateTimeType result = (DateTimeType) capture.getValue();
        ZonedDateTime resultZdt = result.getZonedDateTime(ZoneId.systemDefault());
        ZonedDateTime expected = ZonedDateTime.of(2024, 4, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        assertThat(resultZdt, is(expected));
    }

    @Test
    public void testDateTimeTypeMonthCeiling_AlreadyAtBoundary() {
        // If already at the first of the month, CEILING should not advance to next month
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, -2, RoundingMode.CEILING.name());

        ZonedDateTime firstOfMonth = ZonedDateTime.of(2024, 3, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        State state = new DateTimeType(firstOfMonth);
        roundProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        DateTimeType result = (DateTimeType) capture.getValue();
        ZonedDateTime resultZdt = result.getZonedDateTime(ZoneId.systemDefault());
        assertThat(resultZdt, is(firstOfMonth));
    }

    @Test
    public void testDateTimeTypeYearFloor() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, -3);

        ZonedDateTime midYear = ZonedDateTime.of(2024, 7, 4, 12, 0, 0, 0, ZoneId.systemDefault());
        State state = new DateTimeType(midYear);
        roundProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        DateTimeType result = (DateTimeType) capture.getValue();
        ZonedDateTime resultZdt = result.getZonedDateTime(ZoneId.systemDefault());
        ZonedDateTime expected = ZonedDateTime.of(2024, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        assertThat(resultZdt, is(expected));
    }

    @Test
    public void testDateTimeTypeYearCeiling() {
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, -3, RoundingMode.CEILING.name());

        ZonedDateTime midYear = ZonedDateTime.of(2024, 7, 4, 12, 0, 0, 0, ZoneId.systemDefault());
        State state = new DateTimeType(midYear);
        roundProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        DateTimeType result = (DateTimeType) capture.getValue();
        ZonedDateTime resultZdt = result.getZonedDateTime(ZoneId.systemDefault());
        ZonedDateTime expected = ZonedDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
        assertThat(resultZdt, is(expected));
    }

    @Test
    public void testDateTimeTypeUnsupportedScale_PassesThrough() {
        // scale=-4 is not supported for DateTimeType, should return original state
        ProfileCallback callback = mock(ProfileCallback.class);
        RoundStateProfile roundProfile = createProfile(callback, null, -4);

        ZonedDateTime dt = ZonedDateTime.of(2024, 7, 4, 12, 0, 0, 0, ZoneId.systemDefault());
        State state = new DateTimeType(dt);
        roundProfile.onStateUpdateFromHandler(state);

        ArgumentCaptor<State> capture = ArgumentCaptor.forClass(State.class);
        verify(callback, times(1)).sendUpdate(capture.capture());

        assertThat(capture.getValue(), is(state));
    }

    private RoundStateProfile createProfile(ProfileCallback callback, @Nullable Integer precision,
            @Nullable Integer scale) {
        return createProfile(callback, precision, scale, null);
    }

    private RoundStateProfile createProfile(ProfileCallback callback, @Nullable Integer precision,
            @Nullable Integer scale, @Nullable String mode) {
        ProfileContext context = mock(ProfileContext.class);
        Configuration config = new Configuration();

        config.put(RoundStateProfile.PARAM_PRECISION, precision);
        config.put(RoundStateProfile.PARAM_SCALE, scale);
        config.put(RoundStateProfile.PARAM_MODE, mode == null ? RoundingMode.HALF_UP.name() : mode);
        when(context.getConfiguration()).thenReturn(config);

        return new RoundStateProfile(callback, context);
    }
}
