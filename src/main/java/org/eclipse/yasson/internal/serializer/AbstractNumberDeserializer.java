/*
 * Copyright (c) 2016, 2021 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */

package org.eclipse.yasson.internal.serializer;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.Optional;

import jakarta.json.bind.JsonbException;

import org.eclipse.yasson.internal.JsonbContext;
import org.eclipse.yasson.internal.model.customization.Customization;
import org.eclipse.yasson.internal.properties.MessageKeys;
import org.eclipse.yasson.internal.properties.Messages;

/**
 * Common serializer for numbers, using number format.
 *
 * @param <T> Type to deserialize.
 */
public abstract class AbstractNumberDeserializer<T extends Number> extends AbstractValueTypeDeserializer<T> {

    /**
     * Creates a new instance.
     *
     * @param clazz         Class to work with.
     * @param customization Model customization.
     */
    public AbstractNumberDeserializer(Class<T> clazz, Customization customization) {
        super(clazz, customization);
    }

    /**
     * Returns formatted number value.
     *
     * @param jsonValue    value to be formatted
     * @param integerOnly  format only integer
     * @param jsonbContext context
     * @return formatted number value
     */
    protected final Optional<Number> deserializeFormatted(String jsonValue, boolean integerOnly, JsonbContext jsonbContext) {
        if (getCustomization() == null || getCustomization().getDeserializeNumberFormatter() == null) {
            return Optional.empty();
        }

        final JsonbNumberFormatter numberFormat = getCustomization().getDeserializeNumberFormatter();
        //consider synchronizing on format instance or per thread cache.
        Locale locale = jsonbContext.getConfigProperties().getLocale(numberFormat.getLocale());
        final NumberFormat format = NumberFormat.getInstance(locale);
        ((DecimalFormat) format).applyPattern(numberFormat.getFormat());
        format.setParseIntegerOnly(integerOnly);
        try {
            return Optional.of(format.parse(compatibilityChanger(jsonValue, locale)));
        } catch (ParseException e) {
            throw new JsonbException(Messages.getMessage(MessageKeys.PARSING_NUMBER, jsonValue, numberFormat.getFormat()));
        }
    }

    private String compatibilityChanger(String value, Locale locale) {
        char beforeJdk13GroupSeparator = '\u00A0';
        char frenchGroupingSeparator = DecimalFormatSymbols.getInstance(Locale.FRENCH).getGroupingSeparator();
        if (locale.getLanguage().equals(Locale.FRENCH.getLanguage()) && beforeJdk13GroupSeparator != frenchGroupingSeparator) {
            //JDK-8225245
            return value.replace(beforeJdk13GroupSeparator, frenchGroupingSeparator);
        }
        return value;
    }
}
