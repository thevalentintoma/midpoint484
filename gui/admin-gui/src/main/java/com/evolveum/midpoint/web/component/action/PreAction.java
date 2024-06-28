/*
 * Copyright (c) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.component.action;

import com.evolveum.midpoint.gui.api.page.PageBase;

import com.evolveum.midpoint.prism.Containerable;

import org.apache.wicket.ajax.AjaxRequestTarget;

import java.util.List;

public interface PreAction<C extends Containerable, AGA extends AbstractGuiAction<C>> {


    public void executePreActionAndMainAction(AGA mainAction, List<C> objectsToProcess, PageBase pageBase, AjaxRequestTarget target);
}
