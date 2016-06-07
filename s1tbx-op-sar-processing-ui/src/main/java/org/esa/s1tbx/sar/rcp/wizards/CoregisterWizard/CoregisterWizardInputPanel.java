/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.sar.rcp.wizards.CoregisterWizard;

import org.esa.snap.graphbuilder.rcp.wizards.AbstractMultipleInputPanel;
import org.esa.snap.graphbuilder.rcp.wizards.WizardPanel;

/**
 * Input Panel
 */
public class CoregisterWizardInputPanel extends AbstractMultipleInputPanel {

    public CoregisterWizardInputPanel() {
    }

    public WizardPanel getNextPanel() {

        return new CoregisterWizardMapPanel(productSetPanel.getFileList(), productSetPanel.getTargetFolder());
    }

    protected String getInstructions() {
        return super.getInstructions() +
                "Make sure products overlap.";
    }
}
