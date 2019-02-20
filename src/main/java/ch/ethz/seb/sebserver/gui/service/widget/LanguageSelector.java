/*
 * Copyright (c) 2019 ETH Zürich, Educational Development and Technology (LET)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

package ch.ethz.seb.sebserver.gui.service.widget;

import static ch.ethz.seb.sebserver.gui.service.i18n.PolyglotPageService.POLYGLOT_WIDGET_FUNCTION_KEY;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.widgets.Composite;

import ch.ethz.seb.sebserver.gbl.util.Tuple;
import ch.ethz.seb.sebserver.gui.service.i18n.I18nSupport;

public class LanguageSelector extends SingleSelection {

    private static final long serialVersionUID = -8590909580787576722L;

    private final Consumer<LanguageSelector> updateFunction;

    public LanguageSelector(final Composite parent, final I18nSupport i18nSupport) {
        super(parent, getLanguages(i18nSupport));
        this.updateFunction = updateFunction(i18nSupport);
        this.setData(POLYGLOT_WIDGET_FUNCTION_KEY, this.updateFunction);
    }

    private static final Consumer<LanguageSelector> updateFunction(final I18nSupport i18nSupport) {
        return selection -> selection.applyNewMapping(getLanguages(i18nSupport));
    }

    public static final List<Tuple<String>> getLanguages(final I18nSupport i18nSupport) {
        final Locale currentLocale = i18nSupport.getCurrentLocale();
        return i18nSupport.supportedLanguages()
                .stream()
                .map(locale -> new Tuple<>(locale.toLanguageTag(), locale.getDisplayLanguage(currentLocale)))
                .filter(tuple -> StringUtils.isNoneBlank(tuple._2))
                .sorted((t1, t2) -> t1._2.compareTo(t2._2))
                .collect(Collectors.toList());
    }

    public void clear() {
        super.clearSelection();
        this.updateFunction.accept(this);
    }

}
